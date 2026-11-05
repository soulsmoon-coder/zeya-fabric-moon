package dev.zeya.fabricmoon;

import dev.zeya.fabricmoon.hud.MoonCombatHudSettingsScreen;
import dev.zeya.fabricmoon.hud.MoonCombatTracker;
import dev.zeya.fabricmoon.hud.MoonHudPositionScreen;
import dev.zeya.fabricmoon.hud.MoonHudSettingsScreen;
import dev.zeya.fabricmoon.macros.MoonKeyCommandsSettingsScreen;
import dev.zeya.fabricmoon.macros.MoonMacrosSettingsScreen;
import dev.zeya.fabricmoon.macros.MoonSlotBindSettingsScreen;
import dev.zeya.fabricmoon.macros.MoonSlotLockSettingsScreen;
import dev.zeya.fabricmoon.macros.MoonWardrobeSettingsScreen;
import dev.zeya.fabricmoon.visual.MoonColorSettingsScreen;
import dev.zeya.fabricmoon.visual.MoonGlowSettingsScreen;
import dev.zeya.fabricmoon.visual.MoonMobSizeSettingsScreen;
import dev.zeya.fabricmoon.visual.MoonPlayerSizeSettingsScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class MoonSettingsScreen extends Screen {
   private static final String[] CATEGORIES = {"HUD", "VISUAL", "MACROS", "UTILS"};
   private static final int PANEL_GAP = 8;
   private static final int HEADER_HEIGHT = 22;
   private static final int ROW_HEIGHT = 20;
   private static final int SETTINGS_ROW_HEIGHT = 24;
   private static final int MAX_CONTENT_HEIGHT = 350;
   private static final int[] PANEL_X = {-1, -1, -1, -1};
   private static final int[] PANEL_Y = {30, 30, 30, 30};
   private static final boolean[] COLLAPSED = {false, false, false, false};

   private int draggingCategory = -1;
   private int dragOffsetX;
   private int dragOffsetY;
   private String detailMessage;
   private String openConfig;

   public MoonSettingsScreen() {
      super(Component.literal("ZEYA MOON"));
   }

   protected void init() {
      if (PANEL_X[0] < 0) {
         int panelWidth = this.panelWidth();
         int totalWidth = CATEGORIES.length * panelWidth + (CATEGORIES.length - 1) * PANEL_GAP;
         int left = Math.max(0, (this.width - totalWidth) / 2);
         for (int index = 0; index < CATEGORIES.length; index++) {
            PANEL_X[index] = left + index * (panelWidth + PANEL_GAP);
         }
      }

      this.clampPanels();
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (this.openConfig != null && this.insideSettingsPanel(event.x(), event.y())) {
         if (event.button() == 0 && this.closeButtonContains(event.x(), event.y())) {
            this.openConfig = null;
            return true;
         }

         int row = this.settingsRowAt(event.x(), event.y());
         if (event.button() == 0 && row >= 0) {
            this.activateSettingsRow(row, event.x());
         }
         return true;
      }
      if (this.openConfig != null) {
         this.openConfig = null;
      }

      int category = this.categoryAtHeader(event.x(), event.y());
      if (category >= 0) {
         if (event.button() == 1) {
            COLLAPSED[category] = !COLLAPSED[category];
         } else if (event.button() == 0) {
            this.draggingCategory = category;
            this.dragOffsetX = (int)event.x() - PANEL_X[category];
            this.dragOffsetY = (int)event.y() - PANEL_Y[category];
         }
         return true;
      }

      int[] target = this.featureAt(event.x(), event.y());
      if (target != null) {
         FeatureRow row = this.rows(target[0]).get(target[1]);
         if (event.button() == 0 && row.toggle() != null) {
            row.toggle().run();
            MoonConfig.save();
         } else if (event.button() == 0 || event.button() == 1) {
            this.openConfig = row.id();
            this.detailMessage = row.description();
         }
         return true;
      }

      return super.mouseClicked(event, doubleClick);
   }

   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      if (this.draggingCategory >= 0 && event.button() == 0) {
         int index = this.draggingCategory;
         PANEL_X[index] = (int)event.x() - this.dragOffsetX;
         PANEL_Y[index] = (int)event.y() - this.dragOffsetY;
         this.clampPanel(index);
         return true;
      }
      return super.mouseDragged(event, deltaX, deltaY);
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      if (event.button() == 0) {
         this.draggingCategory = -1;
      }
      return super.mouseReleased(event);
   }

   public boolean keyPressed(KeyEvent event) {
      if (event.key() == 256 && this.openConfig != null) {
         this.openConfig = null;
         return true;
      }
      return super.keyPressed(event);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.openConfig != null && this.insideSettingsPanel(mouseX, mouseY)) {
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      graphics.fill(0, 0, this.width, this.height, 0x96000000);

      for (int category = 0; category < CATEGORIES.length; category++) {
         if (category != this.draggingCategory) {
            this.drawCategory(graphics, category, mouseX, mouseY);
         }
      }
      if (this.draggingCategory >= 0) {
         this.drawCategory(graphics, this.draggingCategory, mouseX, mouseY);
      }

      int[] hovered = this.featureAt(mouseX, mouseY);
      String description = hovered == null
         ? this.detailMessage == null ? "Left-click: toggle | Right-click: settings | Drag categories; right-click a title to collapse." : this.detailMessage
         : this.rows(hovered[0]).get(hovered[1]).description();
      this.drawDescription(graphics, description);
      if (this.openConfig != null) {
         this.drawSettingsPanel(graphics, mouseX, mouseY);
      }
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
   }

   private void drawCategory(GuiGraphicsExtractor graphics, int category, int mouseX, int mouseY) {
      int x = PANEL_X[category];
      int y = PANEL_Y[category];
      int width = this.panelWidth();
      List<FeatureRow> rows = this.rows(category);
      int contentHeight = Math.min(rows.size() * ROW_HEIGHT, MAX_CONTENT_HEIGHT);
      boolean headerHovered = inside(mouseX, mouseY, x, y, width, HEADER_HEIGHT);

      graphics.fill(x - 2, y - 2, x + width + 2, y + HEADER_HEIGHT + (COLLAPSED[category] ? 0 : contentHeight) + 2, 0x4D000000);
      graphics.fill(x, y, x + width, y + HEADER_HEIGHT, 0xE6141414);
      graphics.fill(x, y, x + width, y + 2, accentColor(255));
      graphics.centeredText(this.font, CATEGORIES[category], x + width / 2, y + 7, headerHovered ? MoonScreenStyle.mutedColor() : 0xFFFFFFFF);

      if (COLLAPSED[category]) {
         return;
      }

      for (int index = 0; index < rows.size(); index++) {
         FeatureRow row = rows.get(index);
         int top = y + HEADER_HEIGHT + index * ROW_HEIGHT;
         if (top + ROW_HEIGHT > y + HEADER_HEIGHT + MAX_CONTENT_HEIGHT) {
            break;
         }
         boolean hovered = inside(mouseX, mouseY, x, top, width, ROW_HEIGHT);
         graphics.fill(x, top, x + width, top + ROW_HEIGHT, 0xD10F0F0F);
         if (row.enabled() != null && row.enabled().getAsBoolean()) {
            graphics.fill(x, top, x + width, top + ROW_HEIGHT, accentColor(100));
            graphics.fill(x, top, x + 2, top + ROW_HEIGHT, accentColor(255));
         }
         if (hovered) {
            graphics.fill(x, top, x + width, top + ROW_HEIGHT, 0x38FFFFFF);
         }

         graphics.text(this.font, row.label(), x + 7, top + 6, 0xFFFFFFFF);
         if (row.enabled() == null) {
            graphics.text(this.font, "›", x + width - this.font.width("›") - 6, top + 6, accentColor(255));
         }
      }
   }

   private void drawSettingsPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      List<ConfigRow> rows = this.settingsRows();
      int width = Math.min(310, this.width - 20);
      int height = Math.min(this.height - 44, 38 + rows.size() * SETTINGS_ROW_HEIGHT);
      int left = (this.width - width) / 2;
      int top = Math.max(22, (this.height - height) / 2);
      graphics.fill(left - 3, top - 3, left + width + 3, top + height + 3, 0x80000000);
      graphics.fill(left, top, left + width, top + height, 0xF00A0A0A);
      graphics.fill(left, top, left + width, top + 3, accentColor(255));
      graphics.fill(left, top + 3, left + width, top + 29, 0xF0191919);
      graphics.centeredText(this.font, this.configTitle(), this.width / 2, top + 10, 0xFFFFFFFF);
      graphics.fill(left + width - 25, top + 6, left + width - 7, top + 23, accentColor(64));
      graphics.centeredText(this.font, "x", left + width - 16, top + 9, 0xFFFFFFFF);

      for (int index = 0; index < rows.size(); index++) {
         ConfigRow row = rows.get(index);
         int rowTop = top + 31 + index * SETTINGS_ROW_HEIGHT;
         if (rowTop + SETTINGS_ROW_HEIGHT > top + height - 5) {
            break;
         }
         boolean hovered = inside(mouseX, mouseY, left + 6, rowTop, width - 12, SETTINGS_ROW_HEIGHT);
         graphics.fill(left + 6, rowTop, left + width - 6, rowTop + SETTINGS_ROW_HEIGHT - 1, hovered ? accentColor(48) : 0x18000000);
         graphics.text(this.font, row.label(), left + 12, rowTop + 7, 0xFFFFFFFF);
         this.drawConfigValue(graphics, row, left, rowTop, width);
      }
   }

   private void drawConfigValue(GuiGraphicsExtractor graphics, ConfigRow row, int left, int top, int width) {
      int right = left + width - 12;
      int centerY = top + SETTINGS_ROW_HEIGHT / 2;
      switch (row.kind()) {
         case TOGGLE -> {
            int buttonWidth = 28;
            int x = right - buttonWidth;
            boolean enabled = row.enabled().getAsBoolean();
            graphics.fill(x, centerY - 7, right, centerY + 7, enabled ? accentColor(255) : 0xFF363636);
            int indicatorX = enabled ? right - 11 : x + 3;
            graphics.fill(indicatorX, centerY - 5, indicatorX + 8, centerY + 5, 0xFFFFFFFF);
         }
         case ACTION -> {
            String value = row.value().get();
            int buttonWidth = Math.max(64, Math.min(112, this.font.width(value) + 18));
            int x = right - buttonWidth;
            graphics.fill(x, centerY - 9, right, centerY + 9, 0xFF101010);
            graphics.fill(x, centerY - 9, right, centerY - 7, accentColor(255));
            graphics.fill(x, centerY + 7, right, centerY + 9, accentColor(255));
            graphics.fill(x, centerY - 9, x + 2, centerY + 9, accentColor(255));
            graphics.fill(right - 2, centerY - 9, right, centerY + 9, accentColor(255));
            graphics.centeredText(this.font, value, x + buttonWidth / 2, centerY - 4, 0xFFFFFFFF);
         }
         case COLOR -> {
            int swatchX = right - 28;
            graphics.fill(swatchX, centerY - 9, right, centerY + 9, 0xFF222222);
            graphics.fill(swatchX + 3, centerY - 6, right - 3, centerY + 6, 0xFF000000 | row.color().getAsInt() & 0xFFFFFF);
         }
         case STEPPER -> {
            int plusX = right - 22;
            int valueLeft = plusX - 56;
            int minusX = valueLeft - 22;
            graphics.fill(minusX, centerY - 9, minusX + 20, centerY + 9, accentColor(255));
            graphics.fill(valueLeft, centerY - 9, plusX - 2, centerY + 9, 0xFF171717);
            graphics.fill(plusX, centerY - 9, right, centerY + 9, accentColor(255));
            graphics.centeredText(this.font, "-", minusX + 10, centerY - 4, 0xFFFFFFFF);
            graphics.centeredText(this.font, "+", plusX + 10, centerY - 4, 0xFFFFFFFF);
            graphics.centeredText(this.font, row.value().get(), (valueLeft + plusX - 2) / 2, centerY - 4, 0xFFFFFFFF);
         }
         case INFO -> graphics.text(this.font, row.value().get(), right - this.font.width(row.value().get()), centerY - 4, 0xFFAAAAAA);
      }
   }

   private void drawDescription(GuiGraphicsExtractor graphics, String description) {
      int width = Math.min(520, this.width - 24);
      int left = (this.width - width) / 2;
      int top = this.height - 34;
      String text = description;
      while (!text.isEmpty() && this.font.width(text) > width - 18) {
         text = text.substring(0, text.length() - 1);
      }
      if (!text.equals(description) && text.length() > 3) {
         text = text.substring(0, text.length() - 3) + "...";
      }
      graphics.fill(left, top, left + width, top + 22, 0xC40B0B0B);
      graphics.fill(left, top, left + 2, top + 22, accentColor(255));
      graphics.centeredText(this.font, text, this.width / 2, top + 7, 0xFFE0E0E0);
   }

   private List<FeatureRow> rows(int category) {
      return switch (category) {
         case 0 -> List.of(
            feature("FPS / Ping / TPS", "Shows performance statistics. Right-click for HUD colors and position.", "hud", () -> MoonConfig.showHud, () -> MoonConfig.showHud = !MoonConfig.showHud),
            feature("Enemy Info", "Shows target health and lets you reposition its panel.", "enemy-info", () -> MoonConfig.combatHudShowTargetHealth, () -> MoonConfig.combatHudShowTargetHealth = !MoonConfig.combatHudShowTargetHealth),
            feature("DPS", "Tracks nearby floating damage and health changes. Configure its range and chat summary.", "dps", this::isDpsEnabled, this::toggleDps),
            feature("Network Optimizer", "Smooths network statistics shown by the client.", "network", () -> MoonConfig.networkOptimizer, () -> MoonConfig.networkOptimizer = !MoonConfig.networkOptimizer),
            feature("Hypixel Auto-Route", "Tests Hypixel routes when joining a server.", "hypixel-route", () -> MoonConfig.hypixelAutoRoute, () -> MoonConfig.hypixelAutoRoute = !MoonConfig.hypixelAutoRoute),
            action("Network Status", "Show current FPS, TPS, ping, and route status.", "network-status"),
            action("Test Hypixel Route", "Test the available Hypixel routes.", "test-route")
         );
         case 1 -> List.of(
            feature("Fullbright", "Brightens the local game view.", "fullbright", () -> MoonConfig.fullbright, () -> MoonConfig.fullbright = !MoonConfig.fullbright),
            feature("Hide Armor", "Hides armor models on your character.", "hide-armor", () -> MoonConfig.hideArmor, () -> MoonConfig.hideArmor = !MoonConfig.hideArmor),
            feature("Hide Head Models", "Hides the helmet model without hiding your skin.", "hide-head", () -> MoonConfig.hideHead, () -> MoonConfig.hideHead = !MoonConfig.hideHead),
            feature("Mob Hitboxes", "Draws entity hitboxes without changing collisions.", "hitboxes", () -> MoonConfig.entityHitboxes, () -> MoonConfig.entityHitboxes = !MoonConfig.entityHitboxes),
            action("Player Size", "Adjust player scale and perspective.", "player-size"),
            feature("Player Glow", "Toggle the local player outline and choose its color.", "player-glow", () -> MoonConfig.playerGlow, () -> MoonConfig.playerGlow = !MoonConfig.playerGlow),
            feature("Shrink Large Mobs", "Adjust selected large mobs to match player height.", "shrink-mobs", () -> MoonConfig.shrinkLargeMobs, () -> MoonConfig.shrinkLargeMobs = !MoonConfig.shrinkLargeMobs)
         );
         case 2 -> List.of(
            feature("AutoClick", "Automatically clicks the left mouse button while held.", "auto-click", () -> MoonConfig.leftAutoClick, () -> MoonConfig.leftAutoClick = !MoonConfig.leftAutoClick),
            feature("Auto Fishing", "Automatically casts and reels in the fishing rod.", "fishing", () -> MoonConfig.autoFishing, () -> MoonConfig.autoFishing = !MoonConfig.autoFishing),
            feature("Auto Golden Fish", "Handle Golden Fish cues while auto fishing.", "golden-fish", () -> MoonConfig.autoGoldenFish, () -> MoonConfig.autoGoldenFish = !MoonConfig.autoGoldenFish),
            feature("Oxygen Recovery", "Swims upward when air falls below the configured threshold.", "oxygen", () -> MoonConfig.oxygenRecovery, () -> MoonConfig.oxygenRecovery = !MoonConfig.oxygenRecovery),
            feature("Blaze Dagger Swap", "Selects the matching dagger and attunement for the target.", "blaze-dagger", () -> MoonConfig.blazeDaggerAutoSwitch, () -> MoonConfig.blazeDaggerAutoSwitch = !MoonConfig.blazeDaggerAutoSwitch),
            feature("Key Commands", "Enable configured quick commands.", "key-commands", () -> MoonConfig.keyCommandsEnabled, () -> MoonConfig.keyCommandsEnabled = !MoonConfig.keyCommandsEnabled),
            feature("Slot Bind", "Enable inventory slot bindings.", "slot-bind", () -> MoonConfig.slotBindEnabled, () -> MoonConfig.slotBindEnabled = !MoonConfig.slotBindEnabled),
            feature("Slot Lock", "Enable protection for locked inventory slots.", "slot-lock", () -> MoonConfig.slotLockEnabled, () -> MoonConfig.slotLockEnabled = !MoonConfig.slotLockEnabled),
            feature("Wardrobe / Equipment", "Enable wardrobe and equipment menu support.", "wardrobe", () -> MoonConfig.wardrobeEnabled, () -> MoonConfig.wardrobeEnabled = !MoonConfig.wardrobeEnabled),
            feature("Quick Swap", "Enable quick wardrobe and loadout keybinds.", "quick-swap", () -> MoonConfig.wardrobeQuickSwapEnabled, () -> MoonConfig.wardrobeQuickSwapEnabled = !MoonConfig.wardrobeQuickSwapEnabled)
         );
         default -> List.of(
            feature("Mod Enabled", "Suspend or resume all mod features without changing their individual settings.", "global", () -> MoonConfig.featuresEnabled, () -> MoonConfig.featuresEnabled = !MoonConfig.featuresEnabled),
            action("Global Color", "Choose the mod accent and the HUD text/accent color.", "global-color"),
            action("Macro Settings", "Open detailed settings for fishing, oxygen, and Blaze Dagger.", "macro-settings"),
            action("Key Commands", "Configure commands and key bindings.", "key-command-settings"),
            action("Slot Bind", "Configure inventory slot bindings.", "slot-bind-settings"),
            action("Slot Lock", "Configure locked inventory slots.", "slot-lock-settings"),
            action("Wardrobe Keybinds", "Configure wardrobe and loadout key bindings.", "wardrobe-settings")
         );
      };
   }

   private List<ConfigRow> settingsRows() {
      List<ConfigRow> rows = new ArrayList<>();
      switch (this.openConfig) {
         case "hud" -> {
            rows.add(toggleSetting("Show HUD", () -> MoonConfig.showHud, () -> MoonConfig.showHud = !MoonConfig.showHud));
            rows.add(toggleSetting("Enemy Info", () -> MoonConfig.combatHudShowTargetHealth, () -> MoonConfig.combatHudShowTargetHealth = !MoonConfig.combatHudShowTargetHealth));
            rows.add(toggleSetting("Background", () -> MoonConfig.hudBackgroundEnabled, () -> MoonConfig.hudBackgroundEnabled = !MoonConfig.hudBackgroundEnabled));
            rows.add(colorSetting("Text Color", () -> MoonConfig.hudTextColor, () -> this.openColorPicker("Text Color", MoonConfig.hudTextColor, color -> MoonConfig.hudTextColor = 0xFF000000 | color)));
            rows.add(colorSetting("Accent Color", () -> MoonConfig.hudAccentColor, () -> this.openColorPicker("Accent Color", MoonConfig.hudAccentColor, color -> MoonConfig.hudAccentColor = 0xFF000000 | color)));
            rows.add(colorSetting("Background Color", () -> MoonConfig.hudBackgroundColor, () -> this.openColorPicker("Background Color", MoonConfig.hudBackgroundColor, color -> MoonConfig.hudBackgroundColor = (MoonConfig.hudBackgroundColor & 0xFF000000) | color)));
            rows.add(actionSetting("Move HUD", () -> this.open(new MoonHudPositionScreen())));
            rows.add(actionSetting("More HUD Settings", () -> this.open(new MoonHudSettingsScreen())));
         }
         case "enemy-info" -> {
            rows.add(toggleSetting("Show Enemy Info", () -> MoonConfig.combatHudShowTargetHealth, () -> MoonConfig.combatHudShowTargetHealth = !MoonConfig.combatHudShowTargetHealth));
            rows.add(actionSetting("Move HUD", () -> this.open(new MoonHudPositionScreen())));
         }
         case "dps" -> {
            rows.add(toggleSetting("Show DPS HUD", this::isDpsEnabled, this::toggleDps));
            rows.add(toggleSetting("Chat Damage Summary", () -> MoonConfig.combatHudChatDamage, () -> MoonConfig.combatHudChatDamage = !MoonConfig.combatHudChatDamage));
            rows.add(stepperSetting("Damage Range", () -> MoonConfig.clampCombatHudRange(MoonConfig.combatHudRange) + " blocks",
               () -> changeDpsRange(-1), () -> changeDpsRange(1)));
            rows.add(toggleSetting("Label Debug", () -> MoonConfig.combatHudDebugLabels, () -> MoonConfig.combatHudDebugLabels = !MoonConfig.combatHudDebugLabels));
            rows.add(actionSetting("Reset DPS", MoonCombatTracker::resetDps));
            rows.add(actionSetting("DPS Settings", () -> this.open(new MoonCombatHudSettingsScreen())));
         }
         case "network" -> rows.add(toggleSetting("Enabled", () -> MoonConfig.networkOptimizer, () -> MoonConfig.networkOptimizer = !MoonConfig.networkOptimizer));
         case "hypixel-route" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.hypixelAutoRoute, () -> MoonConfig.hypixelAutoRoute = !MoonConfig.hypixelAutoRoute));
            rows.add(actionSetting("Test Route", () -> ZeyaFabricMoonClient.testHypixelServers(this.minecraft)));
            rows.add(actionSetting("Network Status", () -> ZeyaFabricMoonClient.showNetworkStatus(this.minecraft)));
         }
         case "network-status" -> rows.add(actionSetting("Show Status", () -> ZeyaFabricMoonClient.showNetworkStatus(this.minecraft)));
         case "test-route" -> rows.add(actionSetting("Test Route", () -> ZeyaFabricMoonClient.testHypixelServers(this.minecraft)));
         case "fullbright" -> rows.add(toggleSetting("Enabled", () -> MoonConfig.fullbright, () -> MoonConfig.fullbright = !MoonConfig.fullbright));
         case "hide-armor" -> rows.add(toggleSetting("Enabled", () -> MoonConfig.hideArmor, () -> MoonConfig.hideArmor = !MoonConfig.hideArmor));
         case "hide-head" -> rows.add(toggleSetting("Enabled", () -> MoonConfig.hideHead, () -> MoonConfig.hideHead = !MoonConfig.hideHead));
         case "hitboxes" -> rows.add(toggleSetting("Enabled", () -> MoonConfig.entityHitboxes, () -> MoonConfig.entityHitboxes = !MoonConfig.entityHitboxes));
         case "player-size" -> rows.add(actionSetting("Open Player Size", () -> this.open(new MoonPlayerSizeSettingsScreen())));
         case "player-glow" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.playerGlow, () -> MoonConfig.playerGlow = !MoonConfig.playerGlow));
            rows.add(actionSetting("Glow Color", () -> this.open(new MoonGlowSettingsScreen())));
         }
         case "shrink-mobs" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.shrinkLargeMobs, () -> MoonConfig.shrinkLargeMobs = !MoonConfig.shrinkLargeMobs));
            rows.add(actionSetting("Choose Mobs", () -> this.open(new MoonMobSizeSettingsScreen())));
         }
         case "auto-click" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.leftAutoClick, () -> MoonConfig.leftAutoClick = !MoonConfig.leftAutoClick));
            rows.add(stepperSetting("Clicks per second", () -> MoonConfig.leftCps + " CPS",
               () -> changeCps(-1), () -> changeCps(1)));
         }
         case "fishing" -> {
            rows.add(toggleSetting("Auto Fishing", () -> MoonConfig.autoFishing, () -> MoonConfig.autoFishing = !MoonConfig.autoFishing));
            rows.add(stepperSetting("Cast Delay (ticks)", () -> Integer.toString(MoonConfig.fishingCastDelayTicks),
               () -> changeFishingCastDelay(-1), () -> changeFishingCastDelay(1)));
            rows.add(stepperSetting("Reel Delay (ticks)", () -> Integer.toString(MoonConfig.fishingReelDelayTicks),
               () -> changeFishingReelDelay(-1), () -> changeFishingReelDelay(1)));
            rows.add(stepperSetting("Minimum Wait (seconds)", () -> Integer.toString(MoonConfig.minFishWaitSeconds),
               () -> changeFishWait(-1), () -> changeFishWait(1)));
            rows.add(toggleSetting("Disable While In Screen", () -> MoonConfig.disableFishingWhileOnScreen,
               () -> MoonConfig.disableFishingWhileOnScreen = !MoonConfig.disableFishingWhileOnScreen));
            rows.add(toggleSetting("Auto Golden Fish", () -> MoonConfig.autoGoldenFish, () -> MoonConfig.autoGoldenFish = !MoonConfig.autoGoldenFish));
            rows.add(actionSetting("More Fishing Settings", () -> this.open(new MoonMacrosSettingsScreen())));
         }
         case "golden-fish" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.autoGoldenFish, () -> MoonConfig.autoGoldenFish = !MoonConfig.autoGoldenFish));
            rows.add(stepperSetting("Action Delay (ticks)", () -> Integer.toString(MoonConfig.goldenFishActionDelayTicks),
               () -> changeGoldenFishDelay(-1), () -> changeGoldenFishDelay(1)));
            rows.add(actionSetting("Fishing Settings", () -> this.open(new MoonMacrosSettingsScreen())));
         }
         case "oxygen" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.oxygenRecovery, () -> MoonConfig.oxygenRecovery = !MoonConfig.oxygenRecovery));
            rows.add(stepperSetting("Air Threshold", () -> Integer.toString(MoonConfig.oxygenThreshold),
               () -> changeOxygenThreshold(-10), () -> changeOxygenThreshold(10)));
         }
         case "blaze-dagger" -> {
            rows.add(toggleSetting("Auto-switch", () -> MoonConfig.blazeDaggerAutoSwitch, () -> MoonConfig.blazeDaggerAutoSwitch = !MoonConfig.blazeDaggerAutoSwitch));
            rows.add(stepperSetting("Hotbar Slot", () -> Integer.toString(MoonConfig.blazeDaggerSlot),
               () -> changeBlazeSlot(-1), () -> changeBlazeSlot(1)));
            rows.add(stepperSetting("Swap Delay (ms)", () -> Integer.toString(MoonConfig.clampSwapDelay(MoonConfig.blazeDaggerSwapDelayMs)),
               () -> changeBlazeSwapDelay(-25), () -> changeBlazeSwapDelay(25)));
            rows.add(actionSetting("More Macro Settings", () -> this.open(new MoonMacrosSettingsScreen())));
         }
         case "key-commands" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.keyCommandsEnabled, () -> MoonConfig.keyCommandsEnabled = !MoonConfig.keyCommandsEnabled));
            rows.add(actionSetting("Configure Commands", () -> this.open(new MoonKeyCommandsSettingsScreen())));
         }
         case "slot-bind" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.slotBindEnabled, () -> MoonConfig.slotBindEnabled = !MoonConfig.slotBindEnabled));
            rows.add(actionSetting("Configure Slots", () -> this.open(new MoonSlotBindSettingsScreen())));
         }
         case "slot-lock" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.slotLockEnabled, () -> MoonConfig.slotLockEnabled = !MoonConfig.slotLockEnabled));
            rows.add(actionSetting("Configure Slots", () -> this.open(new MoonSlotLockSettingsScreen())));
         }
         case "wardrobe" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.wardrobeEnabled, () -> MoonConfig.wardrobeEnabled = !MoonConfig.wardrobeEnabled));
            rows.add(toggleSetting("Close After Use", () -> MoonConfig.wardrobeCloseAfterUse, () -> MoonConfig.wardrobeCloseAfterUse = !MoonConfig.wardrobeCloseAfterUse));
            rows.add(toggleSetting("Prevent Unequip", () -> MoonConfig.wardrobePreventUnequip, () -> MoonConfig.wardrobePreventUnequip = !MoonConfig.wardrobePreventUnequip));
            rows.add(toggleSetting("Use Hotbar Binds", () -> MoonConfig.wardrobeUseHotbarBinds, () -> MoonConfig.wardrobeUseHotbarBinds = !MoonConfig.wardrobeUseHotbarBinds));
            rows.add(actionSetting("Configure Keybinds", () -> this.open(new MoonWardrobeSettingsScreen())));
         }
         case "quick-swap" -> {
            rows.add(toggleSetting("Enabled", () -> MoonConfig.wardrobeQuickSwapEnabled, () -> MoonConfig.wardrobeQuickSwapEnabled = !MoonConfig.wardrobeQuickSwapEnabled));
            rows.add(actionSetting("Configure Keybinds", () -> this.open(new MoonWardrobeSettingsScreen())));
         }
         case "global" -> rows.add(toggleSetting("Enabled", () -> MoonConfig.featuresEnabled, () -> MoonConfig.featuresEnabled = !MoonConfig.featuresEnabled));
         case "global-color" -> rows.add(colorSetting("Accent Color", () -> MoonConfig.modColor,
            () -> this.openColorPicker("Global Color", MoonConfig.modColor, MoonConfig::setGlobalColor)));
         case "macro-settings" -> rows.add(actionSetting("Open Macro Settings", () -> this.open(new MoonMacrosSettingsScreen())));
         case "key-command-settings" -> rows.add(actionSetting("Open Key Commands", () -> this.open(new MoonKeyCommandsSettingsScreen())));
         case "slot-bind-settings" -> rows.add(actionSetting("Open Slot Bind", () -> this.open(new MoonSlotBindSettingsScreen())));
         case "slot-lock-settings" -> rows.add(actionSetting("Open Slot Lock", () -> this.open(new MoonSlotLockSettingsScreen())));
         case "wardrobe-settings" -> rows.add(actionSetting("Open Wardrobe", () -> this.open(new MoonWardrobeSettingsScreen())));
         default -> {
         }
      }
      return rows;
   }

   private void activateSettingsRow(int index, double mouseX) {
      List<ConfigRow> rows = this.settingsRows();
      if (index < 0 || index >= rows.size()) {
         return;
      }

      ConfigRow row = rows.get(index);
      if (row.kind() == ConfigKind.STEPPER) {
         int right = (this.width + Math.min(310, this.width - 20)) / 2 - 12;
         int plusX = right - 22;
         int valueLeft = plusX - 56;
         int minusX = valueLeft - 22;
         if (mouseX >= minusX && mouseX < valueLeft - 2) {
            row.decrease().run();
         } else if (mouseX >= plusX) {
            row.increase().run();
         }
      } else if (row.activate() != null) {
         row.activate().run();
      }

      if (row.kind() != ConfigKind.ACTION && row.kind() != ConfigKind.COLOR) {
         MoonConfig.save();
      }
   }

   private void openColorPicker(String title, int initialColor, java.util.function.IntConsumer onApply) {
      this.minecraft.gui.setScreen(new MoonColorSettingsScreen(title, initialColor, this, color -> {
         onApply.accept(color);
         MoonConfig.save();
      }));
   }

   private void open(Screen screen) {
      this.minecraft.gui.setScreen(screen);
   }

   private String configTitle() {
      for (int category = 0; category < CATEGORIES.length; category++) {
         for (FeatureRow row : this.rows(category)) {
            if (row.id().equals(this.openConfig)) {
               return row.label();
            }
         }
      }
      return "Settings";
   }

   private boolean insideSettingsPanel(double mouseX, double mouseY) {
      int width = Math.min(310, this.width - 20);
      int height = Math.min(this.height - 44, 38 + this.settingsRows().size() * SETTINGS_ROW_HEIGHT);
      int left = (this.width - width) / 2;
      int top = Math.max(22, (this.height - height) / 2);
      return inside(mouseX, mouseY, left, top, width, height);
   }

   private boolean closeButtonContains(double mouseX, double mouseY) {
      int width = Math.min(310, this.width - 20);
      int height = Math.min(this.height - 44, 38 + this.settingsRows().size() * SETTINGS_ROW_HEIGHT);
      int left = (this.width - width) / 2;
      int top = Math.max(22, (this.height - height) / 2);
      return inside(mouseX, mouseY, left + width - 25, top + 6, 18, 17);
   }

   private int settingsRowAt(double mouseX, double mouseY) {
      int width = Math.min(310, this.width - 20);
      int height = Math.min(this.height - 44, 38 + this.settingsRows().size() * SETTINGS_ROW_HEIGHT);
      int left = (this.width - width) / 2;
      int top = Math.max(22, (this.height - height) / 2);
      if (mouseY < top + 31) {
         return -1;
      }
      int row = (int)((mouseY - top - 31) / SETTINGS_ROW_HEIGHT);
      if (mouseX < left + 6 || mouseX > left + width - 6 || row < 0 || row >= this.settingsRows().size()) {
         return -1;
      }
      return row;
   }

   private int[] featureAt(double mouseX, double mouseY) {
      for (int category = CATEGORIES.length - 1; category >= 0; category--) {
         if (COLLAPSED[category]) {
            continue;
         }
         int x = PANEL_X[category];
         int y = PANEL_Y[category] + HEADER_HEIGHT;
         int width = this.panelWidth();
         List<FeatureRow> rows = this.rows(category);
         int visibleRows = Math.min(rows.size(), MAX_CONTENT_HEIGHT / ROW_HEIGHT);
         for (int index = 0; index < visibleRows; index++) {
            int rowY = y + index * ROW_HEIGHT;
            if (inside(mouseX, mouseY, x, rowY, width, ROW_HEIGHT)) {
               return new int[]{category, index};
            }
         }
      }
      return null;
   }

   private int categoryAtHeader(double mouseX, double mouseY) {
      for (int category = CATEGORIES.length - 1; category >= 0; category--) {
         if (inside(mouseX, mouseY, PANEL_X[category], PANEL_Y[category], this.panelWidth(), HEADER_HEIGHT)) {
            return category;
         }
      }
      return -1;
   }

   private int panelWidth() {
      return Math.min(150, Math.max(90, (this.width - 40 - PANEL_GAP * (CATEGORIES.length - 1)) / CATEGORIES.length));
   }

   private void clampPanels() {
      for (int index = 0; index < CATEGORIES.length; index++) {
         this.clampPanel(index);
      }
   }

   private void clampPanel(int index) {
      PANEL_X[index] = Math.max(0, Math.min(PANEL_X[index], this.width - this.panelWidth()));
      PANEL_Y[index] = Math.max(0, Math.min(PANEL_Y[index], this.height - HEADER_HEIGHT - 22));
   }

   private boolean isDpsEnabled() {
      return MoonConfig.combatHud && MoonConfig.combatHudShowDps;
   }

   private void toggleDps() {
      boolean enabled = !this.isDpsEnabled();
      MoonConfig.combatHud = enabled;
      MoonConfig.combatHudShowDps = enabled;
   }

   private void changeDpsRange(int amount) {
      MoonConfig.combatHudRange = MoonConfig.clampCombatHudRange(MoonConfig.combatHudRange + amount);
   }

   private void changeCps(int amount) {
      MoonConfig.leftCps = MoonConfig.clampCps(MoonConfig.leftCps + amount);
   }

   private void changeFishingCastDelay(int amount) {
      MoonConfig.fishingCastDelayTicks = MoonConfig.clampDelay(MoonConfig.fishingCastDelayTicks + amount);
   }

   private void changeFishingReelDelay(int amount) {
      MoonConfig.fishingReelDelayTicks = MoonConfig.clampDelay(MoonConfig.fishingReelDelayTicks + amount);
   }

   private void changeFishWait(int amount) {
      MoonConfig.minFishWaitSeconds = MoonConfig.clampSeconds(MoonConfig.minFishWaitSeconds + amount);
   }

   private void changeGoldenFishDelay(int amount) {
      MoonConfig.goldenFishActionDelayTicks = MoonConfig.clampDelay(MoonConfig.goldenFishActionDelayTicks + amount);
   }

   private void changeOxygenThreshold(int amount) {
      MoonConfig.oxygenThreshold = MoonConfig.clampOxygen(MoonConfig.oxygenThreshold + amount);
   }

   private void changeBlazeSlot(int amount) {
      MoonConfig.blazeDaggerSlot = MoonConfig.clampLoadoutSlot(MoonConfig.blazeDaggerSlot + amount);
   }

   private void changeBlazeSwapDelay(int amount) {
      MoonConfig.blazeDaggerSwapDelayMs = MoonConfig.clampSwapDelay(MoonConfig.blazeDaggerSwapDelayMs + amount);
   }

   private static FeatureRow feature(String label, String description, String id, BooleanSupplier enabled, Runnable toggle) {
      return new FeatureRow(label, description, id, enabled, toggle);
   }

   private static FeatureRow action(String label, String description, String id) {
      return new FeatureRow(label, description, id, null, null);
   }

   private static ConfigRow toggleSetting(String label, BooleanSupplier enabled, Runnable toggle) {
      return new ConfigRow(label, ConfigKind.TOGGLE, null, toggle, null, null, enabled, null);
   }

   private static ConfigRow actionSetting(String label, Runnable action) {
      return actionSetting(label, action, () -> "Open");
   }

   private static ConfigRow actionSetting(String label, Runnable action, Supplier<String> value) {
      return new ConfigRow(label, ConfigKind.ACTION, value, action, null, null, null, null);
   }

   private static ConfigRow stepperSetting(String label, Supplier<String> value, Runnable decrease, Runnable increase) {
      return new ConfigRow(label, ConfigKind.STEPPER, value, null, decrease, increase, null, null);
   }

   private static ConfigRow colorSetting(String label, IntSupplier color, Runnable action) {
      return new ConfigRow(label, ConfigKind.COLOR, null, action, null, null, null, color);
   }

   private static boolean inside(double x, double y, int left, int top, int width, int height) {
      return x >= left && x <= left + width && y >= top && y <= top + height;
   }

   private static int accentColor(int alpha) {
      return (alpha & 0xFF) << 24 | MoonConfig.modColor & 0xFFFFFF;
   }

   private record FeatureRow(String label, String description, String id, BooleanSupplier enabled, Runnable toggle) {
   }

   private record ConfigRow(String label, ConfigKind kind, Supplier<String> value, Runnable activate, Runnable decrease, Runnable increase, BooleanSupplier enabled, IntSupplier color) {
   }

   private enum ConfigKind {
      TOGGLE, ACTION, STEPPER, COLOR, INFO
   }
}
