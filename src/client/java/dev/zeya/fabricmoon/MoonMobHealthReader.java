package dev.zeya.fabricmoon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import dev.zeya.fabricmoon.mixin.BossHealthOverlayAccessor;

final class MoonMobHealthReader {
   private static final String NUMBER = "([0-9][0-9,.]*\\s*(?:kk|mi|bi|tri|k|m|b|t)?)";
   private static final String HEART = "(?:\\x{2764}\\x{FE0F}?|\\x{2665}|❤|♥)";
   private static final String HEALTH_WORD = "(?:\\bhp\\b|\\bhealth\\b|\\bvida\\b|\\blife\\b)";
   private static final String PAIR_SEPARATOR = "(?:/|\\bof\\b|\\bde\\b)";
   private static final Pattern[] HEALTH_PAIRS = new Pattern[]{
      Pattern.compile("(?i)" + HEALTH_WORD + "\\s*[:=]?\\s*" + NUMBER + "\\s*" + PAIR_SEPARATOR + "\\s*" + NUMBER),
      Pattern.compile("(?i)" + NUMBER + "\\s*" + PAIR_SEPARATOR + "\\s*" + NUMBER + "\\s*" + HEART),
      Pattern.compile("(?i)" + HEART + "\\s*" + NUMBER + "\\s*" + PAIR_SEPARATOR + "\\s*" + NUMBER),
      Pattern.compile("(?i)" + NUMBER + "\\s*" + PAIR_SEPARATOR + "\\s*" + NUMBER + "\\s*" + HEALTH_WORD),
      Pattern.compile("(?i)(?<![0-9.,])" + NUMBER + "\\s*/\\s*" + NUMBER + "(?![0-9.,])"),
      Pattern.compile("(?i)(?<![0-9.,])" + NUMBER + "\\s+(?:de|of)\\s+" + NUMBER + "(?![0-9.,])")
   };
   private static final Pattern[] HEALTH_VALUES = new Pattern[]{
      Pattern.compile("(?i)" + HEALTH_WORD + "\\s*[:=]?\\s*" + NUMBER),
      Pattern.compile("(?i)" + NUMBER + "\\s*" + HEART),
      Pattern.compile("(?i)" + HEART + "\\s*" + NUMBER),
      Pattern.compile("(?i)" + NUMBER + "\\s*(?:" + HEART + "|" + HEALTH_WORD + ")")
   };
   private static final long CACHE_NANOS = 100000000L;
   private static final long FLOATING_DAMAGE_SCAN_INTERVAL_NANOS = 50000000L;
   private static final long LABEL_MEMORY_NANOS = 10000000000L;
   private static final long USED_LABEL_NANOS = 2000000000L;
   private static final long DISPLAY_NAME_CACHE_NANOS = 1000000000L;
   private static final Pattern DAMAGE_NUMBER = Pattern.compile("^[^0-9A-Za-z]*([0-9][0-9,.]*)\\s*((?i:kk|mi|bi|tri|k|m|b|t)?)[^0-9A-Za-z]*$");
   private static final Map<Integer, String> LABEL_TEXTS = new HashMap<>();
   private static final Map<Integer, Long> LABEL_SEEN = new HashMap<>();
   private static final Map<Integer, Long> COUNTED_LABELS = new HashMap<>();
   private static final Map<Integer, String> COUNTED_LABEL_TEXTS = new HashMap<>();
   private static ClientLevel cachedLevel;
   private static int cachedEntityId = Integer.MIN_VALUE;
   private static long cachedAtNanos;
   private static MoonMobHealthReader.HealthReading cachedReading;
   private static ClientLevel cachedNameLevel;
   private static int cachedNameEntityId = Integer.MIN_VALUE;
   private static long cachedNameAtNanos;
   private static String cachedDisplayName;
   private static long lastFloatingDamageScanNanos;

   private MoonMobHealthReader() {
   }

   static MoonMobHealthReader.HealthReading read(LivingEntity target) {
      long now = System.nanoTime();
      if (target.level() instanceof ClientLevel level && level == cachedLevel && target.getId() == cachedEntityId && now - cachedAtNanos < 100000000L) {
         return cachedReading;
      } else {
         cachedLevel = target.level() instanceof ClientLevel level ? level : null;
         cachedEntityId = target.getId();
         cachedAtNanos = now;
         cachedReading = findReading(target);
         return cachedReading;
      }
   }

