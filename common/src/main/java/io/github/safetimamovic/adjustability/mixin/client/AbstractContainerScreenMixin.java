package io.github.safetimamovic.adjustability.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.safetimamovic.adjustability.client.AdjustClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Adjust key needs the hovered slot, which only the screen knows. And opening the Adjust screen replaces the
 * container screen, whose {@code removed()} would tear the menu down on the client (returning crafting grid items and
 * so on) while the server keeps it open; that is skipped for exactly that one replacement.
 */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin
{
        @Shadow
        protected @Nullable Slot hoveredSlot;

        @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
        private void adjustability$openAdjustScreen(KeyEvent event, CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.onContainerKey((AbstractContainerScreen<?>) (Object) this, this.hoveredSlot, event))
                {
                        cir.setReturnValue(true);
                }
        }

        @WrapOperation(method = "removed", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;removed(Lnet/minecraft/world/entity/player/Player;)V"))
        private void adjustability$keepMenuOpenForAdjust(AbstractContainerMenu menu, Player player,
                Operation<Void> original)
        {
                if (!AdjustClient.consumeOpeningAdjust())
                {
                        original.call(menu, player);
                }
        }
}
