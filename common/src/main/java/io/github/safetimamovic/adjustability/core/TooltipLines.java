package io.github.safetimamovic.adjustability.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.5 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/** Tooltip lines for dormant enchantments. Plain components, so it runs wherever vanilla builds tooltips. */
public final class TooltipLines
{
        /** The player's settings and key hint; set by the client, so a dedicated server uses these defaults. */
        private static BooleanSupplier showDormant = () -> true;
        private static Supplier<@Nullable Component> hint = () -> null;

        /** Dormant enchantments not listed yet, while vanilla writes an item's active ones (the client thread). */
        private static final ThreadLocal<@Nullable Pending> PENDING = new ThreadLocal<>();

        /**
         * Set once {@link #addDormant} has run for the tooltip being built. A mod that writes enchantment tooltips
         * itself and cancels vanilla's (Apothic Enchanting does) skips the hooks in {@code ItemEnchantments}, and
         * {@link #tooltipFinished} then adds the lines to the finished list instead.
         */
        private static final ThreadLocal<@Nullable Boolean> LISTED = new ThreadLocal<>();

        private record Pending(ItemEnchantments dormant, Set<Holder<Enchantment>> off, List<Holder<Enchantment>> left)
        {
        }

        private TooltipLines()
        {
        }

        /** Called once by the client: whether to list dormant enchantments, and the "press Alt" line (or null). */
        public static void setClientHooks(BooleanSupplier showDormantSetting, Supplier<@Nullable Component> hintLine)
        {
                showDormant = showDormantSetting;
                hint = hintLine;
        }

        /**
         * Vanilla starts writing enchantments. For the item's active ones ({@code activeList}), remembers its dormant
         * enchantments so {@link #addDormantUnder} can place them; anything else (a book's stored ones) gets none.
         */
        public static void begin(Item.TooltipContext context, DataComponentGetter components, boolean activeList)
        {
                PENDING.remove();
                HolderLookup.Provider registries = context.registries();
                if (!activeList || registries == null || !DormantEnchantments.has(components)
                        || !showDormant.getAsBoolean())
                {
                        return;
                }
                ItemEnchantments dormant = DormantEnchantments.get(components, registries);
                List<Holder<Enchantment>> sorted = new ArrayList<>(dormant.keySet());
                sorted.sort((a, b) -> Adjuster.id(a).compareTo(Adjuster.id(b)));
                PENDING.set(new Pending(dormant, DormantEnchantments.off(components, registries), sorted));
        }

        /** Vanilla wrote the line for {@code active}: the dormant enchantments it's the first conflict of go under it. */
        public static void addDormantUnder(Consumer<Component> consumer, Holder<Enchantment> active)
        {
                Pending pending = PENDING.get();
                if (pending == null)
                {
                        return;
                }
                pending.left().removeIf(dormant ->
                {
                        if (!Adjuster.conflict(active, dormant))
                        {
                                return false;
                        }
                        consumer.accept(line(dormant, pending.dormant().getLevel(dormant)));
                        return true;
                });
        }

        /**
         * Vanilla is done: the dormant enchantments nothing active conflicts with (switched-off ones say so), with
         * advanced tooltips (F3+H) the
         * dormant entries for enchantments that aren't registered, then the key hint.
         */
        public static void addDormant(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag,
                DataComponentGetter components)
        {
                Pending pending = PENDING.get();
                PENDING.remove();
                if (context.registries() == null || !DormantEnchantments.has(components))
                {
                        return;
                }
                LISTED.set(Boolean.TRUE);
                trailing(context, flag, components, pending).forEach(consumer);
        }

        /** The tooltip build for one item starts: nothing has listed its dormant enchantments yet. */
        public static void tooltipStarted()
        {
                LISTED.remove();
        }

