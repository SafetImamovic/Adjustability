package io.github.safetimamovic.adjustability.mixin;

import io.github.safetimamovic.adjustability.core.TooltipLines;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if <1.21.5 {
/*import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/**
 * Two things, both around the tooltip of an item with dormant enchantments.
 *
 * <p>Around the whole tooltip ({@code getTooltipLines}), on every version: if the enchantment lines came from a mod
 * that replaced vanilla's and cancelled them (Apothic Enchanting does), the hooks in {@link ItemEnchantmentsMixin}
 * never ran, and the dormant lines are added to the finished list instead. See {@link TooltipLines#tooltipFinished}.
 *
 * <p>Before 1.21.5, {@code ItemEnchantments.addToTooltip} didn't get the item, so the start and end of
 * {@link ItemEnchantmentsMixin}'s dormant lines happen here too, around the item's own enchantments (not a book's
 * stored ones).
 */
@Mixin(ItemStack.class)
abstract class ItemStackTooltipMixin
{
        @Inject(method = "getTooltipLines", at = @At("HEAD"))
        private void adjustability$tooltipStarted(Item.TooltipContext context, @Nullable Player player, TooltipFlag flag,
                CallbackInfoReturnable<List<Component>> cir)
        {
                TooltipLines.tooltipStarted();
        }

        @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
        private void adjustability$tooltipFinished(Item.TooltipContext context, @Nullable Player player,
                TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir)
        {
                List<Component> lines = cir.getReturnValue();
                List<Component> fixed = TooltipLines.tooltipFinished(context, (ItemStack) (Object) this, flag, lines);
                if (fixed != lines)
                {
                        cir.setReturnValue(fixed);
                }
        }

        //? if <1.21.5 {
        /*@Inject(method = "addToTooltip", at = @At("HEAD"))
        private void adjustability$planDormant(DataComponentType<?> component, Item.TooltipContext context,
                Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci)
        {
                if (component == DataComponents.ENCHANTMENTS)
                {
                        TooltipLines.begin(context, (ItemStack) (Object) this, true);
                }
        }

        @Inject(method = "addToTooltip", at = @At("TAIL"))
        private void adjustability$addDormant(DataComponentType<?> component, Item.TooltipContext context,
                Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci)
        {
                if (component == DataComponents.ENCHANTMENTS)
                {
                        TooltipLines.addDormant(context, consumer, flag, (ItemStack) (Object) this);
                }
        }
        *///?}
}
