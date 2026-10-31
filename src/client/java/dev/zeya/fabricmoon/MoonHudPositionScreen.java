package dev.zeya.fabricmoon;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class MoonHudPositionScreen extends Screen {
   private static final int STATS_WIDTH = 115;
   private static final int STATS_HEIGHT = 70;
   private static final int COMBAT_WIDTH = 210;
   private static final int FOOTER_HEIGHT = 34;
   private MoonHudPositionScreen.DragTarget dragging = MoonHudPositionScreen.DragTarget.NONE;
   private double dragOffsetX;
   private double dragOffsetY;

   public MoonHudPositionScreen() {
      super(Component.literal("Mover HUD"));
   }

   protected void init() {
      this.addRenderableWidget(
         Button.builder(Component.literal("Restaurar posicoes"), button -> this.resetPositions())
            .bounds(this.width / 2 - 104, this.height - 30, 128, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Concluir"), button -> this.onClose()).bounds(this.width / 2 + 28, this.height - 30, 76, 20).build()
      );
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0 && event.y() < this.height - 34) {
         int statsX = MoonHud.getStatsX(this.width);
         int statsY = MoonHud.getStatsY(this.height - 34);
         if (contains(event.x(), event.y(), statsX - 3, statsY - 3, 115, 70)) {
            this.dragging = MoonHudPositionScreen.DragTarget.STATS;
            this.dragOffsetX = event.x() - MoonConfig.statsHudX;
            this.dragOffsetY = event.y() - statsY;
            return true;
         }

         if (MoonConfig.combatHud && this.hasCombatContent()) {
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
            MoonConfig.statsHudX = Mth.clamp((int)Math.round(event.x() - this.dragOffsetX), 0, Math.max(0, this.width - 115));
            MoonConfig.statsHudY = Mth.clamp((int)Math.round(event.y() - this.dragOffsetY), 0, Math.max(0, availableHeight - 70));
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
      return MoonConfig.combatHudShowDamage || MoonConfig.combatHudShowTargetHealth;
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
      if (MoonConfig.combatHud && this.hasCombatContent()) {
         this.renderCombatPreview(graphics);
      }
   }

   private void renderStatsPreview(GuiGraphicsExtractor graphics) {
      int x = MoonHud.getStatsX(this.width);
      int y = MoonHud.getStatsY(this.height - 34);
      graphics.fill(x - 3, y - 3, x + 112, y + 70 - 3, -805306368);
      graphics.fill(x - 4, y - 4, x + 113, y - 3, -9251384);
      graphics.text(this.font, "FPS: " + this.minecraft.getFps(), x, y, -43521);
      graphics.text(this.font, "TPS: " + formatTps(), x, y + 13, -43521);
      graphics.text(this.font, "Ping: --", x, y + 26, -43521);
      graphics.text(this.font, "CPS L: " + MoonMouseTracker.getLeftClicksPerSecond(), x, y + 39, -43521);
      graphics.text(this.font, "CPS R: " + MoonMouseTracker.getRightClicksPerSecond(), x, y + 52, -43521);
   }

   private void renderCombatPreview(GuiGraphicsExtractor graphics) {
      int x = MoonHud.getCombatX(this.width);
      int y = MoonHud.getCombatY(this.height - 34);
      int row = 0;
      if (MoonConfig.combatHudShowDamage) {
         graphics.text(this.font, "Dano: --", x, y, -13227);
         graphics.text(this.font, "Total: " + MoonHud.formatCompact(MoonCombatTracker.getTotalDamage()), x, y + 12, -1);
         row = 2;
      }

      if (MoonConfig.combatHudShowTargetHealth) {
         graphics.text(this.font, "Alvo: Mob", x, y + row * 12, -4791297);
         graphics.text(this.font, "HP: 20.0 / 20.0 (100%)", x, y + (row + 1) * 12, -1);
         graphics.fill(x, y + (row + 2) * 12, x + 100, y + (row + 2) * 12 + 3, -11149961);
      }
   }

   private static String formatTps() {
      double tps = MoonServerStats.getEstimatedTps();
      return tps < 0.0 ? "--" : String.format(Locale.ROOT, "%.1f", tps);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      graphics.centeredText(this.font, this.title, this.width / 2, 12, -1);
      graphics.centeredText(this.font, "Arraste os paineis para mover. As posicoes sao salvas ao soltar.", this.width / 2, 24, -4799016);
   }

   private enum DragTarget {
      NONE,
      STATS,
      COMBAT;
   }
}
