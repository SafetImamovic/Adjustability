package io.github.safetimamovic.adjustability.tags;

import io.github.safetimamovic.adjustability.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

/** Adjustability's own tags. Code only checks these; vanilla or {@code c:} tags are default contents of them. */
public final class ModTags
{
        /** Always active, never dormant. Curses by default, so they can't be switched off. */
        public static final TagKey<Enchantment> NEVER_DORMANT = TagKey.create(Registries.ENCHANTMENT,
                Constants.id("never_dormant"));

        /** Conflicts involving these keep vanilla behavior: the anvil skips them. */
        public static final TagKey<Enchantment> NOT_COMBINABLE = TagKey.create(Registries.ENCHANTMENT,
                Constants.id("not_combinable"));

        /** With {@code [items] mode = ALLOWLIST}: the only items that can hold dormant enchantments. */
        public static final TagKey<Item> ADJUSTABLE = TagKey.create(Registries.ITEM, Constants.id("adjustable"));

        /** Items that never hold dormant enchantments, in either mode. */
        public static final TagKey<Item> NOT_ADJUSTABLE = TagKey.create(Registries.ITEM, Constants.id("not_adjustable"));

        private ModTags()
        {
        }
}
