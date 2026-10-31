package dev.zeya.fabricmoon;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MoonKeyCommandsSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 400;
   private static final int ITEM_HEIGHT = 28;
   private int editingIndex = -1;
   private int selectedKeyCode = -1;
   private String statusText = "";

   public MoonKeyCommandsSettingsScreen() {
      super(Component.literal("Key Commands"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, this.calculatePanelHeight());
      int buttonWidth = 340;
      int buttonX = centerX - buttonWidth / 2;
      this.addRenderableWidget(Button.builder(ZeyaFabricMoonClient.toggleLabel("Key Commands: habilitado", MoonConfig.keyCommandsEnabled), button -> {
         MoonConfig.keyCommandsEnabled = !MoonConfig.keyCommandsEnabled;
         MoonConfig.save();
         this.clearWidgets();
         this.init();
      }).bounds(buttonX, top + 48, buttonWidth, 22).build());
      this.addRenderableWidget(
         Button.builder(Component.literal("+ Adicionar comando"), button -> this.startNewBinding()).bounds(buttonX, top + 78, buttonWidth, 22).build()
      );
      List<String> bindings = MoonConfig.keyCommandBindings;
      int y = top + 132;

      for (int i = 0; i < bindings.size(); i++) {
         int index = i;
         String binding = bindings.get(i);
         int eq = binding.indexOf(61);
         String displayName = "";
         if (eq > 0 && eq < binding.length() - 1) {
            String keyName = MoonKeyCommands.getKeyDisplayName(binding.substring(0, eq).trim());
            String command = binding.substring(eq + 1).trim();
            displayName = keyName + " → " + command;
         }

         this.addRenderableWidget(
            Button.builder(Component.literal(displayName), button -> this.editBinding(index)).bounds(buttonX, y, buttonWidth - 50, 22).build()
         );
         this.addRenderableWidget(
            Button.builder(Component.literal("X"), button -> this.removeBinding(index)).bounds(buttonX + buttonWidth - 46, y, 40, 22).build()
         );
         y += 28;
      }

      int bottomY = Math.max(y + 20, top + 220);
      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(buttonX, bottomY, buttonWidth, 22)
            .build()
      );
   }

   private void startNewBinding() {
      this.minecraft.gui.setScreen(new MoonKeyCommandsKeySelectScreen(-1, ""));
   }

   private void editBinding(int index) {
      String binding = MoonConfig.keyCommandBindings.get(index);
      int eq = binding.indexOf(61);
      if (eq > 0 && eq < binding.length() - 1) {
         int keyCode = Integer.parseInt(binding.substring(0, eq).trim());
         String command = binding.substring(eq + 1).trim();
         this.minecraft.gui.setScreen(new MoonKeyCommandsKeySelectScreen(index, command));
      }
   }

   private void removeBinding(int index) {
      MoonConfig.keyCommandBindings.remove(index);
      MoonConfig.save();
      MoonKeyCommands.refreshKeyMappings();
      this.clearWidgets();
      this.init();
   }

   private int calculatePanelHeight() {
      int bindingsCount = MoonConfig.keyCommandBindings.size();
      int listHeight = Math.max(0, bindingsCount * 28);
      return Math.max(320, 280 + listHeight);
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, this.calculatePanelHeight());
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 400, this.calculatePanelHeight());
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, this.calculatePanelHeight());
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "Vincular teclas a comandos", centerX, top + 30, MoonScreenStyle.mutedColor());
      int count = MoonConfig.keyCommandBindings.size();
      graphics.centeredText(this.font, "Comandos vinculados: " + count, centerX, top + 108, -4799016);
      int y = top + 156;
      if (count == 0) {
         graphics.centeredText(this.font, "Nenhum comando vinculado.", centerX, y, MoonScreenStyle.mutedColor());
         graphics.centeredText(this.font, "Clique em \"+ Adicionar comando\" para", centerX, y + 16, MoonScreenStyle.mutedColor());
         graphics.centeredText(this.font, "criar um novo vinculo de tecla.", centerX, y + 32, MoonScreenStyle.mutedColor());
      }
   }
}
