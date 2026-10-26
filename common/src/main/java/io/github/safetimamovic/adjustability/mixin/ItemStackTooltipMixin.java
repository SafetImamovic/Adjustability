package io.github.safetimamovic.adjustability.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
//? if <1.21.5 {
/*import io.github.safetimamovic.adjustability.core.TooltipLines;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/**
 * Before 1.21.5, {@code ItemEnchantments.addToTooltip} didn't get the item, so the start and end of
 * {@link ItemEnchantmentsMixin}'s dormant lines happen here, around the item's own enchantments (not a book's stored
 * ones). Empty from 1.21.5 on.
 */
@Mixin(ItemStack.class)
abstract class ItemStackTooltipMixin
{
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
