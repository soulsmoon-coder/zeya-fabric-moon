package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonConfig;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class FullbrightMixin {
   @Inject(method = "extract", at = @At("TAIL"))
   private void zeyafabricmoon$applyFullbright(LightmapRenderState state, float partialTicks, CallbackInfo callback) {
      if (MoonConfig.featuresEnabled && MoonConfig.fullbright) {
         state.needsUpdate = true;
         state.blockFactor = 1.0F;
         state.skyFactor = 1.0F;
         state.brightness = 1.0F;
         state.darknessEffectScale = 0.0F;
         state.bossOverlayWorldDarkening = 0.0F;
         state.nightVisionEffectIntensity = 1.0F;
         state.blockLightTint = LightmapRenderStateExtractor.WHITE;
         state.skyLightColor = LightmapRenderStateExtractor.WHITE;
         state.ambientColor = LightmapRenderStateExtractor.WHITE;
         state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
      }
   }
}
