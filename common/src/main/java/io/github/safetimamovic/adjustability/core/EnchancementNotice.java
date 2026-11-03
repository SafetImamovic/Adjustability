package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.platform.Services;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;

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
        private static final String CONFIG = "moriyashiine.enchancement.common.EnchancementConfig";
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
        public static Optional<Settings> read(HolderLookup.Provider registries)
        {
                if (!Services.PLATFORM.isModLoaded(MOD_ID))
                {
                        return Optional.empty();
                }
                try
                {
                        Method allowed = Class.forName(UTIL).getMethod("isEnchantmentAllowed", Identifier.class);
                        Class<?> config = Class.forName(CONFIG);
                        int limit = config.getField("enchantmentLimit").getInt(null);
                        boolean levelless = config.getField("levellessEnchantments").getBoolean(null);
                        List<Identifier> disallowed = new ArrayList<>();
                        List<Identifier> vanilla = registries.lookupOrThrow(Registries.ENCHANTMENT).listElementIds()
                                .map(key -> key.identifier()).filter(id -> id.getNamespace().equals("minecraft"))
                                .sorted().toList();
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

        /** The notice for {@code settings}; empty when there is nothing to say. */
        public static List<Component> lines(Settings settings, HolderLookup.Provider registries)
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
                                settings.disallowed().size(), names(settings.disallowed(), registries))
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
                HolderLookup.Provider registries = server.registryAccess();
                read(registries).ifPresent(settings -> lines(settings, registries)
                        .forEach(line -> Constants.LOG.warn(line.getString())));
        }

        /** A player joined: operators get the notice in chat. */
        public static void onJoin(ServerPlayer player)
        {
                if (!AdjustabilityConfig.compatNotices() || !operator(player))
                {
                        return;
                }
                HolderLookup.Provider registries = player.level().registryAccess();
                read(registries).ifPresent(settings -> lines(settings, registries).forEach(player::sendSystemMessage));
        }

        private static boolean operator(ServerPlayer player)
        {
                //? if >=1.21.11 {
                return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
                //?} else
                /*return player.createCommandSourceStack().hasPermission(Commands.LEVEL_GAMEMASTERS);*/
        }

        /** The first few names, then "and N more". */
        private static Component names(List<Identifier> ids, HolderLookup.Provider registries)
        {
                var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
                MutableComponent names = Component.empty();
                for (int i = 0; i < Math.min(LISTED, ids.size()); i++)
                {
                        if (i > 0)
                        {
                                names.append(", ");
                        }
                        Identifier id = ids.get(i);
                        names.append(lookup.get(ResourceKey.<Enchantment>create(Registries.ENCHANTMENT, id))
                                .map(holder -> holder.value().description().copy())
                                .orElseGet(() -> Component.literal(id.toString())));
                }
                if (ids.size() > LISTED)
                {
                        names.append(Component.translatableWithFallback("adjustability.compat.more", ", and %s more",
                                ids.size() - LISTED));
                }
                return names;
        }
}
