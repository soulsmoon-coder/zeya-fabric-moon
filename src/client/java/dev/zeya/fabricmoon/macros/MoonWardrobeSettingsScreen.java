package dev.zeya.fabricmoon.macros;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.MoonScreenStyle;
import dev.zeya.fabricmoon.MoonSettingsScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.BeforeKeyPress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class MoonWardrobeSettingsScreen extends Screen implements BeforeKeyPress {
   private static final int PANEL_WIDTH = 780;
   private static final int PANEL_HEIGHT = 225;
   private static final int SLOT_BUTTON_HEIGHT = 20;
   private static final int SLOT_ROW_GAP = 4;
   private int waitingForSlot = -1;
   private int waitingType;
   private boolean keyListenerRegistered;

   public MoonWardrobeSettingsScreen() {
      super(Component.literal("Teclas rápidas de equipamento"));
   }

   protected void init() {
      if (!this.keyListenerRegistered) {
         ScreenKeyboardEvents.beforeKeyPress(this).register(this);
         this.keyListenerRegistered = true;
      }

      int panelWidth = Math.min(PANEL_WIDTH, this.width - 20);
      int left = (this.width - panelWidth) / 2;
      int groupWidth = (panelWidth - 32) / 3;
      int slotButtonWidth = Math.max(34, (groupWidth - 8) / 3);
      int slotGap = 4;
      int groupGap = 8;
      int firstGroupX = left + 12;
      int headingY = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT) + 52;
      int slotsY = headingY + 20;

      int[][] keyGroups = {
         MoonConfig.wardrobeQuickSwapKeysWardrobe,
         MoonConfig.wardrobeQuickSwapKeysEquipment,
         MoonConfig.wardrobeQuickSwapKeysLoadout
      };

      for (int group = 0; group < keyGroups.length; group++) {
         int groupX = firstGroupX + group * (groupWidth + groupGap);
         int[] groupKeys = keyGroups[group];

         for (int slot = 0; slot < groupKeys.length; slot++) {
            int currentGroup = group;
            int currentSlot = slot;
            int row = slot / 3;
            int column = slot % 3;
            int x = groupX + column * (slotButtonWidth + slotGap);
            int y = slotsY + row * (SLOT_BUTTON_HEIGHT + SLOT_ROW_GAP);
            boolean waiting = this.waitingForSlot == slot && this.waitingType == group;
            String keyName = waiting ? "pressione..." : getKeyName(groupKeys[slot]);
            String label = (slot + 1) + ": " + keyName;
            this.addRenderableWidget(Button.builder(
               MoonScreenStyle.colorLabel(Component.literal(label)),
               button -> this.waitForKey(currentGroup, currentSlot)
            ).bounds(x, y, slotButtonWidth, SLOT_BUTTON_HEIGHT).build());
         }
      }

      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      this.addRenderableWidget(Button.builder(
         MoonScreenStyle.colorLabel(Component.literal("Voltar")),
         button -> this.minecraft.gui.setScreen(new MoonSettingsScreen())
      ).bounds(this.width / 2 - 60, top + PANEL_HEIGHT - 30, 120, 20).build());
   }

   private void waitForKey(int group, int slot) {
      this.waitingType = group;
      this.waitingForSlot = slot;
      this.clearWidgets();
      this.init();
   }

   public void beforeKeyPress(Screen screen, KeyEvent event) {
      if (this.waitingForSlot < 0) {
         return;
      }

      if (event.key() == 256) {
         this.waitingForSlot = -1;
         this.clearWidgets();
         this.init();
         return;
      }

      int[] keys = switch (this.waitingType) {
         case 0 -> MoonConfig.wardrobeQuickSwapKeysWardrobe;
         case 1 -> MoonConfig.wardrobeQuickSwapKeysEquipment;
         default -> MoonConfig.wardrobeQuickSwapKeysLoadout;
      };
      keys[this.waitingForSlot] = event.key() == 259 ? -1 : event.key();
      this.waitingForSlot = -1;
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private static String getKeyName(int keyCode) {
      return keyCode < 0 ? "-" : MoonKeyCommands.getKeyDisplayName(keyCode);
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT), Math.min(PANEL_WIDTH, this.width - 20), PANEL_HEIGHT);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int panelWidth = Math.min(PANEL_WIDTH, this.width - 20);
      int left = (this.width - panelWidth) / 2;
      int groupWidth = (panelWidth - 32) / 3;
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      String[] labels = {"WARDROBE", "EQUIPMENT", "LOADOUT"};
      graphics.centeredText(this.font, this.title, this.width / 2, top + 14, MoonConfig.modColorArgb());
      for (int group = 0; group < labels.length; group++) {
         int groupCenter = left + 12 + group * (groupWidth + 8) + groupWidth / 2;
         graphics.centeredText(this.font, labels[group], groupCenter, top + 52, MoonConfig.modColorArgb());
      }

      graphics.centeredText(this.font, "Clique em um slot e pressione a tecla desejada. ESC cancela.", this.width / 2, top + PANEL_HEIGHT - 46, MoonScreenStyle.mutedColor());
   }
}
