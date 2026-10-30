package io.github.safetimamovic.adjustability.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.PanelCommand;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Client side of {@code /adjustability}: {@code help}, {@code config}, {@code keybinds}, {@code tutorial} and
 * {@code panel}, for every
 * player. They share
 * the root with the server's {@code toggle}, {@code inspect}, {@code add} and {@code flatten}; both loaders send a
 * subcommand the client doesn't know on to the server.
 * The server has stubs of these for suggestions (see {@code AdjustabilityCommand}). Each loader registers this with its
 * own command source, which these commands don't use.
 */
public final class AdjustabilityClientCommand
{
        /** Subcommands in help order; from {@code toggle} on they are the server's, the last three for game masters. */
        private static final List<String> SUBCOMMANDS = List.of("help", "config", "keybinds", "tutorial", "panel", "toggle",
                "inspect", "add", "flatten");

        private static UnaryOperator<@Nullable Screen> configScreen = parent -> parent;

        private AdjustabilityClientCommand()
        {
        }

        /** Called by each loader's client entrypoint with its config screen, which takes the screen to go back to. */
        public static void init(UnaryOperator<@Nullable Screen> configScreenFactory)
        {
                configScreen = configScreenFactory;
        }

        public static <S> void register(CommandDispatcher<S> dispatcher)
        {
                dispatcher.register(LiteralArgumentBuilder.<S>literal(Constants.MOD_ID)
                        .executes(context -> help())
                        .then(LiteralArgumentBuilder.<S>literal("help").executes(context -> help()))
                        .then(LiteralArgumentBuilder.<S>literal("config")
                                .executes(context -> open(() -> configScreen.apply(null))))
                        .then(LiteralArgumentBuilder.<S>literal("keybinds")
                                .executes(context -> open(
                                        () -> new KeyBindsScreen(null, Minecraft.getInstance().options))))
                        .then(LiteralArgumentBuilder.<S>literal("tutorial").executes(context -> AdjustTutorial.restart()))
                        .then(PanelCommand.tree(new PanelSettingsActions<>())));
        }

        /** "/adjustability keybinds" and the like; clicking it puts it in the chat box. */
        public static Component link(String subcommand)
        {
                String command = "/" + Constants.MOD_ID + " " + subcommand;
                return Component.literal(command).withStyle(style -> style.withColor(ChatFormatting.AQUA)
                        .withClickEvent(
                                //? if >=1.21.5 {
                                new ClickEvent.SuggestCommand(command)
                                //?} else
                                /*new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)*/
                        ));
        }

        private static int help()
        {
                chat(Component.translatable("adjustability.command.help.header").withStyle(ChatFormatting.GOLD));
                for (String subcommand : SUBCOMMANDS)
                {
                        chat(Component.literal("  ").append(link(subcommand)).append(" ")
                                .append(Component.translatable("adjustability.command.help." + subcommand)
                                        .withStyle(ChatFormatting.GRAY)));
                }
                return SUBCOMMANDS.size();
        }

        /** Next tick: the chat screen the command was typed in closes after running it, and would close this too. */
        private static int open(Supplier<@Nullable Screen> screen)
        {
                Minecraft minecraft = Minecraft.getInstance();
                //? if >=26.2 {
                minecraft.schedule(() -> minecraft.gui.setScreen(screen.get()));
                //?} else if >=26.1 {
                /*minecraft.schedule(() -> minecraft.setScreen(screen.get()));
                *///?} else
                /*minecraft.tell(() -> minecraft.setScreen(screen.get()));*/
                return 1;
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
