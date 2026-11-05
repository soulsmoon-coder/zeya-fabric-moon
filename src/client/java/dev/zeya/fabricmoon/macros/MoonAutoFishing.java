package dev.zeya.fabricmoon.macros;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.network.MoonNetworkPolicy;
import dev.zeya.fabricmoon.mixin.FishingHookAccessor;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MoonAutoFishing {
   private static final int CAST_HOOK_TIMEOUT_TICKS = 60;
   private static final double INDICATOR_SEARCH_RADIUS = 2.0;
   private static final double GOLDEN_FISH_SEARCH_RADIUS = 5.0;
   private static final float AIM_TOLERANCE_DEGREES = 1.2F;
   private static final float AIM_SMOOTHING = 0.14F;
   private static final float AIM_MAX_STEP_DEGREES = 1.5F;
   private static final Pattern TIMER_NAME = Pattern.compile("\\d+(\\.\\d+)?");
   private static int reactionDelayTicks;
   private static int castCooldownTicks;
   private static int castHookWaitTicks;
   private static int hookAgeTicks;
   private static int goldenFishActionCooldownTicks;
   private static ArmorStand trackedArmorStand;
   private static ArmorStand goldenFishTarget;
   private static FishingHook observedHook;
   private static boolean awaitingHookRemoval;
   private static boolean awaitingHookSpawn;
   private static boolean bitePending;
   private static boolean fishingSessionActive;
   private static boolean goldenFishActive;
   private static boolean goldenFishActionPending;
   private static long goldenFishLastCueMillis;

   private MoonAutoFishing() {
   }

   public static void tick(Minecraft client) {
      LocalPlayer player = client.player;
      ClientLevel level = client.level;
      if (player != null && level != null) {
         FishingHook hook = player.fishing;
         InteractionHand rodHand = getRodHand(player);
         if (!MoonNetworkPolicy.allowsModServerActions() || !MoonConfig.featuresEnabled || !MoonConfig.autoFishing) {
            pauseUntilNextManualCast(hook);
         } else if (rodHand == null) {
            pauseUntilNextManualCast(hook);
         } else {
            if (hook != null && hook != observedHook) {
               fishingSessionActive = true;
               awaitingHookSpawn = false;
               castHookWaitTicks = 0;
               resetFishingState();
               resetGoldenFishState();
            }

            observedHook = hook;
            if (fishingSessionActive && (!MoonConfig.disableFishingWhileOnScreen || client.gui.screen() == null)) {
               if (MoonConfig.autoGoldenFish) {
                  if (handleGoldenFish(client)) {
                     return;
                  }
               } else {
                  resetGoldenFishState();
               }

               MultiPlayerGameMode gameMode = client.gameMode;
               if (hook != null && !hook.isRemoved()) {
                  awaitingHookSpawn = false;
                  castHookWaitTicks = 0;
                  if (gameMode != null) {
                     if (!awaitingHookRemoval) {
                        hookAgeTicks++;
                        if (trackedArmorStand != null && trackedArmorStand.isRemoved()) {
                           trackedArmorStand = null;
                        }

                        boolean serverBiteDetected = updateFishingIndicators(level, hook);
                        int minimumWaitTicks = MoonConfig.minFishWaitSeconds * 20;
                        boolean biteDetected = hookAgeTicks >= minimumWaitTicks
                           && (((FishingHookAccessor)hook).zeyafabricmoon$isBiting()
                              || serverBiteDetected
                              || isBiteIndicator(trackedArmorStand));
                        if (biteDetected && !bitePending) {
                           bitePending = true;
                           reactionDelayTicks = MoonConfig.fishingReelDelayTicks;
                           if (reactionDelayTicks > 0) {
                              return;
                           }
                        }

                        if (bitePending && rodHand != null) {
                           if (reactionDelayTicks > 0) {
                              reactionDelayTicks--;
                           }

                           if (reactionDelayTicks <= 0) {
                              useRod(gameMode, player, rodHand);
                              awaitingHookRemoval = true;
                              castCooldownTicks = nextCastCooldownTicks();
                              resetFishingState();
                           }
                        }
                     }
                  }
               } else {
                  awaitingHookRemoval = false;
                  resetFishingState();
                  if (awaitingHookSpawn) {
                     castHookWaitTicks++;
                     if (castHookWaitTicks >= CAST_HOOK_TIMEOUT_TICKS) {
                        awaitingHookSpawn = false;
                        castHookWaitTicks = 0;
                        castCooldownTicks = nextCastCooldownTicks();
                     }
                  } else if (castCooldownTicks > 0) {
                     castCooldownTicks--;
                  } else if (gameMode != null) {
                     useRod(gameMode, player, rodHand);
                     awaitingHookSpawn = true;
                     castHookWaitTicks = 0;
                  }
               }
            }
         }
      } else {
         reset();
      }
   }

   public static void onEntityLoad(Entity entity) {
      if (MoonConfig.featuresEnabled && MoonConfig.autoFishing && entity instanceof ArmorStand armorStand) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            FishingHook hook = player.fishing;
            if (hook != null && armorStand.distanceToSqr(hook) <= 4.0 && (trackedArmorStand == null || isBiteIndicator(armorStand))) {
               trackedArmorStand = armorStand;
            }

            if (goldenFishActive && isPotentialGoldenFish(armorStand) && armorStand.distanceToSqr(hook != null ? hook : player) <= 25.0) {
               goldenFishTarget = armorStand;
            }
         }
      }
   }

   public static void onManualRodClick() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (!MoonConfig.featuresEnabled || !MoonConfig.autoFishing || !MoonNetworkPolicy.allowsModServerActions()
         || player == null || getRodHand(player) == null) {
         return;
      }

      FishingHook hook = player.fishing;
      if (hook != null && !hook.isRemoved()) {
         pauseUntilNextManualCast(hook);
      } else {
         fishingSessionActive = true;
         observedHook = null;
         awaitingHookSpawn = true;
         castHookWaitTicks = 0;
         castCooldownTicks = 0;
         resetFishingState();
         resetGoldenFishState();
      }
   }

   public static void onChatMessage(String text) {
      if (MoonConfig.featuresEnabled && MoonConfig.autoFishing && MoonConfig.autoGoldenFish && text != null) {
         String message = text.replaceAll("\\u00A7.", "").trim();
         if (message.contains("You spot a Golden Fish surface from beneath the lava!")) {
            goldenFishActive = true;
            goldenFishActionPending = true;
            goldenFishLastCueMillis = System.currentTimeMillis();
            goldenFishTarget = null;
         } else if (goldenFishActive
            && (message.contains("The Golden Fish escapes your hook but looks weakened.") || message.contains("The Golden Fish is weak!"))) {
            goldenFishActionPending = true;
            goldenFishLastCueMillis = System.currentTimeMillis();
            goldenFishTarget = null;
         } else if (message.contains("The Golden Fish swims back beneath the lava...") || message.contains("TROPHY FISH! Golden Fish")) {
            resetGoldenFishState();
         }
      } else {
         resetGoldenFishState();
      }
   }

   private static boolean handleGoldenFish(Minecraft client) {
      if (!goldenFishActive) {
         return false;
      }

      if (System.currentTimeMillis() - goldenFishLastCueMillis > 70000L) {
         resetGoldenFishState();
         return false;
      }

      if (!goldenFishActionPending) {
         return true;
      }

      ArmorStand target = findGoldenFishTarget(client);
      if (target == null || !aimAtGoldenFish(client.player, target)) {
         return true;
      }

      if (goldenFishActionCooldownTicks > 0) {
         goldenFishActionCooldownTicks--;
         return true;
      }

      LocalPlayer player = client.player;
      MultiPlayerGameMode gameMode = client.gameMode;
      InteractionHand rodHand = getRodHand(player);
      if (gameMode != null && rodHand != null) {
         useRod(gameMode, player, rodHand);
         goldenFishActionPending = false;
         goldenFishActionCooldownTicks = MoonConfig.clampDelay(MoonConfig.goldenFishActionDelayTicks);
      }

      return true;
   }

   private static ArmorStand findGoldenFishTarget(Minecraft client) {
      if (goldenFishTarget != null
         && !goldenFishTarget.isRemoved()
         && isPotentialGoldenFish(goldenFishTarget)
         && goldenFishTarget.distanceToSqr(client.player.fishing != null ? client.player.fishing : client.player)
            <= GOLDEN_FISH_SEARCH_RADIUS * GOLDEN_FISH_SEARCH_RADIUS) {
         return goldenFishTarget;
      }

      goldenFishTarget = null;
      if (client.level != null && client.player != null) {
         FishingHook hook = client.player.fishing;
         Entity center = hook != null ? hook : client.player;
         AABB searchArea = center.getBoundingBox().inflate(GOLDEN_FISH_SEARCH_RADIUS);
         ArmorStand nearest = null;
         double nearestDistance = Double.MAX_VALUE;

         for (Entity entity : client.level.getEntities(center, searchArea)) {
            if (entity instanceof ArmorStand armorStand && isPotentialGoldenFish(armorStand)) {
               double distance = armorStand.distanceToSqr(center);
               if (distance < nearestDistance) {
                  nearest = armorStand;
                  nearestDistance = distance;
               }
            }
         }

         goldenFishTarget = nearest;
         return nearest;
      }

      return null;
   }

   private static boolean isPotentialGoldenFish(ArmorStand armorStand) {
      if (armorStand.isRemoved() || !armorStand.isAlive()) {
         return false;
      }

      Component customName = armorStand.getCustomName();
      if (customName != null) {
         String name = customName.getString().replaceAll("\\u00A7.", "").trim().toLowerCase(Locale.ROOT);
         if (name.contains("golden fish")) {
            return true;
         }

         if (!name.isEmpty() && !name.equals("!!!") && !TIMER_NAME.matcher(name).matches()) {
            return false;
         }
      }

      return armorStand.getItemBySlot(EquipmentSlot.HEAD).is(Items.PLAYER_HEAD);
   }

   private static boolean aimAtGoldenFish(LocalPlayer player, ArmorStand target) {
      Vec3 eyePosition = player.getEyePosition();
      Vec3 targetPosition = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 difference = targetPosition.subtract(eyePosition);
      double horizontalDistance = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
      float targetYaw = (float)(Math.toDegrees(Math.atan2(difference.z, difference.x)) - 90.0);
      float targetPitch = (float)(-Math.toDegrees(Math.atan2(difference.y, horizontalDistance)));
      float yawDifference = Mth.wrapDegrees(targetYaw - player.getYRot());
      float pitchDifference = targetPitch - player.getXRot();
      if (Math.abs(yawDifference) <= AIM_TOLERANCE_DEGREES && Math.abs(pitchDifference) <= AIM_TOLERANCE_DEGREES) {
         return true;
      }

      float yawStep = Mth.clamp(yawDifference * AIM_SMOOTHING, -AIM_MAX_STEP_DEGREES, AIM_MAX_STEP_DEGREES);
      float pitchStep = Mth.clamp(pitchDifference * AIM_SMOOTHING, -AIM_MAX_STEP_DEGREES, AIM_MAX_STEP_DEGREES);
      player.setYRot(player.getYRot() + yawStep);
      player.setXRot(Mth.clamp(player.getXRot() + pitchStep, -90.0F, 90.0F));
      return false;
   }

   private static void resetGoldenFishState() {
      goldenFishActive = false;
      goldenFishActionPending = false;
      goldenFishLastCueMillis = 0L;
      goldenFishActionCooldownTicks = 0;
      goldenFishTarget = null;
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

      for (int index = 0; index < name.length(); index++) {
         if (name.charAt(index) != '!') {
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
      }

      return player.getOffhandItem().getItem() instanceof FishingRodItem ? InteractionHand.OFF_HAND : null;
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
      castHookWaitTicks = 0;
      awaitingHookRemoval = false;
      awaitingHookSpawn = false;
      observedHook = null;
      fishingSessionActive = false;
      resetFishingState();
      resetGoldenFishState();
   }

   private static void pauseUntilNextManualCast(FishingHook hook) {
      fishingSessionActive = false;
      observedHook = hook;
      castCooldownTicks = 0;
      castHookWaitTicks = 0;
      awaitingHookRemoval = false;
      awaitingHookSpawn = false;
      resetFishingState();
      resetGoldenFishState();
   }

   private static int nextCastCooldownTicks() {
      return MoonConfig.clampDelay(MoonConfig.fishingCastDelayTicks);
   }
}
