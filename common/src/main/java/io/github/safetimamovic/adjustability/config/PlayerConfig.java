package io.github.safetimamovic.adjustability.config;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PlayerSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * The player file, {@code config/adjustability-client.toml}: personal preferences, editable in game (mod list on
 * NeoForge, Mod Menu on Fabric, or {@code /adjustability config}). Not synced.
 */
public final class PlayerConfig
{
        public static final String FILE_NAME = "adjustability-client.toml";

        public static final ModConfigSpec SPEC;

        private static final BooleanValue SHOW_DORMANT;
        private static final BooleanValue SHOW_HINT;

        static
        {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

                builder.comment("Item tooltips.").push("tooltip");
                SHOW_DORMANT = builder.comment("List dormant enchantments in the tooltip, after the active ones.")
                        .define("show_dormant", true);
                SHOW_HINT = builder.comment("On items with dormant enchantments, show which key opens the Adjust panel.")
                        .define("show_hint", true);
                builder.pop();

                SPEC = builder.build();
        }

        private PlayerConfig()
        {
        }

        static PlayerSettings read()
        {
                return new PlayerSettings(get(SHOW_DORMANT), get(SHOW_HINT));
        }

        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
