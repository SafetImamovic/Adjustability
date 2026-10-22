package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.ModRegistries;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ItemMode;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.GrindstoneRules;
import io.github.safetimamovic.adjustability.core.ItemRules;
import io.github.safetimamovic.adjustability.core.Toggler;
import io.github.safetimamovic.adjustability.core.Toggler.Outcome;
import io.github.safetimamovic.adjustability.network.CycleEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.ToggleEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.ToggleNetwork;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

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

                Outcome outcome = Toggler.activate(survivalServerPlayer(helper), pickaxe, silkTouch);

                helper.assertValueEqual(outcome, Outcome.DONE, "outcome");
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

                Toggler.activate(survivalServerPlayer(helper), trident, loyalty);

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

                helper.assertValueEqual(Toggler.activate(survivalServerPlayer(helper), pickaxe, fortune),
                        Outcome.NOT_DORMANT, "outcome");
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

                helper.assertValueEqual(Toggler.activate(player, pickaxe, silkTouch), Outcome.DONE, "first switch");
                helper.assertValueEqual(Toggler.activate(player, pickaxe, fortune), Outcome.COOLDOWN, "second switch");
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
                        silkKey));
                ToggleNetwork.onToggle(player, new ToggleEnchantmentPayload(containerId + 1, HOTBAR_0, silkKey));
                helper.assertValueEqual(Adjuster.activeOf(player.getInventory().getItem(0)).getLevel(fortune), 3,
                        "wrong slot or menu: nothing changes");

                ToggleNetwork.onToggle(player, new ToggleEnchantmentPayload(containerId, HOTBAR_0, silkKey));
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
                helper.assertValueEqual(Toggler.activate(survivalServerPlayer(helper), toSilk, silkTouch),
                        Outcome.DONE, "switch to Silk Touch");
                helper.assertValueEqual(Adjuster.activeOf(toSilk).getLevel(silkTouch), 1, "active Silk Touch");
                helper.assertValueEqual(Adjuster.activeOf(toSilk).getLevel(fortune), 0, "Fortune no longer active");
                helper.assertValueEqual(dormant(helper, toSilk).getLevel(fortune), 3, "Fortune kept, dormant");

                ItemStack toFortune = new ItemStack(Items.DIAMOND_PICKAXE);
                toFortune.set(DataComponents.ENCHANTMENTS, both.toImmutable());
                helper.assertValueEqual(Toggler.activate(survivalServerPlayer(helper), toFortune, fortune),
                        Outcome.DONE, "asking for the one that stays active repairs too");
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

                Toggler.activate(survivalServerPlayer(helper), pickaxe, silkTouch);

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

        private static ServerPlayer survivalServerPlayer(GameTestHelper helper)
        {
                return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        }

        private static void test(ModRegistries.Registrar registrar, String name, Consumer<GameTestHelper> function)
        {
                registrar.register(Registries.TEST_FUNCTION, Constants.id(name), function);
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
