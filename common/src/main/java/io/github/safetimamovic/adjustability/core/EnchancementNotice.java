package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.platform.Services;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Tells operators, and the log, what Enchancement does to vanilla enchantments when it is installed. With its
 * defaults it disallows 36 of them (Fortune, Efficiency, Mending ...) and limits an item to one enchantment, so
 * items can't hold them and there is nothing for Adjustability to switch; without a word, that looks like
 * Adjustability not working.
 *
 * <p>What it reports is read from the loaded mod, not assumed: Enchancement is asked whether each vanilla enchantment
 * is allowed (its own {@code isEnchantmentAllowed}) and its limit and level settings are read, so a player who changed
 * its config gets the truth. It is reflection against a mod this one doesn't depend on, so any failure (another
 * version, renamed members) means no notice, never an error. Nothing is changed.
 */
public final class EnchancementNotice
{
        private static final String MOD_ID = "enchancement";
        private static final String UTIL = "moriyashiine.enchancement.common.util.EnchancementUtil";
        /** Its settings class: renamed between versions (the 1.21 builds call it ModConfig). */
        private static final List<String> CONFIGS = List.of("moriyashiine.enchancement.common.EnchancementConfig",
                "moriyashiine.enchancement.common.ModConfig");
        /** Enchantment names listed before "and N more". */
        private static final int LISTED = 6;

        private EnchancementNotice()
        {
        }

        /**
         * What Enchancement currently does to vanilla enchantments.
         *
         * @param disallowed vanilla enchantments items can't hold, in id order
         * @param limit      the most enchantments one item can hold; 0 or less means no limit
         * @param levelless  whether every enchantment is level 1
         */
        public record Settings(List<Identifier> disallowed, int limit, boolean levelless)
        {
                /** Whether it changes anything worth saying. */
                public boolean any()
                {
                        return !this.disallowed.isEmpty() || this.limit > 0 || this.levelless;
                }
        }

        /** Enchancement's current settings, or empty if it isn't installed or can't be read. */
        public static Optional<Settings> read()
        {
                if (!Services.PLATFORM.isModLoaded(MOD_ID))
                {
                        return Optional.empty();
                }
                try
                {
                        Method allowed = Class.forName(UTIL).getMethod("isEnchantmentAllowed", Identifier.class);
                        Class<?> config = configClass();
                        int limit = config.getField("enchantmentLimit").getInt(null);
                        // Older builds have no such setting: no levelless enchantments.
                        boolean levelless = false;
                        try
                        {
                                levelless = config.getField("levellessEnchantments").getBoolean(null);
                        }
                        catch (NoSuchFieldException exception)
                        {
                                Constants.LOG.debug("Enchancement has no levellessEnchantments setting");
                        }
                        List<Identifier> disallowed = new ArrayList<>();
                        List<Identifier> vanilla = vanillaEnchantments();
                        for (Identifier id : vanilla)
                        {
                                if (!(Boolean) allowed.invoke(null, id))
                                {
                                        disallowed.add(id);
                                }
                        }
                        return Optional.of(new Settings(List.copyOf(disallowed), limit, levelless));
                }
                catch (ReflectiveOperationException | RuntimeException | LinkageError exception)
                {
                        Constants.LOG.debug("Couldn't read Enchancement's settings: {}", exception.toString());
                        return Optional.empty();
                }
        }

        private static Class<?> configClass() throws ClassNotFoundException
        {
                ClassNotFoundException last = new ClassNotFoundException(CONFIGS.toString());
                for (String name : CONFIGS)
                {
                        try
                        {
                                return Class.forName(name);
                        }
                        catch (ClassNotFoundException exception)
                        {
                                last = exception;
                        }
                }
                throw last;
        }

