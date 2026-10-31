package dev.zeya.fabricmoon;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonGlobalSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 320;
   private static final int PANEL_HEIGHT = 210;

   public MoonGlobalSettingsScreen() {
      super(Component.literal("Controle global"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 210);
      int buttonWidth = 270;
      int buttonX = centerX - buttonWidth / 2;
      this.addRenderableWidget(Button.builder(ZeyaFabricMoonClient.toggleLabel("Todas as features", MoonConfig.featuresEnabled), button -> {
         MoonConfig.featuresEnabled = !MoonConfig.featuresEnabled;
         MoonConfig.save();
         this.clearWidgets();
         this.init();
      }).bounds(buttonX, top + 55, buttonWidth, 22).build());
      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(buttonX, top + 145, buttonWidth, 22)
            .build()
      );
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 210);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 320, 210);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 210);
      graphics.centeredText(this.font, this.title, centerX, top + 18, -1);
      graphics.centeredText(this.font, "Desativar suspende os recursos e preserva suas configuracoes.", centerX, top + 94, -4799016);
      graphics.centeredText(this.font, "O painel e os comandos continuam disponiveis.", centerX, top + 110, -7891030);
   }
}
