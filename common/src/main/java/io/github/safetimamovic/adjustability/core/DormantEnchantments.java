package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.Constants;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Reads and writes an item's dormant enchantments. The only class that knows where they are stored.
 *
 * <p>They live in vanilla's {@code minecraft:custom_data}, under {@code adjustability:dormant}, as enchantment id to
 * level: {@code {"adjustability:dormant": {"minecraft:silk_touch": 1}}}. Not in a component of our own, so an item
 * still loads if the mod is removed (it keeps its active enchantments and the entry sits unused), and plain ids, so
 * the data reads without registry access and an enchantment a datapack removed is skipped instead of failing.
 */
public final class DormantEnchantments
{
        private static final String KEY = Constants.MOD_ID + ":dormant";

        private DormantEnchantments()
        {
        }

        /** Whether the item has any dormant enchantments. Cheap; true means {@link #get} is worth calling. */
        public static boolean has(DataComponentGetter components)
        {
                CustomData data = components.get(DataComponents.CUSTOM_DATA);
                //? if >=1.21.5 {
                return data != null && !data.isEmpty() && data.copyTag().getCompoundOrEmpty(KEY).size() > 0;
                //?} else
                /*return data != null && !data.isEmpty() && data.copyTag().getCompound(KEY).size() > 0;*/
        }

        /** The item's dormant enchantments. Ids that aren't (or are no longer) enchantments are left out. */
        public static ItemEnchantments get(DataComponentGetter components, HolderLookup.Provider registries)
        {
                CustomData data = components.get(DataComponents.CUSTOM_DATA);
                if (data == null || data.isEmpty())
                {
                        return ItemEnchantments.EMPTY;
                }
                //? if >=1.21.5 {
                CompoundTag dormant = data.copyTag().getCompoundOrEmpty(KEY);
                //?} else
                /*CompoundTag dormant = data.copyTag().getCompound(KEY);*/
                if (dormant.isEmpty())
                {
                        return ItemEnchantments.EMPTY;
                }
                HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
                ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                //? if >=1.21.5 {
                for (String id : dormant.keySet())
                //?} else
                /*for (String id : dormant.getAllKeys())*/
                {
                        //? if >=1.21.5 {
                        int level = dormant.getIntOr(id, 0);
                        //?} else
                        /*int level = dormant.getInt(id);*/
                        Optional<Holder.Reference<Enchantment>> enchantment = Optional.ofNullable(Identifier.tryParse(id))
                                .flatMap(parsed -> enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, parsed)));
                        if (enchantment.isPresent() && level > 0)
                        {
                                result.set(enchantment.get(), level);
                        }
                        else
                        {
                                Constants.LOG.debug("Skipping unknown dormant enchantment {} (level {})", id, level);
                        }
                }
                return result.toImmutable();
        }

        /** Replaces the item's dormant enchantments. Empty removes the entry, and {@code custom_data} if it's empty. */
        public static void set(ItemStack stack, ItemEnchantments dormant)
        {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag ->
                {
                        CompoundTag entries = new CompoundTag();
                        for (Entry<Holder<Enchantment>> entry : dormant.entrySet())
                        {
                                entry.getKey().unwrapKey().ifPresent(
                                        key -> entries.putInt(key.identifier().toString(), entry.getIntValue()));
                        }
                        if (entries.isEmpty())
                        {
                                tag.remove(KEY);
                        }
                        else
                        {
                                tag.put(KEY, entries);
                        }
                });
        }

        /** Removes all dormant enchantments. */
        public static void clear(ItemStack stack)
        {
                if (has(stack))
                {
                        set(stack, ItemEnchantments.EMPTY);
                }
        }
}
