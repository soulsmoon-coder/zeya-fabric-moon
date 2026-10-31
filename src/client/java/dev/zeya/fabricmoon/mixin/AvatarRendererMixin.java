package dev.zeya.fabricmoon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.zeya.fabricmoon.MoonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
   @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"))
   private void zeyafabricmoon$scaleLocalPlayer(AvatarRenderState state, PoseStack poseStack, CallbackInfo callback) {
      Minecraft client = Minecraft.getInstance();
      if (MoonConfig.featuresEnabled && client.player != null && state.id == client.player.getId()) {
         float size = MoonConfig.playerSize;
         poseStack.scale(size, size, size);
      }
   }

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At("TAIL")
   )
   private void zeyafabricmoon$hideLocalHeadItem(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo callback) {
      Minecraft client = Minecraft.getInstance();
      if (MoonConfig.featuresEnabled && client.player != null && state.id == client.player.getId()) {
         if (MoonConfig.hideHead) {
            state.headEquipment = ItemStack.EMPTY;
            state.heldOnHead.clear();
         }

         if (MoonConfig.playerGlow) {
            state.outlineColor = MoonConfig.playerGlowColor;
         }
      }
   }
}
