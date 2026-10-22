package io.github.safetimamovic.adjustability.config;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.AnvilConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.GrindstoneConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * The creator file, {@code config/adjustability-server.toml}: gameplay rules for pack devs and server admins. Synced to
 * clients. A copy in a world's override folder wins for that world: {@code syncedconfig/} on NeoForge,
 * {@code serverconfig/} on Fabric (Forge Config API Port).
 *
 * <p>NeoForge's config API; on Fabric, Forge Config API Port provides the same classes.
 */
public final class CreatorConfig
{
        public static final String FILE_NAME = "adjustability-server.toml";

        public static final ModConfigSpec SPEC;

        private static final BooleanValue ANVIL_ENABLED;
        private static final BooleanValue COMBINE_BOOKS;
        private static final IntValue DORMANT_COST_PER_LEVEL;
        private static final BooleanValue ALLOW_TOO_EXPENSIVE;
        private static final IntValue MAX_DORMANT_PER_ITEM;
        private static final IntValue MAX_CONFLICTS_PER_COMPONENT;

        private static final BooleanValue REMOVE_DORMANT;

        static
        {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

                builder.comment("Combining conflicting enchantments in an anvil. Which enchantments conflict comes from",
                        "their exclusive_set; enchantments in #adjustability:not_combinable keep vanilla behavior.")
                        .push("anvil");
                ANVIL_ENABLED = builder.comment("Conflicting enchantments can be combined in an anvil: what was active",
                                "stays active, the new conflicting ones become dormant. Off: vanilla behavior.")
                        .define("enabled", true);
                COMBINE_BOOKS = builder.comment("Two enchanted books with conflicting enchantments make one book that",
                                "holds them all. Putting that book on an item splits them into active and dormant.")
                        .define("combine_books", true);
                DORMANT_COST_PER_LEVEL = builder.comment("Extra anvil levels per level of each enchantment that becomes",
                                "dormant, on top of vanilla's price.")
                        .defineInRange("dormant_cost_per_level", 1, 0, 100);
                ALLOW_TOO_EXPENSIVE = builder.comment("The extra cost may take the price past vanilla's survival limit",
                                "of 39 levels.")
                        .define("allow_too_expensive", false);
                MAX_DORMANT_PER_ITEM = builder.comment("Most dormant enchantments one item can hold. 0: no limit.")
                        .defineInRange("max_dormant_per_item", 4, 0, 255);
                MAX_CONFLICTS_PER_COMPONENT = builder.comment("Most enchantments in one group of conflicting enchantments",
                                "on an item (2 = at most two Protections on one armor piece). 0: no limit.")
                        .defineInRange("max_conflicts_per_component", 0, 0, 255);
                builder.pop();

                builder.comment("Grindstones.").push("grindstone");
                REMOVE_DORMANT = builder.comment("A grindstone removes dormant enchantments too. Off: they stay on the",
                                "item while the active ones are removed, and become active again when nothing",
                                "conflicts with them any more.")
                        .define("remove_dormant", true);
                builder.pop();

                SPEC = builder.build();
        }

        private CreatorConfig()
        {
        }

        static AnvilConfig readAnvil()
        {
                return new AnvilConfig(
                        get(ANVIL_ENABLED),
                        get(COMBINE_BOOKS),
                        get(DORMANT_COST_PER_LEVEL),
                        get(ALLOW_TOO_EXPENSIVE),
                        get(MAX_DORMANT_PER_ITEM),
                        get(MAX_CONFLICTS_PER_COMPONENT));
        }

        static GrindstoneConfig readGrindstone()
        {
                return new GrindstoneConfig(get(REMOVE_DORMANT));
        }

        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
