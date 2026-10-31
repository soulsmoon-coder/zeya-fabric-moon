package dev.zeya.fabricmoon;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MoonMouseTracker {
   private static final long WINDOW_MILLIS = 1000L;
   private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
   private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();

   private MoonMouseTracker() {
   }

   public static synchronized void recordLeftClick() {
      recordClick(LEFT_CLICKS);
   }

   public static synchronized void recordRightClick() {
      recordClick(RIGHT_CLICKS);
   }

   public static synchronized int getLeftClicksPerSecond() {
      return countRecentClicks(LEFT_CLICKS);
   }

   public static synchronized int getRightClicksPerSecond() {
      return countRecentClicks(RIGHT_CLICKS);
   }

   private static void recordClick(Deque<Long> clicks) {
      long now = System.currentTimeMillis();
      clicks.addLast(now);
      removeExpired(clicks, now);
   }

   private static int countRecentClicks(Deque<Long> clicks) {
      removeExpired(clicks, System.currentTimeMillis());
      return clicks.size();
   }

   private static void removeExpired(Deque<Long> clicks, long now) {
      while (!clicks.isEmpty() && now - clicks.peekFirst() >= 1000L) {
         clicks.removeFirst();
      }
   }
}
