package dev.zeya.fabricmoon.visual;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.MoonScreenStyle;
import dev.zeya.fabricmoon.MoonSettingsScreen;
import dev.zeya.fabricmoon.ZeyaFabricMoonClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonGlowSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 320;
   private static final int PANEL_HEIGHT = 210;

   public MoonGlowSettingsScreen() {
      super(Component.literal("Player Glow"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      this.addRenderableWidget(Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(ZeyaFabricMoonClient.toggleLabel("Contorno do jogador", MoonConfig.playerGlow)), button -> {
         MoonConfig.playerGlow = !MoonConfig.playerGlow;
         MoonConfig.save();
         this.clearWidgets();
         this.init();
      }).bounds(centerX - 135, top + 55, 270, 24).build());
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("Voltar")), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(centerX - 135, top + 145, 270, 22)
            .build()
      );
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, PANEL_WIDTH, PANEL_HEIGHT);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      graphics.centeredText(this.font, this.title, centerX, top + 18, MoonConfig.modColorArgb());
      graphics.centeredText(this.font, "A cor do contorno segue a cor global do mod.", centerX, top + 110, MoonConfig.modColorArgb());
   }
}
