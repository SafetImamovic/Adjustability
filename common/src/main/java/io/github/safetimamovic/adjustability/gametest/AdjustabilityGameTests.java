package io.github.safetimamovic.adjustability.gametest;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.advancement.ModStats;
import io.github.safetimamovic.adjustability.api.AdjustabilityApi;
import io.github.safetimamovic.adjustability.api.AdjustabilityListener;
import io.github.safetimamovic.adjustability.api.SwitchEvent;
import io.github.safetimamovic.adjustability.api.SwitchResult;
import io.github.safetimamovic.adjustability.api.SwitchSource;
import io.github.safetimamovic.adjustability.ModRegistries;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Cost;
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ExclusivityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ItemMode;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleCost;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.Exclusivity;
import io.github.safetimamovic.adjustability.core.GrindstoneRules;
import io.github.safetimamovic.adjustability.core.ItemRules;
import io.github.safetimamovic.adjustability.core.Toggler;
import io.github.safetimamovic.adjustability.network.CycleEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.ToggleEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.ToggleNetwork;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * In-game tests for the anvil and grindstone rules, run by vanilla's GameTest framework on both loaders. Each test is a
 * function registered here plus a test instance in {@code data/adjustability/test_instance/} pointing at it. They drive
 * the real menus with a mock survival player, so the mixins run exactly as in game.
 */
public final class AdjustabilityGameTests
{
        /** The first hotbar slot in the player's inventory menu. */
        private static final int HOTBAR_0 = InventoryMenu.USE_ROW_SLOT_START;

        private AdjustabilityGameTests()
        {
        }

        public static void register(ModRegistries.Registrar registrar)
        {
                test(registrar, "anvil_book_adds_dormant", AdjustabilityGameTests::anvilBookAddsDormant);
                test(registrar, "anvil_vanilla_merge_unchanged", AdjustabilityGameTests::anvilVanillaMergeUnchanged);
                test(registrar, "anvil_books_combine", AdjustabilityGameTests::anvilBooksCombine);
                test(registrar, "anvil_book_with_conflicts_splits", AdjustabilityGameTests::anvilBookWithConflictsSplits);
                test(registrar, "anvil_rename_keeps_dormant", AdjustabilityGameTests::anvilRenameKeepsDormant);
                test(registrar, "grindstone_removes_dormant", AdjustabilityGameTests::grindstoneRemovesDormant);
                test(registrar, "toggle_switches_active", AdjustabilityGameTests::toggleSwitchesActive);
                test(registrar, "toggle_trident_auto_fill", AdjustabilityGameTests::toggleTridentAutoFill);
                test(registrar, "toggle_refuses_active", AdjustabilityGameTests::toggleRefusesActive);
                test(registrar, "toggle_cooldown", AdjustabilityGameTests::toggleCooldown);
                test(registrar, "toggle_network_checks_slot", AdjustabilityGameTests::toggleNetworkChecksSlot);
                test(registrar, "cycle_steps_held_item", AdjustabilityGameTests::cycleStepsHeldItem);
                test(registrar, "allowlist_mode", AdjustabilityGameTests::allowlistMode);
                test(registrar, "toggle_repairs_conflicting_active",
                        AdjustabilityGameTests::toggleRepairsConflictingActive);
                test(registrar, "toggle_keeps_repair_cost", AdjustabilityGameTests::toggleKeepsRepairCost);
                test(registrar, "anvil_caps_merged_level", AdjustabilityGameTests::anvilCapsMergedLevel);
                test(registrar, "grindstone_dormant_experience", AdjustabilityGameTests::grindstoneDormantExperience);
                test(registrar, "exclusivity_server_rules", AdjustabilityGameTests::exclusivityServerRules);
                test(registrar, "group_costs", AdjustabilityGameTests::groupCosts);
                test(registrar, "pay_costs", AdjustabilityGameTests::payCosts);
                test(registrar, "docs_give_example", AdjustabilityGameTests::docsGiveExample);
                test(registrar, "api_add_and_read", AdjustabilityGameTests::apiAddAndRead);
                test(registrar, "api_listener_switch", AdjustabilityGameTests::apiListenerSwitch);
                test(registrar, "api_listener_anvil", AdjustabilityGameTests::apiListenerAnvil);
                test(registrar, "api_listener_that_throws", AdjustabilityGameTests::apiListenerThatThrows);
                test(registrar, "api_adjustable_item", AdjustabilityGameTests::apiAdjustableItem);
                test(registrar, "none_switch", AdjustabilityGameTests::noneSwitch);
                test(registrar, "none_not_allowed", AdjustabilityGameTests::noneNotAllowed);
                test(registrar, "none_survives_anvil", AdjustabilityGameTests::noneSurvivesAnvil);
                test(registrar, "cycle_through_none", AdjustabilityGameTests::cycleThroughNone);
                test(registrar, "advancements", AdjustabilityGameTests::advancements);
                test(registrar, "tooltip_order", AdjustabilityGameTests::tooltipOrder);
                test(registrar, "generated_docs", ReferenceDocs::generatedDocs);
                test(registrar, "hand_written_docs", ReferenceDocs::handWrittenDocs);
        }

