package io.github.safetimamovic.adjustability.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.safetimamovic.adjustability.core.TooltipLines;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dormant enchantments are listed where vanilla writes enchantments, each right under the first active enchantment it
 * conflicts with; any without one (possible with {@code allow_none}) after the last. Same on both loaders, no tooltip
 * event needed. Before 1.21.5 the start and end are in {@link ItemStackTooltipMixin}, since this method didn't get the
 * item's components yet.
 */
@Mixin(ItemEnchantments.class)
abstract class ItemEnchantmentsMixin
{
        //? if >=1.21.5 {
        @Inject(method = "addToTooltip", at = @At("HEAD"))
        private void adjustability$planDormant(Item.TooltipContext context, Consumer<Component> consumer,
                TooltipFlag flag, DataComponentGetter components, CallbackInfo ci)
        {
                // Only for the active enchantments; stored ones on a book use this method too.
                TooltipLines.begin(context, components, components.get(DataComponents.ENCHANTMENTS) == (Object) this);
        }
        //?}

        @WrapOperation(method = "addToTooltip",
                at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
        private void adjustability$dormantUnder(Consumer<Component> consumer, Object line, Operation<Void> original,
                @Local Holder<Enchantment> enchantment)
        {
                original.call(consumer, line);
                TooltipLines.addDormantUnder(consumer, enchantment);
        }

        //? if >=1.21.5 {
        @Inject(method = "addToTooltip", at = @At("TAIL"))
        private void adjustability$addDormant(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag,
                DataComponentGetter components, CallbackInfo ci)
        {
                if (components.get(DataComponents.ENCHANTMENTS) == (Object) this)
                {
                        TooltipLines.addDormant(context, consumer, flag, components);
                }
        }
        //?}
}
