package io.github.safetimamovic.adjustability.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.safetimamovic.adjustability.core.GrindstoneRules;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Both grindstone inputs accept dormant-only items when the server allows removing their enchantments. */
@Mixin(targets = {"net.minecraft.world.inventory.GrindstoneMenu$2", "net.minecraft.world.inventory.GrindstoneMenu$3"})
abstract class GrindstoneInputSlotMixin
{
        @WrapOperation(method = "mayPlace", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;"
                        + "hasAnyEnchantments(Lnet/minecraft/world/item/ItemStack;)Z"))
        private boolean adjustability$acceptDormant(ItemStack item, Operation<Boolean> original)
        {
                return original.call(item) || GrindstoneRules.canRemoveDormant(item);
        }
}
