package dev.zeya.fabricmoon;

import dev.zeya.fabricmoon.hud.MoonCombatTracker;
import dev.zeya.fabricmoon.hud.MoonHud;
import dev.zeya.fabricmoon.hud.MoonHudPositionScreen;
import dev.zeya.fabricmoon.hud.MoonPingTracker;
import dev.zeya.fabricmoon.hud.MoonServerStats;
import dev.zeya.fabricmoon.macros.MoonAutoClicker;
import dev.zeya.fabricmoon.macros.MoonAutoFishing;
import dev.zeya.fabricmoon.macros.MoonBlazeDaggerMacro;
import dev.zeya.fabricmoon.macros.MoonKeyCommands;
import dev.zeya.fabricmoon.macros.MoonOxygenRecovery;
import dev.zeya.fabricmoon.macros.MoonSlotManager;
import dev.zeya.fabricmoon.macros.MoonWardrobeManager;
import dev.zeya.fabricmoon.macros.MoonWardrobeQuickSwap;
import dev.zeya.fabricmoon.network.MoonHypixelRouter;
import dev.zeya.fabricmoon.network.MoonNetworkOptimizer;
import dev.zeya.fabricmoon.visual.MoonHitboxRenderer;
import com.mojang.brigadier.arguments.FloatArgumentType;
import java.net.InetSocketAddress;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.Load;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.AllowCommand;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

