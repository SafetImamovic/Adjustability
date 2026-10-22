package io.github.safetimamovic.adjustability.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.safetimamovic.adjustability.core.GrindstoneRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The grindstone's result slot, an anonymous class (the fourth in {@code GrindstoneMenu}, the same on both loaders),
 * adds up the experience for each input's enchantments. Dormant ones count too, like active ones, since the
 * grindstone removes them as well. NeoForge still calls these methods when no {@code GrindstoneEvent} sets the amount.
 *
 * <p>Reading dormant enchantments needs the registries, which only {@code getExperienceAmount(Level)} has, so its level
 * is kept for the per-item calls it makes (always on the server thread, one at a time).
 */
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
abstract class GrindstoneResultSlotMixin
{
        @Unique
        private @Nullable Level adjustability$level;

        @Inject(method = "getExperienceAmount", at = @At("HEAD"))
        private void adjustability$rememberLevel(Level level, CallbackInfoReturnable<Integer> cir)
        {
                this.adjustability$level = level;
        }

        @Inject(method = "getExperienceAmount", at = @At("RETURN"))
        private void adjustability$forgetLevel(Level level, CallbackInfoReturnable<Integer> cir)
        {
                this.adjustability$level = null;
        }

        @ModifyReturnValue(method = "getExperienceFromItem", at = @At("RETURN"))
        private int adjustability$countDormant(int amount, ItemStack item)
        {
                Level level = this.adjustability$level;
                return level == null ? amount
                        : amount + GrindstoneRules.dormantExperience(item, level.registryAccess());
        }
}
