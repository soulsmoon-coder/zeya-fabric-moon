package dev.zeya.fabricmoon.macros;

import dev.zeya.fabricmoon.hud.MoonMouseTracker;
import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.network.MoonNetworkPolicy;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class MoonAutoClicker {
   private static long nextLeftClickNanos;

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
         nextLeftClickNanos = clickWhileHeld(
            client.mouseHandler.isLeftPressed(),
            MoonConfig.leftAutoClick,
            MoonConfig.leftCps,
            now,
            nextLeftClickNanos
         );
      } else {
         nextLeftClickNanos = 0L;
      }
   }

   private static long clickWhileHeld(boolean held, boolean enabled, int cps, long now, long nextClick) {
      if (held && enabled) {
         long interval = 1000000000L / MoonConfig.clampCps(cps);
         if (nextClick == 0L) {
            nextClick = now;
         }

         if (now >= nextClick) {
            KeyMapping.click(Type.MOUSE.getOrCreate(0));
            MoonMouseTracker.recordLeftClick();
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