   static String displayName(LivingEntity target) {
      long now = System.nanoTime();
      ClientLevel level = (ClientLevel)target.level();
      if (level == cachedNameLevel && target.getId() == cachedNameEntityId && now - cachedNameAtNanos < 1000000000L) {
         return cachedDisplayName;
      }

      Component nameComponent = target instanceof Player ? target.getName() : target.getDisplayName();
      String entityName = nameComponent != null ? nameComponent.getString().trim() : "";
      if (!isGeneratedName(entityName)) {
         cachedNameLevel = level;
         cachedNameEntityId = target.getId();
         cachedNameAtNanos = now;
         cachedDisplayName = entityName;
         return cachedDisplayName;
      }

      AABB searchArea = target.getBoundingBox().inflate(2.0, 8.0, 2.0);
      String labelName = null;
      double closestDistance = Double.MAX_VALUE;

      for (Entity entity : level.getEntities(target, searchArea)) {
         if ((entity instanceof TextDisplay || entity instanceof ArmorStand)
            && !(entity.getY() < target.getY() + target.getBbHeight() - 0.25)
            && !(entity.getY() > target.getY() + target.getBbHeight() + 6.0)) {
            String text = textOf(entity);

            for (String line : text.split("\\R")) {
               String candidate = cleanNameCandidate(line);
               if (!candidate.isEmpty()
                  && !candidate.equalsIgnoreCase("CLICK")
                  && parseHealthText(candidate) == null
                  && !(parseDamageText(candidate) > 0.0)
                  && !candidate.matches(".*\\d.*[/_].*")) {
                  double distance = entity.getBoundingBox().distanceToSqr(target.getBoundingBox());
                  if (distance < closestDistance) {
                     labelName = candidate;
                     closestDistance = distance;
                  }
               }
            }
         }
      }

      cachedNameLevel = level;
      cachedNameEntityId = target.getId();
      cachedNameAtNanos = now;
      cachedDisplayName = labelName != null ? labelName : (target instanceof Player ? "NPC" : "Mob");
      return cachedDisplayName;
   }

   static boolean hasGeneratedName(LivingEntity target) {
      Component nameComponent = target instanceof Player ? target.getName() : target.getDisplayName();
      String name = nameComponent != null ? nameComponent.getString().trim() : "";
      return isGeneratedName(name);
   }

   private static boolean isGeneratedName(String name) {
      return name.matches(".*\\d.*") && name.matches(".*[A-Za-z].*") && (name.contains("/") || name.contains("_") || name.length() >= 10);
   }

   private static String cleanNameCandidate(String text) {
      return text.replaceAll("\\u00A7.", "").replaceFirst("(?i)\\s+(?:\\[?CLICK\\]?)\\s*$", "").trim();
   }

   static List<MoonMobHealthReader.FloatingDamage> collectFloatingDamages(ClientLevel level, Player player, boolean primeOnly) {
      long now = System.nanoTime();
      if (!primeOnly && now - lastFloatingDamageScanNanos < 50000000L) {
         return List.of();
      }

      lastFloatingDamageScanNanos = now;
      LABEL_SEEN.entrySet().removeIf(entry -> now - entry.getValue() > 10000000000L);
      LABEL_TEXTS.keySet().removeIf(id -> !LABEL_SEEN.containsKey(id));
      COUNTED_LABELS.entrySet().removeIf(entry -> now > entry.getValue());
      COUNTED_LABEL_TEXTS.keySet().removeIf(id -> !COUNTED_LABELS.containsKey(id));
      List<MoonMobHealthReader.FloatingDamage> found = new ArrayList<>();

      for (Entity entity : level.entitiesForRendering()) {
         if (entity instanceof TextDisplay || entity instanceof ArmorStand) {
            String text = textOf(entity).trim();
            if (!text.isEmpty()) {
               int labelId = entity.getId();
               String previousText = LABEL_TEXTS.get(labelId);
               LABEL_TEXTS.put(labelId, text);
               LABEL_SEEN.put(labelId, now);
               Long countedUntil = COUNTED_LABELS.get(labelId);
               if (!primeOnly && (countedUntil == null || now > countedUntil || !text.equals(COUNTED_LABEL_TEXTS.get(labelId)))) {
                  boolean fresh = previousText == null || !previousText.equals(text);
                  if (fresh) {
                     double amount = parseDamageText(text);
                     if (!(amount <= 0.0) && parseHealthText(text) == null) {
                        LivingEntity victim = findLabelVictim(level, player, entity);
                        COUNTED_LABELS.put(labelId, now + 2000000000L);
                        COUNTED_LABEL_TEXTS.put(labelId, text);
                        found.add(new MoonMobHealthReader.FloatingDamage(victim == null ? -1 : victim.getId(), amount));
                     }
                  }
               }
            }
         }
      }

      return found;
   }

