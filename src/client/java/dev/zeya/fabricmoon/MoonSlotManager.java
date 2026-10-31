package dev.zeya.fabricmoon;

import dev.zeya.fabricmoon.mixin.ContainerScreenAccessor;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

public final class MoonSlotManager {
   private static int pendingBindSlot = -1;
   private static boolean lockKeyWasDown;
   private static long lastBlockedFeedbackNanos;

   private MoonSlotManager() {
   }

   public static void refreshKeyMappings() {
      pendingBindSlot = -1;
      lockKeyWasDown = false;
   }

   public static void tick(Minecraft client) {
      if (MoonConfig.featuresEnabled && client.player != null && client.isWindowActive()) {
         boolean lockDown = client.gui.screen() == null
            && MoonConfig.slotLockEnabled
            && MoonConfig.slotLockKeyCode >= 0
            && GLFW.glfwGetKey(client.getWindow().handle(), MoonConfig.slotLockKeyCode) == 1;
         if (lockDown && !lockKeyWasDown) {
            toggleCurrentSlotLock(client);
         }

         lockKeyWasDown = lockDown;
      } else {
         lockKeyWasDown = false;
      }
   }

   public static void onKeyPressed(Minecraft client, int keyCode) {
      if (MoonConfig.featuresEnabled && client.player != null && keyCode >= 0) {
         if (keyCode == MoonConfig.slotLockKeyCode && MoonConfig.slotLockEnabled && client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            Slot slot = ((ContainerScreenAccessor)screen).zeyafabricmoon$getHoveredSlot();
            int inventoryIndex = playerInventoryIndex(client, slot);
            if (inventoryIndex >= 0) {
               toggleSlotLock(client, inventoryIndex);
            }
         } else if (MoonConfig.slotBindEnabled && keyCode == MoonConfig.slotBindKeyCode && client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            Slot var9 = ((ContainerScreenAccessor)screen).zeyafabricmoon$getHoveredSlot();
            int inventoryIndex = playerInventoryIndex(client, var9);
            if (inventoryIndex >= 0) {
               if (pendingBindSlot < 0) {
                  pendingBindSlot = inventoryIndex;
                  sendFeedback(client, "[Slot Bind] Slot " + describeSlot(inventoryIndex) + " selecionado. Selecione um slot da outra area.");
               } else if (pendingBindSlot == inventoryIndex) {
                  pendingBindSlot = -1;
                  sendFeedback(client, "[Slot Bind] Pareamento cancelado.");
               } else {
                  boolean pendingIsHotbar = pendingBindSlot < 9;
                  boolean currentIsHotbar = inventoryIndex < 9;
                  if (pendingIsHotbar == currentIsHotbar) {
                     pendingBindSlot = inventoryIndex;
                     sendFeedback(client, "[Slot Bind] Selecione um slot da outra area para concluir o pareamento.");
                  } else {
                     bindSlots(pendingIsHotbar ? pendingBindSlot : inventoryIndex, pendingIsHotbar ? inventoryIndex : pendingBindSlot);
                  }
               }
            }
         }
      }
   }

   private static int playerInventoryIndex(Minecraft client, Slot slot) {
      if (slot != null && slot.container == client.player.getInventory()) {
         int index = slot.getContainerSlot();
         return index >= 0 && index < 40 ? index : -1;
      } else {
         return -1;
      }
   }

   private static void bindSlots(int hotbarIndex, int inventoryIndex) {
      if (hotbarIndex >= 0 && hotbarIndex < 9 && inventoryIndex >= 9 && inventoryIndex < 40) {
         if (MoonConfig.slotBindings.getOrDefault(inventoryIndex, -1) == hotbarIndex) {
            MoonConfig.slotBindings.remove(inventoryIndex);
            MoonConfig.save();
            pendingBindSlot = -1;
            sendFeedback(Minecraft.getInstance(), "[Slot Bind] Vinculo removido: " + describeSlot(inventoryIndex) + " <-> hotbar " + (hotbarIndex + 1) + ".");
         } else {
            MoonConfig.slotBindings.entrySet().removeIf(entry -> entry.getKey() == inventoryIndex || entry.getValue() == hotbarIndex);
            MoonConfig.slotBindings.put(inventoryIndex, hotbarIndex);
            MoonConfig.save();
            pendingBindSlot = -1;
            sendFeedback(
               Minecraft.getInstance(),
               "[Slot Bind] " + describeSlot(inventoryIndex) + " <-> hotbar " + (hotbarIndex + 1) + ". Shift+clique em qualquer um dos dois para trocar."
            );
         }
      }
   }

