package io.github.safetimamovic.adjustability.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.safetimamovic.adjustability.api.AdjustabilityApi;
import io.github.safetimamovic.adjustability.api.SwitchResult;
import io.github.safetimamovic.adjustability.api.SwitchSource;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ExclusivityConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.Exclusivity;
import io.github.safetimamovic.adjustability.core.GrindstoneRules;
import io.github.safetimamovic.adjustability.core.Toggler;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;

/**
 * Dormant enchantments that are no longer registered, because the datapack or mod that added them was removed. The
 * registry can't change while a test runs, so these use ids that were never registered: the same code path.
 */
final class RemovedEnchantmentTests
{
        /** An id no datapack registers. */
        static final String GONE = "adjustability_test:gone";
        /** Another one. */
        static final String OTHER = "adjustability_test:other";

        private RemovedEnchantmentTests()
        {
        }

        /**
         * An unknown entry is ignored at runtime and kept, at its level, by every write: a switch, the API adding an
         * enchantment, a repair. Garbage (level 0, an id that doesn't parse) is dropped. Only clearing removes it.
         */
        static void unknownDormantKept(GameTestHelper helper)
        {
                HolderLookup.Provider registries = helper.getLevel().registryAccess();
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                pickaxe.enchant(fortune, 3);
                CompoundTag dormant = new CompoundTag();
                dormant.putInt("minecraft:silk_touch", 1);
                dormant.putInt(GONE, 3);
                dormant.putInt("adjustability_test:zero", 0);
                dormant.putInt("not an id!", 2);
                withDormant(pickaxe, dormant);

                helper.assertValueEqual(DormantEnchantments.get(pickaxe, registries).size(), 1, "only Silk Touch read");
                helper.assertValueEqual(DormantEnchantments.unknown(pickaxe, registries), Map.of(GONE, 3), "unknown");

                ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
                helper.assertValueEqual(Toggler.activate(player, pickaxe, silkTouch, SwitchSource.COMMAND),
                        SwitchResult.DONE, "switch");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(silkTouch), 1, "Silk Touch active");
                helper.assertValueEqual(DormantEnchantments.get(pickaxe, registries).getLevel(fortune), 3,
                        "Fortune dormant");
                helper.assertValueEqual(DormantEnchantments.unknown(pickaxe, registries), Map.of(GONE, 3),
                        "kept by a switch");
                helper.assertValueEqual(raw(pickaxe).size(), 2, "garbage dropped: " + raw(pickaxe));

                AdjustabilityApi.addEnchantment(pickaxe, enchantment(helper, Enchantments.EFFICIENCY), 5, registries);
                helper.assertValueEqual(DormantEnchantments.unknown(pickaxe, registries), Map.of(GONE, 3),
                        "kept by the API");

                // Both active: the next write repairs it.
                ItemEnchantments.Mutable both = new ItemEnchantments.Mutable(Adjuster.activeOf(pickaxe));
                both.set(fortune, 3);
                pickaxe.set(DataComponents.ENCHANTMENTS, both.toImmutable());
                Adjuster.write(pickaxe, Adjuster.normalize(pickaxe, registries), registries);
                helper.assertFalse(Adjuster.hasConflict(Adjuster.activeOf(pickaxe)), "repaired");
                helper.assertValueEqual(DormantEnchantments.unknown(pickaxe, registries), Map.of(GONE, 3),
                        "kept by a repair");

                // Nothing registered left dormant: the entry still holds the unknown one.
                ItemStack only = new ItemStack(Items.DIAMOND_PICKAXE);
                only.enchant(fortune, 3);
                CompoundTag gone = new CompoundTag();
                gone.putInt(GONE, 2);
                withDormant(only, gone);
                DormantEnchantments.set(only, ItemEnchantments.EMPTY, registries);
                helper.assertValueEqual(DormantEnchantments.unknown(only, registries), Map.of(GONE, 2),
                        "kept with nothing else dormant");
                helper.assertTrue(DormantEnchantments.has(only), "has() counts it");

