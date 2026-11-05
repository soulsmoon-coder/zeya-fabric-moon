package dev.zeya.fabricmoon;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public final class MoonChatFeedback {
   private MoonChatFeedback() {
   }

   public static void send(Minecraft client, String message) {
      if (client.gui.chatListener() != null) {
         Component component = Component.literal(message)
            .withStyle(style -> style.withColor(TextColor.fromRgb(MoonConfig.modColor)));
         client.gui.chatListener().handleSystemMessage(component, false);
      }
   }
}
