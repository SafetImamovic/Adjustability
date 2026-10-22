package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Cost;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleConfig;
import io.github.safetimamovic.adjustability.graph.ConflictGraph;
import io.github.safetimamovic.adjustability.graph.Selection;
import io.github.safetimamovic.adjustability.tags.ModTags;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/**
 * Switching which enchantments are active, on the server. Every check happens here, whatever asked for the switch (the
 * Adjust panel, the Cycle key, the command): the item can take part, the enchantment is dormant on it, no curse would
 * be switched off, the cooldown has passed and the player can pay.
 */
public final class Toggler
{
        /** Server tick of each player's last switch. Weak: player objects go away on logout and respawn. */
        private static final Map<ServerPlayer, Integer> LAST_SWITCH = new WeakHashMap<>();

        private Toggler()
        {
        }

        /** Makes dormant {@code target} active on {@code stack}, which the caller has checked the player may change. */
        public static Outcome activate(ServerPlayer player, ItemStack stack, Holder<Enchantment> target)
        {
                ToggleConfig config = AdjustabilityConfig.toggle();
                if (!config.enabled())
                {
                        return Outcome.DISABLED;
                }
                if (!ItemRules.canAdjust(stack))
                {
                        return Outcome.NOTHING_TO_SWITCH;
                }
                HolderLookup.Provider registries = player.level().registryAccess();
                // An item can arrive with conflicting active enchantments (/give, another mod): it's repaired first,
                // never by deleting anything, keeping what's active active where possible.
                Adjuster.Split current = Adjuster.resplit(stack, ItemEnchantments.EMPTY, registries);
                boolean broken = !current.active().equals(Adjuster.activeOf(stack))
                        || !current.dormant().equals(Adjuster.dormantOf(stack, registries));
                if (current.dormant().getLevel(target) == 0)
                {
                        if (broken && current.active().getLevel(target) > 0)
                        {
                                Adjuster.write(stack, current);
                                return Outcome.DONE;
                        }
                        return Outcome.NOT_DORMANT;
                }

                ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(current.active());
                current.dormant().entrySet().forEach(entry -> all.set(entry.getKey(), entry.getIntValue()));
                ConflictGraph<Holder<Enchantment>> graph = current.graph();
                Selection<Holder<Enchantment>> next = graph.activate(
                        new Selection<>(current.active().keySet(), current.dormant().keySet()), target,
                        config.autoFill());
                if (next.dormant().stream().anyMatch(enchantment -> enchantment.is(ModTags.NEVER_DORMANT)))
                {
                        return Outcome.WOULD_SWITCH_OFF_CURSE;
                }

                boolean free = player.hasInfiniteMaterials();
                int now = player.level().getServer().getTickCount();
                Integer last = LAST_SWITCH.get(player);
                if (!free && last != null && now - last < config.cooldownTicks())
                {
                        return Outcome.COOLDOWN;
                }
                if (!free && !pay(player, stack, costOf(graph, target, config)))
                {
                        return Outcome.CANT_AFFORD;
                }

                Adjuster.write(stack, Adjuster.split(graph, all.toImmutable(), next));
                LAST_SWITCH.put(player, now);
                return Outcome.DONE;
        }

        /**
         * The enchantment one press of the Cycle key activates: the next choice in the {@code component}-th group of
         * conflicting enchantments on {@code stack} (see {@link ConflictGraph#next}).
         */
        public static Optional<Holder<Enchantment>> nextInCycle(ItemStack stack, int component,
                HolderLookup.Provider registries)
        {
                if (!DormantEnchantments.has(stack) && !Adjuster.hasConflict(Adjuster.activeOf(stack)))
                {
                        return Optional.empty();
                }
                Adjuster.Split current = Adjuster.resplit(stack, ItemEnchantments.EMPTY, registries);
                List<Set<Holder<Enchantment>>> components = current.graph().components();
                if (component < 0 || component >= components.size())
                {
                        return Optional.empty();
                }
                return current.graph().next(new Selection<>(current.active().keySet(), current.dormant().keySet()),
                        components.get(component));
        }

        /**
         * What switching to {@code target} on {@code stack} costs: its group's {@code group_costs} entry, else the
         * default cost.
         */
        public static Cost costOf(ItemStack stack, Holder<Enchantment> target, HolderLookup.Provider registries)
        {
                return costOf(Adjuster.resplit(stack, ItemEnchantments.EMPTY, registries).graph(), target,
                        AdjustabilityConfig.toggle());
        }

