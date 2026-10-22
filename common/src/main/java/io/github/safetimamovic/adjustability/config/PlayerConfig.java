package io.github.safetimamovic.adjustability.config;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelActivation;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelSide;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PlayerSettings;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * The player file, {@code config/adjustability-client.toml}: personal preferences, editable in game (mod list on
 * NeoForge, Mod Menu on Fabric, {@code /adjustability config}), and the Adjust panel's look also with
 * {@code /adjustability panel}. Values are read every time, so changes apply right away. Not synced.
 */
public final class PlayerConfig
{
        public static final String FILE_NAME = "adjustability-client.toml";

        public static final ModConfigSpec SPEC;

        private static final BooleanValue SHOW_DORMANT;
        private static final BooleanValue SHOW_HINT;

        private static final EnumValue<PanelActivation> PANEL_ACTIVATION;
        private static final EnumValue<PanelSide> PANEL_SIDE;
        private static final ConfigValue<String> BACKGROUND_COLOR;
        private static final ConfigValue<String> BORDER_TOP_COLOR;
        private static final ConfigValue<String> BORDER_BOTTOM_COLOR;
        private static final BooleanValue SHOW_BRACKETS;
        private static final ConfigValue<String> BRACKET_COLOR;
        private static final IntValue BRACKET_LENGTH;
        private static final ConfigValue<String> HIGHLIGHT_COLOR;
        private static final ConfigValue<String> ACTIVE_COLOR;
        private static final BooleanValue SHOW_COST;

        /** The Adjust panel's settings, in file order, for {@code /adjustability panel}. */
        public static final List<ConfigValue<?>> PANEL_SETTINGS;

        static
        {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

                builder.comment("Item tooltips.").push("tooltip");
                SHOW_DORMANT = builder.comment("List dormant enchantments in the tooltip, after the active ones.")
                        .define("show_dormant", true);
                SHOW_HINT = builder.comment("On items with dormant enchantments, show which key opens the Adjust panel.")
                        .define("show_hint", true);
                builder.pop();

                builder.comment("The Adjust panel, pinned next to an item with the Adjust key. Also with",
                        "/adjustability panel. Colors are #RRGGBB or #AARRGGBB (AA = opacity, FF = solid).")
                        .push("panel");
                PANEL_ACTIVATION = builder.comment("How the Adjust key works. TOGGLE: press it to pin the panel,",
                                "press it again (or Escape, or click outside) to close it. HOLD: the panel is open",
                                "while you hold the key, and closes when you let go.")
                        .defineEnum("activation", PanelActivation.TOGGLE);
                PANEL_SIDE = builder.comment("Where the panel opens: AUTO (right of the item, left if there's no",
                                "room), RIGHT or LEFT. It always stays inside the screen.")
                        .defineEnum("side", PanelSide.AUTO);
                BACKGROUND_COLOR = color(builder, "background_color", "#F0080008", "The panel's background.");
                BORDER_TOP_COLOR = color(builder, "border_top_color", "#C0FF00FF",
                        "The border at the top; it fades into border_bottom_color down the sides.");
                BORDER_BOTTOM_COLOR = color(builder, "border_bottom_color", "#C0A00090", "The border at the bottom.");
                SHOW_BRACKETS = builder.comment("Draw the bracket decorations at the panel's corners.")
                        .define("show_brackets", true);
                BRACKET_COLOR = color(builder, "bracket_color", "#FFC71FC0", "The corner brackets.");
                BRACKET_LENGTH = builder.comment("Length of each bracket arm, in GUI pixels.")
                        .defineInRange("bracket_length", 9, 2, 32);
                HIGHLIGHT_COLOR = color(builder, "highlight_color", "#50FF00FF",
                        "Behind the row under the mouse.");
                ACTIVE_COLOR = color(builder, "active_color", "#FFFF55FF", "The mark in front of active enchantments.");
                SHOW_COST = builder.comment("Show what a switch costs and the cooldown at the bottom.")
                        .define("show_cost", true);
                builder.pop();

                SPEC = builder.build();
                PANEL_SETTINGS = List.of(PANEL_ACTIVATION, PANEL_SIDE, BACKGROUND_COLOR, BORDER_TOP_COLOR, BORDER_BOTTOM_COLOR,
                        SHOW_BRACKETS, BRACKET_COLOR, BRACKET_LENGTH, HIGHLIGHT_COLOR, ACTIVE_COLOR, SHOW_COST);
        }

        private PlayerConfig()
        {
        }

        static PlayerSettings read()
        {
                return new PlayerSettings(get(SHOW_DORMANT), get(SHOW_HINT), new PanelStyle(
                        get(PANEL_ACTIVATION),
                        get(PANEL_SIDE),
                        argb(BACKGROUND_COLOR),
                        argb(BORDER_TOP_COLOR),
                        argb(BORDER_BOTTOM_COLOR),
                        get(SHOW_BRACKETS),
                        argb(BRACKET_COLOR),
                        get(BRACKET_LENGTH),
                        argb(HIGHLIGHT_COLOR),
                        argb(ACTIVE_COLOR),
                        get(SHOW_COST)));
        }

        /** A setting's key in the file, like {@code bracket_color}. */
        public static String name(ConfigValue<?> value)
        {
                return value.getPath().getLast();
        }

        /** Whether the file is loaded, so settings can be changed (never on a dedicated server). */
        public static boolean canChange()
        {
                return SPEC.isLoaded();
        }

        /**
         * Changes a setting and saves the file.
         *
         * @return false if the value isn't valid for it (out of range, not a color) or the file isn't loaded
         */
        public static <T> boolean change(ConfigValue<T> value, T newValue)
        {
                if (!SPEC.isLoaded() || !value.getSpec().test(newValue))
                {
                        return false;
                }
                value.set(newValue);
                SPEC.save();
                return true;
        }

        /** Puts every panel setting back to its default and saves the file. */
        public static void resetPanel()
        {
                if (!SPEC.isLoaded())
                {
                        return;
                }
                for (ConfigValue<?> value : PANEL_SETTINGS)
                {
                        reset(value);
                }
                SPEC.save();
        }

        public static <T> T current(ConfigValue<T> value)
        {
                return get(value);
        }

        /** {@code #RRGGBB} or {@code #AARRGGBB} (the {@code #} is optional) as ARGB; empty if it isn't one. */
        public static Optional<Integer> parseColor(String text)
        {
                String hex = text.trim();
                if (hex.startsWith("#"))
                {
                        hex = hex.substring(1);
                }
                if (hex.length() != 6 && hex.length() != 8 || !hex.matches("[0-9a-fA-F]+"))
                {
                        return Optional.empty();
                }
                int value = (int) Long.parseLong(hex, 16);
                return Optional.of(hex.length() == 6 ? 0xFF000000 | value : value);
        }

        /** The way colors are written to the file: {@code #AARRGGBB}, upper case. */
        public static String formatColor(String text)
        {
                return parseColor(text).map(argb -> String.format(Locale.ROOT, "#%08X", argb)).orElse(text);
        }

        private static <T> void reset(ConfigValue<T> value)
        {
                value.set(value.getDefault());
        }

        private static ConfigValue<String> color(ModConfigSpec.Builder builder, String name, String defaultValue,
                String comment)
        {
                return builder.comment(comment).define(name, defaultValue,
                        value -> value instanceof String text && parseColor(text).isPresent());
        }

        private static int argb(ConfigValue<String> value)
        {
                return parseColor(get(value)).orElseGet(() -> parseColor(value.getDefault()).orElseThrow());
        }

        /** Not loaded on a dedicated server, which has no client config; the defaults apply there. */
        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
