package dev.zeya.fabricmoon.hud;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.MoonScreenStyle;
import dev.zeya.fabricmoon.MoonSettingsScreen;
import dev.zeya.fabricmoon.ZeyaFabricMoonClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonCombatHudSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 320;
   private static final int PANEL_HEIGHT = 320;

   public MoonCombatHudSettingsScreen() {
      super(Component.literal("DPS"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      int buttonWidth = 270;
      int buttonX = centerX - buttonWidth / 2;
      this.addRenderableWidget(
         Button.builder(
            MoonScreenStyle.colorLabel(ZeyaFabricMoonClient.toggleLabel("Exibir HUD de DPS", this.isDpsEnabled())),
            button -> {
               boolean enabled = !this.isDpsEnabled();
               MoonConfig.combatHud = enabled;
               MoonConfig.combatHudShowDps = enabled;
               MoonConfig.save();
               this.refreshWidgets();
            }
         ).bounds(buttonX, top + 48, buttonWidth, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(
            MoonScreenStyle.colorLabel(ZeyaFabricMoonClient.toggleLabel("Log de nomes/vida (debug)", MoonConfig.combatHudDebugLabels)),
            button -> {
               MoonConfig.combatHudDebugLabels = !MoonConfig.combatHudDebugLabels;
               MoonConfig.save();
               this.refreshWidgets();
            }
         ).bounds(buttonX, top + 80, buttonWidth, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(
            MoonScreenStyle.colorLabel(ZeyaFabricMoonClient.toggleLabel("Resumo de dano no chat (3s)", MoonConfig.combatHudChatDamage)),
            button -> {
               MoonConfig.combatHudChatDamage = !MoonConfig.combatHudChatDamage;
               MoonConfig.save();
               this.refreshWidgets();
            }
         ).bounds(buttonX, top + 112, buttonWidth, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(MoonScreenStyle.colorLabel(Component.literal("-")), button -> this.changeRange(-1))
            .bounds(buttonX, top + 176, 42, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(MoonScreenStyle.colorLabel(Component.literal("+")), button -> this.changeRange(1))
            .bounds(buttonX + buttonWidth - 42, top + 176, 42, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(MoonScreenStyle.colorLabel(Component.literal("Resetar DPS")), button -> MoonCombatTracker.resetDps())
            .bounds(buttonX, top + 208, buttonWidth, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(MoonScreenStyle.colorLabel(Component.literal("Voltar")), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(buttonX, top + 240, buttonWidth, 22)
            .build()
      );
   }

   private boolean isDpsEnabled() {
      return MoonConfig.combatHud && MoonConfig.combatHudShowDps;
   }

   private void changeRange(int amount) {
      MoonConfig.combatHudRange = MoonConfig.clampCombatHudRange(MoonConfig.combatHudRange + amount);
      MoonConfig.save();
      this.refreshWidgets();
   }

   private void refreshWidgets() {
      this.clearWidgets();
      this.init();
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
      graphics.centeredText(this.font, this.title, centerX, top + 14, MoonConfig.modColorArgb());
      graphics.centeredText(
         this.font,
         "Alcance: " + MoonConfig.clampCombatHudRange(MoonConfig.combatHudRange) + " blocos",
         centerX,
         top + 182,
         MoonConfig.modColorArgb()
      );
      graphics.centeredText(this.font, "Dano detectado localmente pelo cliente.", centerX, top + 274, MoonScreenStyle.mutedColor());
   }
}