   private static String describeSlot(int inventoryIndex) {
      return inventoryIndex < 9 ? "hotbar " + (inventoryIndex + 1) : "inventario " + (inventoryIndex - 8);
   }

   private static void toggleCurrentSlotLock(Minecraft client) {
      toggleSlotLock(client, client.player.getInventory().getSelectedSlot());
   }

   private static void toggleSlotLock(Minecraft client, int inventoryIndex) {
      if (inventoryIndex >= 0 && inventoryIndex < 40) {
         if (!MoonConfig.lockedSlots.remove(inventoryIndex)) {
            MoonConfig.lockedSlots.add(inventoryIndex);
         }

         String state = MoonConfig.lockedSlots.contains(inventoryIndex) ? "travado" : "destravado";
         sendFeedback(client, "[Slot Lock] Slot " + describeSlot(inventoryIndex) + " " + state + ".");
         MoonConfig.save();
      }
   }

   public static boolean isSlotLocked(int inventoryIndex) {
      return inventoryIndex >= 0 && inventoryIndex < 40 && MoonConfig.lockedSlots.contains(inventoryIndex);
   }

   public static boolean shouldBlockSelectedSlotDrop() {
      Minecraft client = Minecraft.getInstance();
      return MoonConfig.featuresEnabled && MoonConfig.slotLockEnabled && client.player != null && !(client.gui.screen() instanceof AbstractContainerScreen)
         ? isSlotLocked(client.player.getInventory().getSelectedSlot())
         : false;
   }

   public static boolean onBoundSlotQuickMove(Minecraft client, Slot clickedSlot, int clickedMenuSlot) {
      if (MoonNetworkPolicy.allowsModServerActions()
         && MoonConfig.featuresEnabled
         && MoonConfig.slotBindEnabled
         && client.player != null
         && client.gameMode != null
         && clickedSlot != null
         && clickedSlot.container == client.player.getInventory()) {
         int clickedIndex = clickedSlot.getContainerSlot();
         int boundIndex = findBoundIndex(clickedIndex);
         if (boundIndex < 0) {
            return false;
         } else {
            int hotbarIndex = clickedIndex < 9 ? clickedIndex : boundIndex;
            int swapMenuSlot = clickedIndex < 9 ? findMenuSlotId(client.player.containerMenu, client.player.getInventory(), boundIndex) : clickedMenuSlot;
            if (hotbarIndex >= 0 && hotbarIndex < 9 && swapMenuSlot >= 0) {
               client.gameMode.handleContainerInput(client.player.containerMenu.containerId, swapMenuSlot, hotbarIndex, ContainerInput.SWAP, client.player);
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static int findBoundIndex(int inventoryIndex) {
      Integer direct = MoonConfig.slotBindings.get(inventoryIndex);
      if (direct != null) {
         return direct;
      }

      for (Entry<Integer, Integer> entry : MoonConfig.slotBindings.entrySet()) {
         if (entry.getValue() == inventoryIndex) {
            return entry.getKey();
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

   public static boolean shouldBlockContainerInput(Minecraft client, int slotId, int button, ContainerInput clickType) {
      if (MoonConfig.featuresEnabled && MoonConfig.slotLockEnabled && client.player != null) {
         AbstractContainerMenu menu = client.player.containerMenu;
         if (slotId >= 0 && slotId < menu.slots.size()) {
            Slot slot = (Slot)menu.slots.get(slotId);
            if (slot.container == client.player.getInventory() && isSlotLocked(slot.getContainerSlot())) {
               return true;
            }
         }

         return clickType == ContainerInput.SWAP && button >= 0 && button < 9 && isSlotLocked(button);
      } else {
         return false;
      }
   }

   public static void sendFeedback(Minecraft client, String message) {
      if (client.gui.chatListener() != null) {
         client.gui.chatListener().handleSystemMessage(Component.literal("[ZeyaMoon] " + message), false);
      }
   }

   public static void sendBlockedFeedback(Minecraft client, String message) {
      long now = System.nanoTime();
      if (now - lastBlockedFeedbackNanos >= 750000000L) {
         lastBlockedFeedbackNanos = now;
         sendFeedback(client, message);
      }
   }

   public static void resetSlotBind() {
      pendingBindSlot = -1;
   }
}