public final class ZeyaFabricMoonClient implements ClientModInitializer {
   public void onInitializeClient() {
      MoonConfig.load();
      MoonSlotManager.refreshKeyMappings();
      AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hitResult) -> {
         if (player == Minecraft.getInstance().player) {
            MoonBlazeDaggerMacro.onMobHit(player, entity);
         }

         return InteractionResult.PASS;
      });
      ClientTickEvents.START_CLIENT_TICK.register(MoonAutoClicker::tick);
      ClientTickEvents.START_CLIENT_TICK.register(MoonServerStats::tick);
      ClientTickEvents.START_CLIENT_TICK.register(MoonNetworkOptimizer::tick);
      ClientTickEvents.START_CLIENT_TICK.register(MoonSlotManager::tick);
      ClientTickEvents.START_CLIENT_TICK.register(MoonWardrobeManager::checkMenuState);
      ClientTickEvents.START_CLIENT_TICK.register(MoonWardrobeQuickSwap::tick);
      ClientTickEvents.END_CLIENT_TICK.register(MoonAutoFishing::tick);
      ClientReceiveMessageEvents.GAME.register((message, overlay) -> MoonAutoFishing.onChatMessage(message.getString()));
      ClientTickEvents.END_CLIENT_TICK.register(MoonOxygenRecovery::tick);
      ClientTickEvents.END_CLIENT_TICK.register(MoonCombatTracker::tick);
      MoonKeyCommands.refreshKeyMappings();
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> {
         MoonPingTracker.reset();
         MoonNetworkOptimizer.reset();
         MoonHypixelRouter.reset();
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         MoonPingTracker.reset();
         MoonNetworkOptimizer.reset();
         MoonHypixelRouter.reset();
      });
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
         if (MoonConfig.featuresEnabled && MoonConfig.hypixelAutoRoute) {
            String address = handler.getConnection().getRemoteAddress() instanceof InetSocketAddress isa ? isa.getHostString() : "unknown";
            if (MoonHypixelRouter.isHypixelServer(address)) {
               MoonHypixelRouter.testAndSelectBest().thenAccept(best -> {
                  if (best != null) {
                     String msg = "[Moon] Best Hypixel route: " + best + " (" + MoonHypixelRouter.getBestPing() + " ms)";
                     client.execute(() -> MoonChatFeedback.send(client, msg));
                  }
               });
            }
         }
      });
      MoonHitboxRenderer.register();
      ClientEntityEvents.ENTITY_LOAD.register((Load)(entity, level) -> MoonAutoFishing.onEntityLoad(entity));
      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("zeyafabricmoon", "stats"), MoonHud::render);
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
         var root = ClientCommands.literal("zeyafabric")
            .executes(context -> openSettings(context.getSource().getClient()));
         var fishing = ClientCommands.literal("fishing")
            .executes(context -> toggleAutoFishing(context.getSource().getClient()));
         fishing.then(ClientCommands.literal("on").executes(context -> setAutoFishing(context.getSource().getClient(), true)));
         fishing.then(ClientCommands.literal("off").executes(context -> setAutoFishing(context.getSource().getClient(), false)));
         fishing.then(ClientCommands.literal("toggle").executes(context -> toggleAutoFishing(context.getSource().getClient())));
         root.then(fishing);

         var size = ClientCommands.literal("size")
            .executes(context -> showPlayerSize(context.getSource().getClient()));
         size.then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.1F, 1.5F))
            .executes(context -> setPlayerSize(context.getSource().getClient(), FloatArgumentType.getFloat(context, "value"))));
         size.then(ClientCommands.literal("reset").executes(context -> setPlayerSize(context.getSource().getClient(), 1.0F)));
         root.then(size);
         root.then(ClientCommands.literal("hud").executes(context -> openHudEditor(context.getSource().getClient())));

         var network = ClientCommands.literal("network")
            .executes(context -> showNetworkStatus(context.getSource().getClient()));
         network.then(ClientCommands.literal("test").executes(context -> testHypixelServers(context.getSource().getClient())));
         network.then(ClientCommands.literal("status").executes(context -> showNetworkStatus(context.getSource().getClient())));
         root.then(network);
         dispatcher.register(root);
      });
      ClientSendMessageEvents.ALLOW_COMMAND.register((AllowCommand)command -> !handleLocalCommand(command));
   }

   public static boolean handleLocalCommand(String message) {
      String trimmed = message.trim();
      if (trimmed.startsWith("/")) {
         trimmed = trimmed.substring(1);
      }

      if (trimmed.isEmpty()) {
         return false;
      }

      String[] arguments = trimmed.split("\\s+");
      if (!arguments[0].equalsIgnoreCase("zeyafabric")) {
         return false;
      }

      Minecraft client = Minecraft.getInstance();
      if (arguments.length == 1) {
         openSettings(client);
         return true;
      }

      if (!arguments[1].equalsIgnoreCase("size")) {
         if (arguments[1].equalsIgnoreCase("hud")) {
            if (arguments.length == 2) {
               openHudEditor(client);
            } else {
               sendCommandFeedback(client, "Uso: /zeyafabric hud");
               client.gui.setScreen(null);
            }

            return true;
         }

         if (!arguments[1].equalsIgnoreCase("fishing")) {
            sendCommandFeedback(client, "Uso: /zeyafabric [fishing|size|hud|network]");
            client.gui.setScreen(null);
            return true;
         }

         if (arguments.length > 3) {
            sendCommandFeedback(client, "Uso: /zeyafabric fishing [on|off|toggle]");
            client.gui.setScreen(null);
            return true;
         }

         String action = arguments.length == 2 ? "toggle" : arguments[2].toLowerCase(Locale.ROOT);
         switch (action) {
            case "on":
               setAutoFishing(client, true);
               break;
            case "off":
               setAutoFishing(client, false);
               break;
            case "toggle":
               toggleAutoFishing(client);
               break;
            default:
               sendCommandFeedback(client, "Use on, off ou toggle: /zeyafabric fishing [on|off|toggle]");
         }

         client.gui.setScreen(null);
         return true;
      } else {
         if (arguments.length == 2) {
            showPlayerSize(client);
         } else if (arguments.length == 3 && arguments[2].equalsIgnoreCase("reset")) {
            setPlayerSize(client, 1.0F);
         } else if (arguments.length == 3) {
            try {
               float size = Float.parseFloat(arguments[2]);
               if (Float.isFinite(size) && !(size < 0.1F) && !(size > 1.5F)) {
                  setPlayerSize(client, size);
               } else {
                  sendCommandFeedback(client, "O tamanho deve estar entre 0.1 e 1.5.");
               }
            } catch (NumberFormatException exception) {
               sendCommandFeedback(client, "Uso: /zeyafabric size <0.1-1.5|reset>");
            }
         } else {
            sendCommandFeedback(client, "Uso: /zeyafabric size <0.1-1.5|reset>");
         }

         client.gui.setScreen(null);
         return true;
      }
   }

   public static int openSettings(Minecraft client) {
      client.gui.setScreen(new MoonSettingsScreen());
      return 1;
   }

   public static int openHudEditor(Minecraft client) {
      client.gui.setScreen(new MoonHudPositionScreen());
      return 1;
   }

   private static int toggleAutoFishing(Minecraft client) {
      return setAutoFishing(client, !MoonConfig.autoFishing);
   }

   private static int setAutoFishing(Minecraft client, boolean enabled) {
      MoonConfig.autoFishing = enabled;
      MoonConfig.save();
      sendCommandFeedback(client, "Pesca automatica: " + (enabled ? "ON" : "OFF"));
      return 1;
   }

   private static int showPlayerSize(Minecraft client) {
      sendCommandFeedback(client, String.format(Locale.ROOT, "Player size visual: %.1fx (use /zeyafabric size <0.1-1.5|reset>)", MoonConfig.playerSize));
      return 1;
   }

   private static int setPlayerSize(Minecraft client, float size) {
      if (!Float.isFinite(size)) {
         sendCommandFeedback(client, "O tamanho precisa ser um numero finito entre 0.1 e 1.5.");
         return 0;
      } else {
         MoonConfig.playerSize = MoonConfig.clampSize(size);
         MoonConfig.save();
         sendCommandFeedback(client, String.format(Locale.ROOT, "Player size visual: %.1fx", MoonConfig.playerSize));
         return 1;
      }
   }

   public static int showNetworkStatus(Minecraft client) {
      int ping = MoonNetworkOptimizer.getSmoothedPing();
      double tps = MoonNetworkOptimizer.getSmoothedTps();
      int fps = client.getFps();
      String quality = MoonNetworkOptimizer.getConnectionQualityText();
      double jitter = MoonNetworkOptimizer.getJitter();
      StringBuilder sb = new StringBuilder();
      sb.append("━━━ Network Status ━━━\n");
      sb.append(String.format(Locale.ROOT, "  FPS: %d | TPS: %.1f | Ping: %s ms\n", fps, tps < 0.0 ? 0.0 : tps, ping < 0 ? "--" : ping));
      sb.append(String.format(Locale.ROOT, "  Jitter: %.1f ms | Qualidade: %s\n", jitter, quality));
      sb.append("  TCP Optimized: " + (MoonConfig.networkOptimizer ? "ON" : "OFF"));
      sb.append(" | Auto-Route: " + (MoonConfig.hypixelAutoRoute ? "ON" : "OFF"));
      String bestAddr = MoonHypixelRouter.getBestAddress();
      if (bestAddr != null) {
         sb.append(String.format(Locale.ROOT, "\n  Best route: %s (%d ms)", bestAddr, MoonHypixelRouter.getBestPing()));
      }

      sendCommandFeedback(client, sb.toString());
      return 1;
   }

   public static int testHypixelServers(Minecraft client) {
      sendCommandFeedback(client, "Testando servidores do Hypixel...");
      MoonHypixelRouter.testAndSelectBest().thenAccept(best -> client.execute(() -> {
         if (best != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("━━━ Hypixel Server Test ━━━\n");

            for (MoonHypixelRouter.ServerTestResult result : MoonHypixelRouter.getLastResults()) {
               String marker = result.address.equals(best) ? " <-- BEST" : "";
               sb.append("  ").append(result).append(marker).append("\n");
            }

            sendCommandFeedback(client, sb.toString());
         } else {
            sendCommandFeedback(client, "Nao foi possivel conectar a nenhum servidor do Hypixel.");
         }
      }));
      return 1;
   }

   private static void sendCommandFeedback(Minecraft client, String message) {
      MoonChatFeedback.send(client, "[ZeyaMoon] " + message);
   }

   public static Component toggleLabel(String name, boolean enabled) {
      return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
   }
}
