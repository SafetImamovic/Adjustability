package io.github.safetimamovic.adjustability.mixin.client;

import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.SlotMarkers;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=26.1 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;
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
abstract class AbstractContainerScreenMixin extends Screen
{
        @Unique
        private final SlotMarkers adjustability$markers = new SlotMarkers();

        @Shadow
        protected @Nullable Slot hoveredSlot;

        @Shadow
        protected int leftPos;

        @Shadow
        protected int topPos;

        private AbstractContainerScreenMixin(Component title)
        {
                super(title);
        }

        /**
         * The container screen doesn't handle key releases itself, so this adds it: HOLD mode closes the panel when
         * the Adjust key is let go. Everything else goes on to the screen as before.
         */
        //? if >=26.1 {
        @Override
        public boolean keyReleased(KeyEvent event)
        {
                return AdjustClient.onKeyReleased(this.adjustability$self(), event) || super.keyReleased(event);
        }
        //?} else {
        /*@Override
        public boolean keyReleased(int key, int scanCode, int modifiers)
        {
                return AdjustClient.onKeyReleased(this.adjustability$self(), key, scanCode)
                        || super.keyReleased(key, scanCode, modifiers);
        }
        *///?}

        @Unique
        private AbstractContainerScreen<?> adjustability$self()
        {
                return (AbstractContainerScreen<?>) (Object) this;
        }

        //? if >=26.1 {
        @Inject(method = "extractSlot", at = @At("TAIL"))
        private void adjustability$slotMarker(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
                CallbackInfo ci)
        //?} else {
        /*@Inject(method = "renderSlot", at = @At("TAIL"))
        private void adjustability$slotMarker(GuiGraphicsExtractor graphics, Slot slot, CallbackInfo ci)
        *///?}
        {
                this.adjustability$markers.draw(graphics, slot);
        }

        //? if >=26.1 {
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
        //?} else {
        /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
        private void adjustability$adjustKey(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.onKey(this.adjustability$self(), this.hoveredSlot, this.leftPos, this.topPos, key,
                        scanCode))
                {
                        cir.setReturnValue(true);
                }
        }

        @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
        private void adjustability$panelClick(double x, double y, int button, CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.onMouseClicked(this.adjustability$self(), x, y, button))
                {
                        cir.setReturnValue(true);
                }
        }

        @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
        private void adjustability$panelRelease(double x, double y, int button, CallbackInfoReturnable<Boolean> cir)
        {
        *///?}
                if (AdjustClient.onMouseReleased())
                {
                        cir.setReturnValue(true);
                }
        }

        //? if >=26.1 {
        @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
        private void adjustability$noScrollUnderPanel(double x, double y, double scrollX, double scrollY,
                CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.isPanelOpen(this.adjustability$self()))
                {
                        cir.setReturnValue(true);
                }
        }
        //?} else {
        /*// The container screen doesn't override mouseScrolled here, so this adds it, like keyReleased.
        @Override
        public boolean mouseScrolled(double x, double y, double scrollX, double scrollY)
        {
                return AdjustClient.isPanelOpen(this.adjustability$self()) || super.mouseScrolled(x, y, scrollX, scrollY);
        }
        *///?}

        /**
         * Every container screen draws its item tooltip last, including the recipe book screens that skip this class's
         * extractRenderState. So the panel is drawn there, in place of the tooltip.
         */
        //? if >=26.1 {
        @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
        //?} else
        /*@Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)*/
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
