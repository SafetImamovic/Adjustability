package io.github.safetimamovic.adjustability.mixin;

import io.github.safetimamovic.adjustability.core.TooltipLines;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dormant enchantments are listed right after the item's active ones, where vanilla writes enchantments, instead of at
 * the end of the tooltip. Same on both loaders, no tooltip event needed.
 */
@Mixin(ItemEnchantments.class)
abstract class ItemEnchantmentsMixin
{
        @Inject(method = "addToTooltip", at = @At("TAIL"))
        private void adjustability$addDormant(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag,
                DataComponentGetter components, CallbackInfo ci)
        {
                // Only for the active enchantments; stored ones on a book use this method too.
                if (components.get(DataComponents.ENCHANTMENTS) == (Object) this)
                {
                        TooltipLines.addDormant(context, consumer, components);
                }
        }
}
