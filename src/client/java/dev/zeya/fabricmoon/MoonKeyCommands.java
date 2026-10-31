package dev.zeya.fabricmoon;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class MoonKeyCommands {
   private MoonKeyCommands() {
   }

   public static void refreshKeyMappings() {
   }

   public static void onKeyPressed(Minecraft client, int pressedKeyCode) {
      if (MoonNetworkPolicy.allowsModServerActions()
         && MoonConfig.featuresEnabled
         && MoonConfig.keyCommandsEnabled
         && client.player != null
         && client.gui.screen() == null
         && pressedKeyCode >= 0) {
         for (String binding : MoonConfig.keyCommandBindings) {
            int eq = binding.indexOf(61);
            if (eq > 0 && eq < binding.length() - 1) {
               int keyCode;
               try {
                  keyCode = Integer.parseInt(binding.substring(0, eq).trim());
               } catch (NumberFormatException ignored) {
                  continue;
               }

               if (pressedKeyCode == keyCode) {
                  String command = binding.substring(eq + 1).trim();
                  executeCommand(client, command);
               }
            }
         }
      }
   }

   private static void executeCommand(Minecraft client, String command) {
      if (client.player != null) {
         String toSend = command.startsWith("/") ? command : "/" + command;
         client.player.connection.sendCommand(toSend.substring(1));
      }
   }

   public static List<String> getReadableBindings() {
      List<String> readable = new ArrayList<>();

      for (String binding : MoonConfig.keyCommandBindings) {
         int eq = binding.indexOf(61);
         if (eq > 0 && eq < binding.length() - 1) {
            String keyName = getKeyDisplayName(binding.substring(0, eq).trim());
            String command = binding.substring(eq + 1).trim();
            readable.add(keyName + " = " + command);
         }
      }

      return readable;
   }

   static String getKeyDisplayName(String keyCodeStr) {
      try {
         int keyCode = Integer.parseInt(keyCodeStr);
         switch (keyCode) {
            case 256:
               return "Esc";
            case 257:
               return "Enter";
            case 258:
               return "Tab";
            case 259:
               return "Backspace";
            case 260:
               return "Space";
            case 261:
            case 268:
            case 269:
            case 270:
            case 271:
            case 272:
            case 273:
            case 274:
            case 275:
            case 276:
            case 277:
            case 278:
            case 279:
            case 280:
            case 281:
            case 282:
            case 283:
            case 284:
            case 285:
            case 286:
            case 287:
            case 288:
            case 289:
            case 290:
            case 291:
            case 292:
            case 293:
            case 294:
            case 295:
            case 296:
            case 297:
            case 298:
            case 299:
            case 300:
            case 301:
            case 302:
            case 303:
            case 304:
            case 305:
            case 306:
            case 307:
            case 308:
            case 309:
            case 310:
            case 311:
            case 312:
            case 313:
            case 314:
            case 315:
            case 316:
            case 317:
            case 318:
            case 319:
            case 320:
            case 321:
            case 322:
            case 323:
            case 324:
            case 325:
            case 326:
            case 327:
            case 328:
            case 329:
            case 330:
            case 331:
            case 332:
            case 333:
            case 334:
            case 335:
            case 336:
            case 337:
            case 338:
            case 339:
            default:
               if (keyCode >= 48 && keyCode <= 57) {
                  return "0123456789".substring(keyCode - 48, keyCode - 47);
               } else {
                  if (keyCode >= 65 && keyCode <= 90) {
                     return String.valueOf((char)(65 + (keyCode - 65)));
                  }

                  return "Key " + keyCode;
               }
            case 262:
               return "Insert";
            case 263:
               return "Delete";
            case 264:
               return "Right";
            case 265:
               return "Left";
            case 266:
               return "Down";
            case 267:
               return "Up";
            case 340:
               return "F1";
            case 341:
               return "F2";
            case 342:
               return "F3";
            case 343:
               return "F4";
            case 344:
               return "F5";
            case 345:
               return "F6";
            case 346:
               return "F7";
            case 347:
               return "F8";
            case 348:
               return "F9";
            case 349:
               return "F10";
            case 350:
               return "F11";
            case 351:
               return "F12";
            case 352:
               return "F13";
            case 353:
               return "F14";
            case 354:
               return "F15";
            case 355:
               return "F16";
            case 356:
               return "F17";
            case 357:
               return "F18";
            case 358:
               return "F19";
            case 359:
               return "F20";
         }
      } catch (NumberFormatException e) {
         return keyCodeStr;
      }
   }

   public static String getKeyDisplayName(int keyCode) {
      return getKeyDisplayName(String.valueOf(keyCode));
   }
}
