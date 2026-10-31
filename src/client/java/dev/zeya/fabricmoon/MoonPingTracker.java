package dev.zeya.fabricmoon;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class MoonPingTracker {
   private static final long PING_VALID_MILLIS = 5000L;
   private static final long PING_EXPIRE_MILLIS = 10000L;
   private static final Map<Long, Long> keepAliveReceivedTimes = new HashMap<>();
   private static int lastPing = -1;
   private static long lastPingTime = 0L;

   private MoonPingTracker() {
   }

   public static void onKeepAliveReceived(long id) {
      if (MoonConfig.featuresEnabled) {
         keepAliveReceivedTimes.put(id, System.currentTimeMillis());
      }
   }

   public static void onKeepAliveSent(long id) {
      if (MoonConfig.featuresEnabled) {
         Long receivedAt = keepAliveReceivedTimes.remove(id);
         if (receivedAt != null) {
            long elapsed = System.currentTimeMillis() - receivedAt;
            if (elapsed >= 0L && elapsed <= 5000L) {
               lastPing = (int)elapsed;
               lastPingTime = System.currentTimeMillis();
            }
         }
      }
   }

   public static int getPing() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.player.connection != null) {
         PlayerInfo playerInfo = client.player.connection.getPlayerInfo(client.player.getUUID());
         return playerInfo == null ? -1 : Math.max(-1, playerInfo.getLatency());
      } else {
         return -1;
      }
   }

   public static void reset() {
      keepAliveReceivedTimes.clear();
      lastPing = -1;
      lastPingTime = 0L;
   }

   public static void tick(Minecraft client) {
   }

   public static void recordResponse(long sentAtMillis) {
   }
}