        /**
         * The tooltip for {@code stack} is done. If {@link #addDormant} never ran for it, because another mod replaced
         * vanilla's enchantment lines, the dormant ones are inserted into {@code lines} where they'd have gone: under
         * the line of the first active enchantment each conflicts with, the rest after the last enchantment line. Does
         * nothing if none of the item's active enchantments is listed (hidden in the tooltip, or written in a form this
         * can't recognize).
         *
         * @return {@code lines}, or a new list with the dormant lines added
         */
        public static List<Component> tooltipFinished(Item.TooltipContext context, ItemStack stack, TooltipFlag flag,
                List<Component> lines)
        {
                boolean listed = LISTED.get() != null;
                LISTED.remove();
                HolderLookup.Provider registries = context.registries();
                if (listed || registries == null || !DormantEnchantments.has(stack))
                {
                        return lines;
                }
                // The lines of the item's active enchantments, in tooltip order.
                TreeMap<Integer, Holder<Enchantment>> shown = new TreeMap<>();
                ItemEnchantments active = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                for (Holder<Enchantment> enchantment : active.keySet())
                {
                        String name = Enchantment.getFullname(enchantment, active.getLevel(enchantment)).getString();
                        for (int i = 0; i < lines.size(); i++)
                        {
                                if (!shown.containsKey(i) && lines.get(i).getString().equals(name))
                                {
                                        shown.put(i, enchantment);
                                        break;
                                }
                        }
                }
                if (shown.isEmpty() && (!active.isEmpty() || lines.isEmpty() || enchantmentsHidden(stack)))
                {
                        // Either the enchantment lines are in a form this can't recognize, or they're not shown.
                        return lines;
                }

                Pending pending = null;
                Map<Integer, List<Component>> under = new HashMap<>();
                if (showDormant.getAsBoolean())
                {
                        ItemEnchantments dormant = DormantEnchantments.get(stack, registries);
                        List<Holder<Enchantment>> sorted = new ArrayList<>(dormant.keySet());
                        sorted.sort((a, b) -> Adjuster.id(a).compareTo(Adjuster.id(b)));
                        pending = new Pending(dormant, DormantEnchantments.off(stack, registries), sorted);
                        for (Map.Entry<Integer, Holder<Enchantment>> entry : shown.entrySet())
                        {
                                Pending current = pending;
                                current.left().removeIf(other ->
                                {
                                        if (!Adjuster.conflict(entry.getValue(), other))
                                        {
                                                return false;
                                        }
                                        under.computeIfAbsent(entry.getKey(), key -> new ArrayList<>())
                                                .add(line(other, current.dormant().getLevel(other)));
                                        return true;
                                });
                        }
                }
                List<Component> trailing = trailing(context, flag, stack, pending);

                // With no active enchantment (all switched off) there's no line to follow: right after the item's name.
                int last = shown.isEmpty() ? 0 : shown.lastKey();
                List<Component> result = new ArrayList<>(lines.size() + trailing.size() + under.size());
                for (int i = 0; i < lines.size(); i++)
                {
                        result.add(lines.get(i));
                        result.addAll(under.getOrDefault(i, List.of()));
                        if (i == last)
                        {
                                result.addAll(trailing);
                        }
                }
                return result;
        }

        /** Whether the item's tooltip leaves its enchantments out. Not known before 1.21.5, where it's a private flag. */
        private static boolean enchantmentsHidden(ItemStack stack)
        {
                //? if >=1.21.5 {
                TooltipDisplay display = stack.get(DataComponents.TOOLTIP_DISPLAY);
                return display != null && (display.hideTooltip() || !display.shows(DataComponents.ENCHANTMENTS));
                //?} else {
                /*return false;
                *///?}
        }

        /**
         * What comes after the item's enchantment lines: the dormant ones nothing active conflicts with (switched-off
         * ones say so), with advanced tooltips the entries for enchantments that aren't registered, then the key hint.
         */
        private static List<Component> trailing(Item.TooltipContext context, TooltipFlag flag,
                DataComponentGetter components, @Nullable Pending pending)
        {
                List<Component> lines = new ArrayList<>();
                if (pending != null)
                {
                        for (Holder<Enchantment> enchantment : pending.left())
                        {
                                int level = pending.dormant().getLevel(enchantment);
                                lines.add(pending.off().contains(enchantment)
                                        ? Component.translatable("adjustability.tooltip.off", name(enchantment, level))
                                                .withStyle(ChatFormatting.DARK_GRAY)
                                        : line(enchantment, level));
                        }
                }
                if (flag.isAdvanced() && showDormant.getAsBoolean())
                {
                        DormantEnchantments.unknown(components, context.registries()).forEach((id, level) ->
                                lines.add(Component.translatable("adjustability.tooltip.unknown", id,
                                        Component.translatable("enchantment.level." + level))
                                        .withStyle(ChatFormatting.DARK_GRAY)));
                }
                Component line = hint.get();
                if (line != null)
                {
                        lines.add(line);
                }
                return lines;
        }

        private static Component line(Holder<Enchantment> enchantment, int level)
        {
                return Component.translatable("adjustability.tooltip.dormant", name(enchantment, level))
                        .withStyle(ChatFormatting.DARK_GRAY);
        }

        /** Like {@link Enchantment#getFullname}, without its gray, so the line's dark gray applies. */
        public static MutableComponent name(Holder<Enchantment> enchantment, int level)
        {
                MutableComponent name = enchantment.value().description().copy();
                if (level != 1 || enchantment.value().getMaxLevel() != 1)
                {
                        name.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level));
                }
                return name;
        }
}
