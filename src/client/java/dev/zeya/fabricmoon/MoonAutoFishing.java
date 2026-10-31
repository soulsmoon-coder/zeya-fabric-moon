package dev.zeya.fabricmoon;

import dev.zeya.fabricmoon.mixin.FishingHookAccessor;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class MoonAutoFishing {
   private static final Random RANDOM = new Random();
   private static final int MIN_REACTION_DELAY_TICKS = 1;
   private static final int MAX_REACTION_DELAY_TICKS = 7;
   private static final int REEL_TIMEOUT_TICKS = 60;
   private static final double INDICATOR_SEARCH_RADIUS = 2.0;
   private static int reactionDelayTicks;
   private static int castCooldownTicks;
   private static int hookAgeTicks;
   private static int awaitingRemovalTicks;
   private static ArmorStand trackedArmorStand;
   private static boolean awaitingHookRemoval;
   private static boolean bitePending;

   private MoonAutoFishing() {
   }

   public static void tick(Minecraft client) {
      LocalPlayer player = client.player;
      ClientLevel level = client.level;
      if (!MoonNetworkPolicy.allowsModServerActions() || !MoonConfig.featuresEnabled || !MoonConfig.autoFishing || player == null || level == null) {
         reset();
         return;
      }

      if (MoonConfig.disableFishingWhileOnScreen && client.gui.screen() != null) {
         return;
      }

      FishingHook hook = player.fishing;
      InteractionHand rodHand = getRodHand(player);
      MultiPlayerGameMode gameMode = client.gameMode;
      if (hook == null || hook.isRemoved()) {
         awaitingHookRemoval = false;
         awaitingRemovalTicks = 0;
         resetFishingState();
         if (castCooldownTicks > 0) {
            castCooldownTicks--;
            return;
         }

         if (rodHand != null && gameMode != null) {
            useRod(gameMode, player, rodHand);
            castCooldownTicks = nextCastCooldownTicks();
         }

         return;
      }

      // O anzol ja existe: o cooldown de lancamento nao e mais necessario.
      castCooldownTicks = 0;
      if (gameMode == null) {
         return;
      }

      if (awaitingHookRemoval) {
         awaitingRemovalTicks++;
         if (awaitingRemovalTicks >= REEL_TIMEOUT_TICKS) {
            // O servidor nao removeu o anzol (lag ou pacote perdido). Tenta recolher novamente.
            awaitingHookRemoval = false;
            awaitingRemovalTicks = 0;
            bitePending = true;
            reactionDelayTicks = 1;
         }

         return;
      }

      hookAgeTicks++;
      if (trackedArmorStand != null && trackedArmorStand.isRemoved()) {
         trackedArmorStand = null;
      }

      boolean serverBiteDetected = updateFishingIndicators(level, hook);
      int minimumWaitTicks = Math.max(MoonConfig.minFishWaitTicks, MoonConfig.fishingReelDelaySeconds * 20);
      boolean biteDetected = hookAgeTicks >= minimumWaitTicks
         && (((FishingHookAccessor)hook).zeyafabricmoon$isBiting() || serverBiteDetected || isBiteIndicator(trackedArmorStand));
      if (biteDetected && !bitePending) {
         bitePending = true;
         reactionDelayTicks = randomReactionDelayTicks();
         return;
      }

      if (!bitePending || rodHand == null) {
         return;
      }

      reactionDelayTicks--;
      if (reactionDelayTicks > 0) {
         return;
      }

      useRod(gameMode, player, rodHand);
      awaitingHookRemoval = true;
      awaitingRemovalTicks = 0;
      castCooldownTicks = nextCastCooldownTicks();
      resetFishingState();
   }

   public static void onEntityLoad(Entity entity) {
      if (!MoonConfig.featuresEnabled || !MoonConfig.autoFishing || !(entity instanceof ArmorStand armorStand)) {
         return;
      }

      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return;
      }

      FishingHook hook = player.fishing;
      if (hook != null && armorStand.distanceToSqr(hook) <= 4.0 && (trackedArmorStand == null || isBiteIndicator(armorStand))) {
         trackedArmorStand = armorStand;
      }
   }

   private static boolean isBiteIndicator(ArmorStand armorStand) {
      if (armorStand == null || armorStand.isRemoved()) {
         return false;
      }

      Component customName = armorStand.getCustomName();
      if (customName == null) {
         return false;
      }

      String name = customName.getString().trim();
      if (name.isEmpty()) {
         return false;
      }

      for (int i = 0; i < name.length(); i++) {
         if (name.charAt(i) != '!') {
            return false;
         }
      }

      return true;
   }

   private static boolean updateFishingIndicators(ClientLevel level, FishingHook hook) {
      AABB searchArea = hook.getBoundingBox().inflate(INDICATOR_SEARCH_RADIUS);
      ArmorStand nearestArmorStand = null;
      double nearestDistance = Double.MAX_VALUE;

      for (Entity entity : level.getEntities(hook, searchArea)) {
         if (entity instanceof ArmorStand armorStand) {
            if (isBiteIndicator(armorStand)) {
               trackedArmorStand = armorStand;
               return true;
            }

            double distance = armorStand.distanceToSqr(hook);
            if (distance < nearestDistance) {
               nearestArmorStand = armorStand;
               nearestDistance = distance;
            }
         }
      }

      if (trackedArmorStand == null) {
         trackedArmorStand = nearestArmorStand;
      }

      return false;
   }

   private static InteractionHand getRodHand(LocalPlayer player) {
      ItemStack mainHand = player.getMainHandItem();
      if (mainHand.getItem() instanceof FishingRodItem) {
         return InteractionHand.MAIN_HAND;
      } else {
         return player.getOffhandItem().getItem() instanceof FishingRodItem ? InteractionHand.OFF_HAND : null;
      }
   }

   private static void useRod(MultiPlayerGameMode gameMode, LocalPlayer player, InteractionHand hand) {
      gameMode.useItem(Objects.requireNonNull(player), Objects.requireNonNull(hand));
   }

   private static void resetFishingState() {
      hookAgeTicks = 0;
      reactionDelayTicks = 0;
      trackedArmorStand = null;
      bitePending = false;
   }

   private static void reset() {
      castCooldownTicks = 0;
      awaitingRemovalTicks = 0;
      awaitingHookRemoval = false;
      resetFishingState();
   }

   private static int randomReactionDelayTicks() {
      return MIN_REACTION_DELAY_TICKS + RANDOM.nextInt(MAX_REACTION_DELAY_TICKS - MIN_REACTION_DELAY_TICKS + 1);
   }

   private static int nextCastCooldownTicks() {
      int base = MoonConfig.clampDelay(MoonConfig.fishingCastDelayTicks);
      if (base <= 0) {
         return 1;
      }

      int jitterRange = Math.max(1, base / 4);
      return Math.max(1, base + RANDOM.nextInt(jitterRange * 2 + 1) - jitterRange);
   }
}
