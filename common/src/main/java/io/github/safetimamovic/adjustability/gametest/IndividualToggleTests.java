package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.advancement.ModStats;
import io.github.safetimamovic.adjustability.api.AdjustabilityApi;
import io.github.safetimamovic.adjustability.api.AdjustabilityListener;
import io.github.safetimamovic.adjustability.api.SwitchEvent;
import io.github.safetimamovic.adjustability.api.SwitchResult;
import io.github.safetimamovic.adjustability.api.SwitchSource;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleCost;
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.Toggler;
import io.github.safetimamovic.adjustability.network.DeactivateEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.ToggleNetwork;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/** Individual deactivation uses the same rules as switching without clearing compatible group members. */
final class IndividualToggleTests
{
        private IndividualToggleTests()
        {
        }

        static void toggle(GameTestHelper helper)
        {
                configured(() ->
                {
                        ServerPlayer player = player(helper);
                        ItemStack stack = pickaxe(helper);
                        Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                        Holder<Enchantment> silk = enchantment(helper, Enchantments.SILK_TOUCH);
                        helper.assertValueEqual(Toggler.deactivateEnchantment(player, stack, fortune,
                                SwitchSource.PANEL), SwitchResult.DONE, "Fortune off");
                        helper.assertTrue(Adjuster.activeOf(stack).isEmpty(), "both conflicting enchantments off");
                        helper.assertValueEqual(DormantEnchantments.get(stack, helper.getLevel().registryAccess())
                                .size(), 2, "both levels retained");
                        helper.assertTrue(DormantEnchantments.off(stack, helper.getLevel().registryAccess()).isEmpty(),
                                "a group's inactive choice is not a single-enchantment off flag");
                        helper.assertTrue(Adjuster.normalize(stack, helper.getLevel().registryAccess())
                                .active().isEmpty(),
                                "normalization preserves None");
                        helper.assertValueEqual(Toggler.activate(player, stack, silk, SwitchSource.PANEL),
                                SwitchResult.DONE, "Silk Touch on");
                        helper.assertValueEqual(Adjuster.activeOf(stack).getLevel(fortune), 0, "Fortune stays off");
                        helper.assertValueEqual(Toggler.activate(player, stack, fortune, SwitchSource.PANEL),
                                SwitchResult.DONE, "Fortune on");
                        helper.assertValueEqual(Adjuster.activeOf(stack).getLevel(silk), 0, "Silk Touch switches off");
                });
                helper.succeed();
        }

        static void compatible(GameTestHelper helper)
        {
                configured(() ->
                {
                        ServerPlayer player = player(helper);
                        ItemStack stack = trident(helper);
                        Holder<Enchantment> loyalty = enchantment(helper, Enchantments.LOYALTY);
                        Holder<Enchantment> channeling = enchantment(helper, Enchantments.CHANNELING);
                        helper.assertValueEqual(Toggler.deactivateEnchantment(player, stack, loyalty,
                                SwitchSource.PANEL), SwitchResult.DONE, "Loyalty off");
                        helper.assertValueEqual(Adjuster.activeOf(stack).getLevel(channeling), 1,
                                "Channeling stays on");
                        helper.assertValueEqual(Adjuster.normalize(stack, helper.getLevel().registryAccess()).active()
                                .getLevel(loyalty), 0, "Loyalty remains dormant");
                        helper.assertValueEqual(AdjustabilityApi.deactivateGroup(player, stack, loyalty),
                                SwitchResult.DONE, "the existing group API still clears the whole group");
                        helper.assertTrue(Adjuster.activeOf(stack).isEmpty(), "group None clears Channeling too");
                });
                helper.succeed();
        }

