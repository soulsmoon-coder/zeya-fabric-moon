package dev.zeya.fabricmoon;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonGlowSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 320;
   private static final int PANEL_HEIGHT = 340;
   private static final List<MoonGlowSettingsScreen.ColorPreset> COLOR_PRESETS = List.of(
      new MoonGlowSettingsScreen.ColorPreset("Aqua", 65535),
      new MoonGlowSettingsScreen.ColorPreset("Black", 0),
      new MoonGlowSettingsScreen.ColorPreset("Blue", 255),
      new MoonGlowSettingsScreen.ColorPreset("Fuchsia", 16711935),
      new MoonGlowSettingsScreen.ColorPreset("Gray", 8421504),
      new MoonGlowSettingsScreen.ColorPreset("Green", 32768),
      new MoonGlowSettingsScreen.ColorPreset("Lime", 65280),
      new MoonGlowSettingsScreen.ColorPreset("Maroon", 8388608),
      new MoonGlowSettingsScreen.ColorPreset("Navy", 128),
      new MoonGlowSettingsScreen.ColorPreset("Olive", 8421376),
      new MoonGlowSettingsScreen.ColorPreset("Orange", 16753920),
      new MoonGlowSettingsScreen.ColorPreset("Purple", 8388736),
      new MoonGlowSettingsScreen.ColorPreset("Red", 16711680),
      new MoonGlowSettingsScreen.ColorPreset("Silver", 12632256),
      new MoonGlowSettingsScreen.ColorPreset("Teal", 32896),
      new MoonGlowSettingsScreen.ColorPreset("White", 16777215),
      new MoonGlowSettingsScreen.ColorPreset("Yellow", 16776960)
   );

   public MoonGlowSettingsScreen() {
      super(Component.literal("Glow / contorno do jogador"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 340);
      this.addRenderableWidget(Button.builder(ZeyaFabricMoonClient.toggleLabel("Contorno", MoonConfig.playerGlow), button -> {
         MoonConfig.playerGlow = !MoonConfig.playerGlow;
         MoonConfig.save();
         this.clearWidgets();
         this.init();
      }).bounds(centerX - 135, top + 48, 270, 24).build());
      this.addChannelControls(centerX, top + 88, 0);
      this.addChannelControls(centerX, top + 128, 1);
      this.addChannelControls(centerX, top + 168, 2);
      this.addRenderableWidget(Button.builder(Component.literal("Previous"), button -> this.cyclePreset(-1)).bounds(centerX - 115, top + 218, 100, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Next"), button -> this.cyclePreset(1)).bounds(centerX + 15, top + 218, 100, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(centerX - 135, top + 294, 270, 22)
            .build()
      );
   }

   private void addChannelControls(int centerX, int y, int channel) {
      this.addColorButton(centerX - 128, y, "-16", channel, -16);
      this.addColorButton(centerX - 84, y, "-1", channel, -1);
      this.addColorButton(centerX + 46, y, "+1", channel, 1);
      this.addColorButton(centerX + 90, y, "+16", channel, 16);
   }

   private void addColorButton(int x, int y, String label, int channel, int amount) {
      this.addRenderableWidget(Button.builder(Component.literal(label), button -> this.changeColor(channel, amount)).bounds(x, y, 38, 20).build());
   }

   private void changeColor(int channel, int amount) {
      int shift = (2 - channel) * 8;
      int component = MoonConfig.playerGlowColor >> shift & 0xFF;
      int updated = Math.max(0, Math.min(255, component + amount));
      int mask = 255 << shift;
      MoonConfig.playerGlowColor = MoonConfig.clampColor(MoonConfig.playerGlowColor & ~mask | updated << shift);
      MoonConfig.save();
   }

   private void cyclePreset(int direction) {
      int currentIndex = -1;

      for (int index = 0; index < COLOR_PRESETS.size(); index++) {
         if (COLOR_PRESETS.get(index).rgb() == MoonConfig.playerGlowColor) {
            currentIndex = index;
            break;
         }
      }

      if (currentIndex == -1) {
         currentIndex = direction > 0 ? -1 : 0;
      }

      int nextIndex = Math.floorMod(currentIndex + direction, COLOR_PRESETS.size());
      MoonConfig.playerGlowColor = COLOR_PRESETS.get(nextIndex).rgb();
      MoonConfig.save();
   }

   private String currentColorName() {
      for (MoonGlowSettingsScreen.ColorPreset preset : COLOR_PRESETS) {
         if (preset.rgb() == MoonConfig.playerGlowColor) {
            return preset.name();
         }
      }

      return "Custom";
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 340);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 320, 340);
      int color = 0xFF000000 | MoonConfig.playerGlowColor;
      graphics.fill(this.width / 2 - 30, top + 250, this.width / 2 + 30, top + 270, color);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 340);
      int color = MoonConfig.playerGlowColor;
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "Ajuste RGB para selecionar qualquer cor.", centerX, top + 34, -4799016);
      graphics.centeredText(this.font, "R: " + (color >> 16 & 0xFF), centerX, top + 93, -34953);
      graphics.centeredText(this.font, "G: " + (color >> 8 & 0xFF), centerX, top + 133, -8913033);
      graphics.centeredText(this.font, "B: " + (color & 0xFF), centerX, top + 173, -8947713);
      graphics.centeredText(this.font, "Color: " + this.currentColorName(), centerX, top + 202, -1);
      graphics.centeredText(this.font, String.format(Locale.ROOT, "#%06X", color), centerX, top + 274, -1);
      graphics.centeredText(this.font, "Visual local; nao altera dados do servidor.", centerX, top + 330, -7891030);
   }

   private record ColorPreset(String name, int rgb) {
   }
}
