package dev.zeya.fabricmoon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class MoonCombatTracker {
   private static final long HIT_TRACKING_NANOS = 2000000000L;
   private static final long DAMAGE_DISPLAY_NANOS = 5000000000L;
   private static final long COMBAT_RESET_NANOS = 5000000000L;
   private static final long DAMAGE_UPGRADE_NANOS = 600000000L;
   private static final long LABEL_SUPPRESS_NANOS = 1000000000L;
   private static final Map<Integer, Float> LAST_HEALTH = new HashMap<>();
   private static final Map<Integer, Long> TRACKED_HITS = new HashMap<>();
   private static final Map<Integer, MoonCombatTracker.PendingDamage> PENDING_DAMAGE = new HashMap<>();
   private static final Map<Integer, Long> LABEL_COUNTED = new HashMap<>();
   private static final Set<Integer> CUSTOM_HEALTH_ENTITIES = new HashSet<>();
   private static final List<Double> CHAT_DAMAGE_QUEUE = new ArrayList<>();
   private static double lastDamage = -1.0;
   private static double totalDamage;
   private static long lastDamageNanos;
   private static ClientLevel trackedLevel;
   private static double lastDamageDistance = -1.0;

   private MoonCombatTracker() {
   }

   public static void tick(Minecraft client) {
      if (MoonConfig.featuresEnabled && MoonConfig.combatHud && MoonConfig.combatHudShowDamage && client.player != null && client.level != null) {
         if (trackedLevel != client.level) {
            reset();
            trackedLevel = client.level;
         }

         long now = System.nanoTime();

         for (Integer id : new ArrayList<>(TRACKED_HITS.keySet())) {
            if (client.level.getEntity(id) instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
               observeHealth(livingEntity, now);
            } else {
               LAST_HEALTH.remove(id);
               CUSTOM_HEALTH_ENTITIES.remove(id);
               PENDING_DAMAGE.remove(id);
               LABEL_COUNTED.remove(id);
            }
         }

         TRACKED_HITS.entrySet().removeIf(entry -> now > entry.getValue());
         LAST_HEALTH.keySet().retainAll(TRACKED_HITS.keySet());
         CUSTOM_HEALTH_ENTITIES.retainAll(TRACKED_HITS.keySet());
         LABEL_COUNTED.entrySet().removeIf(entry -> now > entry.getValue());
         applyFloatingDamage(client.level, client.player, now);
         upgradePendingDamage(client.level, now);
         prunePendingDamage(now);
         if (lastDamageNanos != 0L && now - lastDamageNanos > 5000000000L) {
            totalDamage = 0.0;
            lastDamage = -1.0;
            lastDamageNanos = 0L;
            lastDamageDistance = -1.0;
         } else if (lastDamageNanos != 0L && now - lastDamageNanos > 5000000000L) {
            lastDamage = -1.0;
            lastDamageDistance = -1.0;
         }

         flushChatDamage(client);
      } else {
         reset();
      }
   }

   public static void onDamageEvent(int victimId, int causeId, int directId) {
      Minecraft client = Minecraft.getInstance();
      if (MoonConfig.featuresEnabled
         && MoonConfig.combatHud
         && MoonConfig.combatHudShowDamage
         && client.player != null
         && client.level != null
         && (causeId == client.player.getId() || directId == client.player.getId())) {
         if (client.level.getEntity(victimId) instanceof LivingEntity livingEntity) {
            long now = System.nanoTime();
            TRACKED_HITS.put(victimId, now + 2000000000L);
            observeHealth(livingEntity, now);
         }
      }
   }

   public static void onPlayerAttack(Entity target) {
      Minecraft client = Minecraft.getInstance();
      if (MoonConfig.featuresEnabled && MoonConfig.combatHud && MoonConfig.combatHudShowDamage && client.player != null && target instanceof LivingEntity) {
         if (target instanceof LivingEntity livingEntity) {
            int id = livingEntity.getId();
            long now = System.nanoTime();
            TRACKED_HITS.put(id, now + 2000000000L);
            MoonMobHealthReader.HealthReading reading = MoonMobHealthReader.read(livingEntity);
            if (reading != null) {
               CUSTOM_HEALTH_ENTITIES.add(id);
               LAST_HEALTH.put(id, (float)reading.current());
            } else if (!CUSTOM_HEALTH_ENTITIES.contains(id)) {
               LAST_HEALTH.put(id, livingEntity.getHealth());
            }
         }
      }
   }

   private static void observeHealth(LivingEntity entity, long now) {
      int id = entity.getId();
      MoonMobHealthReader.HealthReading customReading = MoonMobHealthReader.read(entity);
      if (customReading != null) {
         CUSTOM_HEALTH_ENTITIES.add(id);
      } else if (CUSTOM_HEALTH_ENTITIES.contains(id)) {
         return;
      }

      float currentHealth = customReading == null ? entity.getHealth() : (float)customReading.current();
      Float previousHealth = LAST_HEALTH.put(id, currentHealth);
      Long trackingDeadline = TRACKED_HITS.get(id);
      if (previousHealth != null && previousHealth > currentHealth && trackingDeadline != null && now <= trackingDeadline) {
         Long suppressedUntil = LABEL_COUNTED.get(id);
         if (suppressedUntil != null && now <= suppressedUntil) {
            return;
         }

         double healthDrop = previousHealth - currentHealth;
         Minecraft client = Minecraft.getInstance();
         if (client.player != null) {
            double dx = entity.getX() - client.player.getX();
            double dy = entity.getY() - client.player.getY();
            double dz = entity.getZ() - client.player.getZ();
            lastDamageDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
         }

         double displayedDamage = MoonMobHealthReader.readFloatingDamage(entity);
         double dealtDamage = displayedDamage > healthDrop ? displayedDamage : healthDrop;
         lastDamage = dealtDamage;
         totalDamage += dealtDamage;
         lastDamageNanos = now;
         queueChatDamage(dealtDamage);
         if (displayedDamage > healthDrop) {
            PENDING_DAMAGE.remove(id);
         } else {
            PENDING_DAMAGE.put(id, new MoonCombatTracker.PendingDamage(healthDrop, now + 600000000L));
         }
      }
   }

   private static void upgradePendingDamage(ClientLevel level, long now) {
      if (!PENDING_DAMAGE.isEmpty()) {
         for (Entry<Integer, MoonCombatTracker.PendingDamage> entry : new ArrayList<>(PENDING_DAMAGE.entrySet())) {
            MoonCombatTracker.PendingDamage pending = entry.getValue();
            if (level.getEntity(entry.getKey()) instanceof LivingEntity livingEntity) {
               double displayedDamage = MoonMobHealthReader.readFloatingDamage(livingEntity);
               if (displayedDamage > pending.healthDrop()) {
                  totalDamage = totalDamage + (displayedDamage - pending.healthDrop());
                  lastDamage = displayedDamage;
                  lastDamageNanos = now;
                  PENDING_DAMAGE.remove(entry.getKey());
                  LABEL_COUNTED.put(entry.getKey(), now + 1000000000L);
                  continue;
               }
            }

            if (now > pending.deadline()) {
               PENDING_DAMAGE.remove(entry.getKey());
            }
         }
      }
   }

   private static void prunePendingDamage(long now) {
      PENDING_DAMAGE.entrySet().removeIf(entry -> now > entry.getValue().deadline());
   }

   private static void applyFloatingDamage(ClientLevel level, Player player, long now) {
      for (MoonMobHealthReader.FloatingDamage damage : MoonMobHealthReader.collectFloatingDamages(level, player, false)) {
         int victimId = damage.victimId();
         double amount = damage.amount();
         MoonCombatTracker.PendingDamage pending = PENDING_DAMAGE.remove(victimId);
         double dealtDamage = amount;
         if (pending != null) {
            if (amount > pending.healthDrop()) {
               totalDamage = totalDamage + (amount - pending.healthDrop());
            } else {
               dealtDamage = pending.healthDrop();
            }
         } else {
            totalDamage += amount;
         }

         lastDamage = dealtDamage;
         lastDamageNanos = now;
         if (victimId >= 0) {
            Entity victim = level.getEntity(victimId);
            if (victim instanceof LivingEntity && player != null) {
               double dx = victim.getX() - player.getX();
               double dy = victim.getY() - player.getY();
               double dz = victim.getZ() - player.getZ();
               lastDamageDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            }

            LABEL_COUNTED.put(victimId, now + 1000000000L);
         }

         if (pending == null) {
            queueChatDamage(dealtDamage);
         }
      }
   }

   private static void queueChatDamage(double damage) {
      if (Double.isFinite(damage) && damage > 0.0) {
         CHAT_DAMAGE_QUEUE.add(damage);
      }
   }

   private static void flushChatDamage(Minecraft client) {
      if (!CHAT_DAMAGE_QUEUE.isEmpty()) {
         StringBuilder message = new StringBuilder("Zeya > ");

         for (double damage : CHAT_DAMAGE_QUEUE) {
            if (message.length() > "Zeya > ".length()) {
               message.append(", ");
            }

            message.append(MoonHud.formatCompact(damage));
         }

         client.gui.chatListener().handleSystemMessage(Component.literal(message.toString()), false);
         CHAT_DAMAGE_QUEUE.clear();
      }
   }

   static MoonMobHealthReader.HealthReading getCustomHealth(LivingEntity entity) {
      return MoonMobHealthReader.read(entity);
   }

   public static double getLastDamage() {
      return lastDamage;
   }

   public static double getTotalDamage() {
      return totalDamage;
   }

   public static double getLastDamageDistance() {
      return lastDamageDistance;
   }

   public static void resetDamage() {
      lastDamage = -1.0;
      totalDamage = 0.0;
      lastDamageNanos = 0L;
      lastDamageDistance = -1.0;
      PENDING_DAMAGE.clear();
      CHAT_DAMAGE_QUEUE.clear();
   }

   private static void reset() {
      CHAT_DAMAGE_QUEUE.clear();
      LAST_HEALTH.clear();
      TRACKED_HITS.clear();
      PENDING_DAMAGE.clear();
      LABEL_COUNTED.clear();
      CUSTOM_HEALTH_ENTITIES.clear();
      lastDamage = -1.0;
      totalDamage = 0.0;
      lastDamageNanos = 0L;
      lastDamageDistance = -1.0;
      trackedLevel = null;
   }

   private record PendingDamage(double healthDrop, long deadline) {
   }
}