        static void rules(GameTestHelper helper)
        {
                configured(() ->
                {
                        ServerPlayer player = player(helper);
                        ItemStack pickaxe = pickaxe(helper);
                        Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                        settings(Map.of("toggle.allow_none", false), () ->
                        {
                                helper.assertValueEqual(Toggler.deactivateEnchantment(player, pickaxe, fortune,
                                        SwitchSource.PANEL), SwitchResult.NONE_NOT_ALLOWED, "server permission");
                                helper.assertValueEqual(Adjuster.activeOf(pickaxe).getLevel(fortune), 3, "unchanged");
                        });
                        helper.assertValueEqual(Toggler.deactivateEnchantment(player, pickaxe,
                                enchantment(helper, Enchantments.SILK_TOUCH), SwitchSource.PANEL),
                                SwitchResult.NOTHING_TO_SWITCH, "a dormant row does not deactivate its partner");
                        ItemStack cursed = new ItemStack(Items.DIAMOND_PICKAXE);
                        Holder<Enchantment> curse = enchantment(helper, Enchantments.VANISHING_CURSE);
                        cursed.enchant(curse, 1);
                        helper.assertValueEqual(Toggler.deactivateEnchantment(player, cursed, curse,
                                SwitchSource.PANEL), SwitchResult.WOULD_SWITCH_OFF_CURSE, "curses protected");
                        ItemStack single = new ItemStack(Items.DIAMOND_PICKAXE);
                        single.enchant(fortune, 3);
                        settings(Map.of("toggle.allow_switch_off", false), () ->
                                helper.assertValueEqual(Toggler.deactivateEnchantment(player, single, fortune,
                                        SwitchSource.PANEL), SwitchResult.SWITCH_OFF_NOT_ALLOWED,
                                        "single-enchantment permission remains separate"));
                });
                helper.succeed();
        }

        static void network(GameTestHelper helper)
        {
                configured(() ->
                {
                        ServerPlayer player = player(helper);
                        ItemStack trident = trident(helper);
                        player.getInventory().setItem(0, trident);
                        int container = player.containerMenu.containerId;
                        int slot = InventoryMenu.USE_ROW_SLOT_START;
                        int switches = player.getStats().getValue(Stats.CUSTOM.get(ModStats.SWITCHES));
                        ToggleNetwork.onDeactivate(player,
                                new DeactivateEnchantmentPayload(container + 1, slot, Enchantments.LOYALTY));
                        ToggleNetwork.onDeactivate(player,
                                new DeactivateEnchantmentPayload(container, -1, Enchantments.LOYALTY));
                        ToggleNetwork.onDeactivate(player,
                                new DeactivateEnchantmentPayload(container, 9999, Enchantments.LOYALTY));
                        ToggleNetwork.onDeactivate(player, new DeactivateEnchantmentPayload(container, slot,
                                ResourceKey.create(Registries.ENCHANTMENT, Constants.id("missing_individual_target"))));
                        helper.assertValueEqual(Adjuster.activeOf(trident)
                                .getLevel(enchantment(helper, Enchantments.LOYALTY)), 3, "invalid requests ignored");
                        ToggleNetwork.onDeactivate(player,
                                new DeactivateEnchantmentPayload(container, slot, Enchantments.LOYALTY));
                        helper.assertValueEqual(Adjuster.activeOf(trident)
                                .getLevel(enchantment(helper, Enchantments.LOYALTY)), 0, "Loyalty switched off");
                        helper.assertValueEqual(Adjuster.activeOf(trident)
                                .getLevel(enchantment(helper, Enchantments.CHANNELING)), 1, "Channeling stays active");
                        helper.assertValueEqual(player.getStats().getValue(Stats.CUSTOM.get(ModStats.SWITCHES)),
                                switches + 1, "only the valid request counted");
                });
                helper.succeed();
        }

        static void costs(GameTestHelper helper)
        {
                configured(() -> settings(Map.of("toggle.cost_type", ToggleCost.XP_LEVELS, "toggle.cost_amount", 1,
                        "toggle.cooldown_ticks", 20), () ->
                {
                        ServerPlayer player = player(helper);
                        player.setExperienceLevels(2);
                        ItemStack pickaxe = pickaxe(helper);
                        Holder<Enchantment> fortune = enchantment(helper, Enchantments.FORTUNE);
                        helper.assertValueEqual(Toggler.deactivateEnchantment(player, pickaxe, fortune,
                                SwitchSource.PANEL), SwitchResult.DONE, "paid deactivation");
                        helper.assertValueEqual(player.experienceLevel, 1, "one level charged");
                        helper.assertValueEqual(Toggler.activate(player, pickaxe,
                                enchantment(helper, Enchantments.SILK_TOUCH), SwitchSource.PANEL),
                                SwitchResult.COOLDOWN,
                                "the normal cooldown applies");
                        helper.assertValueEqual(player.experienceLevel, 1, "cooldown does not charge again");
                        ServerPlayer poor = player(helper);
                        poor.setExperienceLevels(0);
                        ItemStack unchanged = pickaxe(helper);
                        helper.assertValueEqual(Toggler.deactivateEnchantment(poor, unchanged, fortune,
                                SwitchSource.PANEL), SwitchResult.CANT_AFFORD, "the server checks cost");
                        helper.assertValueEqual(Adjuster.activeOf(unchanged).getLevel(fortune), 3,
                                "failed switch unchanged");
                }));
                helper.succeed();
        }

