package dev.zeya.fabricmoon;

import java.util.Map.Entry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.BeforeKeyPress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class MoonSlotBindSettingsScreen extends Screen implements BeforeKeyPress {
   private static final int PANEL_WIDTH = 320;
   private static final int PANEL_HEIGHT = 310;
   private boolean waitingForKey;
   private boolean keyListenerRegistered;

   public MoonSlotBindSettingsScreen() {
      super(Component.literal("Slot Bind"));
   }

   protected void init() {
      if (!this.keyListenerRegistered) {
         ScreenKeyboardEvents.beforeKeyPress(this).register(this);
         this.keyListenerRegistered = true;
      }

      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 310);
      int buttonWidth = 270;
      int buttonX = centerX - buttonWidth / 2;
      this.addRenderableWidget(
         Button.builder(
               ZeyaFabricMoonClient.toggleLabel("Slot Bind: habilitado", MoonConfig.slotBindEnabled),
               button -> this.toggle(() -> MoonConfig.slotBindEnabled = !MoonConfig.slotBindEnabled)
            )
            .bounds(buttonX, top + 48, buttonWidth, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal(this.waitingForKey ? "Pressione uma tecla..." : keyName(MoonConfig.slotBindKeyCode)), button -> {
            this.waitingForKey = true;
            this.clearWidgets();
            this.init();
         }).bounds(buttonX, top + 80, buttonWidth, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(buttonX, top + 238, buttonWidth, 22)
            .build()
      );
   }

   public void beforeKeyPress(Screen screen, KeyEvent event) {
      if (this.waitingForKey) {
         if (event.key() != 256 && event.key() != 258) {
            MoonConfig.slotBindKeyCode = event.key();
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
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 310);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 320, 310);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 310);
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "Como vincular:", centerX, top + 110, MoonScreenStyle.mutedColor());
      graphics.centeredText(this.font, "1. No inventário, aponte para um slot", centerX, top + 130, -4799016);
      graphics.centeredText(this.font, "e pressione a tecla configurada.", centerX, top + 146, -4799016);
      graphics.centeredText(this.font, "2. Aponte para um slot da outra área", centerX, top + 166, -4799016);
      graphics.centeredText(this.font, "e pressione a tecla para parear.", centerX, top + 182, -4799016);
      graphics.centeredText(this.font, "Use Shift+clique no item pareado para trocar.", centerX, top + 198, -4799016);
      graphics.centeredText(this.font, bindingSummary(), centerX, top + 218, MoonConfig.slotBindings.isEmpty() ? MoonScreenStyle.mutedColor() : -9251384);
   }

   private static String bindingSummary() {
      if (MoonConfig.slotBindings.isEmpty()) {
         return "Vinculo ativo: nenhum";
      }

      Entry<Integer, Integer> binding = MoonConfig.slotBindings.entrySet().iterator().next();
      String summary = "Ligado: hotbar " + (binding.getValue() + 1) + " <-> inventario " + (binding.getKey() - 8);
      int remaining = MoonConfig.slotBindings.size() - 1;
      return remaining > 0 ? summary + " (mais " + remaining + ")" : summary;
   }
}
