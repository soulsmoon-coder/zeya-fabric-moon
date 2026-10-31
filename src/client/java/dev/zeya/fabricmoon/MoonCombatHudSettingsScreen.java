package dev.zeya.fabricmoon;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonCombatHudSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 320;
   private static final int PANEL_HEIGHT = 270;

   public MoonCombatHudSettingsScreen() {
      super(Component.literal("Combat HUD"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 270);
      int buttonWidth = 270;
      int buttonX = centerX - buttonWidth / 2;
      this.addRenderableWidget(Button.builder(ZeyaFabricMoonClient.toggleLabel("Combat HUD", MoonConfig.combatHud), button -> {
         MoonConfig.combatHud = !MoonConfig.combatHud;
         MoonConfig.save();
         this.refreshWidgets();
      }).bounds(buttonX, top + 48, buttonWidth, 22).build());
      this.addRenderableWidget(Button.builder(ZeyaFabricMoonClient.toggleLabel("Dano e dano total", MoonConfig.combatHudShowDamage), button -> {
         MoonConfig.combatHudShowDamage = !MoonConfig.combatHudShowDamage;
         MoonConfig.save();
         this.refreshWidgets();
      }).bounds(buttonX, top + 80, buttonWidth, 22).build());
      this.addRenderableWidget(
         Button.builder(ZeyaFabricMoonClient.toggleLabel("Mostrar alvo e vida (mirando)", MoonConfig.combatHudShowTargetHealth), button -> {
            MoonConfig.combatHudShowTargetHealth = !MoonConfig.combatHudShowTargetHealth;
            MoonConfig.save();
            this.refreshWidgets();
         }).bounds(buttonX, top + 112, buttonWidth, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Resetar dano total"), button -> MoonCombatTracker.resetDamage()).bounds(buttonX, top + 150, buttonWidth, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(buttonX, top + 190, buttonWidth, 22)
            .build()
      );
   }

   private void refreshWidgets() {
      this.clearWidgets();
      this.init();
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 270);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 320, 270);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 270);
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "Valores estimados com dados recebidos pelo cliente.", centerX, top + 234, -7891030);
   }
}
