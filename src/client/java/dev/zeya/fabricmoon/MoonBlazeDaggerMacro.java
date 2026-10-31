package dev.zeya.fabricmoon;

import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

public final class MoonBlazeDaggerMacro {
   private static final Random RANDOM = new Random();
   private static final String[] TWILIGHT_IDS = new String[]{"HEARTMAW_DAGGER", "BURSTMAW_DAGGER", "MAWDUST_DAGGER"};
   private static final String[] FIREDUST_IDS = new String[]{"HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER"};
   private static String activeShield;
   private static boolean waitingForShieldUpdate;
   private static long lastModeClickMillis;
   private static final int MIN_SLOT_SWITCH_DELAY = 180;
   private static final int MAX_SLOT_SWITCH_DELAY = 350;
   private static long lastSlotSwitchMillis = 0L;

   private MoonBlazeDaggerMacro() {
   }

   public static void onMobHit(Player player, Entity target) {
      Minecraft client = Minecraft.getInstance();
      if (MoonNetworkPolicy.allowsModServerActions()
         && MoonConfig.featuresEnabled
         && MoonConfig.blazeDaggerAutoSwitch
         && player != null
         && target != null
         && client.level != null
         && client.gui.screen() == null) {
         String shield = findShield(client, target);
         if (shield != null) {
            String[] daggerIds = switch (shield) {
               case "spirit", "crystal" -> TWILIGHT_IDS;
               case "ashen", "auric" -> FIREDUST_IDS;
               default -> null;
            };
            if (daggerIds != null) {
               long now = System.currentTimeMillis();
               if (now - lastSlotSwitchMillis >= 180 + RANDOM.nextInt(170)) {
                  int slot = findDaggerSlot(player, daggerIds);
                  if (slot >= 0 && client.gameMode != null) {
                     lastSlotSwitchMillis = now;
                     player.getInventory().setSelectedSlot(slot);
                     int minDelay = 200 + RANDOM.nextInt(200);
                     if (activeShield != null && !activeShield.equals(shield) && !waitingForShieldUpdate && now - lastModeClickMillis >= minDelay) {
                        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                        lastModeClickMillis = now;
                        waitingForShieldUpdate = true;
                     }
                  }
               }
            }
         }
      }
   }

   public static void onSubtitle(String text) {
      String normalized = normalize(text);

      for (String shield : new String[]{"spirit", "crystal", "ashen", "auric"}) {
         if (normalized.contains(shield)) {
            activeShield = shield;
            waitingForShieldUpdate = false;
            return;
         }
      }
   }

   private static String findShield(Minecraft client, Entity target) {
      AABB area = target.getBoundingBox().inflate(5.0);

      for (Entity entity : client.level.getEntities(target, area)) {
         if (entity instanceof ArmorStand && !(entity.distanceToSqr(target) > 25.0)) {
            Component nameComponent = entity.getName();
            String name = nameComponent != null ? normalize(nameComponent.getString()) : "";

            for (String shield : new String[]{"spirit", "crystal", "ashen", "auric"}) {
               if (name.contains(shield)) {
                  return shield;
               }
            }
         }
      }

      return null;
   }

   private static int findDaggerSlot(Player player, String[] ids) {
      Minecraft client = Minecraft.getInstance();
      int configuredSlot = MoonConfig.clampLoadoutSlot(MoonConfig.blazeDaggerSlot) - 1;
      if (hasAnyId(player.getInventory().getItem(configuredSlot), ids)) {
         return configuredSlot;
      }

      for (int slot = 0; slot < 36; slot++) {
         if (hasAnyId(player.getInventory().getItem(slot), ids)) {
            if (slot >= 9) {
               for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
                  if (player.getInventory().getItem(hotbarSlot).isEmpty()) {
                     int menuSlotId = findMenuSlotId(client.player.containerMenu, player.getInventory(), slot);
                     if (menuSlotId >= 0 && client.gameMode != null) {
                        client.gameMode.handleContainerInput(client.player.containerMenu.containerId, menuSlotId, hotbarSlot, ContainerInput.SWAP, player);
                        return hotbarSlot;
                     }
                  }
               }

               return -1;
            }

            return slot;
         }
      }

      return -1;
   }

   private static int findMenuSlotId(AbstractContainerMenu menu, Inventory inventory, int inventoryIndex) {
      for (int menuIndex = 0; menuIndex < menu.slots.size(); menuIndex++) {
         Slot slot = (Slot)menu.slots.get(menuIndex);
         if (slot.container == inventory && slot.getContainerSlot() == inventoryIndex) {
            return menuIndex;
         }
      }

      return -1;
   }

   private static boolean hasAnyId(ItemStack stack, String[] ids) {
      String id = skyblockId(stack);

      for (String candidate : ids) {
         if (id.equals(candidate)) {
            return true;
         }
      }

      return false;
   }

   private static String skyblockId(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? "" : data.copyTag().getStringOr("id", "");
   }

   private static String normalize(String text) {
      return text.replaceAll("\\u00A7.", "").toLowerCase(Locale.ROOT).trim();
   }
}
