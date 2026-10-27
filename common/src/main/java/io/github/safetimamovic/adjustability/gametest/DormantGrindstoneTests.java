package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.GrindstoneRules;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/** Dormant-only stacks must reach the grindstone's removal hook under the server's policy. */
final class DormantGrindstoneTests
{
        private DormantGrindstoneTests()
        {
        }

        static void allDisabled(GameTestHelper helper)
        {
                configured(true, () ->
                {
                        for (ItemStack input : disabledItems(helper))
                        {
                                for (int slot = 0; slot < 2; slot++)
                                {
                                        GrindstoneMenu menu = menu(helper);
                                        menu.getSlot(slot).set(input.copy());
                                        ItemStack result = menu.getSlot(GrindstoneMenu.RESULT_SLOT).getItem();
                                        helper.assertFalse(result.isEmpty(), "disabled enchantments produce a result: "
                                                + input.getItem() + ", input slot " + slot);
                                        helper.assertTrue(result.is(input.getItem()), "the item survives");
                                        helper.assertTrue(Adjuster.activeOf(result).isEmpty(), "nothing active left");
                                        helper.assertFalse(DormantEnchantments.has(result), "nothing dormant left");
                                        helper.assertTrue(DormantEnchantments.off(result,
                                                helper.getLevel().registryAccess()).isEmpty(), "off flags cleared");
                                        helper.assertTrue(DormantEnchantments.has(menu.getSlot(slot).getItem()),
                                                "preview preserves the input");
                                        helper.assertTrue(GrindstoneRules.dormantExperience(input,
                                                helper.getLevel().registryAccess()) > 0,
                                                "removed enchantments give XP");
                                }
                        }
                });
                helper.succeed();
        }

        static void policy(GameTestHelper helper)
        {
                configured(false, () ->
                {
                        for (ItemStack input : disabledItems(helper))
                        {
                                GrindstoneMenu menu = menu(helper);
                                menu.getSlot(GrindstoneMenu.INPUT_SLOT).set(input);
                                helper.assertTrue(menu.getSlot(GrindstoneMenu.RESULT_SLOT).getItem().isEmpty(),
                                        "remove_dormant=false: a dormant-only item has nothing to remove");
                                helper.assertTrue(DormantEnchantments.has(input), "dormant enchantments stay");
                                helper.assertValueEqual(GrindstoneRules.dormantExperience(input,
                                        helper.getLevel().registryAccess()), 0, "no XP for kept enchantments");
                        }
                });
                for (boolean remove : List.of(false, true))
                {
                        configured(remove, () ->
                        {
                                Holder<Enchantment> curse = enchantment(helper, Enchantments.VANISHING_CURSE);
                                ItemStack input = disabledItems(helper).getLast();
                                input.enchant(curse, 1);
                                GrindstoneMenu menu = menu(helper);
                                menu.getSlot(GrindstoneMenu.INPUT_SLOT).set(input);
                                ItemStack result = menu.getSlot(GrindstoneMenu.RESULT_SLOT).getItem();
                                helper.assertFalse(result.isEmpty(), "a curse-bearing item still produces a result");
                                helper.assertValueEqual(Adjuster.activeOf(result).getLevel(curse), 1, "curse stays");
                                helper.assertValueEqual(DormantEnchantments.has(result), !remove,
                                        "server policy applies to disabled enchantments");
                        });
                }
                helper.succeed();
        }

        static void inputSlots(GameTestHelper helper)
        {
                configured(true, () ->
                {
                        ItemStack off = new ItemStack(Items.STICK);
                        Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);
                        ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        dormant.set(mending, 1);
                        DormantEnchantments.set(off, dormant.toImmutable(), Set.of(mending),
                                helper.getLevel().registryAccess());
                        ItemStack unknown = new ItemStack(Items.DIAMOND);
                        DormantEnchantments.addUnknown(unknown, Map.of("adjustability_test:gone_grindstone", 3),
                                helper.getLevel().registryAccess());
                        for (ItemStack input : List.of(off, unknown))
                        {
                                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                                GrindstoneMenu menu = new GrindstoneMenu(0, player.getInventory());
                                for (int slot = 0; slot < 2; slot++)
                                {
                                        helper.assertTrue(menu.getSlot(slot).mayPlace(input),
                                                "both inputs accept dormant-only non-damageable items");
                                        helper.assertFalse(menu.getSlot(slot).mayPlace(new ItemStack(Items.STICK)),
                                                "plain items remain ineligible");
                                }
                                player.getInventory().setItem(0, input);
                                int source = menu.slots.stream().filter(slot -> slot.getItem() == input)
                                        .findFirst().orElseThrow().index;
                                menu.quickMoveStack(player, source);
                                helper.assertTrue(menu.getSlot(GrindstoneMenu.INPUT_SLOT).hasItem(),
                                        "quick move enters the grindstone");
                                ItemStack result = menu.getSlot(GrindstoneMenu.RESULT_SLOT).getItem();
                                helper.assertFalse(result.isEmpty(), "a removal result is available");
                                helper.assertFalse(DormantEnchantments.has(result), "all dormant entries cleared");
                        }
                        helper.assertValueEqual(GrindstoneRules.dormantExperience(unknown,
                                helper.getLevel().registryAccess()), 0, "unknown enchantments give no XP");
                });
                helper.succeed();
        }

        private static List<ItemStack> disabledItems(GameTestHelper helper)
        {
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                for (ResourceKey<Enchantment> key : List.of(Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION,
                        Enchantments.BLAST_PROTECTION, Enchantments.PROJECTILE_PROTECTION,
                        Enchantments.DEPTH_STRIDER, Enchantments.FROST_WALKER))
                {
                        dormant.set(enchantment(helper, key), 1);
                }
                DormantEnchantments.set(boots, dormant.toImmutable(), helper.getLevel().registryAccess());
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                Holder<Enchantment> silk = enchantment(helper, Enchantments.SILK_TOUCH);
                dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                dormant.set(silk, 1);
                dormant.set(enchantment(helper, Enchantments.FORTUNE), 3);
                DormantEnchantments.set(pickaxe, dormant.toImmutable(), helper.getLevel().registryAccess());
                ItemStack single = new ItemStack(Items.DIAMOND_PICKAXE);
                dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                dormant.set(silk, 1);
                DormantEnchantments.set(single, dormant.toImmutable(), Set.of(silk),
                        helper.getLevel().registryAccess());
                return List.of(boots, pickaxe, single);
        }

        private static GrindstoneMenu menu(GameTestHelper helper)
        {
                return new GrindstoneMenu(0, helper.makeMockPlayer(GameType.SURVIVAL).getInventory());
        }

        private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key)
        {
                return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }

        @SuppressWarnings("unchecked")
        private static void configured(boolean remove, Runnable body)
        {
                ConfigValue<Boolean> removeDormant = (ConfigValue<Boolean>) CreatorConfig.SPEC.getValues()
                        .get("grindstone.remove_dormant");
                boolean previous = removeDormant.get();
                removeDormant.set(remove);
                try
                {
                        body.run();
                }
                finally
                {
                        removeDormant.set(previous);
                }
        }
}