        private static Cost costOf(ConflictGraph<Holder<Enchantment>> graph, Holder<Enchantment> target,
                ToggleConfig config)
        {
                return graph.components().stream().filter(group -> group.contains(target)).findFirst()
                        .flatMap(Exclusivity::groupCost).orElseGet(config::defaultCost);
        }

        /**
         * Tells the player how a switch went, above the hotbar. {@code target} is only null if it didn't happen;
         * {@code stack} is the item it was on, for the cost.
         */
        public static void report(ServerPlayer player, Outcome outcome, @Nullable Holder<Enchantment> target,
                ItemStack stack)
        {
                ToggleConfig config = AdjustabilityConfig.toggle();
                Component message = switch (outcome)
                {
                        case DONE -> Component.translatable("adjustability.toggle.done",
                                TooltipLines.name(Objects.requireNonNull(target), 1)
                                .withStyle(style -> style.withColor(0xC71FC0)));
                        case COOLDOWN -> Component.translatable("adjustability.toggle.cooldown",
                                String.format(Locale.ROOT, "%.1f", config.cooldownTicks() / 20.0));
                        case CANT_AFFORD -> Component.translatable("adjustability.toggle.cant_afford",
                                costText(costOf(stack, Objects.requireNonNull(target),
                                        player.level().registryAccess())));
                        default -> Component.translatable("adjustability.toggle." + outcome.key());
                };
                player.sendOverlayMessage(message);
        }

        /** "1 level", "3 Lapis Lazuli", ...; for the Adjust panel and messages. */
        public static Component costText(Cost cost)
        {
                int amount = cost.amount();
                return switch (cost.type())
                {
                        case NONE -> Component.translatable("adjustability.cost.free");
                        case XP_LEVELS -> Component.translatable("adjustability.cost.levels", amount);
                        case XP_POINTS -> Component.translatable("adjustability.cost.points", amount);
                        case DURABILITY -> Component.translatable("adjustability.cost.durability", amount);
                        case ITEM -> Component.translatable("adjustability.cost.item", amount,
                                BuiltInRegistries.ITEM.getValue(cost.item()).getDefaultInstance().getHoverName());
                };
        }

        /**
         * Takes {@code cost} from the player (or from {@code stack}, for durability), if they can pay all of it.
         *
         * @return false, and nothing taken, if they can't
         */
        public static boolean pay(ServerPlayer player, ItemStack stack, Cost cost)
        {
                int amount = cost.amount();
                if (amount <= 0)
                {
                        return true;
                }
                switch (cost.type())
                {
                        case NONE ->
                        {
                                return true;
                        }
                        case XP_LEVELS ->
                        {
                                if (player.experienceLevel < amount)
                                {
                                        return false;
                                }
                                player.giveExperienceLevels(-amount);
                                return true;
                        }
                        case XP_POINTS ->
                        {
                                if (player.totalExperience < amount)
                                {
                                        return false;
                                }
                                player.giveExperiencePoints(-amount);
                                return true;
                        }
                        case DURABILITY ->
                        {
                                // Never breaks the item: a switch that would is refused.
                                if (!stack.isDamageableItem())
                                {
                                        return true;
                                }
                                if (stack.getMaxDamage() - stack.getDamageValue() <= amount)
                                {
                                        return false;
                                }
                                stack.setDamageValue(stack.getDamageValue() + amount);
                                return true;
                        }
                        case ITEM ->
                        {
                                Item item = BuiltInRegistries.ITEM.getValue(cost.item());
                                var inventory = player.getInventory();
                                var crafting = player.inventoryMenu.getCraftSlots();
                                if (inventory.clearOrCountMatchingItems(candidate -> candidate.is(item), true, amount,
                                        crafting) < amount)
                                {
                                        return false;
                                }
                                inventory.clearOrCountMatchingItems(candidate -> candidate.is(item), false, amount,
                                        crafting);
                                return true;
                        }
                }
                return false;
        }

        public enum Outcome
        {
                DONE,
                DISABLED,
                NOTHING_TO_SWITCH,
                NOT_DORMANT,
                WOULD_SWITCH_OFF_CURSE,
                COOLDOWN,
                CANT_AFFORD;

                String key()
                {
                        return this.name().toLowerCase(Locale.ROOT);
                }
        }
}
