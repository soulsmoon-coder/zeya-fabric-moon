package dev.zeya.fabricmoon;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class MoonServerStats {
   private static final int MAX_SAMPLES = 5;
   private static final long MAX_SAMPLE_AGE_NANOS = 90000000000L;
   private static final long MAX_SAMPLE_TICKS = 2400L;
   private static final Deque<MoonServerStats.TimeSample> SAMPLES = new ArrayDeque<>();
   private static long lastGameTime = Long.MIN_VALUE;
   private static long lastUpdateNanos;
   private static ClientLevel trackedLevel;

   private MoonServerStats() {
   }

   public static synchronized void tick(Minecraft client) {
      if (!MoonConfig.featuresEnabled) {
         trackedLevel = null;
         resetSamples();
      } else {
         if (client.level != trackedLevel) {
            trackedLevel = client.level;
            resetSamples();
         }
      }
   }

   public static synchronized void recordServerTime(long gameTime) {
      if (MoonConfig.featuresEnabled) {
         long now = System.nanoTime();
         if (lastGameTime == Long.MIN_VALUE) {
            SAMPLES.clear();
            lastGameTime = gameTime;
            lastUpdateNanos = now;
         } else if (gameTime == lastGameTime) {
            lastUpdateNanos = now;
         } else if (gameTime < lastGameTime) {
            SAMPLES.clear();
            lastGameTime = gameTime;
            lastUpdateNanos = now;
         } else {
            long elapsed = now - lastUpdateNanos;
            long ticks = gameTime - lastGameTime;
            lastGameTime = gameTime;
            lastUpdateNanos = now;
            if (elapsed > 0L && elapsed <= 90000000000L && ticks <= 2400L) {
               while (!SAMPLES.isEmpty() && now - SAMPLES.peekFirst().recordedAtNanos > 90000000000L) {
                  SAMPLES.removeFirst();
               }

               SAMPLES.addLast(new MoonServerStats.TimeSample(ticks, elapsed, now));

               while (SAMPLES.size() > 5) {
                  SAMPLES.removeFirst();
               }
            } else {
               SAMPLES.clear();
            }
         }
      }
   }

   public static synchronized double getEstimatedTps() {
      long now = System.nanoTime();
      if (!SAMPLES.isEmpty() && now - lastUpdateNanos <= 90000000000L) {
         long ticks = 0L;
         long elapsed = 0L;

         for (MoonServerStats.TimeSample sample : SAMPLES) {
            if (now - sample.recordedAtNanos <= 90000000000L) {
               ticks += sample.ticks;
               elapsed += sample.elapsedNanos;
            }
         }

         return elapsed <= 0L ? -1.0 : Math.min(20.0, ticks * 1.0E9 / elapsed);
      } else {
         return -1.0;
      }
   }

   private static void resetSamples() {
      SAMPLES.clear();
      lastGameTime = Long.MIN_VALUE;
      lastUpdateNanos = 0L;
   }

   private record TimeSample(long ticks, long elapsedNanos, long recordedAtNanos) {
   }
}
