package io.github.safetimamovic.adjustability.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.safetimamovic.adjustability.core.GrindstoneRules;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dormant-only items pass the result check; {@code removeNonCursesFrom} handles their removal. */
@Mixin(GrindstoneMenu.class)
abstract class GrindstoneMenuMixin
{
        /** For registry access; the menu doesn't keep its player. */
        @Unique
        private Player adjustability$player;

        @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
                at = @At("TAIL"))
        private void adjustability$rememberPlayer(int containerId, Inventory inventory, ContainerLevelAccess access,
                CallbackInfo ci)
        {
                this.adjustability$player = inventory.player;
        }

        @WrapOperation(method = "computeResult", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;"
                        + "hasAnyEnchantments(Lnet/minecraft/world/item/ItemStack;)Z"))
        private boolean adjustability$acceptDormant(ItemStack item, Operation<Boolean> original)
        {
                return original.call(item) || GrindstoneRules.canRemoveDormant(item);
        }

        @ModifyReturnValue(method = "removeNonCursesFrom", at = @At("RETURN"))
        private ItemStack adjustability$handleDormant(ItemStack item)
        {
                GrindstoneRules.afterRemovingEnchantments(item, this.adjustability$player.level().registryAccess());
                return item;
        }
}
