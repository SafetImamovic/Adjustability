package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/** Anvil limits count conflicting alternatives without penalizing a group's None choice. */
final class NoneAnvilTests
{
        private NoneAnvilTests()
        {
        }

        static void boots(GameTestHelper helper)
        {
                configured(4, () ->
                {
                        Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
                        for (int none = 0; none < 4; none++)
                        {
                                ItemStack result = combine(helper, boots(helper, none, true), book(unbreaking, 3));
                                helper.assertFalse(result.isEmpty(), "Unbreaking applies with None mask " + none);
                                helper.assertValueEqual(Adjuster.activeOf(result).getLevel(unbreaking), 3,
                                        "Unbreaking active, None mask " + none);
                                helper.assertValueEqual(DormantEnchantments.get(result,
                                        helper.getLevel().registryAccess()).size(), 4 + Integer.bitCount(none),
                                        "all dormant choices kept");
                                helper.assertValueEqual(Adjuster.activeOf(result)
                                        .getLevel(enchantment(helper, Enchantments.PROTECTION)),
                                        (none & 1) == 0 ? 4 : 0, "Protection choice preserved");
                                helper.assertValueEqual(Adjuster.activeOf(result)
                                        .getLevel(enchantment(helper, Enchantments.DEPTH_STRIDER)),
                                        (none & 2) == 0 ? 3 : 0, "Depth Strider choice preserved");
                        }
                });
                helper.succeed();
        }

        static void limits(GameTestHelper helper)
        {
                configured(3, () ->
                {
                        ItemStack boots = boots(helper, 3, false);
                        ItemStack compatible = combine(helper, boots,
                                book(enchantment(helper, Enchantments.UNBREAKING), 3));
                        helper.assertFalse(compatible.isEmpty(), "a compatible enchantment fits at the limit");
                        ItemStack extraChoice = combine(helper, boots,
                                book(enchantment(helper, Enchantments.PROJECTILE_PROTECTION), 4));
                        helper.assertTrue(extraChoice.isEmpty(), "a fourth conflicting alternative exceeds the limit");
                });
                helper.succeed();
        }

        static void pickaxe(GameTestHelper helper)
        {
                configured(4, () ->
                {
                        Holder<Enchantment> silk = enchantment(helper, Enchantments.SILK_TOUCH);
                        Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                        Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
                        for (int state = 0; state < 3; state++)
                        {
                                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                                if (state == 0)
                                {
                                        pickaxe.enchant(silk, 1);
                                }
                                else
                                {
                                        ItemEnchantments.Mutable dormant =
                                                new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                                        dormant.set(silk, 1);
                                        if (state == 2)
                                        {
                                                dormant.set(fortune, 3);
                                        }
                                        DormantEnchantments.set(pickaxe, dormant.toImmutable(),
                                                state == 1 ? Set.of(silk) : Set.of(),
                                                helper.getLevel().registryAccess());
                                }
                                ItemStack result = combine(helper, pickaxe, book(unbreaking, 3));
                                helper.assertFalse(result.isEmpty(), "Unbreaking applies to pickaxe state " + state);
                                helper.assertValueEqual(Adjuster.activeOf(result).getLevel(unbreaking), 3,
                                        "Unbreaking active");
                                helper.assertValueEqual(Adjuster.activeOf(result).getLevel(silk), state == 0 ? 1 : 0,
                                        "Silk Touch state preserved");
                        }
                });
                helper.succeed();
        }

        private static ItemStack boots(GameTestHelper helper, int none, boolean allProtections)
        {
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                for (ResourceKey<Enchantment> key : List.of(Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION,
                        Enchantments.BLAST_PROTECTION, Enchantments.PROJECTILE_PROTECTION,
                        Enchantments.DEPTH_STRIDER, Enchantments.FROST_WALKER))
                {
                        if (!allProtections && key.equals(Enchantments.PROJECTILE_PROTECTION))
                        {
                                continue;
                        }
                        Holder<Enchantment> enchantment = enchantment(helper, key);
                        int level = key.equals(Enchantments.DEPTH_STRIDER) ? 3
                                : key.equals(Enchantments.FROST_WALKER) ? 2 : 4;
                        if (key.equals(Enchantments.PROTECTION) && (none & 1) == 0
                                || key.equals(Enchantments.DEPTH_STRIDER) && (none & 2) == 0)
                        {
                                boots.enchant(enchantment, level);
                        }
                        else
                        {
                                dormant.set(enchantment, level);
                        }
                }
                DormantEnchantments.set(boots, dormant.toImmutable(), helper.getLevel().registryAccess());
                return boots;
        }

        private static ItemStack combine(GameTestHelper helper, ItemStack input, ItemStack addition)
        {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.experienceLevel = 30;
                AnvilMenu anvil = new AnvilMenu(0, player.getInventory());
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(input.copy());
                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(addition);
                return anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        }

        private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key)
        {
                return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }

        private static ItemStack book(Holder<Enchantment> enchantment, int level)
        {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                stored.set(enchantment, level);
                book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
                return book;
        }

        @SuppressWarnings("unchecked")
        private static void configured(int limit, Runnable body)
        {
                ConfigValue<Boolean> allowNone = (ConfigValue<Boolean>) CreatorConfig.SPEC.getValues()
                        .get("toggle.allow_none");
                ConfigValue<Integer> maxDormant = (ConfigValue<Integer>) CreatorConfig.SPEC.getValues()
                        .get("anvil.max_dormant_per_item");
                boolean previousNone = allowNone.get();
                int previousLimit = maxDormant.get();
                allowNone.set(true);
                maxDormant.set(limit);
                try
                {
                        body.run();
                }
                finally
                {
                        allowNone.set(previousNone);
                        maxDormant.set(previousLimit);
                }
        }
}
