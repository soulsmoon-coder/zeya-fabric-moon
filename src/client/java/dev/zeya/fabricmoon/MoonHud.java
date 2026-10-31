package dev.zeya.fabricmoon;

import java.util.Locale;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class MoonHud {
   private static final int LINE_HEIGHT = 13;
   private static final double TARGET_RANGE = 64.0;
   private static final long TARGET_CACHE_NANOS = 50000000L;
   private static ClientLevel cachedTargetLevel;
   private static int cachedCameraEntityId = Integer.MIN_VALUE;
   private static long cachedTargetAtNanos;
   private static LivingEntity cachedTarget;
   private static boolean targetCacheInitialized;

   private MoonHud() {
   }

   public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
      Minecraft client = Minecraft.getInstance();
      if (MoonConfig.featuresEnabled && client.player != null && client.gui.screen() == null) {
         if (MoonConfig.showHud) {
            int fps;
            int ping;
            double tps;
            if (MoonConfig.networkOptimizer) {
               fps = MoonNetworkOptimizer.getSmoothedFps();
               if (fps <= 0) {
                  fps = client.getFps();
               }

               ping = MoonNetworkOptimizer.getSmoothedPing();
               if (ping < 0) {
                  ping = MoonPingTracker.getPing();
                  if (ping < 0) {
                     PlayerInfo playerInfo = client.player.connection.getPlayerInfo(client.player.getUUID());
                     ping = playerInfo == null ? -1 : playerInfo.getLatency();
                  }
               }

               tps = MoonNetworkOptimizer.getSmoothedTps();
               if (tps < 0.0) {
                  tps = MoonServerStats.getEstimatedTps();
               }
            } else {
               fps = client.getFps();
               ping = MoonPingTracker.getPing();
               if (ping < 0) {
                  PlayerInfo playerInfo = client.player.connection.getPlayerInfo(client.player.getUUID());
                  ping = playerInfo == null ? -1 : playerInfo.getLatency();
               }

               tps = MoonServerStats.getEstimatedTps();
            }

            int x = getStatsX(graphics.guiWidth());
            int y = getStatsY(graphics.guiHeight());
            int panelLines = 5;
            if (MoonConfig.networkOptimizer) {
               panelLines = 6;
            }

            graphics.fill(x - 3, y - 3, x + 112, y + 13 * panelLines + 2, -1610612736);
            graphics.text(client.font, "FPS: " + fps, x, y, -43521);
            graphics.text(client.font, "TPS: " + (tps < 0.0 ? "--" : String.format(Locale.ROOT, "%.1f", tps)), x, y + 13, -43521);
            graphics.text(client.font, "Ping: " + (ping < 0 ? "--" : ping + " ms"), x, y + 26, -43521);
            graphics.text(client.font, "CPS L: " + MoonMouseTracker.getLeftClicksPerSecond(), x, y + 39, -43521);
            graphics.text(client.font, "CPS R: " + MoonMouseTracker.getRightClicksPerSecond(), x, y + 52, -43521);
            if (MoonConfig.networkOptimizer) {
               int qualityColor = MoonNetworkOptimizer.getConnectionQualityColor();
               String qualityText = MoonNetworkOptimizer.getConnectionQualityText();
               graphics.text(client.font, "Qualidade: " + qualityText, x, y + 65, qualityColor);
            }
         }

         if (MoonConfig.combatHud) {
            renderCombatHud(graphics, client);
         }
      }
   }

   private static void renderCombatHud(GuiGraphicsExtractor graphics, Minecraft client) {
      boolean showDamage = MoonConfig.combatHudShowDamage && (MoonCombatTracker.getLastDamage() >= 0.0 || MoonCombatTracker.getTotalDamage() > 0.0);
      boolean showTargetHealth = MoonConfig.combatHudShowTargetHealth;
      LivingEntity target = showTargetHealth ? findTarget(client) : null;
      if (showDamage || target != null) {
         int x = getCombatX(graphics.guiWidth());
         int y = getCombatY(graphics.guiHeight());
         int lineHeight = 12;
         int row = 0;
         if (showDamage) {
            double lastDamage = MoonCombatTracker.getLastDamage();
            double distance = MoonCombatTracker.getLastDamageDistance();
            String damageText = formatDamage(lastDamage);
            String distanceText = distance >= 0.0 ? String.format(Locale.ROOT, " (%.1f)", distance) : "";
            int damageColor = getDamageColor(lastDamage);
            graphics.text(client.font, "Dano: " + damageText + distanceText, x, y, damageColor);
            graphics.text(client.font, "Total: " + formatDamage(MoonCombatTracker.getTotalDamage()), x, y + lineHeight, -1);
            row = 2;
         }

         if (target != null) {
            MoonMobHealthReader.HealthReading customHealth = MoonCombatTracker.getCustomHealth(target);
            boolean unknownNpcHealth = customHealth == null && MoonMobHealthReader.hasGeneratedName(target);
            boolean hasComparableHealth = !unknownNpcHealth && (customHealth == null || customHealth.maximum() != null);
            double maxHealth = customHealth != null ? (customHealth.maximum() == null ? 0.0 : customHealth.maximum()) : Math.max(0.0F, target.getMaxHealth());
            double health = customHealth != null ? customHealth.current() : Mth.clamp(target.getHealth(), 0.0F, (float)maxHealth);
            double progress = hasComparableHealth && maxHealth > 0.0 ? Mth.clamp((float)(health / maxHealth), 0.0F, 1.0F) : 0.0;
            graphics.text(client.font, "Alvo: " + MoonMobHealthReader.displayName(target), x, y + lineHeight * row, -4791297);
            String healthLabel = unknownNpcHealth
               ? "HP: --"
               : (
                  customHealth != null && customHealth.maximum() == null
                     ? "HP: " + formatCompact(health)
                     : "HP: " + formatCompact(health) + " / " + formatCompact(maxHealth) + " (" + String.format(Locale.ROOT, "%.0f%%", progress * 100.0) + ")"
               );
            graphics.text(client.font, healthLabel, x, y + lineHeight * (row + 1), -1);
            if (hasComparableHealth && maxHealth > 0.0) {
               int barY = y + lineHeight * (row + 2);
               graphics.fill(x, barY, x + 100, barY + 3, -12966619);
               graphics.fill(x, barY, x + (int)Math.round(100.0 * progress), barY + 3, -11149961);
            }
         }
      }
   }

   private static LivingEntity findTarget(Minecraft client) {
      ClientLevel level = client.level;
      Camera camera = client.gameRenderer.mainCamera();
      Entity cameraEntity = camera.entity();
      if (level != null && camera.isInitialized() && cameraEntity != null) {
         long now = System.nanoTime();
         if (targetCacheInitialized && cachedTargetLevel == level && cachedCameraEntityId == cameraEntity.getId() && now - cachedTargetAtNanos < 50000000L) {
            return cachedTarget != null && cachedTarget.isAlive() ? cachedTarget : null;
         } else if (client.hitResult instanceof EntityHitResult hit
            && hit.getEntity() instanceof LivingEntity livingEntity
            && !(livingEntity instanceof ArmorStand)
            && !(livingEntity instanceof net.minecraft.world.entity.player.Player)
            && livingEntity.isAlive()
            && !livingEntity.isSpectator()) {
            return rememberTarget(level, cameraEntity, livingEntity, now);
         } else {
            Vec3 start = camera.position();
            Vec3 direction = cameraEntity.calculateViewVector(camera.xRot(), camera.yRot());
            Vec3 end = start.add(direction.scale(64.0));
            HitResult blockHit = level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, cameraEntity));
            double maxDistanceSquared = blockHit.getType() == Type.MISS ? 4096.0 : start.distanceToSqr(blockHit.getLocation());
            AABB searchArea = new AABB(start, end).inflate(1.0);
            LivingEntity closestEntity = null;
            double closestDistanceSquared = maxDistanceSquared;

            for (Entity entity : level.getEntities(cameraEntity, searchArea)) {
               if (entity instanceof LivingEntity livingEntity
                  && !(livingEntity instanceof ArmorStand)
                  && !(livingEntity instanceof net.minecraft.world.entity.player.Player)
                  && livingEntity.isAlive()
                  && entity.isPickable()
                  && !entity.isSpectator()) {
                  Vec3 hitPosition = (Vec3)entity.getBoundingBox().inflate(0.1).clip(start, end).orElse(null);
                  if (hitPosition != null) {
                     double distanceSquared = start.distanceToSqr(hitPosition);
                     if (blockHit.getType() == Type.MISS ? !(distanceSquared > maxDistanceSquared) : !(distanceSquared >= maxDistanceSquared - 1.0E-4)) {
                        boolean tiedWithClosest = Math.abs(distanceSquared - closestDistanceSquared) <= 0.01;
                        if (distanceSquared < closestDistanceSquared - 0.01
                           || tiedWithClosest && (closestEntity == null || livingEntity.getId() < closestEntity.getId())) {
                           closestEntity = livingEntity;
                           closestDistanceSquared = distanceSquared;
                        }
                     }
                  }
               }
            }

            return rememberTarget(level, cameraEntity, closestEntity, now);
         }
      } else {
         targetCacheInitialized = false;
         return null;
      }
   }

   private static LivingEntity rememberTarget(ClientLevel level, Entity cameraEntity, LivingEntity target, long now) {
      cachedTargetLevel = level;
      cachedCameraEntityId = cameraEntity.getId();
      cachedTargetAtNanos = now;
      cachedTarget = target;
      targetCacheInitialized = true;
      return target;
   }

   private static String formatDamage(double damage) {
      return damage < 0.0 ? "--" : formatCompact(damage);
   }

   static String formatCompact(double value) {
      double absoluteValue = Math.abs(value);
      String suffix;
      double scaled;
      if (absoluteValue >= 1.0E12) {
         suffix = "t";
         scaled = value / 1.0E12;
      } else if (absoluteValue >= 1.0E9) {
         suffix = "b";
         scaled = value / 1.0E9;
      } else if (absoluteValue >= 1000000.0) {
         suffix = "m";
         scaled = value / 1000000.0;
      } else {
         if (!(absoluteValue >= 1000.0)) {
            return String.format(Locale.ROOT, "%.1f", value);
         }

         suffix = "k";
         scaled = value / 1000.0;
      }

      String formatted = String.format(Locale.ROOT, "%.1f", scaled);
      if (formatted.endsWith(".0")) {
         formatted = formatted.substring(0, formatted.length() - 2);
      }

      return formatted + suffix;
   }

   static int getStatsX(int guiWidth) {
      return Mth.clamp(MoonConfig.statsHudX, 0, Math.max(0, guiWidth - 115));
   }

   static int getStatsY(int guiHeight) {
      int y = MoonConfig.statsHudY < 0 ? Math.max(4, guiHeight / 2 - 18) : MoonConfig.statsHudY;
      return Mth.clamp(y, 0, Math.max(0, guiHeight - 70));
   }

   static int getCombatX(int guiWidth) {
      int x = MoonConfig.combatHudX < 0 ? guiWidth - 210 : MoonConfig.combatHudX;
      return Mth.clamp(x, 0, Math.max(0, guiWidth - 210));
   }

   static int getCombatY(int guiHeight) {
      int lineCount = (MoonConfig.combatHudShowDamage ? 2 : 0) + (MoonConfig.combatHudShowTargetHealth ? 3 : 0);
      int height = lineCount * 12 + 8;
      return Mth.clamp(MoonConfig.combatHudY, 0, Math.max(0, guiHeight - height));
   }

   static int getCombatPanelHeight() {
      int lineCount = (MoonConfig.combatHudShowDamage ? 2 : 0) + (MoonConfig.combatHudShowTargetHealth ? 3 : 0);
      return lineCount * 12 + 8;
   }

   private static int getDamageColor(double damage) {
      if (damage < 0.0) {
         return -13227;
      } else if (damage >= 100000.0) {
         return -11141291;
      } else if (damage >= 10000.0) {
         return -13227;
      } else {
         return damage >= 1000.0 ? -30635 : -43691;
      }
   }
}
