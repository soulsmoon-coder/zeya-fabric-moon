package dev.zeya.fabricmoon;

import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.BeforeKeyPress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class MoonKeyCommandsKeySelectScreen extends Screen implements BeforeKeyPress {
   private static final int PANEL_WIDTH = 400;
   private static final int PANEL_HEIGHT = 280;
   private final int bindingIndex;
   private final String existingCommand;
   private int selectedKey = -1;
   private boolean waitingForKey = false;
   private boolean keyListenerRegistered;
   private EditBox commandBox;

   public MoonKeyCommandsKeySelectScreen(int index, String existingCommand) {
      super(Component.literal("Selecionar tecla"));
      this.bindingIndex = index;
      this.existingCommand = existingCommand;
   }

   protected void init() {
      if (!this.keyListenerRegistered) {
         ScreenKeyboardEvents.beforeKeyPress(this).register(this);
         this.keyListenerRegistered = true;
      }

      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 280);
      int buttonWidth = 340;
      int buttonX = centerX - buttonWidth / 2;
      this.addRenderableWidget(
         Button.builder(Component.literal(this.waitingForKey ? "Aguardando tecla..." : "Pressione aqui e depois aperte uma tecla"), button -> {
            this.waitingForKey = true;
            this.selectedKey = -1;
            this.clearWidgets();
            this.init();
         }).bounds(buttonX, top + 60, buttonWidth, 22).build()
      );
      this.commandBox = new EditBox(this.font, buttonX, top + 110, buttonWidth, 22, Component.literal("Comando:"));
      this.commandBox.setHint(Component.literal("sb level"));
      this.commandBox.setValue(this.existingCommand);
      this.addRenderableWidget(this.commandBox);
      this.addRenderableWidget(
         Button.builder(Component.literal("Salvar"), button -> this.saveBinding()).bounds(buttonX, top + 150, buttonWidth / 2 - 4, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Cancelar"), button -> this.minecraft.gui.setScreen(new MoonKeyCommandsSettingsScreen()))
            .bounds(buttonX + buttonWidth / 2 + 2, top + 150, buttonWidth / 2 - 4, 22)
            .build()
      );
   }

   public void beforeKeyPress(Screen screen, KeyEvent event) {
      if (this.waitingForKey) {
         int keyCode = event.key();
         if (keyCode == 256 || keyCode == 258) {
            this.waitingForKey = false;
            this.clearWidgets();
            this.init();
            return;
         }

         this.selectedKey = keyCode;
         this.waitingForKey = false;
         this.clearWidgets();
         this.init();
      }
   }

   private void saveBinding() {
      if (this.selectedKey >= 0) {
         String command = this.commandBox.getValue().trim();
         if (!command.isEmpty()) {
            if (!command.startsWith("/")) {
               command = "/" + command;
            }

            String binding = this.selectedKey + "=" + command;
            if (this.bindingIndex < 0) {
               MoonConfig.keyCommandBindings.add(binding);
            } else {
               MoonConfig.keyCommandBindings.set(this.bindingIndex, binding);
            }

            MoonConfig.save();
            MoonKeyCommands.refreshKeyMappings();
            this.minecraft.gui.setScreen(new MoonKeyCommandsSettingsScreen());
         }
      }
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 280);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 400, 280);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 280);
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "1. Clique no botao e pressione qualquer tecla", centerX, top + 46, MoonScreenStyle.mutedColor());
      graphics.centeredText(this.font, "2. Digite o comando", centerX, top + 100, MoonScreenStyle.mutedColor());
      if (this.selectedKey >= 0) {
         String keyName = MoonKeyCommands.getKeyDisplayName(this.selectedKey);
         graphics.centeredText(this.font, "Tecla selecionada: " + keyName, centerX, top + 176, -9251384);
      }
   }
}
