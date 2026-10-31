package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonSlotManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class SlotLockGameModeMixin {
   @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
   private void zeyafabricmoon$blockLockedSlotAction(int containerId, int slotId, int button, ContainerInput clickType, Player player, CallbackInfo ci) {
      if (player == Minecraft.getInstance().player && MoonSlotManager.shouldBlockContainerInput(Minecraft.getInstance(), slotId, button, clickType)) {
         MoonSlotManager.sendBlockedFeedback(Minecraft.getInstance(), "[Slot Lock] A operação foi bloqueada.");
         ci.cancel();
      }
   }
}
