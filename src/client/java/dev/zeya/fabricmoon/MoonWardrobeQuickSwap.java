package dev.zeya.fabricmoon;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.InputConstants.Key;
import dev.zeya.fabricmoon.mixin.KeyMappingAccessor;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

public final class MoonWardrobeQuickSwap {
   private static final int[] WARDROBE_SWAP_SLOTS = new int[]{36, 37, 38, 39, 40, 41, 42, 43, 44};
   private static final int[] EQUIPMENT_SWAP_SLOTS = new int[]{36, 37, 38, 39, 40, 41, 42, 43, 44};
   private static final int[] LOADOUT_MENU_SLOTS = new int[]{14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
   private static final long SWAP_COOLDOWN_MS = 300L;
   private static long lastSwapNanos = 0L;
   private static final int CLOSE_MENU_DELAY_TICKS = 10;
   private static int closeMenuTicks = -1;
   private static final boolean[] quickSwapWardrobeWasDown = new boolean[9];
   private static final boolean[] quickSwapEquipmentWasDown = new boolean[9];
   private static final boolean[] quickSwapLoadoutWasDown = new boolean[12];

   private MoonWardrobeQuickSwap() {
   }

   public static boolean onKeyPressed(Minecraft client, int keyCode) {
      if (!MoonConfig.featuresEnabled || !MoonConfig.wardrobeEnabled) {
         return false;
      }

      if (client.player == null || client.gameMode == null) {
         return false;
      }

      if (!MoonNetworkPolicy.allowsModServerActions()) {
         return false;
      }

      long now = System.nanoTime();
      if ((now - lastSwapNanos) / 1000000L < 300L) {
         return false;
      }

      if (client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
         Component title = screen.getTitle();
         if (title == null) {
            return false;
         }

         String rawTitle = title.getString();
         String cleanTitle = rawTitle.replaceAll("§.", "").trim();
         String titleStr = cleanTitle.toLowerCase(Locale.ROOT);
         boolean isArmorSets = titleStr.contains("armor sets") || titleStr.contains("armour sets") || titleStr.matches(".*\\(\\d+/\\d+\\).*sets.*");
         boolean isWardrobe = titleStr.contains("wardrobe") || titleStr.contains("armario") || titleStr.contains("closet");
         boolean isEquipment = titleStr.contains("equipment") || titleStr.contains("equipamento");
         boolean isEquipmentSets = titleStr.contains("equipment sets") || titleStr.contains("equipment set");
         boolean isLoadout = titleStr.contains("loadout") || titleStr.contains("load-out");
         if (!isArmorSets && !isWardrobe && !isEquipment && !isLoadout) {
            return false;
         }

         boolean isWardrobeMenu = isArmorSets || isWardrobe;
         boolean isEquipmentMenu = isEquipment || isEquipmentSets;
         int[] menuSlots;
         int[] keyArray;
         if (isWardrobeMenu) {
            menuSlots = WARDROBE_SWAP_SLOTS;
            keyArray = MoonConfig.wardrobeQuickSwapKeysWardrobe;
         } else if (isEquipmentMenu) {
            menuSlots = EQUIPMENT_SWAP_SLOTS;
            keyArray = MoonConfig.wardrobeQuickSwapKeysEquipment;
         } else {
            menuSlots = LOADOUT_MENU_SLOTS;
            keyArray = MoonConfig.wardrobeQuickSwapKeysLoadout;
         }

         if (MoonConfig.wardrobeQuickSwapEnabled) {
            int slotIndex = -1;

            for (int i = 0; i < keyArray.length; i++) {
               if (keyArray[i] == keyCode) {
                  slotIndex = i;
                  break;
               }
            }

            if (slotIndex >= 0) {
               executeSwap(client, screen, slotIndex + 1, menuSlots[slotIndex]);
               return true;
            }
         }

         if (MoonConfig.wardrobeUseHotbarBinds) {
            int hotbarSlot = findHotbarSlotByKey(client, keyCode);
            if (hotbarSlot >= 0 && hotbarSlot < 9) {
               int loadoutSlot = hotbarSlot + 1;
               int menuSlot = menuSlots[hotbarSlot];
               executeSwap(client, screen, loadoutSlot, menuSlot);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static void checkKeybinds(Minecraft client) {
      boolean[] resetWardrobe = quickSwapWardrobeWasDown;
      boolean[] resetEquipment = quickSwapEquipmentWasDown;
      if (!MoonConfig.featuresEnabled || !MoonConfig.wardrobeEnabled) {
         Arrays.fill(resetWardrobe, false);
         Arrays.fill(resetEquipment, false);
      } else if (client.player == null || client.gameMode == null) {
         Arrays.fill(resetWardrobe, false);
         Arrays.fill(resetEquipment, false);
      } else if (!MoonNetworkPolicy.allowsModServerActions()) {
         Arrays.fill(resetWardrobe, false);
         Arrays.fill(resetEquipment, false);
      } else if (client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
         Component title = screen.getTitle();
         if (title == null) {
            Arrays.fill(resetWardrobe, false);
            Arrays.fill(resetEquipment, false);
         } else {
            String rawTitle = title.getString();
            String cleanTitle = rawTitle.replaceAll("§.", "").trim();
            String titleStr = cleanTitle.toLowerCase(Locale.ROOT);
            boolean isArmorSets = titleStr.contains("armor sets") || titleStr.contains("armour sets") || titleStr.matches(".*\\(\\d+/\\d+\\).*sets.*");
            boolean isWardrobe = titleStr.contains("wardrobe") || titleStr.contains("armario") || titleStr.contains("closet");
            boolean isEquipment = titleStr.contains("equipment") || titleStr.contains("equipamento");
            boolean isEquipmentSets = titleStr.contains("equipment sets") || titleStr.contains("equipment set");
            boolean isLoadout = titleStr.contains("loadout") || titleStr.contains("load-out");
            if (!isArmorSets && !isWardrobe && !isEquipment && !isLoadout) {
               Arrays.fill(resetWardrobe, false);
               Arrays.fill(resetEquipment, false);
            } else {
               boolean isWardrobeMenu = isArmorSets || isWardrobe;
               boolean isEquipmentMenu = isEquipment || isEquipmentSets;
               int[] menuSlots;
               int[] keyArray;
               boolean[] wasDownArray;
               if (isWardrobeMenu) {
                  menuSlots = WARDROBE_SWAP_SLOTS;
                  keyArray = MoonConfig.wardrobeQuickSwapKeysWardrobe;
                  wasDownArray = quickSwapWardrobeWasDown;
               } else if (isEquipmentMenu) {
                  menuSlots = EQUIPMENT_SWAP_SLOTS;
                  keyArray = MoonConfig.wardrobeQuickSwapKeysEquipment;
                  wasDownArray = quickSwapEquipmentWasDown;
               } else {
                  menuSlots = LOADOUT_MENU_SLOTS;
                  keyArray = MoonConfig.wardrobeQuickSwapKeysLoadout;
                  wasDownArray = quickSwapLoadoutWasDown;
               }

               Window window = client.getWindow();
               if (MoonConfig.wardrobeQuickSwapEnabled) {
                  for (int i = 0; i < keyArray.length; i++) {
                     int configuredKey = keyArray[i];
                     if (configuredKey < 0) {
                        wasDownArray[i] = false;
                     } else {
                        boolean isDown = InputConstants.isKeyDown(window, configuredKey);
                        if (isDown && !wasDownArray[i]) {
                           long now = System.nanoTime();
                           if ((now - lastSwapNanos) / 1000000L >= 300L) {
                              executeSwap(client, screen, i + 1, menuSlots[i]);
                           }
                        }

                        wasDownArray[i] = isDown;
                     }
                  }
               } else {
                  Arrays.fill(wasDownArray, false);
               }

               if (isWardrobeMenu) {
                  Arrays.fill(quickSwapEquipmentWasDown, false);
                  Arrays.fill(quickSwapLoadoutWasDown, false);
               } else if (isEquipmentMenu) {
                  Arrays.fill(quickSwapWardrobeWasDown, false);
                  Arrays.fill(quickSwapLoadoutWasDown, false);
               } else {
                  Arrays.fill(quickSwapWardrobeWasDown, false);
                  Arrays.fill(quickSwapEquipmentWasDown, false);
               }
            }
         }
      } else {
         Arrays.fill(resetWardrobe, false);
         Arrays.fill(resetEquipment, false);
      }
   }

   private static int findHotbarSlotByKey(Minecraft client, int keyCode) {
      KeyMapping[] hotbarKeys = client.options.keyHotbarSlots;
      if (hotbarKeys != null) {
         for (int i = 0; i < hotbarKeys.length && i < 9; i++) {
            KeyMappingAccessor accessor = (KeyMappingAccessor)hotbarKeys[i];
            Key boundKey = accessor.zeyafabricmoon$getBoundKey();
            if (boundKey != null && boundKey.getValue() == keyCode) {
               return i;
            }
         }
      }

      return -1;
   }

   private static void executeSwap(Minecraft client, AbstractContainerScreen<?> screen, int loadoutSlot, int menuSlot) {
      AbstractContainerMenu menu = screen.getMenu();
      if (menuSlot >= 0 && menuSlot < menu.slots.size()) {
         client.gameMode.handleContainerInput(menu.containerId, menuSlot, 0, ContainerInput.PICKUP, client.player);
         lastSwapNanos = System.nanoTime();
         closeMenuTicks = 10;
         sendFeedback(client, "Swap rapido slot " + loadoutSlot + " aplicado!");
      } else {
         sendFeedback(client, "Slot de menu invalido.");
      }
   }

   public static void tick(Minecraft client) {
      if (closeMenuTicks >= 0) {
         if (client.player == null) {
            closeMenuTicks = -1;
         }

         if (--closeMenuTicks <= 0) {
            if (client.gui.screen() instanceof AbstractContainerScreen) {
               client.player.closeContainer();
            }

            closeMenuTicks = -1;
         }
      }

      checkKeybinds(client);
   }

   private static void sendFeedback(Minecraft client, String message) {
      if (client.gui.chatListener() != null) {
         client.gui.chatListener().handleSystemMessage(Component.literal("[ZeyaMoon] [QuickSwap] " + message), false);
      }
   }
}
