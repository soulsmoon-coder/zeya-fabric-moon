package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonCombatTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class CombatDamagePacketMixin {
   @Inject(method = "handleDamageEvent", at = @At("TAIL"))
   private void zeyafabricmoon$trackLocalDamage(ClientboundDamageEventPacket packet, CallbackInfo callback) {
      MoonCombatTracker.onDamageEvent(packet.entityId(), packet.sourceCauseId(), packet.sourceDirectId());
   }
}
