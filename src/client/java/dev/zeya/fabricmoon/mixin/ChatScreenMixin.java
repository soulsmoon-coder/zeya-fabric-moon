package dev.zeya.fabricmoon.mixin;

import dev.zeya.fabricmoon.ZeyaFabricMoonClient;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
   @Shadow
   protected EditBox input;

   @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
   private void zeyafabric$handleLocalCommand(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
      if (event.isConfirmation() && ZeyaFabricMoonClient.handleLocalCommand(this.input.getValue())) {
         callback.setReturnValue(true);
      }
   }
}
