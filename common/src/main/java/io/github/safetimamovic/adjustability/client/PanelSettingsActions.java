package io.github.safetimamovic.adjustability.client;

import com.mojang.brigadier.context.CommandContext;
import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.PanelCommand;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

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
                                        .withClickEvent(new ClickEvent.SuggestCommand(command)))
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
                PlayerConfig.resetPanel();
                chat(Component.translatable("adjustability.command.panel.reset"));
                return 1;
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
                Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
        }
}
