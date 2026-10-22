package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Cost;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ExclusivityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleCost;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/**
 * Which enchantments Adjustability treats as conflicting, and what switching within a group costs. By default that's
 * vanilla's {@code exclusive_set}; the server config can turn vanilla's off, add groups of its own, and declare pairs
 * that never conflict ({@code [exclusivity]}), and give groups their own switching cost ({@code [toggle]}).
 *
 * <p>Entries are written as enchantment ids and tags separated by commas, like
 * {@code "minecraft:fortune, minecraft:silk_touch"} or {@code "#minecraft:exclusive_set/mining"}. Two enchantments
 * match an entry when each matches one of its parts (the same part counts, so a single tag is a whole group). Cost
 * entries end in {@code = <cost>}: {@code NONE}, {@code XP_LEVELS <n>}, {@code XP_POINTS <n>},
 * {@code DURABILITY <n>} or {@code ITEM <n> <item id>}.
 */
public final class Exclusivity
{
        /** The item field of costs that take none. */
        private static final Identifier NO_ITEM = Identifier.withDefaultNamespace("air");

        private static @Nullable Parsed cache;

        private Exclusivity()
        {
        }

        /** Whether {@code a} and {@code b} can't both be active, by the server's current settings. */
        public static boolean conflict(Holder<Enchantment> a, Holder<Enchantment> b)
        {
                return conflict(a, b, AdjustabilityConfig.exclusivity());
        }

        /** {@link #conflict(Holder, Holder)} by the given settings. */
        public static boolean conflict(Holder<Enchantment> a, Holder<Enchantment> b, ExclusivityConfig config)
        {
                if (a.equals(b))
                {
                        return false;
                }
                Parsed rules = rules(config);
                if (anyMatches(rules.compatible(), a, b))
                {
                        return false;
                }
                if (rules.useVanilla() && !Enchantment.areCompatible(a, b))
                {
                        return true;
                }
                return anyMatches(rules.groups(), a, b);
        }

        /**
         * The cost of a switch within {@code group}, if a {@code group_costs} entry covers it (any of its enchantments
         * matching); the first such entry.
         */
        public static Optional<Cost> groupCost(Collection<Holder<Enchantment>> group)
        {
                return groupCost(group, AdjustabilityConfig.exclusivity());
        }

        /** {@link #groupCost(Collection)} by the given settings. */
        public static Optional<Cost> groupCost(Collection<Holder<Enchantment>> group, ExclusivityConfig config)
        {
                for (CostEntry entry : rules(config).costs())
                {
                        if (group.stream().anyMatch(enchantment -> entry.selectors().stream()
                                .anyMatch(selector -> selector.matches(enchantment))))
                        {
                                return Optional.of(entry.cost());
                        }
                }
                return Optional.empty();
        }

        /** Config validation: an entry of ids and tags, with at least two parts unless it's a tag. */
        public static boolean isValidGroup(Object value)
        {
                return value instanceof String text && parseSelectors(text).filter(Exclusivity::isGroup).isPresent();
        }

        /** Config validation: {@code <ids and tags> = <cost>}. */
        public static boolean isValidCost(Object value)
        {
                return value instanceof String text && parseCost(text).isPresent();
        }

        private static boolean anyMatches(List<List<Selector>> entries, Holder<Enchantment> a, Holder<Enchantment> b)
        {
                for (List<Selector> entry : entries)
                {
                        if (entry.stream().anyMatch(selector -> selector.matches(a))
                                && entry.stream().anyMatch(selector -> selector.matches(b)))
                        {
                                return true;
                        }
                }
                return false;
        }

        /** Parsed again only when the settings change. */
        private static Parsed rules(ExclusivityConfig config)
        {
                Parsed parsed = cache;
                if (parsed == null || !parsed.source().equals(config))
                {
                        parsed = new Parsed(config, config.useVanilla(), parseAll(config.groups()),
                                parseAll(config.compatible()), config.groupCosts().stream()
                                        .map(Exclusivity::parseCost).flatMap(Optional::stream).toList());
                        cache = parsed;
                }
                return parsed;
        }

        private static List<List<Selector>> parseAll(List<? extends String> entries)
        {
                List<List<Selector>> result = new ArrayList<>();
                for (String entry : entries)
                {
                        parseSelectors(entry).filter(Exclusivity::isGroup).ifPresent(result::add);
                }
                return result;
        }

        private static boolean isGroup(List<Selector> selectors)
        {
                return selectors.size() >= 2 || selectors.size() == 1 && selectors.getFirst().tag() != null;
        }

        private static Optional<List<Selector>> parseSelectors(String text)
        {
                List<Selector> selectors = new ArrayList<>();
                for (String part : text.split(","))
                {
                        String trimmed = part.trim();
                        boolean isTag = trimmed.startsWith("#");
                        Identifier id = Identifier.tryParse(isTag ? trimmed.substring(1) : trimmed);
                        if (trimmed.isEmpty() || id == null)
                        {
                                return Optional.empty();
                        }
                        selectors.add(isTag ? new Selector(null, TagKey.create(Registries.ENCHANTMENT, id))
                                : new Selector(id, null));
                }
                return Optional.of(List.copyOf(selectors));
        }

        private static Optional<CostEntry> parseCost(String text)
        {
                int equals = text.lastIndexOf('=');
                if (equals < 0)
                {
                        return Optional.empty();
                }
                Optional<Cost> cost = parseCostValue(text.substring(equals + 1).trim());
                return cost.flatMap(value -> parseSelectors(text.substring(0, equals))
                        .map(selectors -> new CostEntry(selectors, value)));
        }

        /** {@code NONE}, {@code XP_LEVELS 5}, {@code ITEM 2 minecraft:lapis_lazuli}, ... */
        private static Optional<Cost> parseCostValue(String text)
        {
                String[] words = text.split("\\s+");
                ToggleCost type;
                try
                {
                        type = ToggleCost.valueOf(words[0].toUpperCase(Locale.ROOT));
                }
                catch (IllegalArgumentException exception)
                {
                        return Optional.empty();
                }
                int expectedWords = switch (type)
                {
                        case NONE -> 1;
                        case ITEM -> 3;
                        default -> 2;
                };
                if (words.length != expectedWords)
                {
                        return Optional.empty();
                }
                int amount = 0;
                if (type != ToggleCost.NONE)
                {
                        try
                        {
                                amount = Integer.parseInt(words[1]);
                        }
                        catch (NumberFormatException exception)
                        {
                                return Optional.empty();
                        }
                        if (amount < 0 || amount > 10_000)
                        {
                                return Optional.empty();
                        }
                }
                Identifier item = type == ToggleCost.ITEM ? Identifier.tryParse(words[2]) : NO_ITEM;
                return item == null ? Optional.empty() : Optional.of(new Cost(type, amount, item));
        }

        /** One part of an entry: an enchantment id or an enchantment tag. */
        private record Selector(@Nullable Identifier id, @Nullable TagKey<Enchantment> tag)
        {
                boolean matches(Holder<Enchantment> enchantment)
                {
                        if (this.tag != null)
                        {
                                return enchantment.is(this.tag);
                        }
                        return enchantment.unwrapKey().map(key -> key.identifier().equals(this.id)).orElse(false);
                }
        }

        private record CostEntry(List<Selector> selectors, Cost cost)
        {
        }

        private record Parsed(ExclusivityConfig source, boolean useVanilla, List<List<Selector>> groups,
                List<List<Selector>> compatible, List<CostEntry> costs)
        {
        }
}