   static double readFloatingDamage(LivingEntity victim) {
      long now = System.nanoTime();
      LABEL_SEEN.entrySet().removeIf(entry -> now - entry.getValue() > 10000000000L);
      LABEL_TEXTS.keySet().removeIf(id -> !LABEL_SEEN.containsKey(id));
      COUNTED_LABELS.entrySet().removeIf(entry -> now > entry.getValue());
      COUNTED_LABEL_TEXTS.keySet().removeIf(id -> !COUNTED_LABELS.containsKey(id));
      double best = -1.0;
      int bestId = -1;
      String bestText = null;
      AABB searchArea = victim.getBoundingBox().inflate(10.0, 24.0, 10.0);

      for (Entity entity : victim.level().getEntities(victim, searchArea)) {
         if ((entity instanceof TextDisplay || entity instanceof ArmorStand) && !(horizontalDistanceSquared(entity, victim) > 100.0)) {
            String text = textOf(entity).trim();
            if (!text.isEmpty()) {
               int labelId = entity.getId();
               String previousText = LABEL_TEXTS.put(labelId, text);
               LABEL_SEEN.put(labelId, now);
               Long countedUntil = COUNTED_LABELS.get(labelId);
               if ((countedUntil == null || now > countedUntil || !text.equals(COUNTED_LABEL_TEXTS.get(labelId)))
                  && (previousText == null || !previousText.equals(text))) {
                  double amount = parseDamageText(text);
                  if (amount > best && parseHealthText(text) == null) {
                     best = amount;
                     bestId = labelId;
                     bestText = text;
                  }
               }
            }
         }
      }

      if (bestId >= 0) {
         COUNTED_LABELS.put(bestId, now + 2000000000L);
         COUNTED_LABEL_TEXTS.put(bestId, bestText);
      }

      return best;
   }

   private static LivingEntity findLabelVictim(ClientLevel level, Player player, Entity label) {
      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      AABB nearby = label.getBoundingBox().inflate(10.0, 24.0, 10.0);

      for (Entity entity : level.getEntities(label, nearby)) {
         if (entity instanceof LivingEntity living && entity != player && !(entity instanceof ArmorStand) && living.isAlive() && !living.isSpectator()) {
            double dx = entity.getX() - label.getX();
            double dz = entity.getZ() - label.getZ();
            double horizontal = dx * dx + dz * dz;
            if (!(horizontal > 100.0) && !(label.getY() < entity.getY() - 4.0) && !(label.getY() > entity.getY() + entity.getBbHeight() + 24.0)) {
               double vertical = entity.getY() + entity.getBbHeight() * 0.5 - label.getY();
               double distance = horizontal + vertical * vertical * 0.25;
               if (distance < bestDistance) {
                  bestDistance = distance;
                  best = living;
               }
            }
         }
      }

      return best;
   }

   private static double parseDamageText(String text) {
      text = text.replace("️", "");
      if (text.indexOf(47) >= 0) {
         return -1.0;
      }

      String lower = text.toLowerCase(Locale.ROOT);
      if (!lower.contains("hp") && !lower.contains("health") && !text.contains("❤") && !text.contains("♥")) {
         Matcher matcher = DAMAGE_NUMBER.matcher(text);
         if (!matcher.matches()) {
            for (String line : text.split("\\R")) {
               Matcher lineMatcher = DAMAGE_NUMBER.matcher(line.trim());
               if (lineMatcher.matches()) {
                  double value = parseNumber(lineMatcher.group(1) + lineMatcher.group(2));
                  if (Double.isFinite(value) && value > 0.0) {
                     return value;
                  }
               }
            }

            return -1.0;
         } else {
            double value = parseNumber(matcher.group(1) + matcher.group(2));
            return Double.isFinite(value) && value > 0.0 ? value : -1.0;
         }
      } else {
         return -1.0;
      }
   }

