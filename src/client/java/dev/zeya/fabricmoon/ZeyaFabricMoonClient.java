package dev.zeya.fabricmoon;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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
            MoonCombatTracker.onPlayerAttack(entity);
            MoonBlazeDaggerMacro.onMobHit(player, entity);
         }

         return InteractionResult.PASS;
      });
      UseItemCallback.EVENT.register((UseItemCallback)(player, level, hand) -> {
         MoonLoadoutSwapMacro.onMainHandItemUsed(player, hand, player.getItemInHand(hand).copy());
         return InteractionResult.PASS;
      });
      ClientTickEvents.START_CLIENT_TICK.register(MoonAutoClicker::tick);
      ClientTickEvents.START_CLIENT_TICK.register(MoonServerStats::tick);
      ClientTickEvents.START_CLIENT_TICK.register(MoonNetworkOptimizer::tick);
      ClientTickEvents.START_CLIENT_TICK.register(MoonSlotManager::tick);
      ClientTickEvents.START_CLIENT_TICK.register(MoonWardrobeManager::checkMenuState);
      ClientTickEvents.START_CLIENT_TICK.register(MoonWardrobeQuickSwap::tick);
      ClientTickEvents.END_CLIENT_TICK.register(MoonAutoFishing::tick);
      ClientTickEvents.END_CLIENT_TICK.register(MoonOxygenRecovery::tick);
      ClientTickEvents.END_CLIENT_TICK.register(MoonCombatTracker::tick);
      ClientTickEvents.END_CLIENT_TICK.register(MoonLoadoutSwapMacro::tick);
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
                     client.execute(() -> {
                        if (client.gui.chatListener() != null) {
                           client.gui.chatListener().handleSystemMessage(Component.literal(msg), false);
                        }
                     });
                  }
               });
            }
         }
      });
      MoonHitboxRenderer.register();
      ClientEntityEvents.ENTITY_LOAD.register((Load)(entity, level) -> MoonAutoFishing.onEntityLoad(entity));
      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("zeyafabricmoon", "stats"), MoonHud::render);
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal(
                                       "zeyafabric"
                                    )
                                    .executes(context -> openSettings(((FabricClientCommandSource)context.getSource()).getClient())))
                                 .then(
                                    ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("fishing")
                                                .executes(context -> toggleAutoFishing(((FabricClientCommandSource)context.getSource()).getClient())))
                                             .then(
                                                ClientCommands.literal("on")
                                                   .executes(context -> setAutoFishing(((FabricClientCommandSource)context.getSource()).getClient(), true))
                                             ))
                                          .then(
                                             ClientCommands.literal("off")
                                                .executes(context -> setAutoFishing(((FabricClientCommandSource)context.getSource()).getClient(), false))
                                          ))
                                       .then(
                                          ClientCommands.literal("toggle")
                                             .executes(context -> toggleAutoFishing(((FabricClientCommandSource)context.getSource()).getClient()))
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("rightclick")
                                             .executes(context -> toggleRightAutoClick(((FabricClientCommandSource)context.getSource()).getClient())))
                                          .then(
                                             ClientCommands.literal("on")
                                                .executes(context -> setRightAutoClick(((FabricClientCommandSource)context.getSource()).getClient(), true))
                                          ))
                                       .then(
                                          ClientCommands.literal("off")
                                             .executes(context -> setRightAutoClick(((FabricClientCommandSource)context.getSource()).getClient(), false))
                                       ))
                                    .then(
                                       ClientCommands.literal("toggle")
                                          .executes(context -> toggleRightAutoClick(((FabricClientCommandSource)context.getSource()).getClient()))
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("place")
                                          .executes(context -> toggleRightAutoClick(((FabricClientCommandSource)context.getSource()).getClient())))
                                       .then(
                                          ClientCommands.literal("on")
                                             .executes(context -> setRightAutoClick(((FabricClientCommandSource)context.getSource()).getClient(), true))
                                       ))
                                    .then(
                                       ClientCommands.literal("off")
                                          .executes(context -> setRightAutoClick(((FabricClientCommandSource)context.getSource()).getClient(), false))
                                    ))
                                 .then(
                                    ClientCommands.literal("toggle")
                                       .executes(context -> toggleRightAutoClick(((FabricClientCommandSource)context.getSource()).getClient()))
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("size")
                                    .executes(context -> showPlayerSize(((FabricClientCommandSource)context.getSource()).getClient())))
                                 .then(
                                    ClientCommands.argument("value", FloatArgumentType.floatArg(0.1F, 1.5F))
                                       .executes(
                                          context -> setPlayerSize(
                                             ((FabricClientCommandSource)context.getSource()).getClient(), FloatArgumentType.getFloat(context, "value")
                                          )
                                       )
                                 ))
                              .then(
                                 ClientCommands.literal("reset")
                                    .executes(context -> setPlayerSize(((FabricClientCommandSource)context.getSource()).getClient(), 1.0F))
                              )
                        ))
                     .then(ClientCommands.literal("hud").executes(context -> openHudEditor(((FabricClientCommandSource)context.getSource()).getClient()))))
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("network")
                              .executes(context -> showNetworkStatus(((FabricClientCommandSource)context.getSource()).getClient())))
                           .then(
                              ClientCommands.literal("test")
                                 .executes(context -> testHypixelServers(((FabricClientCommandSource)context.getSource()).getClient()))
                           ))
                        .then(
                           ClientCommands.literal("status")
                              .executes(context -> showNetworkStatus(((FabricClientCommandSource)context.getSource()).getClient()))
                        )
                  )
            )
         );
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
         } else {
            if (!arguments[1].equalsIgnoreCase("fishing")
               && !arguments[1].equalsIgnoreCase("rightclick")
               && !arguments[1].equalsIgnoreCase("place")
               && !arguments[1].equalsIgnoreCase("block")) {
               sendCommandFeedback(client, "Uso: /zeyafabric [fishing|rightclick|place|block|size|hud] [on|off|toggle|valor]");
               client.gui.setScreen(null);
               return true;
            }

            if (arguments.length > 3) {
               sendCommandFeedback(client, "Uso: /zeyafabric " + arguments[1].toLowerCase(Locale.ROOT) + " [on|off|toggle]");
               client.gui.setScreen(null);
               return true;
            }

            String action = arguments.length == 2 ? "toggle" : arguments[2].toLowerCase(Locale.ROOT);
            if (arguments[1].equalsIgnoreCase("fishing")) {
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
            } else {
               switch (action) {
                  case "on":
                     setRightAutoClick(client, true);
                     break;
                  case "off":
                     setRightAutoClick(client, false);
                     break;
                  case "toggle":
                     toggleRightAutoClick(client);
                     break;
                  default:
                     sendCommandFeedback(client, "Use on, off ou toggle: /zeyafabric rightclick [on|off|toggle]");
               }
            }

            client.gui.setScreen(null);
            return true;
         }
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

   private static int toggleRightAutoClick(Minecraft client) {
      return setRightAutoClick(client, !MoonConfig.rightAutoClick);
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

   private static int setRightAutoClick(Minecraft client, boolean enabled) {
      MoonConfig.rightAutoClick = enabled;
      MoonConfig.save();
      sendCommandFeedback(client, "Auto-click para colocar blocos: " + (enabled ? "ON" : "OFF"));
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

   private static int showNetworkStatus(Minecraft client) {
      int ping = MoonNetworkOptimizer.getSmoothedPing();
      double tps = MoonNetworkOptimizer.getSmoothedTps();
      int fps = MoonNetworkOptimizer.getSmoothedFps();
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

   private static int testHypixelServers(Minecraft client) {
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
      client.gui.chatListener().handleSystemMessage(Component.literal("[ZeyaMoon] " + message), false);
   }

   public static Component toggleLabel(String name, boolean enabled) {
      return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
   }
}
