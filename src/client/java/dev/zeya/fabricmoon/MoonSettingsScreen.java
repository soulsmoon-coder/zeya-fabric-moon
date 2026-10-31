package dev.zeya.fabricmoon;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 420;
   private static final int PANEL_HEIGHT = 492;
   private static final int COLUMN_WIDTH = 190;

   public MoonSettingsScreen() {
      super(Component.literal("ZEYA MOON"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 492);
      int leftColumnX = centerX - 190 - 8;
      int rightColumnX = centerX + 8;
      this.addToggle("Mod geral", MoonConfig.featuresEnabled, () -> MoonConfig.featuresEnabled = !MoonConfig.featuresEnabled, leftColumnX, top + 44, 396);
      this.addToggle("HUD de FPS, ping e CPS", MoonConfig.showHud, () -> MoonConfig.showHud = !MoonConfig.showHud, leftColumnX, top + 72, 190);
      this.addRenderableWidget(
         Button.builder(Component.literal("Mover HUD"), button -> ZeyaFabricMoonClient.openHudEditor(this.minecraft))
            .bounds(rightColumnX, top + 72, 190, 22)
            .build()
      );
      this.addToggle("Fullbright", MoonConfig.fullbright, () -> MoonConfig.fullbright = !MoonConfig.fullbright, leftColumnX, top + 98, 190);
      this.addToggle("Esconder armadura", MoonConfig.hideArmor, () -> MoonConfig.hideArmor = !MoonConfig.hideArmor, rightColumnX, top + 98, 190);
      this.addToggle("Esconder item na cabeca", MoonConfig.hideHead, () -> MoonConfig.hideHead = !MoonConfig.hideHead, leftColumnX, top + 124, 190);
      this.addRenderableWidget(
         Button.builder(Component.literal("Macros"), button -> this.minecraft.gui.setScreen(new MoonMacrosSettingsScreen()))
            .bounds(rightColumnX, top + 124, 190, 22)
            .build()
      );
      this.addToggle(
         "Contorno das hitboxes", MoonConfig.entityHitboxes, () -> MoonConfig.entityHitboxes = !MoonConfig.entityHitboxes, leftColumnX, top + 150, 396
      );
      this.addAutoClickControls(leftColumnX, top + 216, false);
      this.addAutoClickControls(rightColumnX, top + 216, true);
      this.addRenderableWidget(
         Button.builder(Component.literal("Player Size"), button -> this.minecraft.gui.setScreen(new MoonPlayerSizeSettingsScreen()))
            .bounds(leftColumnX, top + 288, 190, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Glow / contorno"), button -> this.minecraft.gui.setScreen(new MoonGlowSettingsScreen()))
            .bounds(rightColumnX, top + 288, 190, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Combat HUD"), button -> this.minecraft.gui.setScreen(new MoonCombatHudSettingsScreen()))
            .bounds(leftColumnX, top + 314, 190, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Controle global"), button -> this.minecraft.gui.setScreen(new MoonGlobalSettingsScreen()))
            .bounds(rightColumnX, top + 314, 190, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Slot Bind"), button -> this.minecraft.gui.setScreen(new MoonSlotBindSettingsScreen()))
            .bounds(leftColumnX, top + 360, 190, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Slot Lock"), button -> this.minecraft.gui.setScreen(new MoonSlotLockSettingsScreen()))
            .bounds(rightColumnX, top + 360, 190, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Key Commands"), button -> this.minecraft.gui.setScreen(new MoonKeyCommandsSettingsScreen()))
            .bounds(leftColumnX, top + 386, 396, 22)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Wardrobe / Loadout"), button -> this.minecraft.gui.setScreen(new MoonWardrobeSettingsScreen()))
            .bounds(leftColumnX, top + 412, 190, 22)
            .build()
      );
      this.addRenderableWidget(Button.builder(Component.literal("Fechar"), button -> this.onClose()).bounds(rightColumnX, top + 412, 190, 22).build());
   }

   private void addAutoClickControls(int x, int y, boolean rightButton) {
      boolean enabled = rightButton ? MoonConfig.rightAutoClick : MoonConfig.leftAutoClick;
      String name = rightButton ? "Auto-click direito" : "Auto-click esquerdo";
      this.addToggle(name, enabled, () -> {
         if (rightButton) {
            MoonConfig.rightAutoClick = !MoonConfig.rightAutoClick;
         } else {
            MoonConfig.leftAutoClick = !MoonConfig.leftAutoClick;
         }
      }, x, y, 190);
      this.addRenderableWidget(Button.builder(Component.literal("-"), button -> this.changeCps(rightButton, -1)).bounds(x + 36, y + 26, 44, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("+"), button -> this.changeCps(rightButton, 1)).bounds(x + 110, y + 26, 44, 20).build());
   }

   private void addToggle(String label, boolean enabled, Runnable toggle, int x, int y, int width) {
      this.addRenderableWidget(Button.builder(ZeyaFabricMoonClient.toggleLabel(label, enabled), button -> {
         toggle.run();
         MoonConfig.save();
         this.clearWidgets();
         this.init();
      }).bounds(x, y, width, 22).build());
   }

   private void changeCps(boolean rightButton, int amount) {
      if (rightButton) {
         MoonConfig.rightCps = MoonConfig.clampCps(MoonConfig.rightCps + amount);
      } else {
         MoonConfig.leftCps = MoonConfig.clampCps(MoonConfig.leftCps + amount);
      }

      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, MoonScreenStyle.panelTop(this.height, 492), 420, 492);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 492);
      graphics.centeredText(this.font, this.title, centerX, top + 13, -1);
      MoonScreenStyle.drawSection(graphics, this.font, centerX, "CONTROLES E VISUAL", top + 66);
      MoonScreenStyle.drawSection(graphics, this.font, centerX, "AUTO-CLICK", top + 208);
      MoonScreenStyle.drawSection(graphics, this.font, centerX, "FERRAMENTAS", top + 280);
      MoonScreenStyle.drawSection(graphics, this.font, centerX, "ATALHOS", top + 352);
      graphics.centeredText(this.font, MoonConfig.leftCps + " CPS", centerX - 190 - 8 + 95, top + 268, -4799016);
      graphics.centeredText(this.font, MoonConfig.rightCps + " CPS", centerX + 8 + 95, top + 268, -4799016);
      graphics.centeredText(this.font, "Pressione K para abrir este menu", centerX, top + 464, MoonScreenStyle.mutedColor());
   }
}