   private static MoonMobHealthReader.HealthReading findReading(LivingEntity target) {
      MoonMobHealthReader.HealthReading labelReading = findLabelReading(target);
      MoonMobHealthReader.HealthReading bossBarReading = readBossBar(target, labelReading);
      if (bossBarReading != null) {
         if (labelReading == null || comparableMaximum(bossBarReading) >= comparableMaximum(labelReading)) {
            return bossBarReading;
         }
      }

      if (labelReading == null && target instanceof Player player) {
         MoonMobHealthReader.HealthReading tabReading = readTabListHealth(player);
         if (tabReading != null) {
            return tabReading;
         }
      }

      return labelReading;
   }

   private static MoonMobHealthReader.HealthReading findLabelReading(LivingEntity target) {
      MoonMobHealthReader.HealthReading reading = parseHealthText(textOf(target));
      if (reading != null) {
         return reading;
      }

      AABB searchArea = target.getBoundingBox().inflate(2.0, 8.0, 2.0);
      ClientLevel level = (ClientLevel)target.level();
      MoonMobHealthReader.HealthReading nearestReading = null;
      double nearestDistance = Double.MAX_VALUE;

      for (Entity entity : level.getEntities(target, searchArea)) {
         if (entity instanceof TextDisplay || entity instanceof ArmorStand) {
            double minY = target.getY() - 1.0;
            double maxY = target.getY() + target.getBbHeight() + 6.0;
            if (!(entity.getY() < minY) && !(entity.getY() > maxY) && isClosestHealthLabel(entity, target)) {
               reading = parseHealthText(textOf(entity));
               if (reading != null) {
                  double distance = entity.getBoundingBox().distanceToSqr(target.getBoundingBox());
                  if (distance < nearestDistance) {
                     nearestReading = reading;
                     nearestDistance = distance;
                  }
               }
            }
         }
      }

      return nearestReading;
   }

   private static double comparableMaximum(MoonMobHealthReader.HealthReading reading) {
      return reading.maximum() != null ? reading.maximum() : reading.current();
   }

