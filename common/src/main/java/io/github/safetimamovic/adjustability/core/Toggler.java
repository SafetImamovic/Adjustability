package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.advancement.ModStats;
import io.github.safetimamovic.adjustability.advancement.ModTriggers;
import io.github.safetimamovic.adjustability.api.SwitchEvent;
import io.github.safetimamovic.adjustability.api.SwitchResult;
import io.github.safetimamovic.adjustability.api.SwitchSource;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.effect.ModSounds;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Cost;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleConfig;
import io.github.safetimamovic.adjustability.graph.ConflictGraph;
import io.github.safetimamovic.adjustability.graph.Selection;
import io.github.safetimamovic.adjustability.tags.ModTags;
import java.util.LinkedHashSet;
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
import net.minecraft.sounds.SoundSource;
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

        /**
         * Makes dormant {@code target} active on {@code stack}, which the caller has checked the player may change.
         * Listeners are asked after the checks and before the cost is taken, and told after the switch.
         */
        public static SwitchResult activate(ServerPlayer player, ItemStack stack, Holder<Enchantment> target,
                SwitchSource source)
        {
                return change(player, stack, target, source, false);
        }

        /**
         * The "none" choice: leaves the group {@code anyInGroup} belongs to with nothing active. Only with
         * {@code [toggle] allow_none}; same checks, cost and listeners as {@link #activate}. For an enchantment that
         * conflicts with nothing, this switches it off, which {@code [toggle] allow_switch_off} decides instead.
         */
        public static SwitchResult deactivate(ServerPlayer player, ItemStack stack, Holder<Enchantment> anyInGroup,
                SwitchSource source)
        {
                return change(player, stack, anyInGroup, source, true);
        }

        private static SwitchResult change(ServerPlayer player, ItemStack stack, Holder<Enchantment> target,
                SwitchSource source, boolean none)
        {
                ToggleConfig config = AdjustabilityConfig.toggle();
                if (!config.enabled())
                {
                        return SwitchResult.DISABLED;
                }
                if (!ItemRules.canAdjust(stack))
                {
                        return SwitchResult.NOTHING_TO_SWITCH;
                }
                HolderLookup.Provider registries = player.level().registryAccess();
                // An item can arrive with conflicting active enchantments (/give, another mod): it's repaired first,
                // never by deleting anything, keeping what's active active where possible.
                Adjuster.Split current = Adjuster.normalize(stack, registries);
                boolean broken = !current.active().equals(Adjuster.activeOf(stack))
                        || !current.dormant().equals(Adjuster.dormantOf(stack, registries));
                ConflictGraph<Holder<Enchantment>> graph = current.graph();
                Selection<Holder<Enchantment>> before = new Selection<>(current.active().keySet(),
                        current.dormant().keySet());
                Selection<Holder<Enchantment>> next;
                Set<Holder<Enchantment>> off = new LinkedHashSet<>(current.off());
                if (none)
                {
                        // An enchantment that conflicts with nothing is a group of one: its "none" switches it off.
                        boolean single = graph.singles().contains(target);
                        if (single ? !config.allowSwitchOff() : !config.allowNone())
                        {
                                return single ? SwitchResult.SWITCH_OFF_NOT_ALLOWED : SwitchResult.NONE_NOT_ALLOWED;
                        }
                        Optional<Set<Holder<Enchantment>>> group = single ? Optional.of(Set.of(target))
                                : graph.components().stream().filter(component -> component.contains(target))
                                        .findFirst();
                        if (group.isEmpty() || group.get().stream().noneMatch(before.active()::contains))
                        {
                                return repairedOr(stack, current, registries,
                                        broken ? SwitchResult.NOTHING_TO_SWITCH : null, SwitchResult.NOTHING_TO_SWITCH);
                        }
                        next = graph.deactivate(before, group.get());
                        if (single)
                        {
                                off.add(target);
                        }
                }
                else
                {
                        if (current.dormant().getLevel(target) == 0)
                        {
                                return repairedOr(stack, current, registries,
                                        broken && current.active().getLevel(target) > 0 ? SwitchResult.DONE : null,
                                        SwitchResult.NOT_DORMANT);
                        }
                        next = graph.activate(before, target, config.autoFill());
                        off.remove(target);
                }
                if (next.dormant().stream().anyMatch(enchantment -> enchantment.is(ModTags.NEVER_DORMANT)))
                {
                        return SwitchResult.WOULD_SWITCH_OFF_CURSE;
                }

                boolean free = player.hasInfiniteMaterials();
                int now = player.level().getServer().getTickCount();
                Integer last = LAST_SWITCH.get(player);
                if (!free && last != null && now - last < config.cooldownTicks())
                {
                        return SwitchResult.COOLDOWN;
                }
                SwitchEvent event = new SwitchEvent(player, stack, target, source, current.active(),
                        current.dormant(), none);
                if (!Listeners.allowSwitch(event))
                {
                        return SwitchResult.CANCELLED;
                }
                if (!free && !pay(player, stack, costOf(graph, target, config)))
                {
                        return SwitchResult.CANT_AFFORD;
                }

                ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(current.active());
                current.dormant().entrySet().forEach(entry -> all.set(entry.getKey(), entry.getIntValue()));
                Adjuster.write(stack, Adjuster.split(graph, all.toImmutable(), next, off), registries);
                LAST_SWITCH.put(player, now);
                if (config.playSound())
                {
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SWITCH,
                                SoundSource.PLAYERS, 0.6F, none ? 0.8F : 1.3F);
                }
                ModTriggers.SWITCHED.trigger(player, none, ModStats.countSwitch(player), all.toImmutable());
                Listeners.onSwitched(event);
                return SwitchResult.DONE;
        }

        /**
         * When there's nothing to switch: still writes the repaired split if the item needed it ({@code repaired} not
         * null), and returns {@code repaired} then, else {@code otherwise}.
         */
        private static SwitchResult repairedOr(ItemStack stack, Adjuster.Split current,
                HolderLookup.Provider registries, @Nullable SwitchResult repaired, SwitchResult otherwise)
        {
                if (repaired != null)
                {
                        Adjuster.write(stack, current, registries);
                        return repaired;
                }
                return otherwise;
        }

        /**
         * One press of the Cycle key on the {@code component}-th group of conflicting enchantments: the next choice,
         * and with {@code [toggle] allow_none}, "none" after the last one (see {@link ConflictGraph#next} and
         * {@link ConflictGraph#isLastChoice}).
         */
        public static Optional<CycleStep> nextInCycle(ItemStack stack, int component, HolderLookup.Provider registries)
        {
                if (!DormantEnchantments.has(stack) && !Adjuster.hasConflict(Adjuster.activeOf(stack)))
                {
                        return Optional.empty();
                }
                Adjuster.Split current = Adjuster.normalize(stack, registries);
                List<Set<Holder<Enchantment>>> components = current.graph().components();
                if (component < 0 || component >= components.size())
                {
                        return Optional.empty();
                }
                Set<Holder<Enchantment>> group = components.get(component);
                Selection<Holder<Enchantment>> selection = new Selection<>(current.active().keySet(),
                        current.dormant().keySet());
                if (AdjustabilityConfig.toggle().allowNone() && current.graph().isLastChoice(selection, group))
                {
                        return current.graph().vertices().stream().filter(group::contains).findFirst()
                                .map(any -> new CycleStep(any, true));
                }
                return current.graph().next(selection, group).map(next -> new CycleStep(next, false));
        }

        /**
         * A Cycle key step: make {@code enchantment} active, or with {@code none}, leave its group with nothing active.
         */
        public record CycleStep(Holder<Enchantment> enchantment, boolean none)
        {
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
                Set<Holder<Enchantment>> group = graph.components().stream()
                        .filter(component -> component.contains(target)).findFirst().orElse(Set.of(target));
                return Exclusivity.groupCost(group).orElseGet(config::defaultCost);
        }

        /**
         * Tells the player how a switch went, above the hotbar. {@code target} is only null if it didn't happen;
         * {@code stack} is the item it was on, for the cost.
         */
        public static void report(ServerPlayer player, SwitchResult outcome, @Nullable Holder<Enchantment> target,
                ItemStack stack)
        {
                report(player, outcome, target, stack, false);
        }

        /** {@link #report(ServerPlayer, SwitchResult, Holder, ItemStack)}, for a "none" switch when {@code none}. */
        public static void report(ServerPlayer player, SwitchResult outcome, @Nullable Holder<Enchantment> target,
                ItemStack stack, boolean none)
        {
                ToggleConfig config = AdjustabilityConfig.toggle();
                Component message = switch (outcome)
                {
                        case DONE -> none ? Component.translatable("adjustability.toggle.none",
                                TooltipLines.name(Objects.requireNonNull(target), 1))
                                : Component.translatable("adjustability.toggle.done",
                                TooltipLines.name(Objects.requireNonNull(target), 1)
                                .withStyle(style -> style.withColor(0xC71FC0)));
                        case COOLDOWN -> Component.translatable("adjustability.toggle.cooldown",
                                String.format(Locale.ROOT, "%.1f", config.cooldownTicks() / 20.0));
                        case CANT_AFFORD -> Component.translatable("adjustability.toggle.cant_afford",
                                costText(costOf(stack, Objects.requireNonNull(target),
                                        player.level().registryAccess())));
                        default -> Component.translatable("adjustability.toggle." + outcome.key());
                };
                //? if >=26.1 {
                player.sendOverlayMessage(message);
                //?} else
                /*player.displayClientMessage(message, true);*/
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
                                //? if >=26.1 {
                                BuiltInRegistries.ITEM.getValue(cost.item()).getDefaultInstance().getHoverName());
                                //?} else
                                /*BuiltInRegistries.ITEM.get(cost.item()).getDefaultInstance().getHoverName());*/
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
                                var inventory = player.getInventory();
                                var crafting = player.inventoryMenu.getCraftSlots();
                                //? if >=26.1 {
                                Item item = BuiltInRegistries.ITEM.getValue(cost.item());
                                if (inventory.clearOrCountMatchingItems(candidate -> candidate.is(item), true, amount,
                                        crafting) < amount)
                                {
                                        return false;
                                }
                                inventory.clearOrCountMatchingItems(candidate -> candidate.is(item), false, amount,
                                        crafting);
                                //?} else {
                                /*Item item = BuiltInRegistries.ITEM.get(cost.item());
                                // A count of 0 only counts.
                                if (inventory.clearOrCountMatchingItems(candidate -> candidate.is(item), 0, crafting)
                                        < amount)
                                {
                                        return false;
                                }
                                inventory.clearOrCountMatchingItems(candidate -> candidate.is(item), amount, crafting);
                                *///?}
                                return true;
                        }
                }
                return false;
        }
}
