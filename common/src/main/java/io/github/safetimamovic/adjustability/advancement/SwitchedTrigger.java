package io.github.safetimamovic.adjustability.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
//? if >=26.1 {
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
//?} else {
/*import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.RegistryCodecs;
*///?}

/**
 * {@code adjustability:switched}: a player switched an item's active enchantments (or chose none). Optional
 * conditions: {@code none} (only switches to none, or only to an enchantment), {@code min_switches} (the player's
 * total switches, the {@code adjustability:switches} statistic, including this one), and {@code holds} (enchantments,
 * a list or a #tag, that the item must all hold after the switch, active or dormant).
 */
public class SwitchedTrigger extends SimpleCriterionTrigger<SwitchedTrigger.TriggerInstance>
{
        @Override
        public Codec<TriggerInstance> codec()
        {
                return TriggerInstance.CODEC;
        }

        /** @param held everything the item holds after the switch, active and dormant */
        public void trigger(ServerPlayer player, boolean none, int totalSwitches, ItemEnchantments held)
        {
                trigger(player, instance -> instance.matches(none, totalSwitches, held));
        }

        public record TriggerInstance(
                //? if >=26.1 {
                Optional<Holder<LootItemCondition>> player,
                //?} else
                /*Optional<ContextAwarePredicate> player,*/
                Optional<Boolean> none, Optional<Integer> minSwitches, Optional<HolderSet<Enchantment>> holds)
                implements SimpleCriterionTrigger.SimpleInstance
        {
                public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        //? if >=26.1 {
                        LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        //?} else
                        /*EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),*/
                        Codec.BOOL.optionalFieldOf("none").forGetter(TriggerInstance::none),
                        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("min_switches")
                                .forGetter(TriggerInstance::minSwitches),
                        //? if >=26.1 {
                        RegistryCodecs.holderSet(Registries.ENCHANTMENT).optionalFieldOf("holds")
                        //?} else
                        /*RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("holds")*/
                                .forGetter(TriggerInstance::holds))
                        .apply(instance, TriggerInstance::new));

                boolean matches(boolean wasNone, int totalSwitches, ItemEnchantments held)
                {
                        return none.map(wanted -> wanted == wasNone).orElse(true)
                                && minSwitches.map(min -> totalSwitches >= min).orElse(true)
                                && holds.map(set -> set.stream().allMatch(enchantment -> held.getLevel(enchantment) > 0))
                                        .orElse(true);
                }
        }
}
