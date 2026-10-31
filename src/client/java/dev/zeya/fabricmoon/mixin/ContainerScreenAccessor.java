package dev.zeya.fabricmoon.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
   @Accessor("hoveredSlot")
   Slot zeyafabricmoon$getHoveredSlot();

   @Accessor("menu")
   AbstractContainerMenu zeyafabricmoon$getMenu();

   @Accessor("leftPos")
   int zeyafabricmoon$getLeftPos();

   @Accessor("topPos")
   int zeyafabricmoon$getTopPos();
}
