package dev.zeya.fabricmoon;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MoonHypixelRouter {
   private static final Logger LOGGER = LoggerFactory.getLogger("ZeyaFabricMoon/HypixelRouter");
   private static final String[] HYPELIX_ADDRESSES = new String[]{"mc.hypixel.net", "hypixel.net"};
   private static final int TEST_TIMEOUT_MS = 2000;
   private static volatile String bestAddress = null;
   private static volatile long bestPing = Long.MAX_VALUE;
   private static volatile long lastTestTime = 0L;
   private static final long CACHE_EXPIRE_MS = 300000L;
   private static volatile List<MoonHypixelRouter.ServerTestResult> lastResults = Collections.emptyList();

   private MoonHypixelRouter() {
   }

   public static boolean isHypixelServer(String serverAddress) {
      if (serverAddress == null) {
         return false;
      }

      String normalized = serverAddress.trim().toLowerCase(Locale.ROOT);
      return normalized.contains("hypixel");
   }

   public static boolean isCurrentlyOnHypixel() {
      String best = getBestAddress();
      return best != null && best.contains("hypixel");
   }

   public static CompletableFuture<String> testAndSelectBest() {
      long now = System.currentTimeMillis();
      if (bestAddress != null && now - lastTestTime < 300000L) {
         return CompletableFuture.completedFuture(bestAddress);
      }

      ExecutorService executor = Executors.newFixedThreadPool(HYPELIX_ADDRESSES.length);

      try {
         List<CompletableFuture<MoonHypixelRouter.ServerTestResult>> futures = new ArrayList<>();

         for (String address : HYPELIX_ADDRESSES) {
            futures.add(CompletableFuture.supplyAsync(() -> testAddress(address), executor));
         }

         CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
         return allDone.thenApply(v -> {
            String best = null;
            long bestPing = Long.MAX_VALUE;
            List<MoonHypixelRouter.ServerTestResult> results = new ArrayList<>();

            for (CompletableFuture<MoonHypixelRouter.ServerTestResult> future : futures) {
               try {
                  MoonHypixelRouter.ServerTestResult result = future.get();
                  results.add(result);
                  if (result.success && result.pingMs < bestPing) {
                     bestPing = result.pingMs;
                     best = result.address;
                  }
               } catch (Exception ex) {
                  LOGGER.warn("Failed to get test result: {}", ex.getMessage());
               }
            }

            Collections.sort(results);
            lastResults = Collections.unmodifiableList(results);
            if (best != null) {
               bestAddress = best;
               MoonHypixelRouter.bestPing = bestPing;
               lastTestTime = System.currentTimeMillis();
               LOGGER.info("Best Hypixel server: {} ({} ms)", best, bestPing);
            }

            return best;
         });
      } finally {
         executor.shutdown();
      }
   }

   private static MoonHypixelRouter.ServerTestResult testAddress(String address) {
      long start = System.nanoTime();

      try (Socket socket = new Socket()) {
         socket.setSoTimeout(2000);
         socket.connect(new InetSocketAddress(address, 25565), 2000);
         long elapsed = System.nanoTime() - start;
         long pingMs = TimeUnit.NANOSECONDS.toMillis(elapsed);
         return new MoonHypixelRouter.ServerTestResult(address, true, pingMs);
      } catch (IOException ex) {
         return new MoonHypixelRouter.ServerTestResult(address, false, -1L);
      }
   }

   public static String getBestAddress() {
      long now = System.currentTimeMillis();
      return bestAddress != null && now - lastTestTime < 300000L ? bestAddress : null;
   }

   public static long getBestPing() {
      long now = System.currentTimeMillis();
      return bestAddress != null && now - lastTestTime < 300000L ? bestPing : -1L;
   }

   public static List<MoonHypixelRouter.ServerTestResult> getLastResults() {
      return lastResults;
   }

   public static void reset() {
      bestAddress = null;
      bestPing = Long.MAX_VALUE;
      lastTestTime = 0L;
      lastResults = Collections.emptyList();
   }

   public static class ServerTestResult implements Comparable<MoonHypixelRouter.ServerTestResult> {
      public final String address;
      public final boolean success;
      public final long pingMs;

      ServerTestResult(String address, boolean success, long pingMs) {
         this.address = address;
         this.success = success;
         this.pingMs = pingMs;
      }

      public int compareTo(MoonHypixelRouter.ServerTestResult other) {
         if (this.success != other.success) {
            return this.success ? -1 : 1;
         } else {
            return Long.compare(this.pingMs, other.pingMs);
         }
      }

      @Override
      public String toString() {
         return !this.success ? this.address + " (unreachable)" : this.address + " (" + this.pingMs + " ms)";
      }
   }
}
