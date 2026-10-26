package io.github.safetimamovic.adjustability.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
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
                if (pending != null)
                {
                        for (Holder<Enchantment> enchantment : pending.left())
                        {
                                int level = pending.dormant().getLevel(enchantment);
                                consumer.accept(pending.off().contains(enchantment)
                                        ? Component.translatable("adjustability.tooltip.off", name(enchantment, level))
                                                .withStyle(ChatFormatting.DARK_GRAY)
                                        : line(enchantment, level));
                        }
                }
                if (flag.isAdvanced() && showDormant.getAsBoolean())
                {
                        DormantEnchantments.unknown(components, context.registries()).forEach((id, level) ->
                                consumer.accept(Component.translatable("adjustability.tooltip.unknown", id,
                                        Component.translatable("enchantment.level." + level))
                                        .withStyle(ChatFormatting.DARK_GRAY)));
                }
                Component line = hint.get();
                if (line != null)
                {
                        consumer.accept(line);
                }
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
