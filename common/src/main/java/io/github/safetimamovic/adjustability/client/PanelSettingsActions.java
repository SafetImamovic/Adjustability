package io.github.safetimamovic.adjustability.client;

import com.mojang.brigadier.context.CommandContext;
import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.PanelCommand;
import io.github.safetimamovic.adjustability.config.PanelProfiles;
import io.github.safetimamovic.adjustability.config.PanelProfiles.Look;
import io.github.safetimamovic.adjustability.config.PanelProfiles.ProfileException;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import org.jspecify.annotations.Nullable;

/** The client's {@code /adjustability panel}: changes the player file and says so in chat. */
final class PanelSettingsActions<S> implements PanelCommand.Actions<S>
{
        @Override
        public int list(CommandContext<S> context)
        {
                chat(Component.translatable("adjustability.command.panel.header").withStyle(ChatFormatting.GOLD));
                for (ConfigValue<?> setting : PlayerConfig.PANEL_SETTINGS)
                {
                        String name = PlayerConfig.name(setting);
                        String command = "/" + Constants.MOD_ID + " panel " + name + " ";
                        Object value = PlayerConfig.current(setting);
                        MutableComponent line = Component.literal("  " + name + ": ")
                                .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                                        .withClickEvent(
                                //? if >=1.21.5 {
                                new ClickEvent.SuggestCommand(command)
                                //?} else
                                /*new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)*/
                        ))
                                .append(Component.literal(String.valueOf(value)).withStyle(this.valueStyle(value)));
                        chat(line);
                }
                chat(Component.translatable("adjustability.command.panel.footer").withStyle(ChatFormatting.GRAY));
                return PlayerConfig.PANEL_SETTINGS.size();
        }

        @Override
        public int reset(CommandContext<S> context)
        {
                if (!PlayerConfig.canChange())
                {
                        return this.unavailable();
                }
                PlayerConfig.resetPanel().ifPresentOrElse(this::failure,
                        () -> chat(Component.translatable("adjustability.command.panel.reset")));
                return 1;
        }

        @Override
        public int profile(CommandContext<S> context, String name)
        {
                if (!PlayerConfig.canChange())
                {
                        return this.unavailable();
                }
                try
                {
                        return this.apply(PanelProfiles.load(name), Component.translatable(
                                "adjustability.command.panel.profile", profileName(name)));
                }
                catch (ProfileException exception)
                {
                        return this.failure(exception);
                }
        }

        @Override
        public int save(CommandContext<S> context, String name)
        {
                if (!PlayerConfig.canChange())
                {
                        return this.unavailable();
                }
                try
                {
                        Path file = PanelProfiles.save(name, PlayerConfig.currentLook());
                        chat(Component.translatable("adjustability.command.panel.saved", name,
                                Component.literal(PanelProfiles.display(file)).withStyle(ChatFormatting.GRAY)));
                        return 1;
                }
                catch (ProfileException exception)
                {
                        return this.failure(exception);
                }
        }

        @Override
        public int export(CommandContext<S> context)
        {
                String code = PanelProfiles.encode(PlayerConfig.currentLook());
                Minecraft.getInstance().keyboardHandler.setClipboard(code);
                chat(Component.translatable("adjustability.command.panel.exported",
                        Component.literal(code).withStyle(style -> style.withColor(ChatFormatting.AQUA)
                                .withClickEvent(
                                //? if >=1.21.5 {
                                new ClickEvent.CopyToClipboard(code)
                                //?} else
                                /*new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code)*/
                        ))));
                return 1;
        }

        @Override
        public int importCode(CommandContext<S> context, @Nullable String code)
        {
                if (!PlayerConfig.canChange())
                {
                        return this.unavailable();
                }
                try
                {
                        String text = code != null ? code : Minecraft.getInstance().keyboardHandler.getClipboard();
                        return this.apply(PanelProfiles.decode(text),
                                Component.translatable("adjustability.command.panel.imported"));
                }
                catch (ProfileException exception)
                {
                        return this.failure(exception);
                }
        }

        /** Applies a look, says so, and lists what it had that this version can't use. */
        private int apply(Look look, Component done)
        {
                PlayerConfig.applyLook(look.values());
                chat(done);
                if (!look.skipped().isEmpty())
                {
                        chat(Component.translatable("adjustability.command.panel.skipped",
                                String.join(", ", look.skipped())).withStyle(ChatFormatting.GRAY));
                }
                return 1;
        }

        /** A built-in profile's translated name, else the file's. */
        static Component profileName(String id)
        {
                return PanelProfiles.isBuiltIn(id) ? Component.translatable("adjustability.profile." + id)
                        : Component.literal(id);
        }

        private int failure(ProfileException exception)
        {
                chat(exception.message().copy().withStyle(ChatFormatting.RED));
                return 0;
        }

        @Override
        public <T> int set(CommandContext<S> context, ConfigValue<T> setting, T value)
        {
                if (!PlayerConfig.canChange())
                {
                        return this.unavailable();
                }
                if (!PlayerConfig.change(setting, value))
                {
                        return this.invalid(context, setting, String.valueOf(value));
                }
                chat(Component.translatable("adjustability.command.panel.set", PlayerConfig.name(setting),
                        Component.literal(String.valueOf(value)).withStyle(this.valueStyle(value))));
                return 1;
        }

        @Override
        public int invalid(CommandContext<S> context, ConfigValue<?> setting, String input)
        {
                chat(Component.translatable("adjustability.command.panel.invalid", PlayerConfig.name(setting), input)
                        .withStyle(ChatFormatting.RED));
                return 0;
        }

        private int unavailable()
        {
                chat(Component.translatable("adjustability.command.panel.unavailable").withStyle(ChatFormatting.RED));
                return 0;
        }

        /** Colors are shown in their own color, everything else in white. */
        private Style valueStyle(Object value)
        {
                return value instanceof String text
                        ? PlayerConfig.parseColor(text).map(argb -> Style.EMPTY.withColor(argb & 0xFFFFFF))
                                .orElse(Style.EMPTY.withColor(ChatFormatting.WHITE))
                        : Style.EMPTY.withColor(ChatFormatting.WHITE);
        }

        private static void chat(Component message)
        {
                //? if >=26.2 {
                Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
                //?} else if >=26.1 {
                /*Minecraft.getInstance().gui.getChat().addClientSystemMessage(message);
                *///?} else
                /*Minecraft.getInstance().gui.getChat().addMessage(message);*/
        }
}
