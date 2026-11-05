package dev.zeya.fabricmoon;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public final class MoonScreenStyle {
   private static final int PANEL_COLOR = -300935390;
   private MoonScreenStyle() {
   }

   public static int panelTop(int screenHeight, int panelHeight) {
      return Math.max(8, (screenHeight - panelHeight) / 2);
   }

   public static void drawPanel(GuiGraphicsExtractor graphics, int centerX, int top, int width, int height) {
      int left = centerX - width / 2;
      graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, MoonConfig.modColorArgb());
      graphics.fill(left, top, left + width, top + height, -300935390);
      graphics.fill(left, top, left + width, top + 3, MoonConfig.modColorArgb());
      graphics.fill(left + 16, top + 31, left + width - 16, top + 32, MoonConfig.modColorArgb());
   }

   public static void drawSection(GuiGraphicsExtractor graphics, Font font, int centerX, String label, int y) {
      graphics.centeredText(font, label, centerX, y, MoonConfig.modColorArgb());
   }

   public static int mutedColor() {
      int red = ((MoonConfig.modColor >> 16 & 0xFF) + 255) / 2;
      int green = ((MoonConfig.modColor >> 8 & 0xFF) + 255) / 2;
      int blue = ((MoonConfig.modColor & 0xFF) + 255) / 2;
      return 0xFF000000 | red << 16 | green << 8 | blue;
   }

   public static Component colorLabel(Component label) {
      return Component.literal(label.getString()).withStyle(style -> style.withColor(TextColor.fromRgb(MoonConfig.modColor)));
   }
}
