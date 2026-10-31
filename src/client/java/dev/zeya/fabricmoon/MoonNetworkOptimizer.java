package dev.zeya.fabricmoon;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class MoonNetworkOptimizer {
   private static final double PING_ALPHA = 0.5;
   private static final double TPS_ALPHA = 0.3;
   private static final double FPS_ALPHA = 0.2;
   private static final double JITTER_ALPHA = 0.3;
   private static final double EXCELLENT_JITTER = 10.0;
   private static final double GOOD_JITTER = 30.0;
   private static final double FAIR_JITTER = 50.0;
   private static final int UPDATE_INTERVAL_TICKS = 20;
   private static double smoothedPing = -1.0;
   private static double smoothedTps = -1.0;
   private static double smoothedFps = -1.0;
   private static double jitter = 0.0;
   private static int pingSampleCount = 0;
   private static long lastPingSampleMillis = 0L;
   private static final long PING_EXPIRE_MILLIS = 15000L;
   private static final int MAX_FPS_SAMPLES = 60;
   private static final Deque<Integer> fpsHistory = new ArrayDeque<>();
   private static int fpsMin = Integer.MAX_VALUE;
   private static int fpsMax = Integer.MIN_VALUE;
   private static long fpsSum = 0L;
   private static int tickCounter = 0;
   private static int channelsOptimized = 0;

   private MoonNetworkOptimizer() {
   }

   public static void tick(Minecraft client) {
      if (MoonConfig.featuresEnabled && MoonConfig.networkOptimizer) {
         if (client.player != null && client.level != null) {
            tickCounter++;
            if (tickCounter >= 20) {
               tickCounter = 0;
               int rawPing = MoonPingTracker.getPing();
               if (rawPing < 0) {
                  PlayerInfo playerInfo = client.player.connection.getPlayerInfo(client.player.getUUID());
                  rawPing = playerInfo == null ? -1 : playerInfo.getLatency();
               }

               if (rawPing >= 0) {
                  applyPingSmoothing(rawPing);
               }

               double rawTps = MoonServerStats.getEstimatedTps();
               if (rawTps >= 0.0) {
                  applyTpsSmoothing(rawTps);
               }

               int rawFps = client.getFps();
               if (rawFps > 0) {
                  applyFpsSmoothing(rawFps);
               }
            }
         }
      }
   }

   private static void applyPingSmoothing(int rawPing) {
      if (smoothedPing < 0.0) {
         smoothedPing = rawPing;
         pingSampleCount = 1;
      } else {
         double rawJitter = Math.abs(rawPing - smoothedPing);
         jitter = 0.3 * rawJitter + 0.7 * jitter;
         smoothedPing = 0.5 * rawPing + 0.5 * smoothedPing;
         pingSampleCount++;
      }

      lastPingSampleMillis = System.currentTimeMillis();
   }

   private static void applyTpsSmoothing(double rawTps) {
      if (smoothedTps < 0.0) {
         smoothedTps = rawTps;
      } else {
         smoothedTps = 0.3 * rawTps + 0.7 * smoothedTps;
      }
   }

   private static void applyFpsSmoothing(int rawFps) {
      if (smoothedFps < 0.0) {
         smoothedFps = rawFps;
      } else {
         smoothedFps = 0.2 * rawFps + 0.8 * smoothedFps;
      }

      fpsHistory.addLast(rawFps);
      fpsSum += rawFps;
      if (rawFps < fpsMin) {
         fpsMin = rawFps;
      }

      if (rawFps > fpsMax) {
         fpsMax = rawFps;
      }

      while (fpsHistory.size() > 60) {
         int removed = fpsHistory.removeFirst();
         fpsSum -= removed;
      }
   }

   public static int getSmoothedPing() {
      if (!(smoothedPing < 0.0) && pingSampleCount != 0) {
         return System.currentTimeMillis() - lastPingSampleMillis > 15000L ? -1 : (int)Math.round(smoothedPing);
      } else {
         return -1;
      }
   }

   public static double getSmoothedTps() {
      return smoothedTps;
   }

   public static int getSmoothedFps() {
      return smoothedFps < 0.0 ? 0 : (int)Math.round(smoothedFps);
   }

   public static double getJitter() {
      return jitter;
   }

   public static int getConnectionQuality() {
      if (pingSampleCount < 2) {
         return -1;
      } else if (jitter < 10.0) {
         return 0;
      } else if (jitter < 30.0) {
         return 1;
      } else {
         return jitter < 50.0 ? 2 : 3;
      }
   }

   public static int getConnectionQualityColor() {
      int quality = getConnectionQuality();
      switch (quality) {
         case 0:
            return -11141291;
         case 1:
            return -171;
         case 2:
            return -21931;
         case 3:
            return -43691;
         default:
            return -5592406;
      }
   }

   public static String getConnectionQualityText() {
      int quality = getConnectionQuality();
      switch (quality) {
         case 0:
            return "Excelente";
         case 1:
            return "Bom";
         case 2:
            return "Razoável";
         case 3:
            return "Ruim";
         default:
            return "--";
      }
   }

   public static int getFpsMin() {
      return fpsMin == Integer.MAX_VALUE ? 0 : fpsMin;
   }

   public static int getFpsMax() {
      return fpsMax == Integer.MIN_VALUE ? 0 : fpsMax;
   }

   public static int getFpsAverage() {
      return fpsHistory.isEmpty() ? 0 : (int)(fpsSum / fpsHistory.size());
   }

   public static void onChannelOptimized() {
      channelsOptimized++;
   }

   public static int getChannelsOptimized() {
      return channelsOptimized;
   }

   public static void reset() {
      smoothedPing = -1.0;
      smoothedTps = -1.0;
      smoothedFps = -1.0;
      jitter = 0.0;
      pingSampleCount = 0;
      lastPingSampleMillis = 0L;
      fpsHistory.clear();
      fpsMin = Integer.MAX_VALUE;
      fpsMax = Integer.MIN_VALUE;
      fpsSum = 0L;
      tickCounter = 0;
      channelsOptimized = 0;
   }
}
