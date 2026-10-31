package dev.zeya.fabricmoon;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.BeforeKeyPress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class MoonSlotLockSettingsScreen extends Screen implements BeforeKeyPress {
   private static final int PANEL_WIDTH = 320;
   private static final int PANEL_HEIGHT = 440;
   private boolean waitingForKey;
   private boolean keyListenerRegistered;

   public MoonSlotLockSettingsScreen() {
      super(Component.literal("Slot Lock"));
   }

   protected void init() {
      if (!this.keyListenerRegistered) {
         ScreenKeyboardEvents.beforeKeyPress(this).register(this);
         this.keyListenerRegistered = true;
      }

      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 440);
      int buttonWidth = 270;
      int buttonX = centerX - buttonWidth / 2;
      this.addRenderableWidget(
         Button.builder(
               ZeyaFabricMoonClient.toggleLabel("Slot Lock: habilitado", MoonConfig.slotLockEnabled),
               button -> this.toggle(() -> MoonConfig.slotLockEnabled = !MoonConfig.slotLockEnabled)
            )
            .bounds(buttonX, top + 48, buttonWidth, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal(this.waitingForKey ? "Pressione uma tecla..." : keyName(MoonConfig.slotLockKeyCode)), button -> {
            this.waitingForKey = true;
            this.clearWidgets();
            this.init();
         }).bounds(buttonX, top + 76, buttonWidth, 22).build()
      );

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            int slot = row * 3 + col;
            int finalSlot = slot;
            boolean locked = MoonConfig.lockedSlots.contains(slot);
            String label = slot + 1 + (locked ? " [L]" : "");
            this.addRenderableWidget(
               Button.builder(Component.literal(label), button -> this.toggleSlot(finalSlot)).bounds(buttonX + col * 92, top + 128 + row * 36, 88, 30).build()
            );
         }
      }

      this.addRenderableWidget(
         Button.builder(Component.literal("Limpar todos os slots"), button -> this.clearAllSlots()).bounds(buttonX, top + 248, buttonWidth, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(buttonX, top + 398, buttonWidth, 22)
            .build()
      );
   }

   public void beforeKeyPress(Screen screen, KeyEvent event) {
      if (this.waitingForKey) {
         if (event.key() != 256 && event.key() != 258) {
            MoonConfig.slotLockKeyCode = event.key();
            MoonConfig.save();
            MoonSlotManager.refreshKeyMappings();
            this.waitingForKey = false;
         } else {
            this.waitingForKey = false;
         }

         this.clearWidgets();
         this.init();
      }
   }

   private static String keyName(int keyCode) {
      return keyCode < 0 ? "Tecla: não configurada" : "Tecla: " + MoonKeyCommands.getKeyDisplayName(keyCode);
   }

   private void toggle(Runnable action) {
      action.run();
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private void toggleSlot(int slot) {
      if (!MoonConfig.lockedSlots.remove(slot)) {
         MoonConfig.lockedSlots.add(slot);
      }

      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private void clearAllSlots() {
      MoonConfig.lockedSlots.clear();
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 440);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 320, 440);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 440);
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "Slots da hotbar ([L] = travado):", centerX, top + 104, MoonScreenStyle.mutedColor());
      int lockedCount = MoonConfig.lockedSlots.size();
      graphics.centeredText(this.font, "Total travados: " + lockedCount, centerX, top + 278, -4799016);
      graphics.centeredText(this.font, "Com cadeado: " + lockedSlotsSummary(), centerX, top + 300, lockedCount == 0 ? MoonScreenStyle.mutedColor() : -11174);
      graphics.centeredText(this.font, "Como funciona:", centerX, top + 326, MoonScreenStyle.mutedColor());
      graphics.centeredText(this.font, "Com o inventario aberto, aponte para um slot", centerX, top + 346, -4799016);
      graphics.centeredText(this.font, "e pressione a tecla configurada para travar.", centerX, top + 364, -4799016);
   }

   private static String lockedSlotsSummary() {
      if (MoonConfig.lockedSlots.isEmpty()) {
         return "nenhum";
      }

      List<Integer> sorted = new ArrayList<>(MoonConfig.lockedSlots);
      sorted.sort(Integer::compareTo);
      List<String> labels = new ArrayList<>();

      for (int i = 0; i < Math.min(3, sorted.size()); i++) {
         int slot = sorted.get(i);
         labels.add(slot < 9 ? "H" + (slot + 1) : "Inv." + (slot - 8));
      }

      if (sorted.size() > labels.size()) {
         labels.add("+" + (sorted.size() - labels.size()));
      }

      return String.join(", ", labels);
   }
}
