package io.github.safetimamovic.adjustability.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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

/** Every grindstone result goes through {@code removeNonCursesFrom}; dormant enchantments are handled there too. */
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

        @ModifyReturnValue(method = "removeNonCursesFrom", at = @At("RETURN"))
        private ItemStack adjustability$handleDormant(ItemStack item)
        {
                GrindstoneRules.afterRemovingEnchantments(item, this.adjustability$player.level().registryAccess());
                return item;
        }
}
