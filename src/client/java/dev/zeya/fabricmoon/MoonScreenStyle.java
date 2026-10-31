package dev.zeya.fabricmoon;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class MoonScreenStyle {
   private static final int PANEL_COLOR = -300935390;
   private static final int BORDER_COLOR = -13352108;
   private static final int ACCENT_COLOR = -9251384;
   private static final int MUTED_COLOR = -7891030;

   private MoonScreenStyle() {
   }

   static int panelTop(int screenHeight, int panelHeight) {
      return Math.max(8, (screenHeight - panelHeight) / 2);
   }

   static void drawPanel(GuiGraphicsExtractor graphics, int centerX, int top, int width, int height) {
      int left = centerX - width / 2;
      graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, -13352108);
      graphics.fill(left, top, left + width, top + height, -300935390);
      graphics.fill(left, top, left + width, top + 3, -9251384);
      graphics.fill(left + 16, top + 31, left + width - 16, top + 32, -13352108);
   }

   static void drawSection(GuiGraphicsExtractor graphics, Font font, int centerX, String label, int y) {
      graphics.centeredText(font, label, centerX, y, -9251384);
   }

   static int mutedColor() {
      return -7891030;
   }
}
