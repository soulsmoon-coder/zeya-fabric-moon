package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.MoonSlotManager;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class InventoryScreenMixin {
   @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
   private void zeyafabricmoon$handleSlotLockAndBind(Slot slot, int slotId, int mouseButton, ContainerInput clickType, CallbackInfo callback) {
      if (MoonConfig.featuresEnabled) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null) {
            Inventory inventory = client.player.getInventory();
            if (MoonConfig.slotLockEnabled && slot != null && slot.container == inventory && MoonSlotManager.isSlotLocked(slot.getContainerSlot())) {
               MoonSlotManager.sendBlockedFeedback(client, "[Slot Lock] Esse slot esta travado.");
               callback.cancel();
            } else if (MoonConfig.slotLockEnabled
               && clickType == ContainerInput.SWAP
               && mouseButton >= 0
               && mouseButton < 9
               && MoonSlotManager.isSlotLocked(mouseButton)) {
               MoonSlotManager.sendBlockedFeedback(client, "[Slot Lock] O slot de destino esta travado.");
               callback.cancel();
            } else {
               if (slot != null
                  && slot.container == inventory
                  && clickType == ContainerInput.QUICK_MOVE
                  && MoonSlotManager.onBoundSlotQuickMove(client, slot, slotId)) {
                  callback.cancel();
               }
            }
         }
      }
   }

   @Inject(method = "extractContents", at = @At("TAIL"))
   private void zeyafabricmoon$renderSlotOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
      if (MoonConfig.featuresEnabled) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null) {
            ContainerScreenAccessor screen = (ContainerScreenAccessor)this;
            AbstractContainerMenu menu = screen.zeyafabricmoon$getMenu();
            Inventory inventory = client.player.getInventory();
            int left = screen.zeyafabricmoon$getLeftPos();
            int top = screen.zeyafabricmoon$getTopPos();
            if (MoonConfig.slotLockEnabled) {
               for (Slot slot : menu.slots) {
                  if (slot.container == inventory && MoonSlotManager.isSlotLocked(slot.getContainerSlot())) {
                     drawLock(graphics, left + slot.x + 2, top + slot.y + 1);
                  }
               }
            }

            if (MoonConfig.slotBindEnabled && !MoonConfig.slotBindings.isEmpty()) {
               for (Entry<Integer, Integer> binding : MoonConfig.slotBindings.entrySet()) {
                  Slot inventorySlot = findPlayerSlot(menu, inventory, binding.getKey());
                  Slot hotbarSlot = findPlayerSlot(menu, inventory, binding.getValue());
                  if (inventorySlot != null && hotbarSlot != null) {
                     int x1 = left + inventorySlot.x + 8;
                     int y1 = top + inventorySlot.y + 8;
                     int x2 = left + hotbarSlot.x + 8;
                     int y2 = top + hotbarSlot.y + 8;
                     drawLine(graphics, x1, y1, x2, y2, -15722464, 6);
                     drawLine(graphics, x1, y1, x2, y2, -9251384, 3);
                     drawSlotMarker(graphics, left + inventorySlot.x, top + inventorySlot.y);
                     drawSlotMarker(graphics, left + hotbarSlot.x, top + hotbarSlot.y);
                  }
               }
            }
         }
      }
   }

   private static Slot findPlayerSlot(AbstractContainerMenu menu, Inventory inventory, int inventoryIndex) {
      for (Slot slot : menu.slots) {
         if (slot.container == inventory && slot.getContainerSlot() == inventoryIndex) {
            return slot;
         }
      }

      return null;
   }

   private static void drawLock(GuiGraphicsExtractor graphics, int x, int y) {
      graphics.fill(x, y, x + 11, y + 13, -401598686);
      graphics.fill(x + 2, y + 1, x + 9, y + 7, -11174);
      graphics.fill(x + 4, y + 3, x + 7, y + 7, -15722718);
      graphics.fill(x + 1, y + 6, x + 10, y + 12, -11174);
      graphics.fill(x + 5, y + 8, x + 6, y + 10, -15722718);
   }

   private static void drawSlotMarker(GuiGraphicsExtractor graphics, int x, int y) {
      graphics.fill(x - 2, y - 2, x + 18, y, -15722464);
      graphics.fill(x - 2, y + 16, x + 18, y + 18, -15722464);
      graphics.fill(x - 2, y, x, y + 16, -15722464);
      graphics.fill(x + 16, y, x + 18, y + 16, -15722464);
      graphics.fill(x - 1, y - 1, x + 17, y, -9251384);
      graphics.fill(x - 1, y + 16, x + 17, y + 17, -9251384);
      graphics.fill(x - 1, y, x, y + 16, -9251384);
      graphics.fill(x + 16, y, x + 17, y + 16, -9251384);
   }

   private static void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color, int width) {
      int dx = Math.abs(x1 - x0);
      int sx = x0 < x1 ? 1 : -1;
      int dy = -Math.abs(y1 - y0);
      int sy = y0 < y1 ? 1 : -1;
      int error = dx + dy;
      int remaining = 4096;

      while (remaining-- > 0) {
         int halfWidth = width / 2;
         graphics.fill(x0 - halfWidth, y0 - halfWidth, x0 - halfWidth + width, y0 - halfWidth + width, color);
         if (x0 == x1 && y0 == y1) {
            return;
         }

         int twiceError = 2 * error;
         if (twiceError >= dy) {
            error += dy;
            x0 += sx;
         }

         if (twiceError <= dx) {
            error += dx;
            y0 += sy;
         }
      }
   }
}
