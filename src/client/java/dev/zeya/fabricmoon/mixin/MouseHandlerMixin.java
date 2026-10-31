package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.MoonMouseTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
   @Inject(method = "onButton", at = @At("HEAD"))
   private void zeyafabricmoon$countManualClicks(long handle, MouseButtonInfo buttonInfo, int action, CallbackInfo callback) {
      if (MoonConfig.featuresEnabled && action == 1 && Minecraft.getInstance().gui.screen() == null) {
         if (buttonInfo.button() == 0) {
            MoonMouseTracker.recordLeftClick();
         } else if (buttonInfo.button() == 1) {
            MoonMouseTracker.recordRightClick();
         }
      }
   }
}