        static void events(GameTestHelper helper)
        {
                configured(() ->
                {
                        ServerPlayer player = player(helper);
                        ItemStack trident = trident(helper);
                        player.setItemInHand(InteractionHand.MAIN_HAND, trident);
                        Holder<Enchantment> loyalty = enchantment(helper, Enchantments.LOYALTY);
                        List<SwitchEvent> events = new ArrayList<>();
                        AdjustabilityListener listener = new AdjustabilityListener()
                        {
                                @Override
                                public void onSwitched(SwitchEvent event)
                                {
                                        events.add(event);
                                }
                        };
                        var neither = helper.getLevel().getServer().getAdvancements().get(Constants.id("neither"));
                        player.getAdvancements().revoke(neither, "neither");
                        AdjustabilityApi.addListener(listener);
                        try
                        {
                                Toggler.deactivateEnchantment(player, trident, loyalty, SwitchSource.PANEL);
                                helper.assertValueEqual(events.size(), 1, "one event");
                                helper.assertTrue(events.getFirst().deactivatesOnlyTarget(), "individual event");
                                helper.assertFalse(events.getFirst().none(), "Channeling is still active");
                                helper.assertFalse(player.getAdvancements().getOrStartProgress(neither).isDone(),
                                        "Neither is not awarded for a partially active group");
                                Toggler.deactivateEnchantment(player, trident,
                                        enchantment(helper, Enchantments.CHANNELING), SwitchSource.PANEL);
                                helper.assertTrue(events.getLast().none(), "the last active row leaves None");
                                helper.assertTrue(player.getAdvancements().getOrStartProgress(neither).isDone(),
                                        "Neither is awarded when the whole group is inactive");
                                SwitchEvent original = new SwitchEvent(player, trident, loyalty, SwitchSource.API,
                                        ItemEnchantments.EMPTY, ItemEnchantments.EMPTY, true);
                                helper.assertFalse(original.deactivatesOnlyTarget(), "original constructor valid");
                        }
                        finally
                        {
                                AdjustabilityApi.removeListener(listener);
                        }
                });
                helper.succeed();
        }

        private static ItemStack pickaxe(GameTestHelper helper)
        {
                ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
                stack.enchant(enchantment(helper, Enchantments.FORTUNE), 3);
                ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                dormant.set(enchantment(helper, Enchantments.SILK_TOUCH), 1);
                DormantEnchantments.set(stack, dormant.toImmutable(), helper.getLevel().registryAccess());
                return stack;
        }

        private static ItemStack trident(GameTestHelper helper)
        {
                ItemStack stack = new ItemStack(Items.TRIDENT);
                stack.enchant(enchantment(helper, Enchantments.LOYALTY), 3);
                stack.enchant(enchantment(helper, Enchantments.CHANNELING), 1);
                ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                dormant.set(enchantment(helper, Enchantments.RIPTIDE), 3);
                DormantEnchantments.set(stack, dormant.toImmutable(), helper.getLevel().registryAccess());
                return stack;
        }

        @SuppressWarnings("removal")
        private static ServerPlayer player(GameTestHelper helper)
        {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                return player;
        }

        private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key)
        {
                return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }

        private static void configured(Runnable body)
        {
                settings(Map.of("toggle.enabled", true, "toggle.allow_none", true, "toggle.allow_switch_off", true,
                        "toggle.cooldown_ticks", 0, "toggle.cost_type", ToggleCost.NONE,
                        "toggle.group_costs", List.of()), body);
        }

        @SuppressWarnings("unchecked")
        private static void settings(Map<String, Object> settings, Runnable body)
        {
                Map<ConfigValue<Object>, Object> before = new LinkedHashMap<>();
                settings.forEach((path, setting) ->
                {
                        ConfigValue<Object> value = (ConfigValue<Object>) CreatorConfig.SPEC.getValues().get(path);
                        before.put(value, value.get());
                        value.set(setting);
                });
                try
                {
                        body.run();
                }
                finally
                {
                        before.forEach(ConfigValue::set);
                }
        }
}
