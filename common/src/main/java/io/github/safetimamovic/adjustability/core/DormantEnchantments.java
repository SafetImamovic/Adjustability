package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.Constants;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
//? if <1.21.5
/*import net.minecraft.nbt.Tag;*/
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
 * the data reads without registry access.
 *
 * <p>An id that isn't a registered enchantment (a datapack or mod that added it was removed) is left out of
 * {@link #get} but kept on the item by {@link #set}, at the same level, so it works again if the enchantment comes
 * back. Only {@link #clear} removes it.
 *
 * <p>Dormant enchantments the player switched off (ones that conflict with nothing, so nothing else keeps them
 * dormant) are also listed by id under {@code adjustability:off}:
 * {@code {"adjustability:off": ["minecraft:knockback"]}}.
 * Their levels stay in {@code adjustability:dormant}; an id listed there but not dormant is dropped on write. Older
 * versions ignore the list and turn such an enchantment back on at their next repair, losing nothing.
 */
public final class DormantEnchantments
{
        private static final String KEY = Constants.MOD_ID + ":dormant";
        private static final String OFF_KEY = Constants.MOD_ID + ":off";

        /** Ids already logged as not registered, so each is logged once a session, not on every read. */
        private static final Set<String> LOGGED_UNKNOWN = ConcurrentHashMap.newKeySet();

        private DormantEnchantments()
        {
        }

        /**
         * Whether the item has any dormant enchantments. Cheap; true means {@link #get} is worth calling. Also true
         * for an item whose only entries are enchantments that aren't registered.
         */
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
                CompoundTag dormant = entries(components);
                if (dormant.isEmpty())
                {
                        return ItemEnchantments.EMPTY;
                }
                HolderLookup.RegistryLookup<Enchantment> enchantments =
                        registries.lookupOrThrow(Registries.ENCHANTMENT);
                ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                //? if >=1.21.5 {
                for (String id : dormant.keySet())
                //?} else
                /*for (String id : dormant.getAllKeys())*/
                {
                        int level = level(dormant, id);
                        Optional<Holder.Reference<Enchantment>> enchantment = resolve(enchantments, id);
                        if (enchantment.isPresent() && level > 0)
                        {
                                result.set(enchantment.get(), level);
                        }
                }
                return result.toImmutable();
        }

        /**
         * The item's dormant entries whose id parses but isn't a registered enchantment, id to level, in id order.
         * They're kept on the item and ignored everywhere else.
         */
        public static Map<String, Integer> unknown(DataComponentGetter components, HolderLookup.Provider registries)
        {
                CompoundTag dormant = entries(components);
                Map<String, Integer> result = new TreeMap<>();
                if (dormant.isEmpty())
                {
                        return result;
                }
                HolderLookup.RegistryLookup<Enchantment> enchantments =
                        registries.lookupOrThrow(Registries.ENCHANTMENT);
                //? if >=1.21.5 {
                for (String id : dormant.keySet())
                //?} else
                /*for (String id : dormant.getAllKeys())*/
                {
                        int level = level(dormant, id);
                        if (level > 0 && Identifier.tryParse(id) != null && resolve(enchantments, id).isEmpty())
                        {
                                result.put(id, level);
                        }
                }
                return result;
        }

        /**
         * The item's dormant enchantments that the player switched off, as opposed to ones that wait behind an active
         * enchantment they conflict with. Only registered ones that are dormant.
         */
        public static Set<Holder<Enchantment>> off(DataComponentGetter components, HolderLookup.Provider registries)
        {
                Set<Holder<Enchantment>> result = new LinkedHashSet<>();
                ItemEnchantments dormant = get(components, registries);
                HolderLookup.RegistryLookup<Enchantment> enchantments =
                        registries.lookupOrThrow(Registries.ENCHANTMENT);
                for (String id : offIds(components))
                {
                        resolve(enchantments, id).filter(enchantment -> dormant.getLevel(enchantment) > 0)
                                .ifPresent(result::add);
                }
                return result;
        }

        /**
         * Replaces the item's dormant enchantments, keeping its entries for enchantments that aren't registered (see
         * {@link #unknown}) and the switched-off flags of the ones that stay dormant. Nothing left removes the entry,
         * and {@code custom_data} if it's empty.
         */
        public static void set(ItemStack stack, ItemEnchantments dormant, HolderLookup.Provider registries)
        {
                write(stack, dormant, unknown(stack, registries), offIds(stack));
        }

        /**
         * {@link #set(ItemStack, ItemEnchantments, HolderLookup.Provider)}, with {@code off} as the switched-off ones
         * (only those in {@code dormant} count). Flags on entries that aren't registered stay.
         */
        public static void set(ItemStack stack, ItemEnchantments dormant, Set<Holder<Enchantment>> off,
                HolderLookup.Provider registries)
        {
                Map<String, Integer> unknown = unknown(stack, registries);
                Set<String> ids = new TreeSet<>();
                offIds(stack).stream().filter(unknown::containsKey).forEach(ids::add);
                off.forEach(enchantment -> enchantment.unwrapKey()
                        .ifPresent(key -> ids.add(key.identifier().toString())));
                write(stack, dormant, unknown, ids);
        }

        /**
         * Adds entries for enchantments that aren't registered, such as the ones another item brought to an anvil. An
         * id the item already holds keeps the higher level: with the enchantment gone, its maximum isn't known.
         */
        public static void addUnknown(ItemStack stack, Map<String, Integer> extra, HolderLookup.Provider registries)
        {
                if (extra.isEmpty())
                {
                        return;
                }
                Map<String, Integer> unknown = unknown(stack, registries);
                extra.forEach((id, level) -> unknown.merge(id, level, Math::max));
                write(stack, get(stack, registries), unknown, offIds(stack));
        }

        /** Removes all dormant enchantments, registered or not, and the switched-off flags. */
        public static void clear(ItemStack stack)
        {
                if (has(stack) || !offIds(stack).isEmpty())
                {
                        write(stack, ItemEnchantments.EMPTY, Map.of(), Set.of());
                }
        }

        private static void write(ItemStack stack, ItemEnchantments dormant, Map<String, Integer> unknown,
                Set<String> off)
        {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag ->
                {
                        CompoundTag entries = new CompoundTag();
                        unknown.forEach(entries::putInt);
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
                        ListTag flags = new ListTag();
                        new TreeSet<>(off).stream().filter(entries::contains).map(StringTag::valueOf)
                                .forEach(flags::add);
                        if (flags.isEmpty())
                        {
                                tag.remove(OFF_KEY);
                        }
                        else
                        {
                                tag.put(OFF_KEY, flags);
                        }
                });
        }

        /** The ids listed as switched off, as stored. */
        private static Set<String> offIds(DataComponentGetter components)
        {
                CustomData data = components.get(DataComponents.CUSTOM_DATA);
                Set<String> result = new TreeSet<>();
                if (data == null || data.isEmpty())
                {
                        return result;
                }
                //? if >=1.21.5 {
                ListTag list = data.copyTag().getListOrEmpty(OFF_KEY);
                for (int i = 0; i < list.size(); i++)
                {
                        list.getString(i).ifPresent(result::add);
                }
                //?} else {
                /*ListTag list = data.copyTag().getList(OFF_KEY, Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++)
                {
                        result.add(list.getString(i));
                }
                *///?}
                result.remove("");
                return result;
        }

        private static CompoundTag entries(DataComponentGetter components)
        {
                CustomData data = components.get(DataComponents.CUSTOM_DATA);
                if (data == null || data.isEmpty())
                {
                        return new CompoundTag();
                }
                //? if >=1.21.5 {
                return data.copyTag().getCompoundOrEmpty(KEY);
                //?} else
                /*return data.copyTag().getCompound(KEY);*/
        }

        private static int level(CompoundTag dormant, String id)
        {
                //? if >=1.21.5 {
                return dormant.getIntOr(id, 0);
                //?} else
                /*return dormant.getInt(id);*/
        }

        /** The enchantment {@code id} names, or empty (logged once per id) if it isn't registered. */
        private static Optional<Holder.Reference<Enchantment>> resolve(
                HolderLookup.RegistryLookup<Enchantment> enchantments, String id)
        {
                Optional<Holder.Reference<Enchantment>> enchantment = Optional.ofNullable(Identifier.tryParse(id))
                        .flatMap(parsed -> enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, parsed)));
                if (enchantment.isEmpty() && LOGGED_UNKNOWN.add(id))
                {
                        Constants.LOG.info("An item holds dormant enchantment {}, which isn't registered; keeping it "
                                + "on the item in case it comes back", id);
                }
                return enchantment;
        }
}
