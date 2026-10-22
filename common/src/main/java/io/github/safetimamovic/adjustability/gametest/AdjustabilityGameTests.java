package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.ModRegistries;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;

/**
 * In-game tests for the anvil and grindstone rules, run by vanilla's GameTest framework on both loaders. Each test is a
 * function registered here plus a test instance in {@code data/adjustability/test_instance/} pointing at it. They drive
 * the real menus with a mock survival player, so the mixins run exactly as in game.
 */
public final class AdjustabilityGameTests
{
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
