package io.github.safetimamovic.adjustability.mixin.client;

import io.github.safetimamovic.adjustability.client.AdjustClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Adjust panel lives inside container screens: it needs the hovered slot and the screen's position, its key and
 * clicks, a place to draw on top of everything, and the item tooltip out of the way while it's open. One code path for
 * both loaders; the logic is in {@link AdjustClient}.
 */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin
{
        @Shadow
        protected @Nullable Slot hoveredSlot;

        @Shadow
        protected int leftPos;

        @Shadow
        protected int topPos;

        @Unique
        private AbstractContainerScreen<?> adjustability$self()
        {
                return (AbstractContainerScreen<?>) (Object) this;
        }

        @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
        private void adjustability$adjustKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.onKey(this.adjustability$self(), this.hoveredSlot, this.leftPos, this.topPos, event))
                {
                        cir.setReturnValue(true);
                }
        }

        @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
        private void adjustability$panelClick(MouseButtonEvent event, boolean doubleClick,
                CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.onMouseClicked(this.adjustability$self(), event))
                {
                        cir.setReturnValue(true);
                }
        }

        @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
        private void adjustability$panelRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.onMouseReleased())
                {
                        cir.setReturnValue(true);
                }
        }

        @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
        private void adjustability$noScrollUnderPanel(double x, double y, double scrollX, double scrollY,
                CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.isPanelOpen(this.adjustability$self()))
                {
                        cir.setReturnValue(true);
                }
        }

        /**
         * Every container screen draws its item tooltip last, including the recipe book screens that skip this class's
         * extractRenderState. So the panel is drawn there, in place of the tooltip.
         */
        @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
        private void adjustability$panelInsteadOfTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                CallbackInfo ci)
        {
                if (AdjustClient.isPanelOpen(this.adjustability$self()))
                {
                        AdjustClient.render(this.adjustability$self(), graphics, mouseX, mouseY);
                        ci.cancel();
                }
        }

        @Inject(method = "removed", at = @At("HEAD"))
        private void adjustability$closePanel(CallbackInfo ci)
        {
                AdjustClient.onScreenRemoved(this.adjustability$self());
        }
}
