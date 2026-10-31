package dev.zeya.fabricmoon;

import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class MoonLoadoutSwapMacro {
   private static final Random RANDOM = new Random();
   private static final int[] LOADOUT_MENU_SLOTS = new int[]{14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
   private static final int MAX_TICKS = 40;
   private static final int MIN_HUMAN_DELAY_TICKS = 3;
   private static final int MAX_HUMAN_DELAY_TICKS = 6;
   private static MoonLoadoutSwapMacro.PendingSwap pending;
   private static MoonLoadoutSwapMacro.Stage stage = MoonLoadoutSwapMacro.Stage.IDLE;
   private static int ticksInStage;
   private static int humanDelayTicks;
   private static ItemStack trackedBow = ItemStack.EMPTY;
   private static int trackedBowTicks;

   private MoonLoadoutSwapMacro() {
   }

   public static void onMainHandItemUsed(Player player, InteractionHand hand, ItemStack stack) {
      Minecraft client = Minecraft.getInstance();
      if (MoonNetworkPolicy.allowsModServerActions()
         && hand == InteractionHand.MAIN_HAND
         && player == client.player
         && MoonConfig.featuresEnabled
         && MoonConfig.bonemerangLoadoutSwap
         && isBonemerang(stack)) {
         queue(MoonConfig.bonemerangLoadoutSlot);
      }
   }

   public static void tick(Minecraft client) {
      if (MoonNetworkPolicy.allowsModServerActions() && MoonConfig.featuresEnabled && client.player != null && client.gameMode != null) {
         trackBowUse(client.player);
         if (stage == MoonLoadoutSwapMacro.Stage.IDLE) {
            if (pending == null) {
               return;
            }

            humanDelayTicks = 3 + RANDOM.nextInt(3);
            stage = MoonLoadoutSwapMacro.Stage.SWITCH_WEAPON;
            ticksInStage = 0;
         }

         if (++ticksInStage > 40) {
            reset();
         } else if (humanDelayTicks > 0) {
            humanDelayTicks--;
         } else {
            switch (stage) {
               case IDLE:
               default:
                  break;
               case SWITCH_WEAPON:
                  int slot = findSwapWeaponSlot(client.player);
                  if (slot < 0) {
                     reset();
                     return;
                  }

                  client.player.getInventory().setSelectedSlot(slot);
                  transitionTo(MoonLoadoutSwapMacro.Stage.OPEN_LOADOUTS);
                  humanDelayTicks = 3 + RANDOM.nextInt(3);
                  break;
               case OPEN_LOADOUTS:
                  if (client.gui.screen() != null) {
                     reset();
                     return;
                  }

                  client.player.connection.sendCommand("loadouts");
                  transitionTo(MoonLoadoutSwapMacro.Stage.SELECT_LOADOUT);
                  humanDelayTicks = 3 + RANDOM.nextInt(3);
                  break;
               case SELECT_LOADOUT:
                  if (client.gui.screen() instanceof AbstractContainerScreen<?> screen && normalize(screen.getTitle().getString()).contains("loadout")) {
                     int configIndex = MoonConfig.clampLoadoutSlot(pending.loadoutSlot()) - 1;
                     int menuSlot = LOADOUT_MENU_SLOTS[configIndex];
                     AbstractContainerMenu menu = screen.getMenu();
                     if (menuSlot >= menu.slots.size()) {
                        reset();
                        return;
                     }

                     client.gameMode.handleContainerInput(menu.containerId, menuSlot, 0, ContainerInput.PICKUP, client.player);
                     transitionTo(MoonLoadoutSwapMacro.Stage.CLOSE_MENU);
                  }
                  break;
               case CLOSE_MENU:
                  client.player.closeContainer();
                  reset();
            }
         }
      } else {
         reset();
      }
   }

   private static void queue(int loadoutSlot) {
      if (pending == null) {
         pending = new MoonLoadoutSwapMacro.PendingSwap(MoonConfig.clampLoadoutSlot(loadoutSlot));
      }
   }

   private static void trackBowUse(Player player) {
      if (player.isUsingItem() && player.getUseItem().getItem() instanceof BowItem) {
         trackedBow = player.getUseItem().copy();
         trackedBowTicks = player.getTicksUsingItem();
      } else {
         if (!trackedBow.isEmpty() && MoonConfig.deathBowLoadoutSwap && isDeathBow(trackedBow) && BowItem.getPowerForTime(trackedBowTicks) >= 1.0F) {
            queue(MoonConfig.deathBowLoadoutSlot);
         }

         trackedBow = ItemStack.EMPTY;
         trackedBowTicks = 0;
      }
   }

   private static int findSwapWeaponSlot(Player player) {
      Minecraft client = Minecraft.getInstance();

      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (!stack.isEmpty()) {
            Component hoverName = stack.getHoverName();
            String name = hoverName != null ? normalize(hoverName.getString()) : "";
            String id = skyblockId(stack);
            if (name.contains("atomsplit katana") || name.contains("axe of the shredded") || id.equals("ATOMSPLIT_KATANA") || id.equals("AXE_OF_THE_SHREDDED")) {
               if (slot >= 9) {
                  for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
                     if (player.getInventory().getItem(hotbarSlot).isEmpty()) {
                        int menuSlotId = findMenuSlotId(client.player.containerMenu, player.getInventory(), slot);
                        if (menuSlotId >= 0 && client.gameMode != null) {
                           client.gameMode.handleContainerInput(client.player.containerMenu.containerId, menuSlotId, hotbarSlot, ContainerInput.SWAP, player);
                           return hotbarSlot;
                        }
                     }
                  }

                  return -1;
               } else {
                  return slot;
               }
            }
         }
      }

      return -1;
   }

   private static int findMenuSlotId(AbstractContainerMenu menu, Inventory inventory, int inventoryIndex) {
      for (int menuIndex = 0; menuIndex < menu.slots.size(); menuIndex++) {
         Slot slot = (Slot)menu.slots.get(menuIndex);
         if (slot.container == inventory && slot.getContainerSlot() == inventoryIndex) {
            return menuIndex;
         }
      }

      return -1;
   }

   private static boolean isBonemerang(ItemStack stack) {
      Component hoverName = stack.getHoverName();
      String name = hoverName != null ? normalize(hoverName.getString()) : "";
      return name.contains("bonemerang") || skyblockId(stack).contains("BONEMERANG");
   }

   private static boolean isDeathBow(ItemStack stack) {
      Component hoverName = stack.getHoverName();
      String name = hoverName != null ? normalize(hoverName.getString()) : "";
      return name.contains("death bow") || skyblockId(stack).contains("DEATH_BOW");
   }

   private static String skyblockId(ItemStack stack) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return customData == null ? "" : customData.copyTag().getStringOr("id", "");
   }

   private static String normalize(String text) {
      return text.replaceAll("§.", "").toLowerCase(Locale.ROOT).trim();
   }

   private static void transitionTo(MoonLoadoutSwapMacro.Stage next) {
      stage = next;
      ticksInStage = 0;
   }

   private static void reset() {
      pending = null;
      stage = MoonLoadoutSwapMacro.Stage.IDLE;
      ticksInStage = 0;
      trackedBow = ItemStack.EMPTY;
      trackedBowTicks = 0;
   }

   private record PendingSwap(int loadoutSlot) {
   }

   private enum Stage {
      IDLE,
      SWITCH_WEAPON,
      OPEN_LOADOUTS,
      SELECT_LOADOUT,
      CLOSE_MENU;
   }
}
