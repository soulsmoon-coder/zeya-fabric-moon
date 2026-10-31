package dev.zeya.fabricmoon;

import net.minecraft.client.Minecraft;

public final class MoonOxygenRecovery {
   private static boolean forcingJump;

   private MoonOxygenRecovery() {
   }

   public static void tick(Minecraft client) {
      if (MoonNetworkPolicy.allowsModServerActions() && MoonConfig.featuresEnabled && client.player != null && MoonConfig.oxygenRecovery) {
         boolean shouldSwimUp = client.player.isUnderWater() && client.player.getAirSupply() <= MoonConfig.oxygenThreshold;
         if (shouldSwimUp) {
            if (!forcingJump) {
               forcingJump = true;
            }

            client.options.keyJump.setDown(true);
         } else {
            releaseJump(client);
         }
      } else {
         releaseJump(client);
      }
   }

   private static void releaseJump(Minecraft client) {
      if (forcingJump) {
         client.options.keyJump.setDown(false);
         forcingJump = false;
      }
   }
}
