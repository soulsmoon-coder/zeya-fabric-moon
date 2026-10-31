package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.MoonConfig;
import dev.zeya.fabricmoon.MoonNetworkOptimizer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChannelInboundHandlerAdapter.class)
public abstract class NetworkChannelOptimizerMixin {
   @Inject(method = "channelRegistered", at = @At("HEAD"))
   private void zeyafabricmoon$optimizeChannel(ChannelHandlerContext ctx, CallbackInfo ci) {
      if (MoonConfig.featuresEnabled && MoonConfig.networkOptimizer) {
         Channel channel = ctx.channel();
         if (channel != null) {
            try {
               channel.config().setOption(ChannelOption.TCP_NODELAY, true);
               channel.config().setOption(ChannelOption.SO_KEEPALIVE, true);
               MoonNetworkOptimizer.onChannelOptimized();
            } catch (Exception var5) {
            }
         }
      }
   }
}
