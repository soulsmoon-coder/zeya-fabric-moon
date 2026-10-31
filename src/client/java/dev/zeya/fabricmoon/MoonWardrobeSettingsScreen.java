package dev.zeya.fabricmoon;

import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.BeforeKeyPress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class MoonWardrobeSettingsScreen extends Screen implements BeforeKeyPress {
   private static final int PANEL_WIDTH = 660;
   private static final int PANEL_HEIGHT = 630;
   private int selectedSlot = 1;
   private String statusMessage = "";
   private int waitingForSlot = -1;
   private String waitingType = "quickSwapWardrobe";
   private boolean keyListenerRegistered = false;

   public MoonWardrobeSettingsScreen() {
      super(Component.literal("Gerenciamento de Equipamento"));
   }

   protected void init() {
      super.init();
      if (!this.keyListenerRegistered) {
         ScreenKeyboardEvents.beforeKeyPress(this).register(this);
         this.keyListenerRegistered = true;
      }

      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 630);
      int buttonWidth = 290;
      int buttonX = centerX - 320;
      int rightX = centerX + 20;
      this.addRenderableWidget(Button.builder(Component.literal("Equip/Load/Wardrobe: " + (MoonConfig.wardrobeEnabled ? "ON" : "OFF")), button -> {
         MoonConfig.wardrobeEnabled = !MoonConfig.wardrobeEnabled;
         MoonConfig.save();
         this.clearWidgets();
         this.init();
      }).bounds(buttonX, top + 40, buttonWidth, 22).build());
      this.addRenderableWidget(
         Button.builder(Component.literal("Salvar armadura atual (slot " + this.selectedSlot + ")"), button -> this.saveCurrentArmor())
            .bounds(buttonX, top + 100, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Salvar equipamento de mão (slot " + this.selectedSlot + ")"), button -> this.saveCurrentEquipment())
            .bounds(buttonX, top + 142, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Salvar loadout completo (slot " + this.selectedSlot + ")"), button -> this.saveFullLoadout())
            .bounds(buttonX, top + 184, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
         this.selectedSlot = Math.max(1, this.selectedSlot - 1);
         this.clearWidgets();
         this.init();
      }).bounds(buttonX + 40, top + 232, 50, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.literal("Slot: " + this.selectedSlot + " / 12"), button -> {}).bounds(buttonX + 94, top + 232, 100, 20).build()
      );
      this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
         this.selectedSlot = Math.min(12, this.selectedSlot + 1);
         this.clearWidgets();
         this.init();
      }).bounds(buttonX + 198, top + 232, 50, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.literal("Carregar loadout do slot " + this.selectedSlot), button -> this.loadLoadoutInfo())
            .bounds(buttonX, top + 292, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Remover loadout do slot " + this.selectedSlot), button -> this.removeLoadout())
            .bounds(buttonX, top + 322, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Ver loadouts salvos (" + MoonWardrobeManager.getLoadoutCount() + ")"), button -> this.showLoadoutsSummary())
            .bounds(buttonX, top + 352, buttonWidth, 20)
            .build()
      );
      String toggleText = MoonConfig.wardrobeQuickSwapEnabled ? "Swap Rápido: ON" : "Swap Rápido: OFF";
      this.addRenderableWidget(Button.builder(Component.literal(toggleText), button -> {
         MoonConfig.wardrobeQuickSwapEnabled = !MoonConfig.wardrobeQuickSwapEnabled;
         MoonConfig.save();
         this.clearWidgets();
         this.init();
      }).bounds(rightX, top + 40, 200, 20).build());
      int slotButtonWidth = 96;
      int slotButtonHeight = 20;
      int colGap = 10;
      int rowGap = 4;
      int gridStartX = rightX;
      int wardrobeGridStartY = top + 70;

      for (int i = 0; i < 9; i++) {
         int slotIndex = i;
         int col = slotIndex % 3;
         int row = slotIndex / 3;
         int x = gridStartX + col * (slotButtonWidth + colGap);
         int y = wardrobeGridStartY + row * (slotButtonHeight + rowGap);
         int slotNum = slotIndex + 1;
         int keyCode = MoonConfig.wardrobeQuickSwapKeysWardrobe[slotIndex];
         String keyText = getKeyName(keyCode);
         boolean isWaiting = this.waitingForSlot == slotIndex && this.waitingType.equals("quickSwapWardrobe");
         String label = "W" + slotNum + ": " + keyText;
         if (isWaiting) {
            label = "W" + slotNum + ": pressionar...";
         }

         this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
            if (isWaiting) {
               this.waitingForSlot = -1;
               this.clearWidgets();
               this.init();
            } else {
               this.waitingForSlot = slotIndex;
               this.waitingType = "quickSwapWardrobe";
               this.statusMessage = "Wardrobe Slot " + slotNum + ": pressione qualquer tecla...";
               this.clearWidgets();
               this.init();
            }
         }).bounds(x, y, slotButtonWidth, slotButtonHeight).build());
      }

      int equipGridStartY = wardrobeGridStartY + 3 * (slotButtonHeight + rowGap) + 16;

      for (int i = 0; i < 9; i++) {
         int slotIndex = i;
         int col = slotIndex % 3;
         int row = slotIndex / 3;
         int x = gridStartX + col * (slotButtonWidth + colGap);
         int y = equipGridStartY + row * (slotButtonHeight + rowGap);
         int slotNum = slotIndex + 1;
         int keyCode = MoonConfig.wardrobeQuickSwapKeysEquipment[slotIndex];
         String keyText = getKeyName(keyCode);
         boolean isWaiting = this.waitingForSlot == slotIndex && this.waitingType.equals("quickSwapEquipment");
         String label = "E" + slotNum + ": " + keyText;
         if (isWaiting) {
            label = "E" + slotNum + ": pressionar...";
         }

         this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
            if (isWaiting) {
               this.waitingForSlot = -1;
               this.clearWidgets();
               this.init();
            } else {
               this.waitingForSlot = slotIndex;
               this.waitingType = "quickSwapEquipment";
               this.statusMessage = "Equipment Slot " + slotNum + ": pressione qualquer tecla...";
               this.clearWidgets();
               this.init();
            }
         }).bounds(x, y, slotButtonWidth, slotButtonHeight).build());
      }

      int loadoutGridStartY = equipGridStartY + 3 * (slotButtonHeight + rowGap) + 16;

      for (int i = 0; i < 12; i++) {
         int slotIndex = i;
         int col = slotIndex % 3;
         int row = slotIndex / 3;
         int x = gridStartX + col * (slotButtonWidth + colGap);
         int y = loadoutGridStartY + row * (slotButtonHeight + rowGap);
         int slotNum = slotIndex + 1;
         int keyCode = MoonConfig.wardrobeQuickSwapKeysLoadout[slotIndex];
         String keyText = getKeyName(keyCode);
         boolean isWaiting = this.waitingForSlot == slotIndex && this.waitingType.equals("quickSwapLoadout");
         String label = "L" + slotNum + ": " + keyText;
         if (isWaiting) {
            label = "L" + slotNum + ": pressionar...";
         }

         this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
            if (isWaiting) {
               this.waitingForSlot = -1;
               this.clearWidgets();
               this.init();
            } else {
               this.waitingForSlot = slotIndex;
               this.waitingType = "quickSwapLoadout";
               this.statusMessage = "Loadout Slot " + slotNum + ": pressione qualquer tecla...";
               this.clearWidgets();
               this.init();
            }
         }).bounds(x, y, slotButtonWidth, slotButtonHeight).build());
      }

      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonSettingsScreen()))
            .bounds(centerX - 145, top + 554, 290, 20)
            .build()
      );
   }

   private void saveCurrentArmor() {
      Minecraft client = this.minecraft;
      if (client.player == null) {
         this.statusMessage = "Nao ha jogador conectado.";
      } else {
         MoonWardrobeManager.LoadoutProfile profile = new MoonWardrobeManager.LoadoutProfile();
         profile.head = getItemName(client.player.getItemBySlot(EquipmentSlot.HEAD));
         profile.chest = getItemName(client.player.getItemBySlot(EquipmentSlot.CHEST));
         profile.legs = getItemName(client.player.getItemBySlot(EquipmentSlot.LEGS));
         profile.boots = getItemName(client.player.getItemBySlot(EquipmentSlot.FEET));
         MoonWardrobeManager.saveLoadout(this.selectedSlot, profile);
         MoonConfig.save();
         this.statusMessage = "Armadura salva no slot " + this.selectedSlot + ".";
         MoonWardrobeManager.sendFeedback(client, "Armadura salva no slot " + this.selectedSlot + ".");
         this.clearWidgets();
         this.init();
      }
   }

   private void saveCurrentEquipment() {
      Minecraft client = this.minecraft;
      if (client.player == null) {
         this.statusMessage = "Nao ha jogador conectado.";
      } else {
         MoonWardrobeManager.LoadoutProfile profile = MoonWardrobeManager.getLoadout(this.selectedSlot);
         if (profile == null) {
            profile = new MoonWardrobeManager.LoadoutProfile();
         }

         profile.mainhand = getItemName(client.player.getMainHandItem());
         profile.offhand = getItemName(client.player.getOffhandItem());
         MoonWardrobeManager.saveLoadout(this.selectedSlot, profile);
         MoonConfig.save();
         this.statusMessage = "Equipamento de mao salvo no slot " + this.selectedSlot + ".";
         MoonWardrobeManager.sendFeedback(client, "Equipamento de mao salvo no slot " + this.selectedSlot + ".");
         this.clearWidgets();
         this.init();
      }
   }

   private void saveFullLoadout() {
      Minecraft client = this.minecraft;
      if (client.player == null) {
         this.statusMessage = "Nao ha jogador conectado.";
      } else {
         MoonWardrobeManager.LoadoutProfile profile = MoonWardrobeManager.captureCurrentLoadout(client);
         MoonWardrobeManager.saveLoadout(this.selectedSlot, profile);
         MoonConfig.save();
         this.statusMessage = "Loadout completo salvo no slot " + this.selectedSlot + ".";
         MoonWardrobeManager.sendFeedback(client, "Loadout completo salvo no slot " + this.selectedSlot + ".");
         this.clearWidgets();
         this.init();
      }
   }

   private void loadLoadoutInfo() {
      MoonWardrobeManager.LoadoutProfile profile = MoonWardrobeManager.getLoadout(this.selectedSlot);
      if (profile == null) {
         this.statusMessage = "Nenhum loadout no slot " + this.selectedSlot + ".";
      } else {
         this.statusMessage = "Slot " + this.selectedSlot + ": " + profileToString(profile);
      }

      this.clearWidgets();
      this.init();
   }

   private void removeLoadout() {
      MoonWardrobeManager.removeLoadout(this.selectedSlot);
      MoonConfig.save();
      this.statusMessage = "Loadout removido do slot " + this.selectedSlot + ".";
      if (this.minecraft != null) {
         MoonWardrobeManager.sendFeedback(this.minecraft, "Loadout removido do slot " + this.selectedSlot + ".");
      }

      this.clearWidgets();
      this.init();
   }

   private void showLoadoutsSummary() {
      Map<Integer, MoonWardrobeManager.LoadoutProfile> all = MoonWardrobeManager.getAllLoadouts();
      if (all.isEmpty()) {
         this.statusMessage = "Nenhum loadout salvo.";
      } else {
         StringBuilder sb = new StringBuilder("Loadouts: ");
         boolean first = true;

         for (Entry<Integer, MoonWardrobeManager.LoadoutProfile> entry : all.entrySet()) {
            if (!first) {
               sb.append(", ");
            }

            sb.append("S").append(entry.getKey());
            first = false;
         }

         this.statusMessage = sb.toString();
      }

      this.clearWidgets();
      this.init();
   }

   private static int scaleY(int offset) {
      return offset * 9 / 10;
   }

   private static String getItemName(ItemStack stack) {
      return stack.isEmpty() ? "" : stack.getItem().toString();
   }

   private static String profileToString(MoonWardrobeManager.LoadoutProfile p) {
      StringBuilder sb = new StringBuilder();
      if (!p.head.isEmpty()) {
         sb.append("H:").append(shortName(p.head)).append(" ");
      }

      if (!p.chest.isEmpty()) {
         sb.append("C:").append(shortName(p.chest)).append(" ");
      }

      if (!p.legs.isEmpty()) {
         sb.append("L:").append(shortName(p.legs)).append(" ");
      }

      if (!p.boots.isEmpty()) {
         sb.append("B:").append(shortName(p.boots)).append(" ");
      }

      if (!p.mainhand.isEmpty()) {
         sb.append("M:").append(shortName(p.mainhand)).append(" ");
      }

      if (!p.offhand.isEmpty()) {
         sb.append("O:").append(shortName(p.offhand));
      }

      return sb.toString().trim();
   }

   private static String shortName(String fullName) {
      return fullName.contains(":") ? fullName.substring(fullName.lastIndexOf(58) + 1) : fullName;
   }

   private static String getKeyName(int keyCode) {
      return keyCode <= 0 ? "Nao configurada" : MoonKeyCommands.getKeyDisplayName(keyCode);
   }

   public void beforeKeyPress(Screen screen, KeyEvent event) {
      if (this.waitingForSlot >= 0) {
         int keyCode = event.key();
         if (keyCode == 256) {
            this.waitingForSlot = -1;
            this.clearWidgets();
            this.init();
            return;
         }

         int slot = this.waitingForSlot;
         if (this.waitingType.equals("quickSwapWardrobe")) {
            MoonConfig.wardrobeQuickSwapKeysWardrobe[slot] = keyCode;
            this.statusMessage = "Wardrobe Slot " + (slot + 1) + " configurado: " + getKeyName(keyCode) + "!";
         } else if (this.waitingType.equals("quickSwapEquipment")) {
            MoonConfig.wardrobeQuickSwapKeysEquipment[slot] = keyCode;
            this.statusMessage = "Equipment Slot " + (slot + 1) + " configurado: " + getKeyName(keyCode) + "!";
         } else if (this.waitingType.equals("quickSwapLoadout")) {
            MoonConfig.wardrobeQuickSwapKeysLoadout[slot] = keyCode;
            this.statusMessage = "Loadout Slot " + (slot + 1) + " configurado: " + getKeyName(keyCode) + "!";
         }

         MoonConfig.save();
         this.waitingForSlot = -1;
         this.clearWidgets();
         this.init();
      }
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 630);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 660, 630);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 630);
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "LOADOUT", centerX - 175, top + 78, -9251384);
      graphics.centeredText(this.font, "SWAP RAPIDO", centerX + 175, top + 28, -9251384);
      graphics.centeredText(this.font, "WARDROBE", centerX + 175, top + 60, -9251384);
      graphics.centeredText(this.font, "EQUIPMENT", centerX + 175, top + 70 + 72 + 6, -9251384);
      graphics.centeredText(this.font, "LOADOUT", centerX + 175, top + 70 + 144 + 22, -9251384);
      MoonScreenStyle.drawSection(graphics, this.font, centerX - 175, "ARMADURA", top + 92);
      MoonScreenStyle.drawSection(graphics, this.font, centerX - 175, "EQUIPAMENTO", top + 134);
      MoonScreenStyle.drawSection(graphics, this.font, centerX - 175, "LOADOUT COMPLETO", top + 176);
      MoonScreenStyle.drawSection(graphics, this.font, centerX - 175, "SELECAO DE SLOT", top + 216);
      MoonScreenStyle.drawSection(graphics, this.font, centerX - 175, "GERENCIAR", top + 276);
      graphics.centeredText(this.font, "Salve seus equipamentos para acesso rapido.", centerX - 175, top + 390, MoonScreenStyle.mutedColor());
      graphics.centeredText(this.font, "Clique num slot para configurar a tecla.", centerX + 175, top + 430, MoonScreenStyle.mutedColor());
      if (MoonConfig.wardrobeQuickSwapEnabled) {
         int wCount = 0;

         for (int key : MoonConfig.wardrobeQuickSwapKeysWardrobe) {
            if (key >= 0) {
               wCount++;
            }
         }

         int eCount = 0;

         for (int key : MoonConfig.wardrobeQuickSwapKeysEquipment) {
            if (key >= 0) {
               eCount++;
            }
         }

         int lCount = 0;

         for (int key : MoonConfig.wardrobeQuickSwapKeysLoadout) {
            if (key >= 0) {
               lCount++;
            }
         }

         graphics.centeredText(
            this.font, "Wardrobe: " + wCount + "/9 | Equipment: " + eCount + "/9 | Loadout: " + lCount + "/12", centerX + 175, top + 450, -9251384
         );
      }

      if (!this.statusMessage.isEmpty()) {
         graphics.centeredText(this.font, this.statusMessage, centerX - 175, top + 534, -9251384);
      }
   }
}