        /** Fortune pickaxe + Silk Touch book: Fortune stays active, Silk Touch is dormant, vanilla price + 1. */
        private static void anvilBookAddsDormant(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                AnvilMenu anvil = anvil(helper);
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(enchanted(Items.DIAMOND_PICKAXE, fortune, 3));
                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(silkTouch, 1));

                ItemStack result = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                helper.assertFalse(result.isEmpty(), "the anvil gives a result");
                helper.assertValueEqual(Adjuster.activeOf(result).getLevel(fortune), 3, "active Fortune");
                helper.assertValueEqual(Adjuster.activeOf(result).getLevel(silkTouch), 0, "active Silk Touch");
                helper.assertValueEqual(dormant(helper, result).getLevel(silkTouch), 1, "dormant Silk Touch");
                // Silk Touch's anvil cost is 8, halved for a book; plus dormant_cost_per_level (1) × level 1.
                helper.assertValueEqual(anvil.getCost(), 5, "price");
                helper.succeed();
        }

        /** No conflict: exactly vanilla, and no custom_data on the result. */
        private static void anvilVanillaMergeUnchanged(GameTestHelper helper)
        {
                Holder<Enchantment> efficiency = enchantment(helper, Enchantments.EFFICIENCY);
                Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
                AnvilMenu anvil = anvil(helper);
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(enchanted(Items.DIAMOND_PICKAXE, efficiency, 5));
                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(unbreaking, 3));

                ItemStack result = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                helper.assertValueEqual(Adjuster.activeOf(result).size(), 2, "active enchantments");
                helper.assertFalse(result.has(DataComponents.CUSTOM_DATA), "no custom_data on a vanilla merge");
                helper.succeed();
        }

        /** Fortune book + Silk Touch book: one book that stores both. */
        private static void anvilBooksCombine(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                AnvilMenu anvil = anvil(helper);
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(book(fortune, 3));
                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(silkTouch, 1));

                ItemStack result = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                ItemEnchantments stored = result.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
                        ItemEnchantments.EMPTY);
                helper.assertTrue(result.is(Items.ENCHANTED_BOOK), "the result is a book");
                helper.assertValueEqual(stored.getLevel(fortune), 3, "stored Fortune");
                helper.assertValueEqual(stored.getLevel(silkTouch), 1, "stored Silk Touch");
                helper.assertFalse(DormantEnchantments.has(result), "books have nothing dormant");
                helper.succeed();
        }

        /** A book holding Fortune and Silk Touch on a plain pickaxe: one active, the other dormant. */
        private static void anvilBookWithConflictsSplits(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack book = book(fortune, 3);
                ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(
                        book.get(DataComponents.STORED_ENCHANTMENTS));
                stored.set(silkTouch, 1);
                book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
                AnvilMenu anvil = anvil(helper);
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(new ItemStack(Items.DIAMOND_PICKAXE));
                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book);

                ItemStack result = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                // Nothing was active before, so id order decides: minecraft:fortune before minecraft:silk_touch.
                helper.assertValueEqual(Adjuster.activeOf(result).getLevel(fortune), 3, "active Fortune");
                helper.assertValueEqual(dormant(helper, result).getLevel(silkTouch), 1, "dormant Silk Touch");
                helper.assertValueEqual(Adjuster.activeOf(result).size() + dormant(helper, result).size(), 2,
                        "both kept");
                helper.succeed();
        }

        /** Renaming an item with a dormant enchantment keeps it, at vanilla's renaming price. */
        private static void anvilRenameKeepsDormant(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                AnvilMenu anvil = anvil(helper);
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(pickaxe);
                anvil.setItemName("Both");

                ItemStack result = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                helper.assertValueEqual(Adjuster.activeOf(result).getLevel(fortune), 3, "active Fortune");
                helper.assertValueEqual(dormant(helper, result).getLevel(silkTouch), 1, "dormant Silk Touch");
                helper.assertValueEqual(anvil.getCost(), 1, "renaming price");
                helper.succeed();
        }

        /** The grindstone removes dormant enchantments with the active ones. */
        private static void grindstoneRemovesDormant(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                GrindstoneMenu grindstone = new GrindstoneMenu(0, survivalPlayer(helper).getInventory());
                grindstone.getSlot(GrindstoneMenu.INPUT_SLOT).set(pickaxe);

                ItemStack result = grindstone.getSlot(GrindstoneMenu.RESULT_SLOT).getItem();
                helper.assertFalse(result.isEmpty(), "the grindstone gives a result");
                helper.assertTrue(Adjuster.activeOf(result).isEmpty(), "no active enchantments");
                helper.assertFalse(DormantEnchantments.has(result), "no dormant enchantments");
                helper.succeed();
        }

        /** Making a dormant enchantment active turns off what it conflicts with. */
        private static void toggleSwitchesActive(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));

                SwitchResult outcome = Toggler.activate(survivalServerPlayer(helper), pickaxe, silkTouch,
                        SwitchSource.API);

                helper.assertValueEqual(outcome, SwitchResult.DONE, "outcome");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(silkTouch), 1, "active Silk Touch");
                helper.assertValueEqual(dormant(helper, pickaxe).getLevel(fortune), 3, "dormant Fortune, level kept");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(fortune), 0, "active Fortune");
                helper.succeed();
        }

        /** Riptide to Loyalty also brings Channeling back, which doesn't conflict with Loyalty. */
        private static void toggleTridentAutoFill(GameTestHelper helper)
        {
                Holder<Enchantment> riptide = enchantment(helper, Enchantments.RIPTIDE);
                Holder<Enchantment> loyalty = enchantment(helper, Enchantments.LOYALTY);
                Holder<Enchantment> channeling = enchantment(helper, Enchantments.CHANNELING);
                ItemStack trident = enchanted(Items.TRIDENT, riptide, 3);
                ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                dormant.set(loyalty, 3);
                dormant.set(channeling, 1);
                DormantEnchantments.set(trident, dormant.toImmutable());

                Toggler.activate(survivalServerPlayer(helper), trident, loyalty, SwitchSource.API);

                ItemEnchantments active = Adjuster.activeOf(trident);
                helper.assertValueEqual(active.getLevel(loyalty), 3, "active Loyalty");
                helper.assertValueEqual(active.getLevel(channeling), 1, "active Channeling");
                helper.assertValueEqual(dormant(helper, trident).getLevel(riptide), 3, "dormant Riptide");
                helper.succeed();
        }

        /** An enchantment that's already active can't be switched to. */
        private static void toggleRefusesActive(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));

                helper.assertValueEqual(Toggler.activate(survivalServerPlayer(helper), pickaxe, fortune,
                                SwitchSource.API),
                        SwitchResult.NOT_DORMANT, "outcome");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(fortune), 3, "Fortune still active");
                helper.succeed();
        }

        /** A survival player waits cooldown_ticks (default 20) between switches. */
        private static void toggleCooldown(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                ServerPlayer player = survivalServerPlayer(helper);

                helper.assertValueEqual(Toggler.activate(player, pickaxe, silkTouch, SwitchSource.API),
                        SwitchResult.DONE, "first switch");
                helper.assertValueEqual(Toggler.activate(player, pickaxe, fortune, SwitchSource.API),
                        SwitchResult.COOLDOWN, "second switch");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(silkTouch), 1, "still Silk Touch");
                helper.succeed();
        }

        /** The payload's slot is checked: a real inventory slot works, the crafting result slot is refused. */
        // A connected mock player (for messages); vanilla marks the helper for removal but has no replacement yet.
        @SuppressWarnings("removal")
        private static void toggleNetworkChecksSlot(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                player.getInventory().setItem(0, pickaxe);
                int containerId = player.inventoryMenu.containerId;
                ResourceKey<Enchantment> silkKey = silkTouch.unwrapKey().orElseThrow();

                ToggleNetwork.onToggle(player, new ToggleEnchantmentPayload(containerId, InventoryMenu.RESULT_SLOT,
                        silkKey, false));
                ToggleNetwork.onToggle(player, new ToggleEnchantmentPayload(containerId + 1, HOTBAR_0, silkKey, false));
                helper.assertValueEqual(Adjuster.activeOf(player.getInventory().getItem(0)).getLevel(fortune), 3,
                        "wrong slot or menu: nothing changes");

                ToggleNetwork.onToggle(player, new ToggleEnchantmentPayload(containerId, HOTBAR_0, silkKey, false));
                helper.assertValueEqual(Adjuster.activeOf(player.getInventory().getItem(0)).getLevel(silkTouch), 1,
                        "hotbar slot: switched");
                helper.succeed();
        }

        /** The Cycle key steps the held item to its next choice and back. */
        // A connected mock player (for messages); vanilla marks the helper for removal but has no replacement yet.
        @SuppressWarnings("removal")
        private static void cycleStepsHeldItem(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

                ToggleNetwork.onCycle(player, new CycleEnchantmentPayload(0));
                helper.assertValueEqual(Adjuster.activeOf(player.getMainHandItem()).getLevel(silkTouch), 1,
                        "first press: Silk Touch");
                ToggleNetwork.onCycle(player, new CycleEnchantmentPayload(0));
                helper.assertValueEqual(Adjuster.activeOf(player.getMainHandItem()).getLevel(fortune), 3,
                        "second press: back to Fortune");
                helper.succeed();
        }

        /** ALLOWLIST mode only takes items in #adjustability:adjustable, which is empty by default. */
        private static void allowlistMode(GameTestHelper helper)
        {
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                helper.assertTrue(ItemRules.canAdjust(pickaxe, ItemMode.DENYLIST), "DENYLIST: a pickaxe takes part");
                helper.assertFalse(ItemRules.canAdjust(pickaxe, ItemMode.ALLOWLIST),
                        "ALLOWLIST: a pickaxe not in the tag doesn't");
                helper.assertFalse(ItemRules.canAdjust(ItemStack.EMPTY, ItemMode.DENYLIST), "never an empty slot");
                helper.succeed();
        }

        /**
         * An item with two conflicting active enchantments (/give, another mod) is repaired by the first switch,
         * without losing either: what's asked for ends up active, the other dormant.
         */
        private static void toggleRepairsConflictingActive(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemEnchantments.Mutable both = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                both.set(fortune, 3);
                both.set(silkTouch, 1);

                ItemStack toSilk = new ItemStack(Items.DIAMOND_PICKAXE);
                toSilk.set(DataComponents.ENCHANTMENTS, both.toImmutable());
                helper.assertValueEqual(Toggler.activate(survivalServerPlayer(helper), toSilk, silkTouch,
                                SwitchSource.API),
                        SwitchResult.DONE, "switch to Silk Touch");
                helper.assertValueEqual(Adjuster.activeOf(toSilk).getLevel(silkTouch), 1, "active Silk Touch");
                helper.assertValueEqual(Adjuster.activeOf(toSilk).getLevel(fortune), 0, "Fortune no longer active");
                helper.assertValueEqual(dormant(helper, toSilk).getLevel(fortune), 3, "Fortune kept, dormant");

                ItemStack toFortune = new ItemStack(Items.DIAMOND_PICKAXE);
                toFortune.set(DataComponents.ENCHANTMENTS, both.toImmutable());
                helper.assertValueEqual(Toggler.activate(survivalServerPlayer(helper), toFortune, fortune,
                                SwitchSource.API),
                        SwitchResult.DONE, "asking for the one that stays active repairs too");
                helper.assertValueEqual(Adjuster.activeOf(toFortune).getLevel(fortune), 3, "active Fortune");
                helper.assertValueEqual(dormant(helper, toFortune).getLevel(silkTouch), 1, "Silk Touch kept, dormant");
                helper.succeed();
        }

        /** Switching never touches the anvil's prior-work penalty. */
        private static void toggleKeepsRepairCost(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                pickaxe.set(DataComponents.REPAIR_COST, 7);

                Toggler.activate(survivalServerPlayer(helper), pickaxe, silkTouch, SwitchSource.API);

                helper.assertValueEqual(pickaxe.getOrDefault(DataComponents.REPAIR_COST, 0), 7, "repair cost");
                helper.succeed();
        }

        /** Merging into a dormant set never goes past an enchantment's max level. */
        private static void anvilCapsMergedLevel(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, silkTouch, 1);
                DormantEnchantments.set(pickaxe, single(fortune, 3));
                AnvilMenu anvil = anvil(helper);
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(pickaxe);
                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(fortune, 3));

                ItemStack result = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                int levels = Adjuster.activeOf(result).getLevel(fortune) + dormant(helper, result).getLevel(fortune);
                helper.assertValueEqual(levels, 3, "Fortune level, active or dormant");
                helper.succeed();
        }

        /** The grindstone pays experience for dormant enchantments too, like for active ones. */
        private static void grindstoneDormantExperience(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                helper.assertValueEqual(GrindstoneRules.dormantExperience(pickaxe, helper.getLevel().registryAccess()),
                        silkTouch.value().getMinCost(1), "experience for dormant Silk Touch");

                // End to end through the result slot's own experience code (private, in an anonymous class, so by
                // reflection). Vanilla pays a random amount between half and all of the sum: Fortune III alone
                // averages about 25, with Silk Touch about 36. 40 samples tell them apart with a wide margin.
                Player player = survivalPlayer(helper);
                GrindstoneMenu grindstone = new GrindstoneMenu(0, player.getInventory());
                grindstone.getSlot(GrindstoneMenu.INPUT_SLOT).set(pickaxe);
                Slot result = grindstone.getSlot(GrindstoneMenu.RESULT_SLOT);
                int samples = 40;
                int total = 0;
                try
                {
                        Method experience = result.getClass().getDeclaredMethod("getExperienceAmount", Level.class);
                        experience.setAccessible(true);
                        for (int i = 0; i < samples; i++)
                        {
                                total += (int) experience.invoke(result, helper.getLevel());
                        }
                }
                catch (ReflectiveOperationException exception)
                {
                        throw new IllegalStateException("The grindstone result slot changed", exception);
                }
                double average = (double) total / samples;
                helper.assertTrue(average > 30.5, "average experience " + average + " should include Silk Touch's");
                helper.succeed();
        }

        /** Extra groups, never-conflicting pairs, tags as groups, and turning vanilla's exclusive sets off. */
        private static void exclusivityServerRules(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
                Holder<Enchantment> fireAspect = enchantment(helper, Enchantments.FIRE_ASPECT);
                Holder<Enchantment> infinity = enchantment(helper, Enchantments.INFINITY);
                Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);

                ExclusivityConfig vanilla = new ExclusivityConfig(true, List.of(), List.of(), List.of());
                helper.assertTrue(Exclusivity.conflict(fortune, silkTouch, vanilla), "vanilla: Fortune / Silk Touch");
                helper.assertFalse(Exclusivity.conflict(sharpness, fireAspect, vanilla), "vanilla: Sharpness + Fire");

                ExclusivityConfig custom = new ExclusivityConfig(true,
                        List.of("minecraft:sharpness, minecraft:fire_aspect"),
                        List.of("minecraft:infinity, minecraft:mending"), List.of());
                helper.assertTrue(Exclusivity.conflict(fireAspect, sharpness, custom), "extra group conflicts");
                helper.assertFalse(Exclusivity.conflict(infinity, mending, custom), "compatible overrides vanilla");
                helper.assertTrue(Exclusivity.conflict(fortune, silkTouch, custom), "vanilla still applies");

                ExclusivityConfig noVanilla = new ExclusivityConfig(false, List.of(), List.of(), List.of());
                helper.assertFalse(Exclusivity.conflict(fortune, silkTouch, noVanilla), "vanilla off");
                ExclusivityConfig tagGroup = new ExclusivityConfig(false, List.of("#minecraft:exclusive_set/mining"),
                        List.of(), List.of());
                helper.assertTrue(Exclusivity.conflict(fortune, silkTouch, tagGroup), "a single tag is a group");

                helper.assertFalse(Exclusivity.isValidGroup("minecraft:fortune"), "one id isn't a group");
                helper.assertFalse(Exclusivity.isValidGroup("minecraft:fortune, not an id!"), "bad id refused");
                helper.succeed();
        }

        /** group_costs: any cost type per group, the first matching entry wins, other groups use the default. */
        private static void groupCosts(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                Holder<Enchantment> riptide = enchantment(helper, Enchantments.RIPTIDE);
                Holder<Enchantment> loyalty = enchantment(helper, Enchantments.LOYALTY);
                Holder<Enchantment> protection = enchantment(helper, Enchantments.PROTECTION);
                Holder<Enchantment> fireProtection = enchantment(helper, Enchantments.FIRE_PROTECTION);
                ExclusivityConfig config = new ExclusivityConfig(true, List.of(), List.of(), List.of(
                        "#minecraft:exclusive_set/mining = ITEM 2 minecraft:lapis_lazuli",
                        "minecraft:riptide = XP_LEVELS 5",
                        "minecraft:loyalty = XP_POINTS 99"));

                helper.assertValueEqual(Exclusivity.groupCost(List.of(fortune, silkTouch), config),
                        Optional.of(new Cost(ToggleCost.ITEM, 2, Identifier.withDefaultNamespace("lapis_lazuli"))),
                        "mining group");
                helper.assertValueEqual(Exclusivity.groupCost(List.of(loyalty, riptide), config).map(Cost::type),
                        Optional.of(ToggleCost.XP_LEVELS), "trident group: the first entry that matches");
                helper.assertTrue(Exclusivity.groupCost(List.of(protection, fireProtection), config).isEmpty(),
                        "armor: no entry, the default cost applies");

                helper.assertTrue(Exclusivity.isValidCost("#minecraft:exclusive_set/armor = NONE"), "NONE");
                helper.assertTrue(Exclusivity.isValidCost("minecraft:riptide = durability 10"), "any case");
                helper.assertFalse(Exclusivity.isValidCost("minecraft:riptide = ITEM 2"), "ITEM needs an item");
                helper.assertFalse(Exclusivity.isValidCost("minecraft:riptide = GOLD 2"), "unknown cost type");
                helper.assertFalse(Exclusivity.isValidCost("minecraft:riptide"), "no cost");
                helper.succeed();
        }

        /** Paying takes the whole cost or nothing. */
        private static void payCosts(GameTestHelper helper)
        {
                ServerPlayer player = survivalServerPlayer(helper);
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                player.getInventory().setItem(5, new ItemStack(Items.LAPIS_LAZULI, 3));
                Cost twoLapis = new Cost(ToggleCost.ITEM, 2, Identifier.withDefaultNamespace("lapis_lazuli"));
                helper.assertTrue(Toggler.pay(player, pickaxe, twoLapis), "3 lapis pay for 2");
                helper.assertValueEqual(player.getInventory().getItem(5).getCount(), 1, "lapis left");
                helper.assertFalse(Toggler.pay(player, pickaxe, twoLapis), "1 lapis doesn't pay for 2");
                helper.assertValueEqual(player.getInventory().getItem(5).getCount(), 1, "nothing taken");

                player.experienceLevel = 5;
                Cost threeLevels = new Cost(ToggleCost.XP_LEVELS, 3, Identifier.withDefaultNamespace("air"));
                helper.assertTrue(Toggler.pay(player, pickaxe, threeLevels), "5 levels pay for 3");
                helper.assertValueEqual(player.experienceLevel, 2, "levels left");
                helper.assertFalse(Toggler.pay(player, pickaxe, threeLevels), "2 levels don't pay for 3");

                Cost durability = new Cost(ToggleCost.DURABILITY, 10, Identifier.withDefaultNamespace("air"));
                helper.assertTrue(Toggler.pay(player, pickaxe, durability), "durability");
                helper.assertValueEqual(pickaxe.getDamageValue(), 10, "damage taken");
                pickaxe.setDamageValue(pickaxe.getMaxDamage() - 5);
                helper.assertFalse(Toggler.pay(player, pickaxe, durability), "never breaks the item");
                helper.succeed();
        }

        /**
         * The /give example on the storage docs page (docs/guide/storage.md) parses and gives what the page says:
         * Fortune III active, Silk Touch dormant. Keep the two in sync.
         */
        private static void docsGiveExample(GameTestHelper helper)
        {
                String example = "minecraft:diamond_pickaxe[minecraft:enchantments={\"minecraft:fortune\":3},"
                        + "minecraft:custom_data={\"adjustability:dormant\":{\"minecraft:silk_touch\":1}}]";
                ItemStack stack;
                try
                {
                        stack = ItemArgument.item(CommandBuildContext.simple(helper.getLevel().registryAccess(),
                                        helper.getLevel().enabledFeatures()))
                                .parse(new StringReader(example)).createItemStack(1);
                }
                catch (CommandSyntaxException exception)
                {
                        throw new IllegalStateException("The documented /give example doesn't parse", exception);
                }
                helper.assertValueEqual(Adjuster.activeOf(stack).getLevel(enchantment(helper, Enchantments.FORTUNE)),
                        3, "active Fortune");
                helper.assertValueEqual(dormant(helper, stack).getLevel(enchantment(helper, Enchantments.SILK_TOUCH)),
                        1, "dormant Silk Touch");
                helper.succeed();
        }

        /** Reading and adding through the API; the docs' reward example. */
        private static void apiAddAndRead(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                Holder<Enchantment> efficiency = enchantment(helper, Enchantments.EFFICIENCY);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);

                helper.assertTrue(AdjustabilityApi.addEnchantment(pickaxe, silkTouch, 1,
                        helper.getLevel().registryAccess()), "added");
                helper.assertTrue(AdjustabilityApi.hasDormant(pickaxe), "Silk Touch went dormant");
                helper.assertValueEqual(AdjustabilityApi.dormantEnchantments(pickaxe,
                        helper.getLevel().registryAccess()).getLevel(silkTouch), 1, "dormant Silk Touch");
                AdjustabilityApi.addEnchantment(pickaxe, efficiency, 5, helper.getLevel().registryAccess());
                helper.assertValueEqual(AdjustabilityApi.activeEnchantments(pickaxe).getLevel(efficiency), 5,
                        "no conflict: active");
                helper.assertTrue(AdjustabilityApi.conflict(fortune, silkTouch), "conflict");

                ItemStack reward = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                helper.assertValueEqual(ApiExamples.giveAndSwitch(survivalServerPlayer(helper), reward, silkTouch),
                        SwitchResult.DONE, "docs example: give and switch");
                helper.assertValueEqual(AdjustabilityApi.activeEnchantments(reward).getLevel(silkTouch), 1,
                        "Silk Touch active");

                AdjustabilityApi.flatten(pickaxe);
                helper.assertFalse(AdjustabilityApi.hasDormant(pickaxe), "flattened");
                helper.succeed();
        }

        /** A listener refuses a switch (the docs' "not while burning" example) and hears about allowed ones. */
        private static void apiListenerSwitch(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                int[] switched = {0};
                AdjustabilityListener counter = new AdjustabilityListener()
                {
                        @Override
                        public void onSwitched(SwitchEvent event)
                        {
                                switched[0]++;
                        }
                };
                AdjustabilityApi.addListener(ApiExamples.NO_SWITCHING_WHILE_BURNING);
                AdjustabilityApi.addListener(counter);
                try
                {
                        ServerPlayer burning = survivalServerPlayer(helper);
                        burning.igniteForSeconds(5);
                        ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                        DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                        helper.assertValueEqual(AdjustabilityApi.activate(burning, pickaxe, silkTouch),
                                SwitchResult.CANCELLED, "refused while burning");
                        helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(fortune), 3, "nothing changed");
                        helper.assertValueEqual(switched[0], 0, "no onSwitched for a refused switch");

                        helper.assertValueEqual(AdjustabilityApi.activate(survivalServerPlayer(helper), pickaxe,
                                silkTouch), SwitchResult.DONE, "allowed when not burning");
                        helper.assertValueEqual(switched[0], 1, "onSwitched once");
                }
                finally
                {
                        AdjustabilityApi.removeListener(ApiExamples.NO_SWITCHING_WHILE_BURNING);
                        AdjustabilityApi.removeListener(counter);
                }
                helper.succeed();
        }

        /** A listener can refuse an anvil result (the docs' golden pickaxe example). */
        private static void apiListenerAnvil(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                AdjustabilityApi.addListener(ApiExamples.NO_ADJUSTABLE_GOLD);
                try
                {
                        AnvilMenu gold = anvil(helper);
                        gold.getSlot(AnvilMenu.INPUT_SLOT).set(enchanted(Items.GOLDEN_PICKAXE, fortune, 3));
                        gold.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(silkTouch, 1));
                        helper.assertTrue(gold.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(), "gold: refused");

                        AnvilMenu diamond = anvil(helper);
                        diamond.getSlot(AnvilMenu.INPUT_SLOT).set(enchanted(Items.DIAMOND_PICKAXE, fortune, 3));
                        diamond.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(silkTouch, 1));
                        helper.assertFalse(diamond.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                                "diamond: allowed");
                }
                finally
                {
                        AdjustabilityApi.removeListener(ApiExamples.NO_ADJUSTABLE_GOLD);
                }
                helper.succeed();
        }

        /** A listener that throws is logged and counts as allowing. */
        private static void apiListenerThatThrows(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                AdjustabilityListener broken = new AdjustabilityListener()
                {
                        @Override
                        public boolean allowSwitch(SwitchEvent event)
                        {
                                throw new IllegalStateException("a broken listener, on purpose (GameTest)");
                        }
                };
                AdjustabilityApi.addListener(broken);
                try
                {
                        ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                        DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                        helper.assertValueEqual(AdjustabilityApi.activate(survivalServerPlayer(helper), pickaxe,
                                silkTouch), SwitchResult.DONE, "switched anyway");
                }
                finally
                {
                        AdjustabilityApi.removeListener(broken);
                }
                helper.succeed();
        }

        /** An item's own rules through AdjustableItem: a limit of one, and none once renamed. */
        private static void apiAdjustableItem(GameTestHelper helper)
        {
                Item item = TestAdjustableItem.instance();
                if (item == null)
                {
                        helper.fail("The test item is only registered in the development environment");
                        return;
                }
                ItemStack stack = new ItemStack(item);
                helper.assertTrue(ItemRules.canAdjust(stack, ItemMode.DENYLIST), "takes part");
                helper.assertValueEqual(ItemRules.maxDormant(stack, 4), 1, "its limit is lower than the server's");
                helper.assertValueEqual(ItemRules.maxDormant(stack, 0), 1, "its limit with no server limit");
                helper.assertValueEqual(ItemRules.maxDormant(new ItemStack(Items.DIAMOND_PICKAXE), 0),
                        Integer.MAX_VALUE, "no limit at all for a vanilla item");
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed"));
                helper.assertFalse(AdjustabilityApi.canHoldDormant(stack), "renamed: the item says no");
                helper.succeed();
        }

        /** With allow_none: a group can be left with nothing active, and switched back on. */
        // A connected mock player; vanilla marks the helper for removal but has no replacement yet.
        @SuppressWarnings("removal")
        private static void noneSwitch(GameTestHelper helper)
        {
                withAllowNone(() ->
                {
                        Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                        Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                        ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                        DormantEnchantments.set(pickaxe, single(silkTouch, 1));

                        helper.assertValueEqual(Toggler.deactivate(helper.makeMockServerPlayerInLevel(), pickaxe,
                                silkTouch, SwitchSource.API), SwitchResult.DONE, "none");
                        helper.assertTrue(Adjuster.activeOf(pickaxe).isEmpty(), "nothing active");
                        helper.assertValueEqual(dormant(helper, pickaxe).size(), 2, "both kept, dormant");

                        helper.assertValueEqual(Toggler.activate(helper.makeMockServerPlayerInLevel(), pickaxe,
                                silkTouch, SwitchSource.API), SwitchResult.DONE, "back on");
                        helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(silkTouch), 1, "Silk Touch");
                        helper.assertValueEqual(dormant(helper, pickaxe).getLevel(fortune), 3, "Fortune dormant");
                });
                helper.succeed();
        }

        /** Without allow_none (the default), none is refused. */
        private static void noneNotAllowed(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                helper.assertValueEqual(Toggler.deactivate(survivalServerPlayer(helper), pickaxe, fortune,
                        SwitchSource.API), SwitchResult.NONE_NOT_ALLOWED, "refused");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(fortune), 3, "Fortune still active");
                helper.succeed();
        }

        /** With allow_none, an anvil keeps a group left on none that way while adding something else. */
        private static void noneSurvivesAnvil(GameTestHelper helper)
        {
                withAllowNone(() ->
                {
                        Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                        Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                        Holder<Enchantment> efficiency = enchantment(helper, Enchantments.EFFICIENCY);
                        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                        ItemEnchantments.Mutable both = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        both.set(fortune, 3);
                        both.set(silkTouch, 1);
                        DormantEnchantments.set(pickaxe, both.toImmutable());
                        AnvilMenu anvil = anvil(helper);
                        anvil.getSlot(AnvilMenu.INPUT_SLOT).set(pickaxe);
                        anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(efficiency, 5));

                        ItemStack result = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                        helper.assertValueEqual(Adjuster.activeOf(result).getLevel(efficiency), 5, "new: active");
                        helper.assertValueEqual(Adjuster.activeOf(result).getLevel(fortune)
                                + Adjuster.activeOf(result).getLevel(silkTouch), 0, "mining group still on none");
                        helper.assertValueEqual(dormant(helper, result).size(), 2, "both still dormant");
                });
                helper.succeed();
        }

        /** With allow_none, the Cycle key goes Fortune, Silk Touch, none, Fortune. */
        // A connected mock player; vanilla marks the helper for removal but has no replacement yet.
        @SuppressWarnings("removal")
        private static void cycleThroughNone(GameTestHelper helper)
        {
                withAllowNone(() ->
                {
                        Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                        Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                        ServerPlayer player = helper.makeMockServerPlayerInLevel();
                        ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, silkTouch, 1);
                        DormantEnchantments.set(pickaxe, single(fortune, 3));
                        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

                        ToggleNetwork.onCycle(player, new CycleEnchantmentPayload(0));
                        helper.assertTrue(Adjuster.activeOf(player.getMainHandItem()).isEmpty(),
                                "after the last choice: none");
                        ToggleNetwork.onCycle(player, new CycleEnchantmentPayload(0));
                        helper.assertValueEqual(Adjuster.activeOf(player.getMainHandItem()).getLevel(fortune), 3,
                                "after none: the first choice");
                });
                helper.succeed();
        }

        /**
         * The advancement tab loads, holding an item with a dormant enchantment unlocks its root, and a switch on a
         * tool with Fortune and Silk Touch unlocks "Change of Plans" and "Best of Both" and counts the statistic.
         */
        @SuppressWarnings("removal")
        private static void advancements(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
                for (String name : List.of("root", "change_of_plans", "best_of_both", "every_angle_covered",
                        "indecisive", "neither"))
                {
                        helper.assertTrue(manager.get(Constants.id(name)) != null, "advancement " + name + " loads");
                }

                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, fortune, 3);
                DormantEnchantments.set(pickaxe, single(silkTouch, 1));
                player.getInventory().setItem(0, pickaxe);
                player.inventoryMenu.broadcastChanges();
                helper.assertTrue(done(player, manager, "root"), "root: holding a dormant enchantment");
                helper.assertFalse(done(player, manager, "change_of_plans"), "no switch yet");

                Toggler.activate(player, player.getInventory().getItem(0), silkTouch, SwitchSource.API);
                helper.assertTrue(done(player, manager, "change_of_plans"), "change of plans");
                helper.assertTrue(done(player, manager, "best_of_both"), "best of both");
                helper.assertFalse(done(player, manager, "indecisive"), "not yet 100 switches");
                helper.assertValueEqual(player.getStats().getValue(Stats.CUSTOM.get(ModStats.SWITCHES)), 1,
                        "switches statistic");
                helper.succeed();
        }

        /**
         * Each dormant enchantment is listed right under the active one it conflicts with: Silk Touch under Fortune
         * (not after Unbreaking), Fire Protection under Protection; a book's stored enchantments get no dormant lines.
         */
        private static void tooltipOrder(GameTestHelper helper)
        {
                ItemStack pickaxe = enchanted(Items.DIAMOND_PICKAXE, enchantment(helper, Enchantments.FORTUNE), 3);
                pickaxe.enchant(enchantment(helper, Enchantments.EFFICIENCY), 5);
                pickaxe.enchant(enchantment(helper, Enchantments.UNBREAKING), 3);
                DormantEnchantments.set(pickaxe, single(enchantment(helper, Enchantments.SILK_TOUCH), 1));
                List<String> lines = tooltip(helper, pickaxe);
                int fortune = lines.indexOf("Fortune III");
                helper.assertTrue(fortune >= 0 && lines.size() > fortune + 1, "Fortune listed: " + lines);
                helper.assertValueEqual(lines.get(fortune + 1), "  ⇄ Silk Touch", "Silk Touch under Fortune: " + lines);
                helper.assertValueEqual(lines.stream().filter(line -> line.contains("Silk Touch")).count(), 1L,
                        "listed once");

                ItemStack boots = enchanted(Items.DIAMOND_BOOTS, enchantment(helper, Enchantments.PROTECTION), 4);
                boots.enchant(enchantment(helper, Enchantments.FEATHER_FALLING), 4);
                DormantEnchantments.set(boots, single(enchantment(helper, Enchantments.FIRE_PROTECTION), 4));
                lines = tooltip(helper, boots);
                int protection = lines.indexOf("Protection IV");
                helper.assertTrue(protection >= 0, "Protection listed: " + lines);
                helper.assertValueEqual(lines.get(protection + 1), "  ⇄ Fire Protection IV", "under Protection: " + lines);

                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                book.set(DataComponents.STORED_ENCHANTMENTS, single(enchantment(helper, Enchantments.FORTUNE), 3));
                DormantEnchantments.set(book, single(enchantment(helper, Enchantments.SILK_TOUCH), 1));
                lines = tooltip(helper, book);
                helper.assertValueEqual(lines.stream().filter(line -> line.contains("Silk Touch")).count(), 1L,
                        "book lists Silk Touch once: " + lines);
                helper.succeed();
        }

        private static List<String> tooltip(GameTestHelper helper, ItemStack stack)
        {
                return stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL).stream()
                        .map(Component::getString).toList();
        }

        private static boolean done(ServerPlayer player, ServerAdvancementManager manager, String name)
        {
                return player.getAdvancements().getOrStartProgress(manager.get(Constants.id(name))).isDone();
        }

        /** Runs {@code body} with {@code [toggle] allow_none} on, as an admin would set it, and resets it after. */
        @SuppressWarnings("unchecked")
        private static void withAllowNone(Runnable body)
        {
                ConfigValue<Boolean> allowNone = (ConfigValue<Boolean>) CreatorConfig.SPEC.getValues()
                        .get("toggle.allow_none");
                boolean before = allowNone.get();
                allowNone.set(true);
                try
                {
                        body.run();
                }
                finally
                {
                        allowNone.set(before);
                }
        }

        private static ServerPlayer survivalServerPlayer(GameTestHelper helper)
        {
                return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        }

        private static void test(ModRegistries.Registrar registrar, String name, Consumer<GameTestHelper> function)
        {
                registrar.register(Registries.TEST_FUNCTION, Constants.id(name), () -> function);
        }

        private static AnvilMenu anvil(GameTestHelper helper)
        {
                return new AnvilMenu(0, survivalPlayer(helper).getInventory());
        }

        private static Player survivalPlayer(GameTestHelper helper)
        {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.experienceLevel = 30;
                return player;
        }

        private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key)
        {
                return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }

        private static ItemEnchantments dormant(GameTestHelper helper, ItemStack stack)
        {
                return DormantEnchantments.get(stack, helper.getLevel().registryAccess());
        }

        private static ItemEnchantments single(Holder<Enchantment> enchantment, int level)
        {
                ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                enchantments.set(enchantment, level);
                return enchantments.toImmutable();
        }

        private static ItemStack enchanted(Item item, Holder<Enchantment> enchantment,
                int level)
        {
                ItemStack stack = new ItemStack(item);
                stack.set(DataComponents.ENCHANTMENTS, single(enchantment, level));
                return stack;
        }

        private static ItemStack book(Holder<Enchantment> enchantment, int level)
        {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                book.set(DataComponents.STORED_ENCHANTMENTS, single(enchantment, level));
                return book;
        }
}
