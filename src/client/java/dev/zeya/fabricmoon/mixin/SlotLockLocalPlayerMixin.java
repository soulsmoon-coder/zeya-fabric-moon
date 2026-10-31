package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonSlotManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class SlotLockLocalPlayerMixin {
   @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
   private void zeyafabricmoon$blockDropOfLockedSlot(boolean all, CallbackInfoReturnable<Boolean> callback) {
      if (MoonSlotManager.shouldBlockSelectedSlotDrop()) {
         callback.setReturnValue(false);
      }
   }
}
