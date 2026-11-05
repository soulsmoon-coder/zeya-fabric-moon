package dev.zeya.fabricmoon.hud;

import dev.zeya.fabricmoon.MoonConfig;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class MoonHudPositionScreen extends Screen {
   private MoonHudPositionScreen.DragTarget dragging = MoonHudPositionScreen.DragTarget.NONE;
   private double dragOffsetX;
   private double dragOffsetY;

   public MoonHudPositionScreen() {
      super(Component.literal("Mover HUD"));
   }

   protected void init() {
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("Restaurar posicoes")), button -> this.resetPositions())
            .bounds(this.width / 2 - 104, this.height - 30, 128, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("Concluir")), button -> this.onClose()).bounds(this.width / 2 + 28, this.height - 30, 76, 20).build()
      );
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0 && event.y() < this.height - 34) {
         int statsX = MoonHud.getStatsX(this.width);
         int statsY = MoonHud.getStatsY(this.height - 34);
         if (contains(event.x(), event.y(), statsX - 3, statsY - 3, MoonHud.getStatsPanelWidth(), MoonHud.getStatsPanelHeight())) {
            this.dragging = MoonHudPositionScreen.DragTarget.STATS;
            this.dragOffsetX = event.x() - MoonConfig.statsHudX;
            this.dragOffsetY = event.y() - statsY;
            return true;
         }

         if (this.hasCombatContent()) {
            int combatX = MoonHud.getCombatX(this.width);
            int combatY = MoonHud.getCombatY(this.height - 34);
            int combatHeight = MoonHud.getCombatPanelHeight();
            if (contains(event.x(), event.y(), combatX - 4, combatY - 4, 210, combatHeight)) {
               this.dragging = MoonHudPositionScreen.DragTarget.COMBAT;
               this.dragOffsetX = event.x() - combatX;
               this.dragOffsetY = event.y() - combatY;
               return true;
            }
         }
      }

      return super.mouseClicked(event, doubleClick);
   }

   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      if (this.dragging != MoonHudPositionScreen.DragTarget.NONE && event.button() == 0) {
         int availableHeight = Math.max(0, this.height - 34);
         if (this.dragging == MoonHudPositionScreen.DragTarget.STATS) {
            MoonConfig.statsHudX = Mth.clamp(
               (int)Math.round(event.x() - this.dragOffsetX),
               0,
               Math.max(0, this.width - MoonHud.getStatsPanelWidth())
            );
            MoonConfig.statsHudY = Mth.clamp(
               (int)Math.round(event.y() - this.dragOffsetY),
               0,
               Math.max(0, availableHeight - MoonHud.getStatsPanelHeight())
            );
         } else {
            MoonConfig.combatHudX = Mth.clamp((int)Math.round(event.x() - this.dragOffsetX), 0, Math.max(0, this.width - 210));
            MoonConfig.combatHudY = Mth.clamp((int)Math.round(event.y() - this.dragOffsetY), 0, Math.max(0, availableHeight - MoonHud.getCombatPanelHeight()));
         }

         return true;
      } else {
         return super.mouseDragged(event, deltaX, deltaY);
      }
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      if (event.button() == 0 && this.dragging != MoonHudPositionScreen.DragTarget.NONE) {
         this.dragging = MoonHudPositionScreen.DragTarget.NONE;
         MoonConfig.save();
         return true;
      } else {
         return super.mouseReleased(event);
      }
   }

   private void resetPositions() {
      MoonConfig.statsHudX = 4;
      MoonConfig.statsHudY = -1;
      MoonConfig.combatHudX = -1;
      MoonConfig.combatHudY = 8;
      MoonConfig.save();
   }

   private boolean hasCombatContent() {
      return (MoonConfig.combatHud && MoonConfig.combatHudShowDps) || MoonConfig.combatHudShowTargetHealth;
   }

   private static boolean contains(double x, double y, int left, int top, int width, int height) {
      return x >= left && x <= left + width && y >= top && y <= top + height;
   }

   public void onClose() {
      MoonConfig.save();
      super.onClose();
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      graphics.fill(0, 0, this.width, this.height, -1728053248);
      this.renderStatsPreview(graphics);
      if (this.hasCombatContent()) {
         this.renderCombatPreview(graphics);
      }
   }

   private void renderStatsPreview(GuiGraphicsExtractor graphics) {
      int x = MoonHud.getStatsX(this.width);
      int y = MoonHud.getStatsY(this.height - 34);
      if (MoonConfig.hudBackgroundEnabled) {
         MoonHud.drawHudBackground(graphics, x - 3, y - 3, MoonHud.getStatsPanelWidth(), MoonHud.getStatsPanelHeight());
      }
      graphics.text(this.font, "FPS: " + this.minecraft.getFps(), x, y, MoonConfig.hudTextColor);
      graphics.text(this.font, "TPS: " + formatTps(), x, y + 13, MoonConfig.hudTextColor);
      graphics.text(this.font, "Ping: --", x, y + 26, MoonConfig.hudTextColor);
   }

   private void renderCombatPreview(GuiGraphicsExtractor graphics) {
      int x = MoonHud.getCombatX(this.width);
      int y = MoonHud.getCombatY(this.height - 34);
      int combatColor = MoonConfig.hudTextColor;
      int row = 0;
      if (MoonConfig.combatHud && MoonConfig.combatHudShowDps) {
         graphics.text(this.font, "DPS: " + MoonHud.formatCompact(MoonCombatTracker.getDps()), x, y, combatColor);
         row = 1;
      }

      if (MoonConfig.combatHudShowTargetHealth) {
         graphics.text(this.font, "Target: Mob", x, y + row * 12, combatColor);
         graphics.text(this.font, "HP: 20.0 / 20.0 (100%)", x, y + (row + 1) * 12, combatColor);
         graphics.fill(x, y + (row + 2) * 12, x + 100, y + (row + 2) * 12 + 3, MoonConfig.hudAccentColor);
      }
   }

   private static String formatTps() {
      double tps = MoonServerStats.getEstimatedTps();
      return tps < 0.0 ? "--" : String.format(Locale.ROOT, "%.1f", tps);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      graphics.centeredText(this.font, this.title, this.width / 2, 12, MoonConfig.modColorArgb());
      graphics.centeredText(this.font, "Arraste os paineis para mover. As posicoes sao salvas ao soltar.", this.width / 2, 24, MoonConfig.modColorArgb());
   }

   private enum DragTarget {
      NONE,
      STATS,
      COMBAT;
   }
}
