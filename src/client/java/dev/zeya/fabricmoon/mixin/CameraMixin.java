package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {
   @ModifyArg(method = "alignWithEntity(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 1)
   private float zeyafabricmoon$lookFromBelow(float pitch) {
      Minecraft client = Minecraft.getInstance();
      return MoonConfig.featuresEnabled && MoonConfig.thirdPersonLookFromBelow && client.options.getCameraType() != CameraType.FIRST_PERSON ? -35.0F : pitch;
   }
}
