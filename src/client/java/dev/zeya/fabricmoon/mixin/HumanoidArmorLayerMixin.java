package dev.zeya.fabricmoon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.zeya.fabricmoon.MoonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
   @Inject(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void zeyafabricmoon$hideLocalArmor(
      PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState state, float yRot, float xRot, CallbackInfo callback
   ) {
      Minecraft client = Minecraft.getInstance();
      if (MoonConfig.featuresEnabled
         && MoonConfig.hideArmor
         && state instanceof AvatarRenderState avatar
         && client.player != null
         && avatar.id == client.player.getId()) {
         callback.cancel();
      }
   }
}
