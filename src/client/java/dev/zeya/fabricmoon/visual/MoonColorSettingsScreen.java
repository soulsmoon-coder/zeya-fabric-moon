package dev.zeya.fabricmoon.visual;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.MoonScreenStyle;
import dev.zeya.fabricmoon.MoonSettingsScreen;
import java.awt.Color;
import java.util.Locale;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class MoonColorSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 340;
   private static final int PANEL_HEIGHT = 270;
   private static final int PICKER_WIDTH = 180;
   private static final int PICKER_HEIGHT = 100;
   private static final int HUE_WIDTH = 12;
   private final Screen parent;
   private final IntConsumer onApply;
   private float hue;
   private float saturation;
   private float brightness;
   private boolean draggingPicker;
   private boolean draggingHue;

   public MoonColorSettingsScreen() {
      this("Seletor de cor global", MoonConfig.modColor, new MoonSettingsScreen(), MoonConfig::setGlobalColor);
   }

   public MoonColorSettingsScreen(String title, int initialColor, Screen parent, IntConsumer onApply) {
      super(Component.literal(title));
      this.parent = parent;
      this.onApply = onApply;
      float[] hsb = Color.RGBtoHSB(
         initialColor >> 16 & 0xFF,
         initialColor >> 8 & 0xFF,
         initialColor & 0xFF,
         null
      );
      this.hue = hsb[0];
      this.saturation = hsb[1];
      this.brightness = hsb[2];
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      this.addRenderableWidget(
         Button.builder(MoonScreenStyle.colorLabel(Component.literal("Aplicar")), button -> {
            this.onApply.accept(this.currentRgb());
            MoonConfig.save();
            this.minecraft.gui.setScreen(this.parent);
         }).bounds(centerX - 106, top + 224, 100, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(MoonScreenStyle.colorLabel(Component.literal("Cancelar")), button -> this.minecraft.gui.setScreen(this.parent))
            .bounds(centerX + 6, top + 224, 100, 22)
            .build()
      );
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0) {
         int left = this.pickerLeft();
         int top = this.pickerTop();
         if (inside(event.x(), event.y(), left, top, PICKER_WIDTH, PICKER_HEIGHT)) {
            this.draggingPicker = true;
            this.updatePicker(event.x(), event.y());
            return true;
         }

         int hueX = left + PICKER_WIDTH + 8;
         if (inside(event.x(), event.y(), hueX, top, HUE_WIDTH, PICKER_HEIGHT)) {
            this.draggingHue = true;
            this.updateHue(event.y());
            return true;
         }
      }

      return super.mouseClicked(event, doubleClick);
   }

   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      if (event.button() == 0 && this.draggingPicker) {
         this.updatePicker(event.x(), event.y());
         return true;
      }

      if (event.button() == 0 && this.draggingHue) {
         this.updateHue(event.y());
         return true;
      }

      return super.mouseDragged(event, deltaX, deltaY);
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      if (event.button() == 0 && (this.draggingPicker || this.draggingHue)) {
         this.draggingPicker = false;
         this.draggingHue = false;
         return true;
      }

      return super.mouseReleased(event);
   }

   private void updatePicker(double mouseX, double mouseY) {
      this.saturation = Mth.clamp((float)((mouseX - this.pickerLeft()) / PICKER_WIDTH), 0.0F, 1.0F);
      this.brightness = 1.0F - Mth.clamp((float)((mouseY - this.pickerTop()) / PICKER_HEIGHT), 0.0F, 1.0F);
   }

   private void updateHue(double mouseY) {
      this.hue = 1.0F - Mth.clamp((float)((mouseY - this.pickerTop()) / PICKER_HEIGHT), 0.0F, 1.0F);
   }

   private int currentRgb() {
      return Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 0xFFFFFF;
   }

   private int pickerLeft() {
      return (this.width - (PICKER_WIDTH + HUE_WIDTH + 8)) / 2;
   }

   private int pickerTop() {
      return MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT) + 72;
   }

   private static boolean inside(double x, double y, int left, int top, int width, int height) {
      return x >= left && x <= left + width && y >= top && y <= top + height;
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int panelTop = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      int left = this.pickerLeft();
      int top = this.pickerTop();
      MoonScreenStyle.drawPanel(graphics, this.width / 2, panelTop, PANEL_WIDTH, PANEL_HEIGHT);

      graphics.fill(left, top, left + PICKER_WIDTH, top + PICKER_HEIGHT, Color.HSBtoRGB(this.hue, 1.0F, 1.0F));
      for (int x = 0; x < PICKER_WIDTH; x++) {
         int alpha = Math.round(255.0F * (1.0F - x / (float)(PICKER_WIDTH - 1)));
         graphics.fill(left + x, top, left + x + 1, top + PICKER_HEIGHT, alpha << 24 | 0xFFFFFF);
      }

      for (int y = 0; y < PICKER_HEIGHT; y++) {
         int alpha = Math.round(255.0F * y / (PICKER_HEIGHT - 1));
         graphics.fill(left, top + y, left + PICKER_WIDTH, top + y + 1, alpha << 24);
         graphics.fill(
            left + PICKER_WIDTH + 8,
            top + y,
            left + PICKER_WIDTH + 8 + HUE_WIDTH,
            top + y + 1,
            Color.HSBtoRGB(1.0F - y / (float)(PICKER_HEIGHT - 1), 1.0F, 1.0F)
         );
      }

      int selectorX = left + Math.round(this.saturation * (PICKER_WIDTH - 1));
      int selectorY = top + Math.round((1.0F - this.brightness) * (PICKER_HEIGHT - 1));
      graphics.fill(selectorX - 2, selectorY - 2, selectorX + 3, selectorY + 3, 0xFF000000);
      graphics.fill(selectorX - 1, selectorY - 1, selectorX + 2, selectorY + 2, -1);
      int hueY = top + Math.round((1.0F - this.hue) * (PICKER_HEIGHT - 1));
      graphics.fill(left + PICKER_WIDTH + 6, hueY - 1, left + PICKER_WIDTH + 8 + HUE_WIDTH, hueY + 2, -1);
      graphics.fill(this.width / 2 - 24, panelTop + 190, this.width / 2 + 24, panelTop + 210, 0xFF000000 | this.currentRgb());
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, PANEL_HEIGHT);
      graphics.centeredText(this.font, this.title, centerX, top + 14, MoonConfig.modColorArgb());
      graphics.centeredText(
         this.font,
         String.format(Locale.ROOT, "#%06X", this.currentRgb()),
         centerX,
         top + 190,
         0xFFFFFFFF
      );
   }
}