                DormantEnchantments.clear(only);
                helper.assertFalse(DormantEnchantments.has(only), "clear removes it");
                helper.assertFalse(only.has(DataComponents.CUSTOM_DATA), "and the empty custom_data");
                helper.succeed();
        }

        /** Server rules naming an enchantment that isn't registered are accepted and match nothing. */
        static void unknownInConfig(GameTestHelper helper)
        {
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
                Holder<Enchantment> fireAspect = enchantment(helper, Enchantments.FIRE_ASPECT);
                helper.assertTrue(Exclusivity.isValidGroup(GONE + ", minecraft:sharpness"), "a valid group");
                ExclusivityConfig config = new ExclusivityConfig(true, List.of(GONE + ", minecraft:sharpness"),
                        List.of(GONE + ", minecraft:fortune"), List.of(GONE + " = XP_LEVELS 5"));
                helper.assertFalse(Exclusivity.conflict(sharpness, fireAspect, config), "extra group with it");
                helper.assertTrue(Exclusivity.conflict(fortune, silkTouch, config), "compatible with it");
                helper.assertTrue(Exclusivity.groupCost(List.of(fortune, silkTouch), config).isEmpty(),
                        "cost for it");
                helper.succeed();
        }

        /**
         * The active enchantment a dormant one waited behind isn't registered any more. Vanilla can't decode such an
         * item at all, so it's lost when it loads: nothing for Adjustability to do. Should vanilla start keeping the
         * item without that entry, this fails, and the dormant partner, which then conflicts with nothing, has to
         * take over; the second half checks it does.
         */
        static void unknownActivePartner(GameTestHelper helper)
        {
                HolderLookup.Provider registries = helper.getLevel().registryAccess();
                //? if >=1.21.5 {
                String enchantments = "{\"" + GONE + "\": 1}";
                //?} else
                /*String enchantments = "{\"levels\": {\"" + GONE + "\": 1}}";*/
                JsonElement json = JsonParser.parseString("{\"id\": \"minecraft:diamond_pickaxe\", \"count\": 1, "
                        + "\"components\": {\"minecraft:enchantments\": " + enchantments + ", "
                        + "\"minecraft:custom_data\": {\"adjustability:dormant\": {\"minecraft:silk_touch\": 1}}}}");
                DataResult<ItemStack> parsed = ItemStack.CODEC.parse(
                        registries.createSerializationContext(JsonOps.INSTANCE), json);
                helper.assertTrue(parsed.result().isEmpty() && parsed.error().isPresent(),
                        "vanilla now loads an item with an unregistered active enchantment: " + parsed);

                // The same item with the entry dropped: Silk Touch conflicts with nothing left and becomes active.
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                pickaxe.enchant(enchantment(helper, Enchantments.EFFICIENCY), 5);
                CompoundTag dormant = new CompoundTag();
                dormant.putInt("minecraft:silk_touch", 1);
                withDormant(pickaxe, dormant);
                Adjuster.Split split = Adjuster.normalize(pickaxe, registries);
                helper.assertValueEqual(split.active().getLevel(enchantment(helper, Enchantments.SILK_TOUCH)), 1,
                        "Silk Touch takes over");
                helper.assertTrue(split.dormant().isEmpty(), "nothing dormant");
                helper.succeed();
        }

        /**
         * The anvil keeps unknown entries from both items, the higher level when both hold one, also when there's
         * nothing to split; they don't add to the price.
         */
        static void anvilKeepsUnknown(GameTestHelper helper)
        {
                HolderLookup.Provider registries = helper.getLevel().registryAccess();
                Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                ItemStack left = new ItemStack(Items.DIAMOND_PICKAXE);
                left.enchant(fortune, 3);
                CompoundTag leftDormant = new CompoundTag();
                leftDormant.putInt(GONE, 3);
                withDormant(left, leftDormant);
                ItemStack right = new ItemStack(Items.DIAMOND_PICKAXE);
                right.enchant(enchantment(helper, Enchantments.SILK_TOUCH), 1);
                CompoundTag rightDormant = new CompoundTag();
                rightDormant.putInt(GONE, 4);
                rightDormant.putInt(OTHER, 1);
                withDormant(right, rightDormant);

                AnvilMenu anvil = anvil(helper);
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(left);
                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right);
                ItemStack result = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                helper.assertFalse(result.isEmpty(), "a result");
                helper.assertValueEqual(DormantEnchantments.unknown(result, registries), Map.of(GONE, 4, OTHER, 1),
                        "both items' unknown entries");
                helper.assertValueEqual(DormantEnchantments.get(result, registries).size(), 1, "Silk Touch dormant");

                // No conflict, nothing to split: the left item's entry is still on the result.
                anvil = anvil(helper);
                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(left.copy());
                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(enchantment(helper, Enchantments.UNBREAKING), 3));
                result = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                helper.assertValueEqual(DormantEnchantments.unknown(result, registries), Map.of(GONE, 3),
                        "kept without a split");
                helper.succeed();
        }

        /** The grindstone removes unknown entries with everything else, and gives experience only for known ones. */
        static void grindstoneDropsUnknown(GameTestHelper helper)
        {
                HolderLookup.Provider registries = helper.getLevel().registryAccess();
                Holder<Enchantment> silkTouch = enchantment(helper, Enchantments.SILK_TOUCH);
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                pickaxe.enchant(enchantment(helper, Enchantments.FORTUNE), 3);
                CompoundTag dormant = new CompoundTag();
                dormant.putInt("minecraft:silk_touch", 1);
                dormant.putInt(GONE, 3);
                withDormant(pickaxe, dormant);
                helper.assertValueEqual(GrindstoneRules.dormantExperience(pickaxe, registries),
                        silkTouch.value().getMinCost(1), "experience for Silk Touch only");

                GrindstoneMenu grindstone = new GrindstoneMenu(0, player(helper).getInventory());
                grindstone.getSlot(GrindstoneMenu.INPUT_SLOT).set(pickaxe);
                ItemStack result = grindstone.getSlot(GrindstoneMenu.RESULT_SLOT).getItem();
                helper.assertFalse(result.isEmpty(), "a result");
                helper.assertFalse(DormantEnchantments.has(result), "nothing dormant left, known or not");
                helper.succeed();
        }

        /** Flatten removes Adjustability's data, unknown entries included. */
        static void flattenDropsUnknown(GameTestHelper helper)
        {
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                pickaxe.enchant(enchantment(helper, Enchantments.FORTUNE), 3);
                CompoundTag dormant = new CompoundTag();
                dormant.putInt(GONE, 3);
                withDormant(pickaxe, dormant);
                AdjustabilityApi.flatten(pickaxe);
                helper.assertFalse(DormantEnchantments.has(pickaxe), "flattened");
                helper.assertValueEqual(Adjuster.activeOf(pickaxe).size(), 1, "Fortune stays");
                helper.succeed();
        }

        /** With advanced tooltips (F3+H) an unknown entry gets a line; normal tooltips don't show it. */
        static void unknownTooltip(GameTestHelper helper)
        {
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                pickaxe.enchant(enchantment(helper, Enchantments.FORTUNE), 3);
                CompoundTag dormant = new CompoundTag();
                dormant.putInt("minecraft:silk_touch", 1);
                dormant.putInt(GONE, 3);
                withDormant(pickaxe, dormant);
                String line = "  ⇄ Unknown: " + GONE + " III";
                helper.assertTrue(tooltip(helper, pickaxe, TooltipFlag.ADVANCED).contains(line),
                        "advanced: " + tooltip(helper, pickaxe, TooltipFlag.ADVANCED));
                List<String> normal = tooltip(helper, pickaxe, TooltipFlag.NORMAL);
                helper.assertFalse(normal.stream().anyMatch(text -> text.contains(GONE)), "normal: " + normal);
                helper.succeed();
        }

        private static List<String> tooltip(GameTestHelper helper, ItemStack stack, TooltipFlag flag)
        {
                return stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, flag).stream()
                        .map(Component::getString).toList();
        }

        private static AnvilMenu anvil(GameTestHelper helper)
        {
                return new AnvilMenu(0, player(helper).getInventory());
        }

        private static Player player(GameTestHelper helper)
        {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.experienceLevel = 30;
                return player;
        }

        private static ItemStack book(Holder<Enchantment> enchantment, int level)
        {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                stored.set(enchantment, level);
                book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
                return book;
        }

        private static void withDormant(ItemStack stack, CompoundTag dormant)
        {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put("adjustability:dormant", dormant));
        }

        private static CompoundTag raw(ItemStack stack)
        {
                //? if >=1.21.5 {
                return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                        .getCompoundOrEmpty("adjustability:dormant");
                //?} else
                /*return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                        .getCompound("adjustability:dormant");*/
        }

        private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key)
        {
                return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }
}
