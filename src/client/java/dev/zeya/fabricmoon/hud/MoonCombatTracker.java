package dev.zeya.fabricmoon.hud;

import dev.zeya.fabricmoon.MoonConfig;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class MoonCombatTracker {
   private static final long DPS_WINDOW_NANOS = 3000000000L;
   private static final long COMBAT_IDLE_RESET_NANOS = 5000000000L;
   private static final long MIN_DPS_DURATION_NANOS = 500000000L;
   private static final long CHAT_SUMMARY_INTERVAL_NANOS = 3000000000L;
   private static final long DAMAGE_UPGRADE_NANOS = 600000000L;
   private static final long LABEL_SUPPRESS_NANOS = 1000000000L;
   private static final long ENTITY_SCAN_INTERVAL_NANOS = 200000000L;
   private static final Map<Integer, Float> LAST_HEALTH = new HashMap<>();
   private static final Map<Integer, PendingDamage> PENDING_DAMAGE = new HashMap<>();
   private static final Map<Integer, Long> LABEL_COUNTED = new HashMap<>();
   private static final Set<Integer> CUSTOM_HEALTH_ENTITIES = new HashSet<>();
   private static final Set<Integer> TRACKED_ENTITY_IDS = new HashSet<>();
   private static final ArrayDeque<DamageSample> DPS_SAMPLES = new ArrayDeque<>();
   private static ClientLevel trackedLevel;
   private static long lastEntityScanNanos;
   private static long combatStartNanos;
   private static long lastDamageNanos;
   private static long lastChatSummaryNanos;
   private static double pendingChatDamage;

   private MoonCombatTracker() {
   }

   public static void tick(Minecraft client) {
      if (!MoonConfig.featuresEnabled || !MoonConfig.combatHud || !MoonConfig.combatHudShowDps || client.player == null || client.level == null) {
         reset();
         return;
      }

      if (trackedLevel != client.level) {
         reset();
         trackedLevel = client.level;
      }

      long now = System.nanoTime();
      pruneDps(now);
      flushChatSummary(client, now);
      double range = MoonConfig.clampCombatHudRange(MoonConfig.combatHudRange);
      double rangeSquared = range * range;
      if (now - lastEntityScanNanos < ENTITY_SCAN_INTERVAL_NANOS) {
         applyFloatingDamages(client.level, client.player, rangeSquared, now);
         return;
      }

      lastEntityScanNanos = now;
      AABB searchArea = new AABB(
         client.player.getX() - range, client.player.getY() - range, client.player.getZ() - range,
         client.player.getX() + range, client.player.getY() + range, client.player.getZ() + range
      );
      TRACKED_ENTITY_IDS.clear();
      for (Entity entity : client.level.getEntities(client.player, searchArea)) {
         if (entity instanceof LivingEntity living && !(living instanceof ArmorStand)
            && living.isAlive() && !living.isSpectator()
            && entity.position().distanceToSqr(client.player.position()) <= rangeSquared) {
            TRACKED_ENTITY_IDS.add(living.getId());
            observeHealth(living, now, client.player);
         }
      }

      LAST_HEALTH.keySet().retainAll(TRACKED_ENTITY_IDS);
      CUSTOM_HEALTH_ENTITIES.retainAll(TRACKED_ENTITY_IDS);
      LABEL_COUNTED.entrySet().removeIf(entry -> now > entry.getValue());
      applyFloatingDamages(client.level, client.player, rangeSquared, now);
      upgradePendingDamage(client.level, client.player, rangeSquared, now);
   }

   public static void onDamageEvent(int victimId, int causeId, int directId) {
      Minecraft client = Minecraft.getInstance();
      if (isTrackingEnabled(client)) {
         long now = System.nanoTime();
         Entity victim = client.level.getEntity(victimId);
         if (TRACKED_ENTITY_IDS.contains(victimId) && victim instanceof LivingEntity living && !(living instanceof ArmorStand)) {
            observeHealth(living, now, client.player);
         }
      }
   }

   private static boolean isTrackingEnabled(Minecraft client) {
      return MoonConfig.featuresEnabled && MoonConfig.combatHud && MoonConfig.combatHudShowDps && client.player != null && client.level != null;
   }

   private static double rangeSquared() {
      int range = MoonConfig.clampCombatHudRange(MoonConfig.combatHudRange);
      return range * (double)range;
   }

   private static void observeHealth(LivingEntity entity, long now, Player player) {
      if (entity.position().distanceToSqr(player.position()) > rangeSquared()) {
         return;
      }

      int id = entity.getId();
      MoonMobHealthReader.HealthReading customReading = MoonMobHealthReader.read(entity);
      if (customReading != null) {
         CUSTOM_HEALTH_ENTITIES.add(id);
      } else if (CUSTOM_HEALTH_ENTITIES.contains(id)) {
         return;
      }

      float currentHealth = customReading == null ? entity.getHealth() : (float)customReading.current();
      Float previousHealth = LAST_HEALTH.put(id, currentHealth);
      if (previousHealth != null && previousHealth > currentHealth) {
         Long suppressedUntil = LABEL_COUNTED.get(id);
         if (suppressedUntil != null && now <= suppressedUntil) {
            return;
         }

         double healthDrop = previousHealth - currentHealth;
         double displayedDamage = MoonMobHealthReader.readFloatingDamage(entity, player, rangeSquared());
         recordDamage(displayedDamage > healthDrop ? displayedDamage : healthDrop, id, now);
         if (displayedDamage <= healthDrop) {
            PENDING_DAMAGE.put(id, new PendingDamage(healthDrop, now + DAMAGE_UPGRADE_NANOS));
         } else {
            PENDING_DAMAGE.remove(id);
         }
      }
   }

   private static void upgradePendingDamage(ClientLevel level, Player player, double rangeSquared, long now) {
      Iterator<Map.Entry<Integer, PendingDamage>> iterator = PENDING_DAMAGE.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<Integer, PendingDamage> entry = iterator.next();
         PendingDamage pending = entry.getValue();
         if (TRACKED_ENTITY_IDS.contains(entry.getKey())
            && level.getEntity(entry.getKey()) instanceof LivingEntity livingEntity) {
            double displayedDamage = MoonMobHealthReader.readFloatingDamage(livingEntity, player, rangeSquared);
            if (displayedDamage > pending.healthDrop()) {
               recordDamage(displayedDamage - pending.healthDrop(), entry.getKey(), now);
               iterator.remove();
               LABEL_COUNTED.put(entry.getKey(), now + LABEL_SUPPRESS_NANOS);
               continue;
            }
         }

         if (now > pending.deadline()) {
            iterator.remove();
         }
      }
   }

   private static void applyFloatingDamages(ClientLevel level, Player player, double rangeSquared, long now) {
      for (MoonMobHealthReader.FloatingDamage damage : MoonMobHealthReader.collectFloatingDamages(level, player, rangeSquared)) {
         int victimId = damage.victimId();
         if (victimId >= 0 && !TRACKED_ENTITY_IDS.contains(victimId)) {
            continue;
         }

         if (victimId < 0) {
            recordDamage(damage.amount(), victimId, now);
            continue;
         }

         PendingDamage pending = PENDING_DAMAGE.remove(victimId);
         double amount = damage.amount();
         if (pending == null) {
            recordDamage(amount, victimId, now);
         } else if (amount > pending.healthDrop()) {
            recordDamage(amount - pending.healthDrop(), victimId, now);
         }

         LABEL_COUNTED.put(victimId, now + LABEL_SUPPRESS_NANOS);
      }
   }

   private static void recordDamage(double damage, int targetId, long now) {
      if (Double.isFinite(damage) && damage > 0.0) {
         if (lastDamageNanos == 0L || now - lastDamageNanos > COMBAT_IDLE_RESET_NANOS) {
            DPS_SAMPLES.clear();
            combatStartNanos = now;
         }

         DPS_SAMPLES.addLast(new DamageSample(damage, targetId, now));
         lastDamageNanos = now;
         pendingChatDamage += damage;
      }
   }

   private static void flushChatSummary(Minecraft client, long now) {
      if (!MoonConfig.combatHudChatDamage) {
         pendingChatDamage = 0.0;
         return;
      }

      if (!(pendingChatDamage > 0.0) || now - lastChatSummaryNanos < CHAT_SUMMARY_INTERVAL_NANOS || client.player == null || client.gui == null) {
         return;
      }

      String message = "[Zeya] Dano: " + MoonHud.formatCompact(pendingChatDamage)
         + " | DPS: " + MoonHud.formatCompact(getDps());
      Component coloredMessage = Component.literal(message)
         .withStyle(style -> style.withColor(TextColor.fromRgb(MoonConfig.modColor)));
      client.gui.hud.getChat().addClientSystemMessage(coloredMessage);
      pendingChatDamage = 0.0;
      lastChatSummaryNanos = now;
   }

   private static void pruneDps(long now) {
      while (!DPS_SAMPLES.isEmpty() && now - DPS_SAMPLES.peekFirst().atNanos() > DPS_WINDOW_NANOS) {
         DPS_SAMPLES.removeFirst();
      }

      if (lastDamageNanos != 0L && now - lastDamageNanos > COMBAT_IDLE_RESET_NANOS) {
         DPS_SAMPLES.clear();
         combatStartNanos = 0L;
         lastDamageNanos = 0L;
      }
   }

   public static double getDps() {
      long now = System.nanoTime();
      pruneDps(now);
      if (DPS_SAMPLES.isEmpty() || combatStartNanos == 0L) {
         return 0.0;
      }

      double damage = DPS_SAMPLES.stream().mapToDouble(DamageSample::amount).sum();
      long durationNanos = Math.min(DPS_WINDOW_NANOS, now - combatStartNanos);
      durationNanos = Math.max(MIN_DPS_DURATION_NANOS, durationNanos);
      return damage / (durationNanos / 1_000_000_000.0);
   }

   static MoonMobHealthReader.HealthReading getCustomHealth(LivingEntity entity) {
      return MoonMobHealthReader.read(entity);
   }

   public static void resetDps() {
      DPS_SAMPLES.clear();
      combatStartNanos = 0L;
      lastDamageNanos = 0L;
      lastChatSummaryNanos = 0L;
      pendingChatDamage = 0.0;
      clearTrackedEntities();
   }

   private static void clearTrackedEntities() {
      LAST_HEALTH.clear();
      PENDING_DAMAGE.clear();
      LABEL_COUNTED.clear();
      CUSTOM_HEALTH_ENTITIES.clear();
      TRACKED_ENTITY_IDS.clear();
   }

   private static void reset() {
      clearTrackedEntities();
      DPS_SAMPLES.clear();
      trackedLevel = null;
      lastEntityScanNanos = 0L;
      combatStartNanos = 0L;
      lastDamageNanos = 0L;
      lastChatSummaryNanos = 0L;
      pendingChatDamage = 0.0;
   }

   private record PendingDamage(double healthDrop, long deadline) {
   }

   private record DamageSample(double amount, int targetId, long atNanos) {
   }
}
