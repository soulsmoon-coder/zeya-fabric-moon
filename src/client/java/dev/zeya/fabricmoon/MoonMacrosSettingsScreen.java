package dev.zeya.fabricmoon;

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
         Button.builder(
               ZeyaFabricMoonClient.toggleLabel("Pesca automatica", MoonConfig.autoFishing),
               button -> this.toggle(() -> MoonConfig.autoFishing = !MoonConfig.autoFishing)
            )
            .bounds(leftX, top + 48, buttonWidth, 22)
            .build()
      );
      this.addFishingDelayControls(leftCenter, top + 83, 0, -1, 1);
      this.addFishingDelayControls(leftCenter, top + 108, 1, -1, 1);
      this.addFishingDelayControls(leftCenter, top + 138, 2, -5, 5);
      this.addRenderableWidget(
         Button.builder(
               ZeyaFabricMoonClient.toggleLabel("Pausar pesca com menus abertos", MoonConfig.disableFishingWhileOnScreen),
               button -> this.toggle(() -> MoonConfig.disableFishingWhileOnScreen = !MoonConfig.disableFishingWhileOnScreen)
            )
            .bounds(leftX, top + 172, buttonWidth, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(
               ZeyaFabricMoonClient.toggleLabel("Recuperar oxigenio", MoonConfig.oxygenRecovery),
               button -> this.toggle(() -> MoonConfig.oxygenRecovery = !MoonConfig.oxygenRecovery)
            )
            .bounds(leftX, top + 202, buttonWidth, 22)
            .build()
      );
      this.addOxygenThresholdControls(leftCenter, top + 232);
      this.addRenderableWidget(
         Button.builder(
               ZeyaFabricMoonClient.toggleLabel("Bonemerang: trocar loadout", MoonConfig.bonemerangLoadoutSwap),
               button -> this.toggle(() -> MoonConfig.bonemerangLoadoutSwap = !MoonConfig.bonemerangLoadoutSwap)
            )
            .bounds(rightX, top + 48, buttonWidth, 22)
            .build()
      );
      this.addLoadoutSlotControls(rightCenter, top + 78, true);
      this.addRenderableWidget(
         Button.builder(
               ZeyaFabricMoonClient.toggleLabel("Death Bow: trocar loadout", MoonConfig.deathBowLoadoutSwap),
               button -> this.toggle(() -> MoonConfig.deathBowLoadoutSwap = !MoonConfig.deathBowLoadoutSwap)
            )
            .bounds(rightX, top + 118, buttonWidth, 22)
            .build()
      );
      this.addLoadoutSlotControls(rightCenter, top + 148, false);
      this.addRenderableWidget(
         Button.builder(
               ZeyaFabricMoonClient.toggleLabel("Blaze Dagger: auto-switch", MoonConfig.blazeDaggerAutoSwitch),
               button -> this.toggle(() -> MoonConfig.blazeDaggerAutoSwitch = !MoonConfig.blazeDaggerAutoSwitch)
            )
            .bounds(rightX, top + 192, buttonWidth, 22)
            .build()
      );
      this.addBlazeDaggerSlotControls(rightCenter, top + 222);
      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(centerX - 280, top + 306, 560, 22)
            .build()
      );
      this.addRenderableWidget(Button.builder(Component.literal("-"), button -> this.changeMacroDelay(-25)).bounds(centerX - 112, top + 266, 34, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("+"), button -> this.changeMacroDelay(25)).bounds(centerX + 78, top + 266, 34, 20).build());
   }

   private void addFishingDelayControls(int centerX, int y, int setting, int decrement, int increment) {
      this.addRenderableWidget(
         Button.builder(Component.literal("-"), button -> this.changeFishingDelay(setting, decrement)).bounds(centerX - 112, y, 34, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("+"), button -> this.changeFishingDelay(setting, increment)).bounds(centerX + 78, y, 34, 20).build()
      );
   }

   private void addLoadoutSlotControls(int centerX, int y, boolean bonemerang) {
      int buttonWidth = 34;
      this.addRenderableWidget(
         Button.builder(Component.literal("-"), button -> this.changeLoadoutSlot(bonemerang, -1)).bounds(centerX - 112, y, buttonWidth, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("+"), button -> this.changeLoadoutSlot(bonemerang, 1)).bounds(centerX + 78, y, buttonWidth, 20).build()
      );
   }

   private void addOxygenThresholdControls(int centerX, int y) {
      this.addRenderableWidget(Button.builder(Component.literal("-"), button -> this.changeOxygenThreshold(-10)).bounds(centerX - 112, y, 34, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("+"), button -> this.changeOxygenThreshold(10)).bounds(centerX + 78, y, 34, 20).build());
   }

   private void toggle(Runnable action) {
      action.run();
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private void changeLoadoutSlot(boolean bonemerang, int amount) {
      if (bonemerang) {
         MoonConfig.bonemerangLoadoutSlot = MoonConfig.clampLoadoutSlot(MoonConfig.bonemerangLoadoutSlot + amount);
      } else {
         MoonConfig.deathBowLoadoutSlot = MoonConfig.clampLoadoutSlot(MoonConfig.deathBowLoadoutSlot + amount);
      }

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
            MoonConfig.fishingReelDelaySeconds = MoonConfig.clampSeconds(MoonConfig.fishingReelDelaySeconds + amount);
            break;
         case 2:
            MoonConfig.minFishWaitTicks = MoonConfig.clampDelay(MoonConfig.minFishWaitTicks + amount);
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
         Button.builder(Component.literal("-"), button -> this.changeBlazeDaggerSlot(-1)).bounds(centerX - 112, y, buttonWidth, 20).build()
      );
      this.addRenderableWidget(Button.builder(Component.literal("+"), button -> this.changeBlazeDaggerSlot(1)).bounds(centerX + 78, y, buttonWidth, 20).build());
   }

   private void changeBlazeDaggerSlot(int amount) {
      MoonConfig.blazeDaggerSlot = MoonConfig.clampLoadoutSlot(MoonConfig.blazeDaggerSlot + amount);
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private void changeMacroDelay(int amount) {
      MoonConfig.macroExtraDelayMillis = MoonConfig.clampMacroDelay(MoonConfig.macroExtraDelayMillis + amount);
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
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "Pesca automatica", leftCenter, top + 32, MoonScreenStyle.mutedColor());
      graphics.centeredText(this.font, "Espera entre lancamentos: " + MoonConfig.fishingCastDelayTicks + " ticks", leftCenter, top + 88, -4799016);
      graphics.centeredText(this.font, "Espera apos lancar: " + MoonConfig.fishingReelDelaySeconds + " s", leftCenter, top + 113, -4799016);
      graphics.centeredText(this.font, "Espera minima: " + MoonConfig.minFishWaitTicks + " ticks", leftCenter, top + 143, -4799016);
      graphics.centeredText(this.font, "Oxigenio para subir: " + MoonConfig.oxygenThreshold, leftCenter, top + 237, -4799016);
      graphics.centeredText(this.font, "Troca de loadout", rightCenter, top + 32, MoonScreenStyle.mutedColor());
      graphics.centeredText(this.font, "Loadout: " + MoonConfig.bonemerangLoadoutSlot, rightCenter, top + 83, -4799016);
      graphics.centeredText(this.font, "Loadout: " + MoonConfig.deathBowLoadoutSlot, rightCenter, top + 153, -4799016);
      graphics.centeredText(this.font, "Blaze Dagger", rightCenter, top + 176, MoonScreenStyle.mutedColor());
      graphics.centeredText(this.font, "Slot configurado: " + MoonConfig.blazeDaggerSlot, rightCenter, top + 227, -4799016);
      graphics.centeredText(this.font, "Intervalo extra do auto-click: " + MoonConfig.macroExtraDelayMillis + " ms", centerX, top + 258, -4799016);
      graphics.centeredText(
         this.font, "Macros multiplayer: somente singleplayer ou servidores autorizados no arquivo de config", centerX, top + 294, MoonScreenStyle.mutedColor()
      );
   }
}
