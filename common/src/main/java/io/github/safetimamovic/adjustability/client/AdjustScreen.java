package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.TooltipLines;
import io.github.safetimamovic.adjustability.core.Toggler;
import io.github.safetimamovic.adjustability.graph.ConflictGraph;
import io.github.safetimamovic.adjustability.network.ToggleEnchantmentPayload;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Lists an item's groups of conflicting enchantments, one button per enchantment; clicking a dormant one asks the
 * server to make it active. Built only from vanilla widgets. It opens over the container screen it came from without
 * closing the container, reads the item straight from the open menu, and rebuilds when the server's update arrives, so
 * it never shows a switch the server refused.
 */
public final class AdjustScreen extends Screen
{
        private static final int BUTTON_WIDTH = 200;

        private final Screen parent;
        private final int containerId;
        private final int slot;
        private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        private ItemStack shown = ItemStack.EMPTY;

        public AdjustScreen(Screen parent, int containerId, int slot, Component itemName)
        {
                super(Component.translatable("adjustability.screen.title", itemName));
                this.parent = parent;
                this.containerId = containerId;
                this.slot = slot;
        }

        @Override
        protected void init()
        {
                this.layout = new HeaderAndFooterLayout(this);
                this.layout.addTitleHeader(this.title, this.font);
                LinearLayout contents = this.layout.addToContents(LinearLayout.vertical().spacing(8));

                ItemStack stack = this.item();
                this.shown = stack.copy();
                List<Set<Holder<Enchantment>>> groups = List.of();
                if (!stack.isEmpty() && this.minecraft.level != null)
                {
                        HolderLookup.Provider registries = this.minecraft.level.registryAccess();
                        ItemEnchantments active = Adjuster.activeOf(stack);
                        ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(active);
                        Adjuster.dormantOf(stack, registries).entrySet()
                                .forEach(entry -> all.set(entry.getKey(), entry.getIntValue()));
                        ItemEnchantments levels = all.toImmutable();
                        ConflictGraph<Holder<Enchantment>> graph = Adjuster.graph(levels);
                        groups = graph.components();
                        for (Set<Holder<Enchantment>> group : groups)
                        {
                                LinearLayout column = contents.addChild(LinearLayout.vertical().spacing(2));
                                for (Holder<Enchantment> enchantment : graph.vertices())
                                {
                                        if (group.contains(enchantment))
                                        {
                                                column.addChild(this.button(enchantment, levels.getLevel(enchantment),
                                                        active.getLevel(enchantment) > 0));
                                        }
                                }
                        }
                }
                if (groups.isEmpty())
                {
                        contents.addChild(new StringWidget(Component.translatable("adjustability.screen.nothing"),
                                this.font));
                }
                else
                {
                        ToggleConfig config = AdjustabilityConfig.toggle();
                        contents.addChild(new StringWidget(Component.translatable("adjustability.screen.cost",
                                Toggler.costText(config),
                                String.format(Locale.ROOT, "%.1f", config.cooldownTicks() / 20.0))
                                .withStyle(ChatFormatting.GRAY), this.font));
                }

                this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                        .width(BUTTON_WIDTH).build());
                this.layout.visitWidgets(this::addRenderableWidget);
                this.repositionElements();
        }

        private Button button(Holder<Enchantment> enchantment, int level, boolean active)
        {
                Component name = TooltipLines.name(enchantment, level);
                Button button = Button.builder(active
                                ? Component.translatable("adjustability.screen.active", name)
                                : name,
                        pressed -> enchantment.unwrapKey().ifPresent(key -> AdjustClient.send(
                                new ToggleEnchantmentPayload(this.containerId, this.slot, key))))
                        .width(BUTTON_WIDTH).build();
                // The active one can't be pressed: it shows the current choice.
                button.active = !active;
                return button;
        }

        @Override
        protected void repositionElements()
        {
                this.layout.arrangeElements();
        }

        @Override
        public void tick()
        {
                if (this.minecraft.player == null || this.minecraft.player.containerMenu.containerId != this.containerId)
                {
                        this.minecraft.gui.setScreen(null);
                        return;
                }
                if (!ItemStack.matches(this.item(), this.shown))
                {
                        this.rebuildWidgets();
                }
        }

        @Override
        public void onClose()
        {
                this.minecraft.gui.setScreen(this.parent);
        }

        @Override
        public boolean isPauseScreen()
        {
                return false;
        }

        private ItemStack item()
        {
                if (this.minecraft.player == null)
                {
                        return ItemStack.EMPTY;
                }
                AbstractContainerMenu menu = this.minecraft.player.containerMenu;
                if (menu.containerId != this.containerId || this.slot < 0 || this.slot >= menu.slots.size())
                {
                        return ItemStack.EMPTY;
                }
                return menu.getSlot(this.slot).getItem();
        }
}
