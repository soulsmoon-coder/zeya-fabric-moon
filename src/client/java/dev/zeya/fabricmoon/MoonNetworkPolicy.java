package dev.zeya.fabricmoon;

import java.util.Locale;

public final class MoonNetworkPolicy {
   private MoonNetworkPolicy() {
   }

   public static boolean allowsModServerActions() {
      return true;
   }

   private static String normalize(String address) {
      return address.trim().toLowerCase(Locale.ROOT);
   }
}
