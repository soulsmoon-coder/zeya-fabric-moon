package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LargeMobRenderScaleMixin {
   private static final float MIN_SCALE_FACTOR = 0.02F;
   private static final float MAX_SCALE_FACTOR = 20.0F;

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void zeyafabricmoon$matchPlayerSize(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo callback) {
      if (MoonConfig.featuresEnabled && MoonConfig.shrinkLargeMobs && entity instanceof Mob) {
         if (entity.getType() != EntityTypes.MAGMA_CUBE) {
            String id = EntityType.getKey(entity.getType()).toString();
            if (MoonConfig.isMobPlayerSizeTarget(id)) {
               LivingEntity player = Minecraft.getInstance().player;
               if (player != null) {
                  float entityHeight = entity.getBbHeight();
                  if (!(entityHeight <= 0.05F)) {
                     float factor = player.getBbHeight() * MoonConfig.mobSize / entityHeight;
                     if (Float.isFinite(factor)) {
                        state.scale = state.scale * Math.max(0.02F, Math.min(20.0F, factor));
                     }
                  }
               }
            }
         }
      }
   }
}
