package io.github.safetimamovic.adjustability.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** What {@link CreativeModeInventoryScreenMixin} needs from the container screen it extends. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor
{
        @Accessor("hoveredSlot")
        @Nullable Slot adjustability$hoveredSlot();

        @Accessor("leftPos")
        int adjustability$leftPos();

        @Accessor("topPos")
        int adjustability$topPos();
}
