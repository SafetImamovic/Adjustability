package io.github.safetimamovic.adjustability.config;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.BorderStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Frame;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.GroupStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Layout;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Marks;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.NonePosition;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelActivation;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelPreset;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelSide;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PlayerSettings;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.RowOrder;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Text;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import org.jspecify.annotations.Nullable;

/**
 * The player file, {@code config/adjustability-client.toml}: personal preferences, editable in game (mod list on
 * NeoForge, Mod Menu on Fabric, {@code /adjustability config}), and the Adjust panel's look also with
 * {@code /adjustability panel}. Values are read every time, so changes apply right away. Not synced.
 *
 * <p>The panel's first settings stay directly in {@code [panel]}, where 0.1.0 put them, so older files keep their
 * values; the settings added later live in the {@code [panel.*]} sections.
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
        private static final EnumValue<BorderStyle> BORDER_STYLE;

        private static final ConfigValue<String> MARK_STYLE;
        private static final ConfigValue<String> DORMANT_MARK_COLOR;
        private static final BooleanValue ANIMATE_MARKS;

        private static final ConfigValue<String> TITLE_COLOR;
        private static final ConfigValue<String> ACTIVE_TEXT_COLOR;
        private static final ConfigValue<String> DORMANT_TEXT_COLOR;
        private static final ConfigValue<String> FOOTER_TEXT_COLOR;
        private static final BooleanValue TEXT_SHADOW;
        private static final BooleanValue SHOW_TITLE;
        private static final BooleanValue SHOW_ITEM_ICON;

        private static final IntValue PADDING;
        private static final IntValue ROW_HEIGHT;
        private static final EnumValue<GroupStyle> GROUP_STYLE;
        private static final ConfigValue<String> SEPARATOR_COLOR;
        private static final EnumValue<RowOrder> ROW_ORDER;
        private static final EnumValue<NonePosition> NONE_ROW;

        /** The row mark drawn when {@code mark_style} names none (say the mod that added it was removed). */
        public static final String DEFAULT_MARK_STYLE = "adjustability:circle";

        /** Adjustability's own row marks; the client adds the ones other mods register. */
        public static final List<String> BUILT_IN_MARK_STYLES = List.of(DEFAULT_MARK_STYLE, "adjustability:square",
                "adjustability:checkbox", "adjustability:toggle", "adjustability:radio", "adjustability:text_only");

        private static Supplier<Collection<String>> markStyles = () -> BUILT_IN_MARK_STYLES;

        /** The Adjust panel's settings, in file order, for {@code /adjustability panel}. */
        public static final List<ConfigValue<?>> PANEL_SETTINGS;

        /** The settings that are colors ({@code #AARRGGBB} strings), for the YACL screen's color pickers. */
        private static final Set<ConfigValue<?>> COLORS;

        /** What each preset changes from the defaults. */
        private static final Map<PanelPreset, Map<ConfigValue<?>, Object>> PRESETS;

        static
        {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

                builder.comment("Item tooltips.").push("tooltip");
                SHOW_DORMANT = builder.comment("List dormant enchantments in the tooltip, each under the active one it conflicts with.")
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
                BORDER_STYLE = builder.comment("GRADIENT: fades from border_top_color to border_bottom_color, like a",
                                "tooltip. SOLID: border_top_color all around. NONE: no border. SPRITE: the",
                                "adjustability:panel GUI sprite, which resource packs can replace (it includes the",
                                "background, so background_color and the border colors aren't used).")
                        .defineEnum("border_style", BorderStyle.GRADIENT);

                builder.comment("The mark in front of each row; active_color above colors the active one.")
                        .push("marks");
                MARK_STYLE = builder.comment("adjustability:circle, square, checkbox, toggle (a switch), radio or",
                                "text_only (no mark, only the text color). Other mods can add their own.")
                        .define("mark_style", DEFAULT_MARK_STYLE,
                                value -> value instanceof String text && text.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
                DORMANT_MARK_COLOR = color(builder, "dormant_mark_color", "#FFAAAAAA",
                        "The mark in front of dormant enchantments.");
                ANIMATE_MARKS = builder.comment("Animate a mark when its enchantment switches (a toggle's knob",
                                "slides).")
                        .define("animate_marks", true);
                builder.pop();

                builder.comment("The item's name at the top, the rows and the cost line.").push("text");
                TITLE_COLOR = color(builder, "title_color", "#FFFFFFFF", "The item's name.");
                ACTIVE_TEXT_COLOR = color(builder, "active_text_color", "#FFFFFFFF",
                        "Active enchantments, and the row under the mouse.");
                DORMANT_TEXT_COLOR = color(builder, "dormant_text_color", "#FFAAAAAA", "Dormant enchantments.");
                FOOTER_TEXT_COLOR = color(builder, "footer_text_color", "#FF808080",
                        "The cost line, each group's cost and the group headings.");
                TEXT_SHADOW = builder.comment("Draw the title and the rows with a shadow.").define("text_shadow", true);
                SHOW_TITLE = builder.comment("Show the item's name at the top.").define("show_title", true);
                SHOW_ITEM_ICON = builder.comment("Show the item in front of its name.").define("show_item_icon", false);
                builder.pop();

                builder.comment("Spacing, and how the groups of conflicting enchantments are told apart.")
                        .push("layout");
                PADDING = builder.comment("Space between the border and the text, in GUI pixels.")
                        .defineInRange("padding", 5, 2, 16);
                ROW_HEIGHT = builder.comment("Height of each row, in GUI pixels.")
                        .defineInRange("row_height", 11, 9, 24);
                GROUP_STYLE = builder.comment("Between groups: LINE (a gap with a thin line), GAP, NONE, or HEADER",
                                "(the group's name above it, like Mining).")
                        .defineEnum("group_style", GroupStyle.LINE);
                SEPARATOR_COLOR = color(builder, "separator_color", "#30FF00FF", "The line between groups.");
                ROW_ORDER = builder.comment("Order of the rows in a group: REGISTRY (the game's), ALPHABETICAL or",
                                "ACTIVE_FIRST.")
                        .defineEnum("row_order", RowOrder.REGISTRY);
                NONE_ROW = builder.comment("Where the None row goes when the server allows switching a group off:",
                                "LAST or FIRST.")
                        .defineEnum("none_row", NonePosition.LAST);
                builder.pop();
                builder.pop();

                SPEC = builder.build();
                PANEL_SETTINGS = List.of(PANEL_ACTIVATION, PANEL_SIDE, BACKGROUND_COLOR, BORDER_TOP_COLOR, BORDER_BOTTOM_COLOR,
                        SHOW_BRACKETS, BRACKET_COLOR, BRACKET_LENGTH, HIGHLIGHT_COLOR, ACTIVE_COLOR, SHOW_COST,
                        BORDER_STYLE, MARK_STYLE, DORMANT_MARK_COLOR, ANIMATE_MARKS, TITLE_COLOR, ACTIVE_TEXT_COLOR,
                        DORMANT_TEXT_COLOR, FOOTER_TEXT_COLOR, TEXT_SHADOW, SHOW_TITLE, SHOW_ITEM_ICON, PADDING,
                        ROW_HEIGHT, GROUP_STYLE, SEPARATOR_COLOR, ROW_ORDER, NONE_ROW);
                COLORS = Set.of(BACKGROUND_COLOR, BORDER_TOP_COLOR, BORDER_BOTTOM_COLOR, BRACKET_COLOR, HIGHLIGHT_COLOR,
                        ACTIVE_COLOR, DORMANT_MARK_COLOR, TITLE_COLOR, ACTIVE_TEXT_COLOR, DORMANT_TEXT_COLOR,
                        FOOTER_TEXT_COLOR, SEPARATOR_COLOR);
                PRESETS = Map.of(
                        PanelPreset.MAGENTA, Map.of(),
                        // Vanilla's tooltip colors.
                        PanelPreset.VANILLA, Map.ofEntries(
                                Map.entry(BACKGROUND_COLOR, "#F0100010"),
                                Map.entry(BORDER_TOP_COLOR, "#505000FF"),
                                Map.entry(BORDER_BOTTOM_COLOR, "#5028007F"),
                                Map.entry(SHOW_BRACKETS, false),
                                Map.entry(HIGHLIGHT_COLOR, "#30FFFFFF"),
                                Map.entry(ACTIVE_COLOR, "#FFFFFFFF"),
                                Map.entry(SEPARATOR_COLOR, "#305000FF"),
                                Map.entry(MARK_STYLE, "adjustability:radio")),
                        PanelPreset.MINIMAL, Map.ofEntries(
                                Map.entry(BACKGROUND_COLOR, "#D0000000"),
                                Map.entry(BORDER_STYLE, BorderStyle.NONE),
                                Map.entry(SHOW_BRACKETS, false),
                                Map.entry(HIGHLIGHT_COLOR, "#30FFFFFF"),
                                Map.entry(ACTIVE_COLOR, "#FFFFFFFF"),
                                Map.entry(DORMANT_MARK_COLOR, "#FF777777"),
                                Map.entry(DORMANT_TEXT_COLOR, "#FF999999"),
                                Map.entry(MARK_STYLE, "adjustability:square"),
                                Map.entry(GROUP_STYLE, GroupStyle.GAP),
                                Map.entry(SHOW_COST, false),
                                Map.entry(TEXT_SHADOW, false),
                                Map.entry(PADDING, 4)),
                        PanelPreset.HIGH_CONTRAST, Map.ofEntries(
                                Map.entry(BACKGROUND_COLOR, "#FF000000"),
                                Map.entry(BORDER_STYLE, BorderStyle.SOLID),
                                Map.entry(BORDER_TOP_COLOR, "#FFFFFFFF"),
                                Map.entry(SHOW_BRACKETS, false),
                                Map.entry(HIGHLIGHT_COLOR, "#80FFFF00"),
                                Map.entry(ACTIVE_COLOR, "#FFFFFF00"),
                                Map.entry(DORMANT_MARK_COLOR, "#FFFFFFFF"),
                                Map.entry(TITLE_COLOR, "#FFFFFF00"),
                                Map.entry(ACTIVE_TEXT_COLOR, "#FFFFFF00"),
                                Map.entry(DORMANT_TEXT_COLOR, "#FFFFFFFF"),
                                Map.entry(FOOTER_TEXT_COLOR, "#FFFFFFFF"),
                                Map.entry(SEPARATOR_COLOR, "#FFFFFFFF"),
                                Map.entry(MARK_STYLE, "adjustability:checkbox"),
                                Map.entry(GROUP_STYLE, GroupStyle.HEADER),
                                Map.entry(ROW_HEIGHT, 13)));
        }

        private PlayerConfig()
        {
        }

        static PlayerSettings read()
        {
                return new PlayerSettings(get(SHOW_DORMANT), get(SHOW_HINT), panel(value -> null));
        }

        /**
         * The Adjust panel's look, each setting from {@code pending} if it has a value of the setting's type for it,
         * else from the file. The YACL screen's preview passes the values being edited, not yet saved.
         */
        public static PanelStyle panel(Function<ConfigValue<?>, @Nullable Object> pending)
        {
                return new PanelStyle(
                        get(PANEL_ACTIVATION, pending),
                        get(PANEL_SIDE, pending),
                        new Frame(
                                get(BORDER_STYLE, pending),
                                argb(BACKGROUND_COLOR, pending),
                                argb(BORDER_TOP_COLOR, pending),
                                argb(BORDER_BOTTOM_COLOR, pending),
                                get(SHOW_BRACKETS, pending),
                                argb(BRACKET_COLOR, pending),
                                get(BRACKET_LENGTH, pending),
                                argb(HIGHLIGHT_COLOR, pending)),
                        new Marks(
                                get(MARK_STYLE, pending),
                                argb(ACTIVE_COLOR, pending),
                                argb(DORMANT_MARK_COLOR, pending),
                                get(ANIMATE_MARKS, pending)),
                        new Text(
                                argb(TITLE_COLOR, pending),
                                argb(ACTIVE_TEXT_COLOR, pending),
                                argb(DORMANT_TEXT_COLOR, pending),
                                argb(FOOTER_TEXT_COLOR, pending),
                                get(TEXT_SHADOW, pending),
                                get(SHOW_TITLE, pending),
                                get(SHOW_ITEM_ICON, pending),
                                get(SHOW_COST, pending)),
                        new Layout(
                                get(PADDING, pending),
                                get(ROW_HEIGHT, pending),
                                get(GROUP_STYLE, pending),
                                argb(SEPARATOR_COLOR, pending),
                                get(ROW_ORDER, pending),
                                get(NONE_ROW, pending)));
        }

        /** Whether a setting is the row mark's id. */
        public static boolean isMarkStyle(ConfigValue<?> value)
        {
                return value == MARK_STYLE;
        }

        /** The ids {@code mark_style} can take: Adjustability's own and those other mods registered. */
        public static Collection<String> markStyles()
        {
                return markStyles.get();
        }

        /** Set once by the client, which knows the registered row marks. */
        public static void markStyles(Supplier<Collection<String>> registered)
        {
                markStyles = registered;
        }

        /**
         * Every look setting's value in a preset, in {@link #PANEL_SETTINGS} order: the defaults, then the preset's
         * own. The key mode and the side aren't part of a look.
         */
        public static Map<ConfigValue<?>, Object> preset(PanelPreset preset)
        {
                Map<ConfigValue<?>, Object> values = new LinkedHashMap<>();
                for (ConfigValue<?> value : PANEL_SETTINGS)
                {
                        if (value != PANEL_ACTIVATION && value != PANEL_SIDE)
                        {
                                values.put(value, PRESETS.get(preset).getOrDefault(value, value.getDefault()));
                        }
                }
                return values;
        }

        /** Applies a preset and saves the file; false if the file isn't loaded. */
        @SuppressWarnings("unchecked")
        public static boolean applyPreset(PanelPreset preset)
        {
                if (!SPEC.isLoaded())
                {
                        return false;
                }
                preset(preset).forEach((value, newValue) -> ((ConfigValue<Object>) value).set(newValue));
                SPEC.save();
                return true;
        }

        /** Whether a setting is a color, an {@code #AARRGGBB} string. */
        public static boolean isColor(ConfigValue<?> value)
        {
                return COLORS.contains(value);
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

        private static int argb(ConfigValue<String> value, Function<ConfigValue<?>, @Nullable Object> pending)
        {
                return parseColor(get(value, pending)).orElseGet(() -> parseColor(value.getDefault()).orElseThrow());
        }

        @SuppressWarnings("unchecked")
        private static <T> T get(ConfigValue<T> value, Function<ConfigValue<?>, @Nullable Object> pending)
        {
                Object override = pending.apply(value);
                return value.getDefault().getClass().isInstance(override) ? (T) override : get(value);
        }

        /** Not loaded on a dedicated server, which has no client config; the defaults apply there. */
        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
