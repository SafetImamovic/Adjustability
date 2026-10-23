package io.github.safetimamovic.adjustability.mixin.client;

import io.github.safetimamovic.adjustability.client.AdjustClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=26.1 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

/**
 * The creative inventory handles its tabs, scroll bar and search box before the container screen gets a key or click,
 * and the hotbar's Adjust panel sits over the tabs. So the panel gets them first here, the same way
 * {@link AbstractContainerScreenMixin} does. Whatever the panel doesn't take goes on as before, and reaching
 * {@link AbstractContainerScreenMixin} again changes nothing: {@link AdjustClient} only acts on what it takes.
 */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin
{
        @Unique
        private AbstractContainerScreen<?> adjustability$self()
        {
                return (AbstractContainerScreen<?>) (Object) this;
        }

        @Unique
        private AbstractContainerScreenAccessor adjustability$screen()
        {
                return (AbstractContainerScreenAccessor) this;
        }

        //? if >=26.1 {
        @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
        private void adjustability$adjustKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir)
        {
                AbstractContainerScreenAccessor screen = this.adjustability$screen();
                if (AdjustClient.onKey(this.adjustability$self(), screen.adjustability$hoveredSlot(),
                        screen.adjustability$leftPos(), screen.adjustability$topPos(), event))
                {
                        cir.setReturnValue(true);
                }
        }

        @Inject(method = "keyReleased", at = @At("HEAD"), cancellable = true)
        private void adjustability$adjustKeyReleased(KeyEvent event, CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.onKeyReleased(this.adjustability$self(), event))
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
        //?} else {
        /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
        private void adjustability$adjustKey(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
        {
                AbstractContainerScreenAccessor screen = this.adjustability$screen();
                if (AdjustClient.onKey(this.adjustability$self(), screen.adjustability$hoveredSlot(),
                        screen.adjustability$leftPos(), screen.adjustability$topPos(), key, scanCode))
                {
                        cir.setReturnValue(true);
                }
        }

        @Inject(method = "keyReleased", at = @At("HEAD"), cancellable = true)
        private void adjustability$adjustKeyReleased(int key, int scanCode, int modifiers,
                CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.onKeyReleased(this.adjustability$self(), key, scanCode))
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
                if (AdjustClient.onMouseReleased())
                {
                        cir.setReturnValue(true);
                }
        }
        *///?}

        @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
        private void adjustability$noScrollUnderPanel(double x, double y, double scrollX, double scrollY,
                CallbackInfoReturnable<Boolean> cir)
        {
                if (AdjustClient.isPanelOpen(this.adjustability$self()))
                {
                        cir.setReturnValue(true);
                }
        }
}
