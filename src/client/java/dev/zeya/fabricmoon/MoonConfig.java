package dev.zeya.fabricmoon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MoonConfig {
   private static final Logger LOGGER = LoggerFactory.getLogger("Zeya Fabric Moon");
   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("zeyafabricmoon.properties");
   public static boolean featuresEnabled = true;
   public static boolean showHud = true;
   public static boolean hideArmor = false;
   public static boolean hideHead = false;
   public static boolean fullbright = false;
   public static boolean leftAutoClick = false;
   public static boolean rightAutoClick = false;
   public static int leftCps = 10;
   public static int rightCps = 10;
   public static int macroExtraDelayMillis = 0;
   public static String authorizedMacroServers = "";
   public static boolean autoFishing = false;
   public static boolean bonemerangLoadoutSwap = false;
   public static int bonemerangLoadoutSlot = 1;
   public static boolean deathBowLoadoutSwap = false;
   public static int deathBowLoadoutSlot = 1;
   public static boolean blazeDaggerAutoSwitch = false;
   public static int blazeDaggerSlot = 1;
   public static int blazeDaggerModeDelayTicks = 10;
   public static int fishingCastDelayTicks = 20;
   public static int fishingReelDelaySeconds = 0;
   public static int minFishWaitTicks = 0;
   public static boolean disableFishingWhileOnScreen = true;
   public static boolean oxygenRecovery = false;
   public static int oxygenThreshold = 10;
   public static float playerSize = 1.0F;
   public static float mobSize = 1.0F;
   public static boolean shrinkLargeMobs = false;
   public static final Set<String> mobPlayerSizeTargets = new LinkedHashSet<>();
   public static boolean thirdPersonLookFromBelow = false;
   public static boolean playerGlow = false;
   public static int playerGlowColor = 7525832;
   public static boolean entityHitboxes = false;
   public static boolean combatHud = false;
   public static boolean combatHudShowDamage = true;
   public static boolean combatHudShowTargetHealth = false;
   public static boolean networkOptimizer = true;
   public static boolean hypixelAutoRoute = true;
   public static int statsHudX = 4;
   public static int statsHudY = -1;
   public static int combatHudX = -1;
   public static int combatHudY = 8;
   public static boolean slotBindEnabled = true;
   public static boolean slotLockEnabled = true;
   public static int slotBindKeyCode = -1;
   public static int slotLockKeyCode = -1;
   public static final Set<Integer> lockedSlots = new LinkedHashSet<>();
   public static final Map<Integer, Integer> slotBindings = new LinkedHashMap<>();
   public static boolean keyCommandsEnabled = true;
   public static final List<String> keyCommandBindings = new ArrayList<>();
   public static boolean wardrobeEnabled = true;
   public static boolean wardrobeCloseAfterUse = true;
   public static boolean wardrobePreventUnequip = false;
   public static boolean wardrobeUseHotbarBinds = true;
   public static final Map<String, String> wardrobeLoadouts = new LinkedHashMap<>();
   public static boolean wardrobeQuickSwapEnabled = false;
   public static final int[] wardrobeQuickSwapKeysWardrobe = new int[9];
   public static final int[] wardrobeQuickSwapKeysEquipment = new int[9];
   public static final int[] wardrobeQuickSwapKeysLoadout = new int[12];

   private MoonConfig() {
   }

   public static void load() {
      if (Files.exists(FILE)) {
         Properties properties = new Properties();

         try (InputStream input = Files.newInputStream(FILE)) {
            properties.load(input);
            featuresEnabled = Boolean.parseBoolean(properties.getProperty("featuresEnabled", "true"));
            showHud = Boolean.parseBoolean(properties.getProperty("showHud", "true"));
            hideArmor = Boolean.parseBoolean(properties.getProperty("hideArmor", "false"));
            hideHead = Boolean.parseBoolean(properties.getProperty("hideHead", "false"));
            fullbright = Boolean.parseBoolean(properties.getProperty("fullbright", "false"));
            leftAutoClick = Boolean.parseBoolean(properties.getProperty("leftAutoClick", "false"));
            rightAutoClick = Boolean.parseBoolean(properties.getProperty("rightAutoClick", "false"));
            leftCps = clampCps(parseInteger(properties, "leftCps", 10));
            rightCps = clampCps(parseInteger(properties, "rightCps", 10));
            macroExtraDelayMillis = clampMacroDelay(parseInteger(properties, "macroExtraDelayMillis", 0));
            authorizedMacroServers = properties.getProperty("authorizedMacroServers", "").trim();
            autoFishing = Boolean.parseBoolean(properties.getProperty("autoFishing", "false"));
            bonemerangLoadoutSwap = Boolean.parseBoolean(properties.getProperty("bonemerangLoadoutSwap", "false"));
            bonemerangLoadoutSlot = clampLoadoutSlot(parseInteger(properties, "bonemerangLoadoutSlot", 1));
            deathBowLoadoutSwap = Boolean.parseBoolean(properties.getProperty("deathBowLoadoutSwap", "false"));
            deathBowLoadoutSlot = clampLoadoutSlot(parseInteger(properties, "deathBowLoadoutSlot", 1));
            blazeDaggerAutoSwitch = Boolean.parseBoolean(properties.getProperty("blazeDaggerAutoSwitch", "false"));
            blazeDaggerSlot = clampLoadoutSlot(parseInteger(properties, "blazeDaggerSlot", 1));
            blazeDaggerModeDelayTicks = clampDelay(parseInteger(properties, "blazeDaggerModeDelayTicks", 10));
            fishingCastDelayTicks = clampDelay(parseInteger(properties, "fishingCastDelayTicks", 20));
            fishingReelDelaySeconds = clampSeconds(parseInteger(properties, "fishingReelDelaySeconds", parseInteger(properties, "fishingReelDelayTicks", 0)));
            minFishWaitTicks = clampDelay(parseInteger(properties, "minFishWaitTicks", 0));
            disableFishingWhileOnScreen = Boolean.parseBoolean(properties.getProperty("disableFishingWhileOnScreen", "true"));
            oxygenRecovery = Boolean.parseBoolean(properties.getProperty("oxygenRecovery", "false"));
            oxygenThreshold = clampOxygen(parseInteger(properties, "oxygenThreshold", 10));

            try {
               playerSize = clampSize(Float.parseFloat(properties.getProperty("playerSize", "1.0")));
            } catch (NumberFormatException exception) {
               LOGGER.warn("Invalid playerSize in {}; using 1.0", FILE, exception);
               playerSize = 1.0F;
            }

            shrinkLargeMobs = Boolean.parseBoolean(properties.getProperty("shrinkLargeMobs", "false"));

            try {
               mobSize = clampMobSize(Float.parseFloat(properties.getProperty("mobSize", "1.0")));
            } catch (NumberFormatException exception) {
               LOGGER.warn("Invalid mobSize in {}; using 1.0", FILE, exception);
               mobSize = 1.0F;
            }

            mobPlayerSizeTargets.clear();

            for (String entry : properties.getProperty("mobPlayerSizeTargets", "").split(",")) {
               String id = entry.trim();
               if (!id.isEmpty()) {
                  mobPlayerSizeTargets.add(id);
               }
            }

            thirdPersonLookFromBelow = Boolean.parseBoolean(properties.getProperty("thirdPersonLookFromBelow", "false"));
            playerGlow = Boolean.parseBoolean(properties.getProperty("playerGlow", "false"));
            entityHitboxes = Boolean.parseBoolean(properties.getProperty("entityHitboxes", "false"));
            combatHud = Boolean.parseBoolean(properties.getProperty("combatHud", "false"));
            combatHudShowDamage = Boolean.parseBoolean(properties.getProperty("combatHudShowDamage", "true"));
            combatHudShowTargetHealth = Boolean.parseBoolean(properties.getProperty("combatHudShowTargetHealth", "false"));
            networkOptimizer = Boolean.parseBoolean(properties.getProperty("networkOptimizer", "true"));
            hypixelAutoRoute = Boolean.parseBoolean(properties.getProperty("hypixelAutoRoute", "true"));
            statsHudX = clampHudPosition(parseInteger(properties, "statsHudX", 4));
            statsHudY = clampHudPosition(parseInteger(properties, "statsHudY", -1));
            combatHudX = clampHudPosition(parseInteger(properties, "combatHudX", -1));
            combatHudY = clampHudPosition(parseInteger(properties, "combatHudY", 8));
            slotBindEnabled = Boolean.parseBoolean(properties.getProperty("slotBindEnabled", "true"));
            slotLockEnabled = Boolean.parseBoolean(properties.getProperty("slotLockEnabled", "false"));
            slotBindKeyCode = parseInteger(properties, "slotBindKeyCode", -1);
            slotLockKeyCode = parseInteger(properties, "slotLockKeyCode", -1);
            slotBindings.clear();

            for (String pair : properties.getProperty("slotBindings", "").split(",")) {
               String[] parts = pair.split(":", 2);
               if (parts.length == 2) {
                  try {
                     int first = Integer.parseInt(parts[0]);
                     int second = Integer.parseInt(parts[1]);
                     if (second >= 36 && second <= 44) {
                        second -= 36;
                     }

                     int hotbar = first >= 0 && first < 9 ? first : second;
                     int inventory = first >= 9 && first < 40 ? first : second;
                     if (hotbar >= 0 && hotbar < 9 && inventory >= 9 && inventory < 40) {
                        slotBindings.put(inventory, hotbar);
                     }
                  } catch (NumberFormatException var19) {
                  }
               }
            }

            lockedSlots.clear();

            for (String entry : properties.getProperty("lockedSlots", "").split(",")) {
               String slot = entry.trim();
               if (!slot.isEmpty()) {
                  try {
                     lockedSlots.add(Integer.parseInt(slot));
                  } catch (NumberFormatException var16) {
                  }
               }
            }

            keyCommandsEnabled = Boolean.parseBoolean(properties.getProperty("keyCommandsEnabled", "true"));
            keyCommandBindings.clear();

            for (String entry : properties.getProperty("keyCommandBindings", "").split("\\|")) {
               String binding = entry.trim();
               if (!binding.isEmpty()) {
                  keyCommandBindings.add(binding);
               }
            }

            wardrobeEnabled = Boolean.parseBoolean(properties.getProperty("wardrobeEnabled", "true"));
            wardrobeCloseAfterUse = Boolean.parseBoolean(properties.getProperty("wardrobeCloseAfterUse", "true"));
            wardrobePreventUnequip = Boolean.parseBoolean(properties.getProperty("wardrobePreventUnequip", "false"));
            wardrobeUseHotbarBinds = Boolean.parseBoolean(properties.getProperty("wardrobeUseHotbarBinds", "true"));
            wardrobeQuickSwapEnabled = Boolean.parseBoolean(properties.getProperty("wardrobeQuickSwapEnabled", "false"));
            String wKeyStr = properties.getProperty("wardrobeQuickSwapKeysWardrobe", "");
            String[] wKeyParts = wKeyStr.split(",");

            for (int i = 0; i < 9; i++) {
               wardrobeQuickSwapKeysWardrobe[i] = -1;
            }

            for (int i = 0; i < Math.min(9, wKeyParts.length); i++) {
               String part = wKeyParts[i].trim();
               if (!part.isEmpty()) {
                  wardrobeQuickSwapKeysWardrobe[i] = Integer.parseInt(part);
               }
            }

            String eKeyStr = properties.getProperty("wardrobeQuickSwapKeysEquipment", "");
            String[] eKeyParts = eKeyStr.split(",");

            for (int i = 0; i < 9; i++) {
               wardrobeQuickSwapKeysEquipment[i] = -1;
            }

            for (int i = 0; i < Math.min(9, eKeyParts.length); i++) {
               String part = eKeyParts[i].trim();
               if (!part.isEmpty()) {
                  wardrobeQuickSwapKeysEquipment[i] = Integer.parseInt(part);
               }
            }

            String lKeyStr = properties.getProperty("wardrobeQuickSwapKeysLoadout", "");
            String[] lKeyParts = lKeyStr.split(",");

            for (int i = 0; i < 12; i++) {
               wardrobeQuickSwapKeysLoadout[i] = -1;
            }

            for (int i = 0; i < Math.min(12, lKeyParts.length); i++) {
               String part = lKeyParts[i].trim();
               if (!part.isEmpty()) {
                  wardrobeQuickSwapKeysLoadout[i] = Integer.parseInt(part);
               }
            }

            String legacyKeyStr = properties.getProperty("wardrobeQuickSwapKeys", "");
            if (legacyKeyStr.isEmpty() && wKeyStr.isEmpty()) {
               int legacyKey = parseInteger(properties, "wardrobeQuickSwapKeyCode", -1);
               int legacySlot = clampLoadoutSlot(parseInteger(properties, "wardrobeQuickSwapSlot", 1));
               if (legacyKey >= 0 && legacySlot >= 1 && legacySlot <= 12) {
                  if (legacySlot <= 9) {
                     wardrobeQuickSwapKeysWardrobe[legacySlot - 1] = legacyKey;
                  } else {
                     wardrobeQuickSwapKeysLoadout[legacySlot - 1] = legacyKey;
                  }
               }
            }

            wardrobeLoadouts.clear();

            for (String entry : properties.getProperty("wardrobeLoadouts", "").split("\\|")) {
               String[] parts = entry.split(":", 2);
               if (parts.length == 2) {
                  wardrobeLoadouts.put(parts[0].trim(), parts[1].trim());
               }
            }

            try {
               String color = properties.getProperty("playerGlowColor", "72D5C8").trim();
               if (color.startsWith("#")) {
                  color = color.substring(1);
               }

               playerGlowColor = clampColor(Integer.parseInt(color, 16));
            } catch (NumberFormatException exception) {
               LOGGER.warn("Invalid playerGlowColor in {}; using 72D5C8", FILE, exception);
               playerGlowColor = 7525832;
            }
         } catch (IOException exception) {
            LOGGER.error("Could not read configuration from {}", FILE, exception);
         }
      }
   }

   public static void save() {
      Properties properties = new Properties();
      properties.setProperty("featuresEnabled", Boolean.toString(featuresEnabled));
      properties.setProperty("showHud", Boolean.toString(showHud));
      properties.setProperty("hideArmor", Boolean.toString(hideArmor));
      properties.setProperty("hideHead", Boolean.toString(hideHead));
      properties.setProperty("fullbright", Boolean.toString(fullbright));
      properties.setProperty("leftAutoClick", Boolean.toString(leftAutoClick));
      properties.setProperty("rightAutoClick", Boolean.toString(rightAutoClick));
      properties.setProperty("leftCps", Integer.toString(leftCps));
      properties.setProperty("rightCps", Integer.toString(rightCps));
      properties.setProperty("macroExtraDelayMillis", Integer.toString(macroExtraDelayMillis));
      properties.setProperty("authorizedMacroServers", authorizedMacroServers);
      properties.setProperty("autoFishing", Boolean.toString(autoFishing));
      properties.setProperty("bonemerangLoadoutSwap", Boolean.toString(bonemerangLoadoutSwap));
      properties.setProperty("bonemerangLoadoutSlot", Integer.toString(bonemerangLoadoutSlot));
      properties.setProperty("deathBowLoadoutSwap", Boolean.toString(deathBowLoadoutSwap));
      properties.setProperty("deathBowLoadoutSlot", Integer.toString(deathBowLoadoutSlot));
      properties.setProperty("blazeDaggerAutoSwitch", Boolean.toString(blazeDaggerAutoSwitch));
      properties.setProperty("blazeDaggerSlot", Integer.toString(blazeDaggerSlot));
      properties.setProperty("blazeDaggerModeDelayTicks", Integer.toString(blazeDaggerModeDelayTicks));
      properties.setProperty("fishingCastDelayTicks", Integer.toString(fishingCastDelayTicks));
      properties.setProperty("fishingReelDelaySeconds", Integer.toString(fishingReelDelaySeconds));
      properties.setProperty("minFishWaitTicks", Integer.toString(minFishWaitTicks));
      properties.setProperty("disableFishingWhileOnScreen", Boolean.toString(disableFishingWhileOnScreen));
      properties.setProperty("oxygenRecovery", Boolean.toString(oxygenRecovery));
      properties.setProperty("oxygenThreshold", Integer.toString(oxygenThreshold));
      properties.setProperty("playerSize", Float.toString(playerSize));
      properties.setProperty("shrinkLargeMobs", Boolean.toString(shrinkLargeMobs));
      properties.setProperty("mobSize", Float.toString(mobSize));
      properties.setProperty("mobPlayerSizeTargets", String.join(",", mobPlayerSizeTargets));
      properties.setProperty("thirdPersonLookFromBelow", Boolean.toString(thirdPersonLookFromBelow));
      properties.setProperty("playerGlow", Boolean.toString(playerGlow));
      properties.setProperty("playerGlowColor", String.format(Locale.ROOT, "%06X", clampColor(playerGlowColor)));
      properties.setProperty("entityHitboxes", Boolean.toString(entityHitboxes));
      properties.setProperty("combatHud", Boolean.toString(combatHud));
      properties.setProperty("combatHudShowDamage", Boolean.toString(combatHudShowDamage));
      properties.setProperty("combatHudShowTargetHealth", Boolean.toString(combatHudShowTargetHealth));
      properties.setProperty("networkOptimizer", Boolean.toString(networkOptimizer));
      properties.setProperty("hypixelAutoRoute", Boolean.toString(hypixelAutoRoute));
      properties.setProperty("statsHudX", Integer.toString(statsHudX));
      properties.setProperty("statsHudY", Integer.toString(statsHudY));
      properties.setProperty("combatHudX", Integer.toString(combatHudX));
      properties.setProperty("combatHudY", Integer.toString(combatHudY));
      properties.setProperty("slotBindEnabled", Boolean.toString(slotBindEnabled));
      properties.setProperty("slotLockEnabled", Boolean.toString(slotLockEnabled));
      properties.setProperty("slotBindKeyCode", Integer.toString(slotBindKeyCode));
      properties.setProperty("slotLockKeyCode", Integer.toString(slotLockKeyCode));
      List<String> slotBindingStrings = new ArrayList<>();

      for (Entry<Integer, Integer> entry : slotBindings.entrySet()) {
         slotBindingStrings.add(entry.getKey() + ":" + entry.getValue());
      }

      properties.setProperty("slotBindings", String.join(",", slotBindingStrings));
      List<String> slotStrings = new ArrayList<>();

      for (int slot : lockedSlots) {
         slotStrings.add(Integer.toString(slot));
      }

      properties.setProperty("lockedSlots", String.join(",", slotStrings));
      properties.setProperty("keyCommandsEnabled", Boolean.toString(keyCommandsEnabled));
      properties.setProperty("keyCommandBindings", String.join("|", keyCommandBindings));
      properties.setProperty("wardrobeEnabled", Boolean.toString(wardrobeEnabled));
      properties.setProperty("wardrobeCloseAfterUse", Boolean.toString(wardrobeCloseAfterUse));
      properties.setProperty("wardrobePreventUnequip", Boolean.toString(wardrobePreventUnequip));
      properties.setProperty("wardrobeUseHotbarBinds", Boolean.toString(wardrobeUseHotbarBinds));
      properties.setProperty("wardrobeQuickSwapEnabled", Boolean.toString(wardrobeQuickSwapEnabled));
      StringBuilder wKeyBuilder = new StringBuilder();

      for (int i = 0; i < 9; i++) {
         if (i > 0) {
            wKeyBuilder.append(',');
         }

         wKeyBuilder.append(wardrobeQuickSwapKeysWardrobe[i]);
      }

      properties.setProperty("wardrobeQuickSwapKeysWardrobe", wKeyBuilder.toString());
      StringBuilder eKeyBuilder = new StringBuilder();

      for (int i = 0; i < 9; i++) {
         if (i > 0) {
            eKeyBuilder.append(',');
         }

         eKeyBuilder.append(wardrobeQuickSwapKeysEquipment[i]);
      }

      properties.setProperty("wardrobeQuickSwapKeysEquipment", eKeyBuilder.toString());
      StringBuilder lKeyBuilder = new StringBuilder();

      for (int i = 0; i < 12; i++) {
         if (i > 0) {
            lKeyBuilder.append(',');
         }

         lKeyBuilder.append(wardrobeQuickSwapKeysLoadout[i]);
      }

      properties.setProperty("wardrobeQuickSwapKeysLoadout", lKeyBuilder.toString());
      List<String> loadoutStrings = new ArrayList<>();

      for (Entry<String, String> entry : wardrobeLoadouts.entrySet()) {
         loadoutStrings.add(entry.getKey() + ":" + entry.getValue());
      }

      properties.setProperty("wardrobeLoadouts", String.join("|", loadoutStrings));

      try {
         Files.createDirectories(FILE.getParent());

         try (OutputStream output = Files.newOutputStream(FILE)) {
            properties.store(output, "Zeya Fabric Moon client visuals");
         }
      } catch (IOException exception) {
         LOGGER.error("Could not save configuration to {}", FILE, exception);
      }
   }

   public static float clampSize(float size) {
      return !Float.isFinite(size) ? 1.0F : Math.max(0.1F, Math.min(1.5F, size));
   }

   public static float clampMobSize(float size) {
      return !Float.isFinite(size) ? 1.0F : Math.max(0.1F, Math.min(3.0F, size));
   }

   public static int clampCps(int cps) {
      return Math.max(1, Math.min(15, cps));
   }

   public static int clampMacroDelay(int millis) {
      return Math.max(0, Math.min(2000, millis));
   }

   public static int clampDelay(int ticks) {
      return Math.max(0, Math.min(200, ticks));
   }

   public static int clampSeconds(int seconds) {
      return Math.max(0, Math.min(200, seconds));
   }

   public static int clampLoadoutSlot(int slot) {
      return Math.max(1, Math.min(12, slot));
   }

   public static int clampOxygen(int air) {
      return Math.max(0, Math.min(300, air));
   }

   public static int clampColor(int color) {
      return Math.max(0, Math.min(16777215, color));
   }

   public static int clampHudPosition(int position) {
      return Math.max(-1, Math.min(10000, position));
   }

   public static int clampSlot(int slot) {
      return Math.max(0, Math.min(35, slot));
   }

   public static boolean isMobPlayerSizeTarget(String id) {
      return id != null && !id.isEmpty() && !"minecraft:magma_cube".equals(id) && mobPlayerSizeTargets.contains(id);
   }

   public static void toggleMobPlayerSizeTarget(String id) {
      if (id != null && !id.isEmpty() && !"minecraft:magma_cube".equals(id)) {
         if (!mobPlayerSizeTargets.remove(id)) {
            mobPlayerSizeTargets.add(id);
         }
      }
   }

   public static void addMobPlayerSizeTargets(Collection<String> ids) {
      for (String id : ids) {
         if (id != null && !id.isEmpty() && !"minecraft:magma_cube".equals(id)) {
            mobPlayerSizeTargets.add(id);
         }
      }
   }

   public static void clearMobPlayerSizeTargets() {
      mobPlayerSizeTargets.clear();
   }

   private static int parseInteger(Properties properties, String key, int fallback) {
      try {
         return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
      } catch (NumberFormatException exception) {
         LOGGER.warn("Invalid {} in {}; using {}", new Object[]{key, FILE, fallback, exception});
         return fallback;
      }
   }
}
