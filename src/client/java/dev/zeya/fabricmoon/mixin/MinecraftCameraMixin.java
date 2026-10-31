package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Minecraft.class)
public abstract class MinecraftCameraMixin {
   @ModifyArg(
      method = "handleKeybinds",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;setCameraType(Lnet/minecraft/client/CameraType;)V"),
      index = 0
   )
   private CameraType zeyafabricmoon$skipFrontCamera(CameraType cameraType) {
      return MoonConfig.featuresEnabled && cameraType == CameraType.THIRD_PERSON_FRONT ? CameraType.FIRST_PERSON : cameraType;
   }
}
