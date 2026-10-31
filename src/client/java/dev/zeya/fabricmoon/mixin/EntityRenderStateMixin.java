package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin {
   @Inject(method = "appearsGlowing", at = @At("HEAD"), cancellable = true)
   private void zeyafabricmoon$keepLocalGlowEnabledForBlack(CallbackInfoReturnable<Boolean> callback) {
      if (MoonConfig.featuresEnabled && MoonConfig.playerGlow && (Object)this instanceof AvatarRenderState state) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && state.id == client.player.getId()) {
            callback.setReturnValue(true);
         }
      }
   }
}
