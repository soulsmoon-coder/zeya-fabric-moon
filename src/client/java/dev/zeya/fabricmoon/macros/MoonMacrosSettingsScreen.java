package dev.zeya.fabricmoon.macros;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.MoonScreenStyle;
import dev.zeya.fabricmoon.MoonSettingsScreen;
import dev.zeya.fabricmoon.ZeyaFabricMoonClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonMacrosSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 600;
   private static final int PANEL_HEIGHT = 320;

   public MoonMacrosSettingsScreen() {
      super(Component.literal("Macros"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 320);
      int buttonWidth = 270;
      int columnGap = 16;
      int leftX = centerX - 300 + 20;
      int rightX = leftX + buttonWidth + columnGap;
      int leftCenter = leftX + buttonWidth / 2;
      int rightCenter = rightX + buttonWidth / 2;
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(
               ZeyaFabricMoonClient.toggleLabel("Pesca automatica", MoonConfig.autoFishing)),
               button -> this.toggle(() -> MoonConfig.autoFishing = !MoonConfig.autoFishing)
            )
            .bounds(leftX, top + 48, buttonWidth, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(
               ZeyaFabricMoonClient.toggleLabel("Auto Golden Fish", MoonConfig.autoGoldenFish)),
               button -> this.toggle(() -> MoonConfig.autoGoldenFish = !MoonConfig.autoGoldenFish)
            )
            .bounds(leftX, top + 75, buttonWidth, 22)
            .build()
      );
      this.addFishingDelayControls(leftCenter, top + 108, 0, -1, 1);
      this.addFishingDelayControls(leftCenter, top + 133, 1, -1, 1);
      this.addFishingDelayControls(leftCenter, top + 163, 2, -1, 1);
      this.addFishingDelayControls(leftCenter, top + 188, 3, -1, 1);
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(
               ZeyaFabricMoonClient.toggleLabel("Pausar pesca com menus abertos", MoonConfig.disableFishingWhileOnScreen)),
               button -> this.toggle(() -> MoonConfig.disableFishingWhileOnScreen = !MoonConfig.disableFishingWhileOnScreen)
            )
            .bounds(leftX, top + 211, buttonWidth, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(
               ZeyaFabricMoonClient.toggleLabel("Recuperar oxigenio", MoonConfig.oxygenRecovery)),
               button -> this.toggle(() -> MoonConfig.oxygenRecovery = !MoonConfig.oxygenRecovery)
            )
            .bounds(leftX, top + 239, buttonWidth, 22)
            .build()
      );
      this.addOxygenThresholdControls(leftCenter, top + 266);
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(
               ZeyaFabricMoonClient.toggleLabel("Blaze Dagger: auto-switch", MoonConfig.blazeDaggerAutoSwitch)),
               button -> this.toggle(() -> MoonConfig.blazeDaggerAutoSwitch = !MoonConfig.blazeDaggerAutoSwitch)
            )
            .bounds(rightX, top + 48, buttonWidth, 22)
            .build()
      );
      this.addBlazeDaggerSlotControls(rightCenter, top + 78);
      this.addBlazeDaggerDelayControls(rightCenter, top + 108);
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("Voltar")), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(centerX - 280, top + 306, 560, 22)
            .build()
      );
   }

   private void addFishingDelayControls(int centerX, int y, int setting, int decrement, int increment) {
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("-")), button -> this.changeFishingDelay(setting, decrement)).bounds(centerX - 112, y, 34, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("+")), button -> this.changeFishingDelay(setting, increment)).bounds(centerX + 78, y, 34, 20).build()
      );
   }

   private void addOxygenThresholdControls(int centerX, int y) {
      this.addRenderableWidget(Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("-")), button -> this.changeOxygenThreshold(-10)).bounds(centerX - 112, y, 34, 20).build());
      this.addRenderableWidget(Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("+")), button -> this.changeOxygenThreshold(10)).bounds(centerX + 78, y, 34, 20).build());
   }

   private void toggle(Runnable action) {
      action.run();
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private void changeFishingDelay(int setting, int amount) {
      switch (setting) {
         case 0:
            MoonConfig.fishingCastDelayTicks = MoonConfig.clampDelay(MoonConfig.fishingCastDelayTicks + amount);
            break;
         case 1:
            MoonConfig.minFishWaitSeconds = MoonConfig.clampSeconds(MoonConfig.minFishWaitSeconds + amount);
            break;
         case 2:
            MoonConfig.fishingReelDelayTicks = MoonConfig.clampDelay(MoonConfig.fishingReelDelayTicks + amount);
            break;
         case 3:
            MoonConfig.goldenFishActionDelayTicks = MoonConfig.clampDelay(MoonConfig.goldenFishActionDelayTicks + amount);
            break;
         default:
            throw new IllegalArgumentException("Unknown fishing delay setting: " + setting);
      }

      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private void changeOxygenThreshold(int amount) {
      MoonConfig.oxygenThreshold = MoonConfig.clampOxygen(MoonConfig.oxygenThreshold + amount);
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private void addBlazeDaggerSlotControls(int centerX, int y) {
      int buttonWidth = 34;
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("-")), button -> this.changeBlazeDaggerSlot(-1)).bounds(centerX - 112, y, buttonWidth, 20).build()
      );
      this.addRenderableWidget(Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("+")), button -> this.changeBlazeDaggerSlot(1)).bounds(centerX + 78, y, buttonWidth, 20).build());
   }

   private void addBlazeDaggerDelayControls(int centerX, int y) {
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("-")), button -> this.changeBlazeDaggerDelay(-25))
            .bounds(centerX - 112, y, 34, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(dev.zeya.fabricmoon.MoonScreenStyle.colorLabel(Component.literal("+")), button -> this.changeBlazeDaggerDelay(25))
            .bounds(centerX + 78, y, 34, 20)
            .build()
      );
   }

   private void changeBlazeDaggerSlot(int amount) {
      MoonConfig.blazeDaggerSlot = MoonConfig.clampLoadoutSlot(MoonConfig.blazeDaggerSlot + amount);
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private void changeBlazeDaggerDelay(int amount) {
      MoonConfig.blazeDaggerSwapDelayMs = MoonConfig.clampSwapDelay(MoonConfig.blazeDaggerSwapDelayMs + amount);
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 320);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 600, 320);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 320);
      int leftCenter = centerX - 145;
      int rightCenter = centerX + 145;
      graphics.centeredText(this.font, this.title, centerX, top + 14, MoonConfig.modColorArgb());
      graphics.centeredText(this.font, "Pesca automatica", leftCenter, top + 32, MoonScreenStyle.mutedColor());
      graphics.centeredText(this.font, "Espera entre lancamentos: " + MoonConfig.fishingCastDelayTicks + " ticks", leftCenter, top + 113, MoonConfig.modColorArgb());
      graphics.centeredText(this.font, "Espera minima apos lancar: " + MoonConfig.minFishWaitSeconds + " s", leftCenter, top + 138, MoonConfig.modColorArgb());
      graphics.centeredText(this.font, "Atraso apos fisgada: " + MoonConfig.fishingReelDelayTicks + " ticks", leftCenter, top + 168, MoonConfig.modColorArgb());
      graphics.centeredText(this.font, "Atraso Golden Fish: " + MoonConfig.goldenFishActionDelayTicks + " ticks", leftCenter, top + 193, MoonConfig.modColorArgb());
      graphics.centeredText(this.font, "Oxigenio para subir: " + MoonConfig.oxygenThreshold, leftCenter, top + 271, MoonConfig.modColorArgb());
      graphics.centeredText(this.font, "Blaze Dagger", rightCenter, top + 32, MoonScreenStyle.mutedColor());
      graphics.centeredText(this.font, "Slot configurado: " + MoonConfig.blazeDaggerSlot, rightCenter, top + 83, MoonConfig.modColorArgb());
      graphics.centeredText(this.font, "Swap Delay: " + MoonConfig.clampSwapDelay(MoonConfig.blazeDaggerSwapDelayMs) + " ms", rightCenter, top + 113, MoonConfig.modColorArgb());
      graphics.centeredText(
         this.font, "Macros multiplayer: somente singleplayer ou servidores autorizados no arquivo de config", centerX, top + 294, MoonScreenStyle.mutedColor()
      );
   }
}
