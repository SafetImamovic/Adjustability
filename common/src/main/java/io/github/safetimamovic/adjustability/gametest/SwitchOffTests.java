package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.api.AdjustabilityApi;
import io.github.safetimamovic.adjustability.api.SwitchResult;
import io.github.safetimamovic.adjustability.api.SwitchSource;
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.Toggler;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Switching off an enchantment that conflicts with nothing ({@code [toggle] allow_switch_off}), and that nothing but
 * switching it on turns it back on. Each switch uses a fresh mock player, so the cooldown doesn't get in the way.
 */
final class SwitchOffTests
{
        private SwitchOffTests()
        {
        }

        /** Unbreaking off, then on again; Fortune and Silk Touch (a real group) aren't touched. */
        static void switchOffSingle(GameTestHelper helper)
        {
                HolderLookup.Provider registries = helper.getLevel().registryAccess();
                Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
                ItemStack pickaxe = pickaxe(helper);

                helper.assertValueEqual(Toggler.deactivate(player(helper), pickaxe, unbreaking, SwitchSource.COMMAND),
                        SwitchResult.DONE, "switch off");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(unbreaking), 0, "Unbreaking not active");
                helper.assertValueEqual(DormantEnchantments.get(pickaxe, registries).getLevel(unbreaking), 3,
                        "its level kept");
                helper.assertValueEqual(DormantEnchantments.off(pickaxe, registries), Set.of(unbreaking), "flagged off");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(enchantment(helper, Enchantments.FORTUNE)),
                        3, "Fortune still active");
                helper.assertValueEqual(Toggler.deactivate(player(helper), pickaxe, unbreaking, SwitchSource.COMMAND),
                        SwitchResult.NOTHING_TO_SWITCH, "already off");

                helper.assertValueEqual(Toggler.activate(player(helper), pickaxe, unbreaking, SwitchSource.COMMAND),
                        SwitchResult.DONE, "switch on");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(unbreaking), 3, "Unbreaking active");
                helper.assertTrue(DormantEnchantments.off(pickaxe, registries).isEmpty(), "flag gone");
                helper.succeed();
        }

        /** A repair, a switch in another group, the API, renaming in an anvil and a book of the same enchantment. */
        static void offSurvivesRepair(GameTestHelper helper)
        {
                HolderLookup.Provider registries = helper.getLevel().registryAccess();
                Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
                ItemStack pickaxe = pickaxe(helper);
                Toggler.deactivate(player(helper), pickaxe, unbreaking, SwitchSource.COMMAND);

                helper.assertTrue(Adjuster.normalize(pickaxe, registries).dormant().getLevel(unbreaking) > 0,
                        "normalize leaves it off");
                helper.assertTrue(Adjuster.resplit(pickaxe, ItemEnchantments.EMPTY, registries).dormant()
                        .getLevel(unbreaking) > 0, "resplit leaves it off");
                helper.assertValueEqual(Toggler.activate(player(helper), pickaxe,
                        enchantment(helper, Enchantments.SILK_TOUCH), SwitchSource.COMMAND), SwitchResult.DONE,
                        "switch in the mining group");
                assertOff(helper, pickaxe, unbreaking, "after a switch elsewhere");

                AdjustabilityApi.addEnchantment(pickaxe, enchantment(helper, Enchantments.MENDING), 1, registries);
                assertOff(helper, pickaxe, unbreaking, "after the API adds Mending");

                ItemStack renamed = anvil(helper, pickaxe, ItemStack.EMPTY);
                helper.assertFalse(renamed.isEmpty(), "renaming result");
                assertOff(helper, renamed, unbreaking, "after renaming in an anvil");
                ItemStack higher = anvil(helper, pickaxe, book(unbreaking, 3));
                helper.assertFalse(higher.isEmpty(), "anvil result with Unbreaking");
                assertOff(helper, higher, unbreaking, "after a book of the same enchantment");
                helper.assertValueEqual(DormantEnchantments.get(higher, registries).getLevel(unbreaking), 3,
                        "levels still merge (III is the maximum)");
                helper.succeed();
        }

        /** A curse conflicts with nothing on vanilla items, and still can't be switched off. */
        static void offRefusesCurse(GameTestHelper helper)
        {
                Holder<Enchantment> vanishing = enchantment(helper, Enchantments.VANISHING_CURSE);
                ItemStack pickaxe = pickaxe(helper);
                pickaxe.enchant(vanishing, 1);
                helper.assertValueEqual(Toggler.deactivate(player(helper), pickaxe, vanishing, SwitchSource.COMMAND),
                        SwitchResult.WOULD_SWITCH_OFF_CURSE, "curse");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(vanishing), 1, "still active");
                helper.succeed();
        }

        /** With {@code allow_switch_off} off, singles stay on, even with {@code allow_none} on. */
        @SuppressWarnings("unchecked")
        static void switchOffDisabled(GameTestHelper helper)
        {
                Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
                ConfigValue<Boolean> allowSwitchOff = (ConfigValue<Boolean>) CreatorConfig.SPEC.getValues()
                        .get("toggle.allow_switch_off");
                ConfigValue<Boolean> allowNone = (ConfigValue<Boolean>) CreatorConfig.SPEC.getValues()
                        .get("toggle.allow_none");
                boolean switchOffBefore = allowSwitchOff.get();
                boolean noneBefore = allowNone.get();
                allowSwitchOff.set(false);
                allowNone.set(true);
                try
                {
                        ItemStack pickaxe = pickaxe(helper);
                        helper.assertValueEqual(Toggler.deactivate(player(helper), pickaxe, unbreaking,
                                SwitchSource.COMMAND), SwitchResult.SWITCH_OFF_NOT_ALLOWED, "refused");
                        helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(unbreaking), 3, "still on");
                        helper.assertValueEqual(Toggler.deactivate(player(helper), pickaxe,
                                enchantment(helper, Enchantments.FORTUNE), SwitchSource.COMMAND), SwitchResult.DONE,
                                "a real group may still go to none");
                }
                finally
                {
                        allowSwitchOff.set(switchOffBefore);
                        allowNone.set(noneBefore);
                }
                helper.succeed();
        }

        /** {@code max_dormant_per_item} counts only enchantments waiting behind a conflict. */
        @SuppressWarnings("unchecked")
        static void offNotCountedInMaxDormant(GameTestHelper helper)
        {
                ConfigValue<Integer> maxDormant = (ConfigValue<Integer>) CreatorConfig.SPEC.getValues()
                        .get("anvil.max_dormant_per_item");
                int before = maxDormant.get();
                maxDormant.set(1);
                try
                {
                        ItemStack pickaxe = pickaxe(helper);
                        Toggler.deactivate(player(helper), pickaxe, enchantment(helper, Enchantments.UNBREAKING),
                                SwitchSource.COMMAND);
                        Toggler.deactivate(player(helper), pickaxe, enchantment(helper, Enchantments.EFFICIENCY),
                                SwitchSource.COMMAND);
                        helper.assertFalse(anvil(helper, pickaxe, ItemStack.EMPTY).isEmpty(),
                                "renaming works with Silk Touch dormant and two switched off");
                }
                finally
                {
                        maxDormant.set(before);
                }
                helper.succeed();
        }

        /** Flatten turns switched-off enchantments back on and removes the rest. */
        static void flattenRestoresOff(GameTestHelper helper)
        {
                HolderLookup.Provider registries = helper.getLevel().registryAccess();
                Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
                ItemStack pickaxe = pickaxe(helper);
                Toggler.deactivate(player(helper), pickaxe, unbreaking, SwitchSource.COMMAND);
                AdjustabilityApi.flatten(pickaxe, registries);
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(unbreaking), 3, "Unbreaking back on");
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(silkTouch), 0, "Silk Touch gone");
                helper.assertFalse(pickaxe.has(DataComponents.CUSTOM_DATA), "a plain vanilla item");
                helper.succeed();
        }

        /** The API keeps "dormant" (waiting behind a conflict) and "switched off" apart. */
        static void apiSwitchedOff(GameTestHelper helper)
        {
                HolderLookup.Provider registries = helper.getLevel().registryAccess();
                Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = pickaxe(helper);
                helper.assertValueEqual(AdjustabilityApi.deactivateGroup(player(helper), pickaxe, unbreaking),
                        SwitchResult.DONE, "switched off through the API");
                ItemEnchantments off = AdjustabilityApi.switchedOffEnchantments(pickaxe, registries);
                helper.assertValueEqual(off.getLevel(unbreaking), 3, "listed as switched off");
                helper.assertValueEqual(AdjustabilityApi.dormantEnchantments(pickaxe, registries).keySet(),
                        Set.of(silkTouch), "dormant is only what waits behind a conflict");
                helper.assertValueEqual(AdjustabilityApi.activate(player(helper), pickaxe, unbreaking),
                        SwitchResult.DONE, "switched on through the API");
                helper.assertTrue(AdjustabilityApi.switchedOffEnchantments(pickaxe, registries).isEmpty(), "none off");
                helper.succeed();
        }

        private static void assertOff(GameTestHelper helper, ItemStack stack, Holder<Enchantment> enchantment,
                String when)
        {
                HolderLookup.Provider registries = helper.getLevel().registryAccess();
                helper.assertValueEqual(Adjuster.activeOf(stack).getLevel(enchantment), 0, when + ": not active");
                helper.assertTrue(DormantEnchantments.off(stack, registries).contains(enchantment),
                        when + ": still flagged off");
        }

        /** Efficiency V, Fortune III and Unbreaking III active, Silk Touch dormant behind Fortune. */
        private static ItemStack pickaxe(GameTestHelper helper)
        {
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                pickaxe.enchant(enchantment(helper, Enchantments.EFFICIENCY), 5);
                pickaxe.enchant(enchantment(helper, Enchantments.FORTUNE), 3);
                pickaxe.enchant(enchantment(helper, Enchantments.UNBREAKING), 3);
                ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                dormant.set(enchantment(helper, Enchantments.SILK_TOUCH), 1);
                DormantEnchantments.set(pickaxe, dormant.toImmutable(), helper.getLevel().registryAccess());
                return pickaxe;
        }

        private static ItemStack anvil(GameTestHelper helper, ItemStack left, ItemStack right)
        {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.experienceLevel = 30;
                AnvilMenu anvil = new AnvilMenu(0, player.getInventory());
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(left.copy());
                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right);
                if (right.isEmpty())
                {
                        anvil.setItemName("Renamed");
                }
                return anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem().copy();
        }

        private static ItemStack book(Holder<Enchantment> enchantment, int level)
        {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                stored.set(enchantment, level);
                book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
                return book;
        }

        private static ServerPlayer player(GameTestHelper helper)
        {
                return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        }

        private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key)
        {
                return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }
}
