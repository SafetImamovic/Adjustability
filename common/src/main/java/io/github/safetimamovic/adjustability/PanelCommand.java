package io.github.safetimamovic.adjustability;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.safetimamovic.adjustability.config.PanelProfiles;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import org.jspecify.annotations.Nullable;

/**
 * The {@code /adjustability panel} tree: {@code panel} lists the Adjust panel's settings, {@code panel <setting>
 * <value>} changes one, {@code panel reset} restores the defaults (or the pack's default profile). Profiles
 * ({@link PanelProfiles}): {@code panel profile <name>} applies one, {@code panel save <name>} saves the current look
 * as one, {@code panel export} gives the look's share code and {@code panel import [code]} applies a code (from the clipboard
 * without one: codes can be longer than the chat box takes). The settings are the player file's {@code [panel]}
 * section ({@link PlayerConfig#PANEL_SETTINGS}), so the command and the config screen always offer the same ones.
 *
 * <p>Built once for the client, which runs it, and once for the server, whose copy only exists so the arguments are
 * suggested (see {@code AdjustabilityCommand}). What each node does is passed in, so nothing here touches client
 * classes.
 */
public final class PanelCommand
{
        private static final String VALUE = "value";
        private static final String NAME = "name";
        private static final String CODE = "code";

        private PanelCommand()
        {
        }

        /** What the nodes do; the client changes settings, the server only refuses. */
        public interface Actions<S>
        {
                int list(CommandContext<S> context);

                int reset(CommandContext<S> context);

                int profile(CommandContext<S> context, String name);

                int save(CommandContext<S> context, String name);

                int export(CommandContext<S> context);

                /** {@code code}: {@code null} for the one on the clipboard. */
                int importCode(CommandContext<S> context, @Nullable String code);

                <T> int set(CommandContext<S> context, ConfigValue<T> setting, T value);

                /** {@code input} isn't a value {@code setting} can take. */
                int invalid(CommandContext<S> context, ConfigValue<?> setting, String input);
        }

        public static <S> LiteralArgumentBuilder<S> tree(Actions<S> actions)
        {
                LiteralArgumentBuilder<S> panel = LiteralArgumentBuilder.<S>literal("panel").executes(actions::list)
                        .then(LiteralArgumentBuilder.<S>literal("reset").executes(actions::reset));
                panel.then(LiteralArgumentBuilder.<S>literal("profile")
                        .then(RequiredArgumentBuilder.<S, String>argument(NAME, StringArgumentType.word())
                                .suggests((context, builder) ->
                                {
                                        PanelProfiles.ids().forEach(builder::suggest);
                                        return builder.buildFuture();
                                })
                                .executes(context -> actions.profile(context,
                                        StringArgumentType.getString(context, NAME)))));
                panel.then(LiteralArgumentBuilder.<S>literal("save")
                        .then(RequiredArgumentBuilder.<S, String>argument(NAME, StringArgumentType.word())
                                .executes(context -> actions.save(context,
                                        StringArgumentType.getString(context, NAME)))));
                panel.then(LiteralArgumentBuilder.<S>literal("export").executes(actions::export));
                panel.then(LiteralArgumentBuilder.<S>literal("import")
                        .executes(context -> actions.importCode(context, null))
                        .then(RequiredArgumentBuilder.<S, String>argument(CODE, StringArgumentType.greedyString())
                                .executes(context -> actions.importCode(context,
                                        StringArgumentType.getString(context, CODE)))));
                for (ConfigValue<?> setting : PlayerConfig.PANEL_SETTINGS)
                {
                        panel.then(setting(setting, actions));
                }
                return panel;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static <S> LiteralArgumentBuilder<S> setting(ConfigValue<?> setting, Actions<S> actions)
        {
                LiteralArgumentBuilder<S> node = LiteralArgumentBuilder.literal(PlayerConfig.name(setting));
                if (setting instanceof BooleanValue bool)
                {
                        return node.then(RequiredArgumentBuilder.<S, Boolean>argument(VALUE, BoolArgumentType.bool())
                                .executes(context -> actions.set(context, bool,
                                        BoolArgumentType.getBool(context, VALUE))));
                }
                if (setting instanceof IntValue integer)
                {
                        return node.then(RequiredArgumentBuilder.<S, Integer>argument(VALUE,
                                        IntegerArgumentType.integer())
                                .executes(context -> actions.set(context, integer,
                                        IntegerArgumentType.getInteger(context, VALUE))));
                }
                if (setting instanceof EnumValue enumValue)
                {
                        Enum<?>[] constants = ((Enum<?>) enumValue.getDefault()).getDeclaringClass().getEnumConstants();
                        return node.then(RequiredArgumentBuilder.<S, String>argument(VALUE, StringArgumentType.word())
                                .suggests((context, builder) ->
                                {
                                        Arrays.stream(constants).map(Enum::name).forEach(builder::suggest);
                                        return builder.buildFuture();
                                })
                                .executes(context ->
                                {
                                        String input = StringArgumentType.getString(context, VALUE);
                                        Optional<Enum<?>> match = Arrays.stream(constants)
                                                .filter(constant -> constant.name().equalsIgnoreCase(input))
                                                .findFirst();
                                        return match.isPresent()
                                                ? actions.set(context, (ConfigValue) enumValue, match.get())
                                                : actions.invalid(context, enumValue, input);
                                }));
                }
                if (PlayerConfig.isMarkStyle(setting))
                {
                        ConfigValue<String> mark = (ConfigValue<String>) setting;
                        return node.then(RequiredArgumentBuilder.<S, String>argument(VALUE,
                                        StringArgumentType.greedyString())
                                .suggests((context, builder) ->
                                {
                                        PlayerConfig.markStyles().forEach(builder::suggest);
                                        return builder.buildFuture();
                                })
                                .executes(context ->
                                {
                                        String input = StringArgumentType.getString(context, VALUE).trim();
                                        return PlayerConfig.markStyles().contains(input)
                                                ? actions.set(context, mark, input)
                                                : actions.invalid(context, mark, input);
                                }));
                }
                // The rest are colors, kept as text in the file.
                ConfigValue<String> color = (ConfigValue<String>) setting;
                return node.then(RequiredArgumentBuilder.<S, String>argument(VALUE, StringArgumentType.greedyString())
                        .suggests((context, builder) ->
                        {
                                builder.suggest(PlayerConfig.current(color));
                                builder.suggest(color.getDefault());
                                return builder.buildFuture();
                        })
                        .executes(context ->
                        {
                                String input = StringArgumentType.getString(context, VALUE);
                                return PlayerConfig.parseColor(input).isPresent()
                                        ? actions.set(context, color, PlayerConfig.formatColor(input))
                                        : actions.invalid(context, color, input.toUpperCase(Locale.ROOT));
                        }));
        }
}
