package dev.zeya.fabricmoon;

import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonPlayerSizeSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 340;
   private static final int PANEL_HEIGHT = 300;

   public MoonPlayerSizeSettingsScreen() {
      super(Component.literal("Player Size"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 300);
      int buttonWidth = 280;
      int buttonX = centerX - buttonWidth / 2;
      this.addRenderableWidget(Button.builder(Component.literal("- 0.1"), button -> this.changeSize(-0.1F)).bounds(centerX - 115, top + 64, 70, 22).build());
      this.addRenderableWidget(Button.builder(Component.literal("1.0x"), button -> this.setSize(1.0F)).bounds(centerX - 35, top + 64, 70, 22).build());
      this.addRenderableWidget(Button.builder(Component.literal("+ 0.1"), button -> this.changeSize(0.1F)).bounds(centerX + 45, top + 64, 70, 22).build());
      this.addRenderableWidget(
         Button.builder(ZeyaFabricMoonClient.toggleLabel("Mobs do tamanho do player", MoonConfig.shrinkLargeMobs), button -> this.toggleLargeMobReduction())
            .bounds(buttonX, top + 96, buttonWidth, 24)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(
               Component.literal("Escolher mobs (" + MoonMobSizes.selectedCount() + ")"),
               button -> this.minecraft.gui.setScreen(new MoonMobSizeSettingsScreen())
            )
            .bounds(buttonX, top + 124, buttonWidth, 22)
            .build()
      );
      this.addRenderableWidget(Button.builder(this.perspectiveLabel(), button -> this.togglePerspective()).bounds(buttonX, top + 156, buttonWidth, 24).build());
      this.addRenderableWidget(
         Button.builder(Component.literal("Visao por baixo: " + (MoonConfig.thirdPersonLookFromBelow ? "ON" : "OFF")), button -> this.toggleLookFromBelow())
            .bounds(buttonX, top + 190, buttonWidth, 24)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(buttonX, top + 258, buttonWidth, 22)
            .build()
      );
   }

   private void changeSize(float amount) {
      this.setSize(MoonConfig.playerSize + amount);
   }

   private void setSize(float size) {
      MoonConfig.playerSize = MoonConfig.clampSize(size);
      MoonConfig.save();
   }

   private void toggleLargeMobReduction() {
      MoonConfig.shrinkLargeMobs = !MoonConfig.shrinkLargeMobs;
      if (MoonConfig.shrinkLargeMobs && MoonMobSizes.selectedCount() == 0) {
         MoonMobSizes.selectAllLargerThanPlayer();
      }

      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private Component perspectiveLabel() {
      String perspective = this.minecraft.options.getCameraType() == CameraType.FIRST_PERSON ? "1 Pessoa" : "3 Pessoa";
      return Component.literal("Alternar: 1 Pessoa / 3 Pessoa (atual: " + perspective + ")");
   }

   private void togglePerspective() {
      CameraType cameraType = this.minecraft.options.getCameraType();
      this.minecraft.options.setCameraType(cameraType == CameraType.FIRST_PERSON ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
      this.clearWidgets();
      this.init();
   }

   private void toggleLookFromBelow() {
      MoonConfig.thirdPersonLookFromBelow = !MoonConfig.thirdPersonLookFromBelow;
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 300);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 340, 300);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 300);
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "Tamanho atual: " + String.format(Locale.ROOT, "%.1fx", MoonConfig.playerSize), centerX, top + 45, -4799016);
      graphics.centeredText(this.font, "A escala visual afeta apenas a terceira pessoa.", centerX, top + 224, -7891030);
      graphics.centeredText(
         this.font,
         "Tamanho dos mobs: " + String.format(Locale.ROOT, "%.1fx", MoonConfig.mobSize) + " (ajuste em \"Escolher mobs\")",
         centerX,
         top + 236,
         -7891030
      );
   }
}