   private static MoonMobHealthReader.HealthReading readBossBar(LivingEntity target, MoonMobHealthReader.HealthReading labelReading) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.gui == null) {
         return null;
      }

      BossHealthOverlay overlay = client.gui.hud.getBossOverlay();
      Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor)overlay).zeyafabricmoon$getEvents();
      if (events.isEmpty()) {
         return null;
      }

      String targetName = normalizeBossName(displayName(target));
      MoonMobHealthReader.HealthReading fallback = null;

      for (LerpingBossEvent event : events.values()) {
         String barText = event.getName() != null ? event.getName().getString() : "";
         boolean matches = bossBarMatches(targetName, normalizeBossName(barText));
         MoonMobHealthReader.HealthReading reading = parseHealthText(barText);
         if (reading == null) {
            if (matches && labelReading != null && labelReading.maximum() == null) {
               return applyBossBarProgress(labelReading, event.getProgress());
            }

            continue;
         }

         reading = applyBossBarProgress(reading, event.getProgress());
         if (matches) {
            return reading;
         }

         if (fallback == null && events.size() == 1 && !(target instanceof Player)) {
            fallback = reading;
         }
      }

      return fallback;
   }

   private static MoonMobHealthReader.HealthReading applyBossBarProgress(MoonMobHealthReader.HealthReading reading, float progress) {
      if (reading.maximum() != null || !(progress > 0.0F) || progress > 1.0F) {
         return reading;
      }

      double estimatedMaximum = reading.current() / progress;
      if (!Double.isFinite(estimatedMaximum) || estimatedMaximum < reading.current()) {
         return reading;
      }

      return new MoonMobHealthReader.HealthReading(reading.current(), estimatedMaximum);
   }

   private static boolean bossBarMatches(String targetName, String barName) {
      if (targetName.length() < 3 || barName.length() < 3) {
         return false;
      }

      if (targetName.equals("npc") || targetName.equals("mob") || targetName.equals("entity") || targetName.equals("unknown")) {
         return false;
      }

      return barName.contains(targetName) || targetName.contains(barName);
   }

   private static String normalizeBossName(String text) {
      return text == null ? "" : text.replaceAll("\\u00A7.", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
   }

   private static MoonMobHealthReader.HealthReading readTabListHealth(Player target) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.player.connection == null) {
         return null;
      }

      PlayerInfo info = client.player.connection.getPlayerInfo(target.getUUID());
      if (info == null) {
         return null;
      }

      Component tabName = info.getTabListDisplayName();
      return tabName != null ? parseHealthText(tabName.getString()) : null;
   }

   private static boolean isClosestHealthLabel(Entity label, LivingEntity target) {
      double targetDistance = label.getBoundingBox().distanceToSqr(target.getBoundingBox());
      AABB searchArea = label.getBoundingBox().inflate(2.0, 4.0, 2.0);

      for (Entity entity : target.level().getEntities(label, searchArea)) {
         if (entity instanceof LivingEntity other && other != target && !(other instanceof ArmorStand) && other.isAlive()) {
            double otherDistance = label.getBoundingBox().distanceToSqr(other.getBoundingBox());
            if (otherDistance <= targetDistance + 0.01) {
               return false;
            }
         }
      }

      return true;
   }

   private static String textOf(Entity entity) {
      if (entity instanceof TextDisplay textDisplay) {
         Component text = textDisplay.getText();
         return text != null ? text.getString() : "";
      } else {
         return (entity instanceof LivingEntity || entity instanceof ArmorStand) && entity.hasCustomName() ? entity.getCustomName().getString() : "";
      }
   }

   private static MoonMobHealthReader.HealthReading parseHealthText(String text) {
      if (text.isBlank()) {
         return null;
      }

      text = text.replace("️", "");

      for (Pattern pattern : HEALTH_PAIRS) {
         Matcher pair = pattern.matcher(text);
         if (pair.find()) {
            double current = parseNumber(pair.group(1));
            double maximum = parseNumber(pair.group(2));
            if (Double.isFinite(current) && Double.isFinite(maximum) && current >= 0.0 && maximum > 0.0 && current <= maximum) {
               return new MoonMobHealthReader.HealthReading(current, maximum);
            }
         }
      }

      for (Pattern pattern : HEALTH_VALUES) {
         Matcher value = pattern.matcher(text);
         if (value.find()) {
            double current = parseNumber(value.group(1));
            if (Double.isFinite(current) && current >= 0.0) {
               return new MoonMobHealthReader.HealthReading(current, null);
            }
         }
      }

      return null;
   }

   private static double parseNumber(String number) {
      String normalized = number.replace(" ", "").toLowerCase(Locale.ROOT);
      double multiplier = 1.0;
      if (normalized.endsWith("tri")) {
         multiplier = 1.0E12;
         normalized = normalized.substring(0, normalized.length() - 3);
      } else if (normalized.endsWith("bi")) {
         multiplier = 1.0E9;
         normalized = normalized.substring(0, normalized.length() - 2);
      } else if (normalized.endsWith("mi") || normalized.endsWith("kk")) {
         multiplier = 1000000.0;
         normalized = normalized.substring(0, normalized.length() - 2);
      } else {
         char last = normalized.charAt(normalized.length() - 1);
         if (Character.isLetter(last)) {
            multiplier = switch (last) {
               case 'b' -> 1.0E9;
               case 'k' -> 1000.0;
               case 'm' -> 1000000.0;
               case 't' -> 1.0E12;
               default -> 1.0;
            };
            normalized = normalized.substring(0, normalized.length() - 1);
         }
      }

      if (normalized.isEmpty()) {
         return -1.0;
      }

      int comma = normalized.lastIndexOf(44);
      int period = normalized.lastIndexOf(46);
      if (comma >= 0 && period >= 0) {
         char decimalSeparator = (char)(comma > period ? 44 : 46);
         char groupingSeparator = (char)(decimalSeparator == ',' ? 46 : 44);
         normalized = normalized.replace(String.valueOf(groupingSeparator), "");
         normalized = normalized.replace(decimalSeparator, '.');
      } else if (comma >= 0 || period >= 0) {
         char separator = (char)(comma >= 0 ? 44 : 46);
         int separatorCount = 0;

         for (int index = 0; index < normalized.length(); index++) {
            if (normalized.charAt(index) == separator) {
               separatorCount++;
            }
         }

         int fractionalDigits = normalized.length() - normalized.lastIndexOf(separator) - 1;
         if (separatorCount != 1 || multiplier == 1.0 && fractionalDigits == 3) {
            normalized = normalized.replace(String.valueOf(separator), "");
         } else {
            normalized = normalized.replace(separator, '.');
         }
      }

      try {
         return Double.parseDouble(normalized) * multiplier;
      } catch (NumberFormatException exception) {
         return -1.0;
      }
   }

   private static double horizontalDistanceSquared(Entity first, Entity second) {
      double dx = first.getX() - second.getX();
      double dz = first.getZ() - second.getZ();
      return dx * dx + dz * dz;
   }

   record FloatingDamage(int victimId, double amount) {
   }

   record HealthReading(double current, Double maximum) {
   }
}
