package dev.zeya.fabricmoon.hud;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.MoonScreenStyle;
import dev.zeya.fabricmoon.MoonSettingsScreen;
import dev.zeya.fabricmoon.ZeyaFabricMoonClient;
import dev.zeya.fabricmoon.visual.MoonColorSettingsScreen;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonHudSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 340;
   private static final int PANEL_HEIGHT = 330;

   public MoonHudSettingsScreen() {
      super(Component.literal("HUD"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      this.addToggle("FPS / Ping / TPS", MoonConfig.showHud,
         () -> MoonConfig.showHud = !MoonConfig.showHud, centerX, top + 54);
      this.addToggle("Enemy Info", MoonConfig.combatHudShowTargetHealth,
         () -> MoonConfig.combatHudShowTargetHealth = !MoonConfig.combatHudShowTargetHealth, centerX, top + 82);
      this.addAction("Mover HUD", () -> this.minecraft.gui.setScreen(new MoonHudPositionScreen()), centerX, top + 112);
      this.addColorAction("Cor do texto", "HUD Text Color", MoonConfig.hudTextColor,
         color -> MoonConfig.hudTextColor = 0xFF000000 | color, centerX, top + 150);
      this.addColorAction("Cor do fundo", "HUD Background Color", MoonConfig.hudBackgroundColor,
         color -> MoonConfig.hudBackgroundColor = (MoonConfig.hudBackgroundColor & 0xFF000000) | color, centerX, top + 178);
      this.addColorAction("Cor de destaque", "HUD Accent Color", MoonConfig.hudAccentColor,
         color -> MoonConfig.hudAccentColor = 0xFF000000 | color, centerX, top + 206);
      this.addToggle("Fundo do HUD de informacoes", MoonConfig.hudBackgroundEnabled,
         () -> MoonConfig.hudBackgroundEnabled = !MoonConfig.hudBackgroundEnabled, centerX, top + 234);
      this.addRenderableWidget(
         this.button("Voltar", button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(centerX - 70, top + PANEL_HEIGHT - 34, 140, 22)
            .build()
      );
   }

   private void addToggle(String label, boolean enabled, Runnable toggle, int centerX, int y) {
      this.addRenderableWidget(
         this.button(ZeyaFabricMoonClient.toggleLabel(label, enabled), button -> {
            toggle.run();
            MoonConfig.save();
            this.clearWidgets();
            this.init();
         }).bounds(centerX - 135, y, 270, 22).build()
      );
   }

   private void addAction(String label, Runnable action, int centerX, int y) {
      this.addRenderableWidget(
         this.button(label + "  ›", button -> action.run()).bounds(centerX - 135, y, 270, 22).build()
      );
   }

   private void addColorAction(String label, String pickerTitle, int color, IntConsumer apply, int centerX, int y) {
      this.addAction(label, () -> this.minecraft.gui.setScreen(
         new MoonColorSettingsScreen(pickerTitle, color, this, apply)
      ), centerX, y);
   }

   private Button.Builder button(String label, Button.OnPress action) {
      return this.button(Component.literal(label), action);
   }

   private Button.Builder button(Component label, Button.OnPress action) {
      return Button.builder(MoonScreenStyle.colorLabel(label), action);
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      graphics.fill(0, 0, this.width, this.height, -1728053248);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, PANEL_WIDTH, PANEL_HEIGHT);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      graphics.centeredText(this.font, this.title, this.width / 2, top + 14, MoonConfig.modColorArgb());
   }
}
