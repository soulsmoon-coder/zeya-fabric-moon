package dev.zeya.fabricmoon;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MoonWardrobeManager {
   private static final Map<Integer, MoonWardrobeManager.LoadoutProfile> loadoutProfiles = new HashMap<>();
   private static boolean inWardrobeMenu = false;
   private static boolean inLoadoutMenu = false;
   private static long lastClickNanos = 0L;
   private static final long CLICK_COOLDOWN_MS = 150L;

   private MoonWardrobeManager() {
   }

   public static void checkMenuState(Minecraft client) {
      if (client.gui.screen() == null) {
         inWardrobeMenu = false;
         inLoadoutMenu = false;
      } else if (!(client.gui.screen() instanceof AbstractContainerScreen)) {
         inWardrobeMenu = false;
         inLoadoutMenu = false;
      } else {
         Component title = ((AbstractContainerScreen)client.gui.screen()).getTitle();
         if (title == null) {
            inWardrobeMenu = false;
            inLoadoutMenu = false;
         } else {
            String titleStr = title.getString().toLowerCase();
            inWardrobeMenu = titleStr.contains("wardrobe") || titleStr.contains("armario");
            inLoadoutMenu = titleStr.contains("loadout") || titleStr.contains("equipamento");
         }
      }
   }

   public static MoonWardrobeManager.LoadoutProfile captureCurrentLoadout(Minecraft client) {
      if (client.player == null) {
         return new MoonWardrobeManager.LoadoutProfile();
      }

      MoonWardrobeManager.LoadoutProfile profile = new MoonWardrobeManager.LoadoutProfile();
      profile.head = getItemKey(client.player.getItemBySlot(EquipmentSlot.HEAD));
      profile.chest = getItemKey(client.player.getItemBySlot(EquipmentSlot.CHEST));
      profile.legs = getItemKey(client.player.getItemBySlot(EquipmentSlot.LEGS));
      profile.boots = getItemKey(client.player.getItemBySlot(EquipmentSlot.FEET));
      profile.mainhand = getItemKey(client.player.getMainHandItem());
      profile.offhand = getItemKey(client.player.getOffhandItem());
      return profile;
   }

   public static void saveLoadout(int slot, MoonWardrobeManager.LoadoutProfile profile) {
      slot = Math.max(1, Math.min(12, slot));
      loadoutProfiles.put(slot, profile);
   }

   public static MoonWardrobeManager.LoadoutProfile getLoadout(int slot) {
      slot = Math.max(1, Math.min(12, slot));
      return loadoutProfiles.get(slot);
   }

   public static void removeLoadout(int slot) {
      slot = Math.max(1, Math.min(12, slot));
      loadoutProfiles.remove(slot);
   }

   public static int getLoadoutCount() {
      return loadoutProfiles.size();
   }

   public static Map<Integer, MoonWardrobeManager.LoadoutProfile> getAllLoadouts() {
      return new HashMap<>(loadoutProfiles);
   }

   private static String getItemKey(ItemStack stack) {
      return !stack.isEmpty() && !stack.is(Items.AIR) ? stack.getItem().toString() : "";
   }

   public static boolean isClickOnCooldown() {
      long now = System.nanoTime();
      return (now - lastClickNanos) / 1000000L < 150L;
   }

   public static void recordClick() {
      lastClickNanos = System.nanoTime();
   }

   public static boolean isInWardrobeMenu() {
      return inWardrobeMenu;
   }

   public static boolean isInLoadoutMenu() {
      return inLoadoutMenu;
   }

   public static void sendFeedback(Minecraft client, String message) {
      if (client.gui.chatListener() != null) {
         client.gui.chatListener().handleSystemMessage(Component.literal("[ZeyaMoon] [Wardrobe] " + message), false);
      }
   }

   public static class LoadoutProfile {
      public String head = "";
      public String chest = "";
      public String legs = "";
      public String boots = "";
      public String mainhand = "";
      public String offhand = "";

      @Override
      public String toString() {
         return String.format("Head:%s Chest:%s Legs:%s Boots:%s Main:%s Off:%s", this.head, this.chest, this.legs, this.boots, this.mainhand, this.offhand);
      }
   }
}
