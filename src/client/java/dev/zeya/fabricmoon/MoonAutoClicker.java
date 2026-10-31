package dev.zeya.fabricmoon;

import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.Random;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class MoonAutoClicker {
   private static final Random RANDOM = new Random();
   private static long nextLeftClickNanos;
   private static long nextRightClickNanos;

   private MoonAutoClicker() {
   }

   public static void tick(Minecraft client) {
      if (MoonNetworkPolicy.allowsModServerActions()
         && MoonConfig.featuresEnabled
         && client.player != null
         && client.level != null
         && client.gui.screen() == null
         && client.isWindowActive()) {
         long now = System.nanoTime();
         nextLeftClickNanos = clickWhileHeld(client.mouseHandler.isLeftPressed(), MoonConfig.leftAutoClick, MoonConfig.leftCps, 0, now, nextLeftClickNanos);
         nextRightClickNanos = clickWhileHeld(client.mouseHandler.isRightPressed(), MoonConfig.rightAutoClick, MoonConfig.rightCps, 1, now, nextRightClickNanos);
      } else {
         nextLeftClickNanos = 0L;
         nextRightClickNanos = 0L;
      }
   }

   private static long clickWhileHeld(boolean held, boolean enabled, int cps, int button, long now, long nextClick) {
      if (held && enabled) {
         long baseInterval = 1000000000L / MoonConfig.clampCps(cps) + MoonConfig.clampMacroDelay(MoonConfig.macroExtraDelayMillis) * 1000000L;
         long jitter = (long)(RANDOM.nextDouble() * (long)(baseInterval * 0.3)) - (long)(baseInterval * 0.15);
         long interval = baseInterval + jitter;
         if (nextClick == 0L) {
            nextClick = now;
         }

         if (now >= nextClick) {
            KeyMapping.click(Type.MOUSE.getOrCreate(button));
            if (button == 0) {
               MoonMouseTracker.recordLeftClick();
            } else {
               MoonMouseTracker.recordRightClick();
            }

            nextClick += interval;
            if (nextClick < now - interval * 2L) {
               nextClick = now + interval;
            }
         }

         return nextClick;
      } else {
         return 0L;
      }
   }
}
