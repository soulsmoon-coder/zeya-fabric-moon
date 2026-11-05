package dev.zeya.fabricmoon.macros;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.network.MoonNetworkPolicy;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

public final class MoonBlazeDaggerMacro {
   private static final String[] TWILIGHT_IDS = new String[]{"HEARTMAW_DAGGER", "BURSTMAW_DAGGER", "MAWDUST_DAGGER"};
   private static final String[] FIREDUST_IDS = new String[]{"HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER"};
   private static String activeTwilightShield = "spirit";
   private static String activeFiredustShield = "ashen";
   private static boolean waitingForShieldUpdate;
   private static long lastActionMillis;

   private MoonBlazeDaggerMacro() {
   }

   public static void onMobHit(Player player, Entity target) {
      Minecraft client = Minecraft.getInstance();
      if (MoonNetworkPolicy.allowsModServerActions()
         && MoonConfig.featuresEnabled
         && MoonConfig.blazeDaggerAutoSwitch
         && player == client.player
         && target != null
         && client.level != null
         && client.gameMode != null
         && client.gui.screen() == null) {
         String shield = findShield(client, target);
         if (shield == null) {
            return;
         }

         String[] daggerIds = switch (shield) {
            case "spirit", "crystal" -> TWILIGHT_IDS;
            case "ashen", "auric" -> FIREDUST_IDS;
            default -> null;
         };
         if (daggerIds == null) {
            return;
         }

         int daggerSlot = findDaggerSlot(client.player, daggerIds);
         if (daggerSlot < 0) {
            return;
         }

         long now = System.currentTimeMillis();
         if (now - lastActionMillis < MoonConfig.clampSwapDelay(MoonConfig.blazeDaggerSwapDelayMs)) {
            return;
         }

         if (client.player.getInventory().getSelectedSlot() != daggerSlot) {
            client.player.getInventory().setSelectedSlot(daggerSlot);
            lastActionMillis = now;
            return;
         }

         String activeShield = daggerIds == TWILIGHT_IDS ? activeTwilightShield : activeFiredustShield;
         if (!shield.equals(activeShield) && !waitingForShieldUpdate) {
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            lastActionMillis = now;
            waitingForShieldUpdate = true;
            setActiveShield(shield);
         }
      }
   }

   public static void onSubtitle(String text) {
      String normalized = normalize(text);

      for (String shield : new String[]{"spirit", "crystal", "ashen", "auric"}) {
         if (normalized.contains(shield)) {
            setActiveShield(shield);
            waitingForShieldUpdate = false;
            return;
         }
      }
   }

   private static void setActiveShield(String shield) {
      if ("spirit".equals(shield) || "crystal".equals(shield)) {
         activeTwilightShield = shield;
      } else if ("ashen".equals(shield) || "auric".equals(shield)) {
         activeFiredustShield = shield;
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