        /** The notice for {@code settings}; empty when there is nothing to say. */
        public static List<Component> lines(Settings settings)
        {
                if (!settings.any())
                {
                        return List.of();
                }
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatableWithFallback("adjustability.compat.enchancement",
                        "Adjustability: Enchancement is installed. With its current settings:")
                        .withStyle(ChatFormatting.GOLD));
                if (!settings.disallowed().isEmpty())
                {
                        lines.add(Component.translatableWithFallback("adjustability.compat.enchancement.disallowed",
                                "  - %s vanilla enchantments are disallowed, so items can't hold them and Adjustability"
                                        + " has nothing to switch: %s",
                                settings.disallowed().size(), names(settings.disallowed()))
                                .withStyle(ChatFormatting.YELLOW));
                }
                if (settings.limit() > 0)
                {
                        lines.add(Component.translatableWithFallback("adjustability.compat.enchancement.limit",
                                "  - An item can hold at most %s enchantment(s), which keeps conflicting ones from"
                                        + " being active together anyway.", settings.limit())
                                .withStyle(ChatFormatting.YELLOW));
                }
                if (settings.levelless())
                {
                        lines.add(Component.translatableWithFallback("adjustability.compat.enchancement.levelless",
                                "  - Enchantments have no levels.").withStyle(ChatFormatting.YELLOW));
                }
                // Levels don't matter to switching, so levelless alone gets no fix.
                if (!settings.disallowed().isEmpty() || settings.limit() > 0)
                {
                        MutableComponent steps = Component.empty();
                        if (!settings.disallowed().isEmpty())
                        {
                                steps.append(Component.translatableWithFallback(
                                        "adjustability.compat.enchancement.fix.disallowed",
                                        "remove the enchantments you want to switch from \"disallowedEnchantments\" (or"
                                                + " set it to [])"));
                        }
                        if (settings.limit() > 0)
                        {
                                if (!settings.disallowed().isEmpty())
                                {
                                        steps.append(Component.translatableWithFallback(
                                                "adjustability.compat.enchancement.fix.and", " and "));
                                }
                                steps.append(Component.translatableWithFallback(
                                        "adjustability.compat.enchancement.fix.limit",
                                        "set \"enchantmentLimit\" to 0 (no limit)"));
                        }
                        lines.add(Component.translatableWithFallback("adjustability.compat.enchancement.fix",
                                "Suggested fix, in Enchancement's config (enchancement.json): %s. Then restart the game"
                                        + " or server.", steps).withStyle(ChatFormatting.GREEN));
                }
                lines.add(Component.translatableWithFallback("adjustability.compat.enchancement.footer",
                        "Turn this notice off with [compat] notices in adjustability-server.toml.")
                        .withStyle(ChatFormatting.GRAY));
                return lines;
        }

        /** The server started: the notice goes to the log. */
        public static void onServerStarted(MinecraftServer server)
        {
                if (!AdjustabilityConfig.compatNotices())
                {
                        return;
                }
                read().ifPresent(settings -> lines(settings).forEach(line -> Constants.LOG.warn(line.getString())));
        }

        /** A player joined: operators get the notice in chat. */
        public static void onJoin(ServerPlayer player)
        {
                if (!AdjustabilityConfig.compatNotices() || !operator(player))
                {
                        return;
                }
                read().ifPresent(settings -> lines(settings).forEach(player::sendSystemMessage));
        }

        private static boolean operator(ServerPlayer player)
        {
                //? if >=1.21.11 {
                return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
                //?} else
                /*return player.createCommandSourceStack().hasPermission(Commands.LEVEL_GAMEMASTERS);*/
        }

        /**
         * Vanilla's enchantments, from the game's own list of them. Not from the registry: Enchancement hides what it
         * disallows from the registry's listings, so listing the registry would find none of them.
         */
        public static List<Identifier> vanillaEnchantments()
        {
                List<Identifier> ids = new ArrayList<>();
                for (Field field : Enchantments.class.getFields())
                {
                        if (Modifier.isStatic(field.getModifiers()) && field.getType() == ResourceKey.class)
                        {
                                try
                                {
                                        if (field.get(null) instanceof ResourceKey<?> key)
                                        {
                                                ids.add(key.identifier());
                                        }
                                }
                                catch (IllegalAccessException exception)
                                {
                                        Constants.LOG.debug("Couldn't read {}: {}", field.getName(), exception.toString());
                                }
                        }
                }
                ids.sort(null);
                return List.copyOf(ids);
        }

        /** The first few names (their translation keys, which need no registry), then "and N more". */
        private static Component names(List<Identifier> ids)
        {
                MutableComponent names = Component.empty();
                for (int i = 0; i < Math.min(LISTED, ids.size()); i++)
                {
                        if (i > 0)
                        {
                                names.append(", ");
                        }
                        Identifier id = ids.get(i);
                        names.append(Component.translatable("enchantment." + id.getNamespace() + "." + id.getPath()));
                }
                if (ids.size() > LISTED)
                {
                        names.append(Component.translatableWithFallback("adjustability.compat.more", ", and %s more",
                                ids.size() - LISTED));
                }
                return names;
        }
}
