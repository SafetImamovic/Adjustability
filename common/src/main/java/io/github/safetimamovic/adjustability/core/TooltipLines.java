package io.github.safetimamovic.adjustability.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Tooltip lines for dormant enchantments. Plain components, so it runs wherever vanilla builds tooltips. */
public final class TooltipLines
{
        private TooltipLines()
        {
        }

        public static void addDormant(Item.TooltipContext context, Consumer<Component> consumer,
                DataComponentGetter components)
        {
                HolderLookup.Provider registries = context.registries();
                if (registries == null || !DormantEnchantments.has(components))
                {
                        return;
                }
                ItemEnchantments dormant = DormantEnchantments.get(components, registries);
                List<Holder<Enchantment>> sorted = new ArrayList<>(dormant.keySet());
                sorted.sort((a, b) -> Adjuster.id(a).compareTo(Adjuster.id(b)));
                for (Holder<Enchantment> enchantment : sorted)
                {
                        consumer.accept(Component.translatable("adjustability.tooltip.dormant",
                                name(enchantment, dormant.getLevel(enchantment))).withStyle(ChatFormatting.DARK_GRAY));
                }
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
