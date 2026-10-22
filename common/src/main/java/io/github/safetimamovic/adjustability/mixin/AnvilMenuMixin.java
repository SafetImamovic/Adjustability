package io.github.safetimamovic.adjustability.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.safetimamovic.adjustability.core.AnvilMerge;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla merges enchantments in {@code createResult}; NeoForge moves that body to {@code createResultInternal} and
 * makes {@code createResult} a final wrapper that fires {@code AnvilUpdateEvent} after it. So the compatibility check is
 * wrapped in whichever holds vanilla's logic, and the split runs at the end of {@code createResult} on both loaders
 * (on NeoForge after other mods' anvil events).
 */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin extends ItemCombinerMenu
{
        @Shadow
        @Final
        private DataSlot cost;

        private AnvilMenuMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access,
                ItemCombinerMenuSlotDefinition definition)
        {
                super(menuType, containerId, inventory, access, definition);
        }

        @WrapOperation(method = {"createResult", "createResultInternal"}, at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/item/enchantment/Enchantment;areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z"))
        private boolean adjustability$letConflictsThrough(Holder<Enchantment> enchantment, Holder<Enchantment> other,
                Operation<Boolean> original)
        {
                return original.call(enchantment, other)
                        || AnvilMerge.mayCombine(this.inputSlots.getItem(0), enchantment, other);
        }

        @Inject(method = "createResult", at = @At("TAIL"))
        private void adjustability$splitResult(CallbackInfo ci)
        {
                ItemStack result = this.resultSlots.getItem(0);
                ItemStack adjusted = AnvilMerge.afterCreateResult(this.player, this.inputSlots.getItem(0),
                        this.inputSlots.getItem(1), result, this.cost);
                if (adjusted != result)
                {
                        this.resultSlots.setItem(0, adjusted);
                }
                this.broadcastChanges();
        }
}
