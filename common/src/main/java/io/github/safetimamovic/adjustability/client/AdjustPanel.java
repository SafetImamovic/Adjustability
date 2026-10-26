package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.client.PanelLayout.Content;
import io.github.safetimamovic.adjustability.client.PanelLayout.Entry;
import io.github.safetimamovic.adjustability.client.PanelLayout.Group;
import io.github.safetimamovic.adjustability.client.PanelLayout.Header;
import io.github.safetimamovic.adjustability.client.PanelLayout.Row;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Cost;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.SinglesSection;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.Exclusivity;
import io.github.safetimamovic.adjustability.core.Toggler;
import io.github.safetimamovic.adjustability.core.TooltipLines;
import io.github.safetimamovic.adjustability.graph.ConflictGraph;
import io.github.safetimamovic.adjustability.network.ToggleEnchantmentPayload;
import io.github.safetimamovic.adjustability.tags.ModTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
//? if >=1.21.5 {
import net.minecraft.util.Util;
//?} else
/*import net.minecraft.Util;*/
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/**
 * The Adjust panel: a tooltip-like menu pinned next to an item in a container screen, one row per enchantment in each
 * group of conflicting enchantments. Clicking a dormant row asks the server to make it active. When the server allows
 * it, a last section lists the enchantments that conflict with nothing, each switched on or off by a click. It is
 * drawn by the container screen itself (see {@code AbstractContainerScreenMixin}), so the container stays open and
 * nothing about the menu changes on the client. Rows are rebuilt from the slot's item every frame, so the panel shows
 * what the server says, never a switch it refused.
 *
 * <p>The look is Adjustability's own by default: a dark panel with a magenta gradient border and bracket decorations
 * at the corners. Every part of it is a player setting ({@code [panel]} in {@code adjustability-client.toml}, or
 * {@code /adjustability panel}), read every frame. {@link PanelLayout} places things and {@link PanelRenderer} draws
 * them, so the YACL screen's preview ({@link PanelPreview}) draws the same panel.
 */
public final class AdjustPanel
{
        /** How long a mark takes to animate a switch, in milliseconds. */
        private static final float ANIMATION_MILLIS = 100;

        private final AbstractContainerScreen<?> screen;
        private final int containerId;
        private final int slot;
        /** The same item in the menu the server has open; only differs from the above in the creative inventory. */
        private final int serverContainerId;
        private final int serverSlot;
        /** Top-right corner of the slot, in screen coordinates. */
        private final int slotRight;
        private final int slotTop;

        private int x;
        private int y;
        private Content content = new Content(Component.empty(), ItemStack.EMPTY, List.of(), List.of(), List.of(),
                Component.empty(), 0, 0);
        /** How active each row's mark is drawn (0 to 1), by {@link #key}; moves toward the row's state. */
        private final Map<String, Float> marks = new HashMap<>();
        private long lastFrame = Util.getMillis();
        /** The index of the singles' group, -1 if there is none. */
        private int singlesGroup = -1;

        /** The player's last fold or unfold of the singles' section this session, and the setting it applies to. */
        private static @Nullable Boolean collapsedOverride;
        private static @Nullable SinglesSection collapsedFor;

        AdjustPanel(AbstractContainerScreen<?> screen, int containerId, int slot, int serverContainerId, int serverSlot,
                int slotRight, int slotTop)
        {
                this.screen = screen;
                this.containerId = containerId;
                this.slot = slot;
                this.serverContainerId = serverContainerId;
                this.serverSlot = serverSlot;
                this.slotRight = slotRight;
                this.slotTop = slotTop;
        }

        public AbstractContainerScreen<?> screen()
        {
                return this.screen;
        }

        int slot()
        {
                return this.slot;
        }

        /**
         * Rebuilds the rows from the slot's item and places the panel next to the slot, inside the screen.
         *
         * @return false if there is nothing left to adjust (the item moved, or lost its conflicts), and the panel should
         *         close
         */
        boolean refresh(Font font)
        {
                PanelStyle style = AdjustabilityConfig.player().panel();
                ItemStack stack = this.item();
                Minecraft minecraft = Minecraft.getInstance();
                if (stack.isEmpty() || minecraft.level == null)
                {
                        return false;
                }
                ItemEnchantments active = Adjuster.activeOf(stack);
                ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(active);
                Adjuster.dormantOf(stack, minecraft.level.registryAccess()).entrySet()
                        .forEach(entry -> all.set(entry.getKey(), entry.getIntValue()));
                ItemEnchantments levels = all.toImmutable();
                ConflictGraph<Holder<Enchantment>> graph = Adjuster.graph(levels);
                // What the server will treat as active: the stored split, repaired if it conflicts (see Toggler).
                ItemEnchantments shownActive = Adjuster.normalize(stack, minecraft.level.registryAccess()).active();
                ToggleConfig toggle = AdjustabilityConfig.toggle();
                boolean allowNone = toggle.allowNone();
                List<Set<Holder<Enchantment>>> components = graph.components();
                // Enchantments that conflict with nothing, if the server lets them be switched off; curses can't be.
                SinglesSection section = style.layout().singles();
                List<Holder<Enchantment>> singles = toggle.allowSwitchOff() && section != SinglesSection.HIDDEN
                        ? graph.singles().stream().filter(enchantment -> !enchantment.is(ModTags.NEVER_DORMANT))
                                .toList()
                        : List.of();
                if (components.isEmpty() && singles.isEmpty())
                {
                        return false;
                }

                // One cost for every group goes in the footer; otherwise each group shows its own.
                List<Cost> costs = components.stream()
                        .map(group -> Exclusivity.groupCost(group).orElseGet(toggle::defaultCost)).toList();
                List<Cost> singleCosts = singles.stream()
                        .map(enchantment -> Exclusivity.groupCost(Set.of(enchantment)).orElseGet(toggle::defaultCost))
                        .distinct().toList();
                List<Cost> allCosts = Stream.concat(costs.stream(), singleCosts.stream()).distinct().toList();
                boolean oneCost = allCosts.size() == 1;
                Component footer = footer(oneCost ? allCosts.getFirst() : null, toggle.cooldownTicks());
                List<Component> groupCosts = new ArrayList<>();
                if (!oneCost && style.text().showCost())
                {
                        costs.stream().map(Toggler::costText).forEach(groupCosts::add);
                        if (!singles.isEmpty())
                        {
                                // The singles' heading shows their cost only when they share one.
                                groupCosts.add(singleCosts.size() == 1 ? Toggler.costText(singleCosts.getFirst())
                                        : Component.empty());
                        }
                }

                List<Group> groups = new ArrayList<>();
                for (Set<Holder<Enchantment>> component : components)
                {
                        List<Holder<Enchantment>> members = graph.vertices().stream().filter(component::contains)
                                .toList();
                        List<Entry> entries = members.stream()
                                .map(enchantment -> new Entry(enchantment,
                                        TooltipLines.name(enchantment, levels.getLevel(enchantment)),
                                        shownActive.getLevel(enchantment) > 0, false))
                                .toList();
                        // Any enchantment of the group stands for it; the server makes the whole group dormant.
                        Entry none = allowNone ? new Entry(members.getFirst(),
                                Component.translatable("adjustability.panel.none"),
                                entries.stream().noneMatch(Entry::active), true) : null;
                        groups.add(new Group(groupName(component, members.getFirst()), entries, none, false));
                }
                this.singlesGroup = -1;
                if (!singles.isEmpty())
                {
                        boolean collapsed = collapsed(section);
                        long off = singles.stream().filter(enchantment -> shownActive.getLevel(enchantment) == 0)
                                .count();
                        List<Entry> entries = collapsed ? List.of() : singles.stream()
                                .map(enchantment -> new Entry(enchantment,
                                        TooltipLines.name(enchantment, levels.getLevel(enchantment)),
                                        shownActive.getLevel(enchantment) > 0, false))
                                .toList();
                        this.singlesGroup = groups.size();
                        groups.add(new Group(singlesHeading(collapsed, off), entries, null, true));
                }
                this.content = PanelLayout.layout(font, style, stack.getHoverName(), stack, groups, groupCosts,
                        footer);
                this.x = PanelLayout.placeX(style, this.slotRight, this.content.width(), this.screen.width);
                this.y = PanelLayout.placeY(style, this.slotTop, this.content.height(), this.screen.height);
                return true;
        }

        void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY)
        {
                PanelStyle style = AdjustabilityConfig.player().panel();
                long now = Util.getMillis();
                float step = style.marks().animate() ? (now - this.lastFrame) / ANIMATION_MILLIS : 1;
                this.lastFrame = now;
                Map<String, Float> marks = new HashMap<>();
                for (Row row : this.content.rows())
                {
                        float target = row.active() ? 1 : 0;
                        float shown = this.marks.getOrDefault(key(row), target);
                        marks.put(key(row), shown < target ? Math.min(target, shown + step)
                                : Math.max(target, shown - step));
                }
                this.marks.clear();
                this.marks.putAll(marks);
                //? if >=26.1 {
                graphics.nextStratum();
                //?} else {
                /*// Above the items and their counts, where tooltips draw.
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 400);
                *///?}
                Row hovered = this.rowAt(mouseX, mouseY).filter(this::clickable).orElse(null);
                Header hoveredHeader = this.sectionHeaderAt(mouseX, mouseY).orElse(null);
                PanelRenderer.draw(graphics, font, style, this.x, this.y, this.content, hovered, hoveredHeader,
                        row -> this.marks.getOrDefault(key(row), row.active() ? 1F : 0F));
                //? if <26.1
                /*graphics.pose().popPose();*/
        }

        /** A row's identity across frames, for its mark's animation. */
        private static String key(Row row)
        {
                String enchantment = row.enchantment() == null ? "" : row.enchantment().getRegisteredName();
                return row.none() ? "none/" + row.group() : enchantment;
        }

        /**
         * A group's heading: the exclusive-set tag most of it shares (vanilla's are translated, like Mining; others
         * get their tag's last word), else the name of its first enchantment.
         */
        static Component groupName(Set<Holder<Enchantment>> group, Holder<Enchantment> first)
        {
                TagKey<Enchantment> best = null;
                long bestCount = 1;
                for (Holder<Enchantment> enchantment : group)
                {
                        HolderSet<Enchantment> set = enchantment.value().exclusiveSet();
                        Optional<TagKey<Enchantment>> tag = set.unwrapKey();
                        long count = group.stream().filter(set::contains).count();
                        if (tag.isPresent() && count > bestCount)
                        {
                                best = tag.get();
                                bestCount = count;
                        }
                }
                if (best == null)
                {
                        return first.value().description();
                }
                Identifier id = best.location();
                String word = id.getPath().substring(id.getPath().lastIndexOf('/') + 1).replace('_', ' ');
                String fallback = word.isEmpty() ? id.toString()
                        : word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
                return Component.translatableWithFallback(
                        "adjustability.group." + id.getNamespace() + "." + id.getPath().replace('/', '.'), fallback);
        }

        /** The singles' heading: a fold arrow, and how many are switched off. */
        static Component singlesHeading(boolean collapsed, long off)
        {
                return Component.literal(collapsed ? "▶ " : "▼ ").append(off > 0
                        ? Component.translatable("adjustability.panel.singles.off", off)
                        : Component.translatable("adjustability.panel.singles"));
        }

        /** Whether the singles' section is folded: the player's last click this session, else the setting. */
        private static boolean collapsed(SinglesSection setting)
        {
                if (collapsedFor != setting)
                {
                        collapsedFor = setting;
                        collapsedOverride = null;
                }
                return collapsedOverride != null ? collapsedOverride : setting == SinglesSection.COLLAPSED;
        }

        /** The line at the bottom: the cooldown, and the cost of a switch if one cost covers every group. */
        static Component footer(@Nullable Cost oneCost, int cooldownTicks)
        {
                String cooldown = String.format(Locale.ROOT, "%.1f", cooldownTicks / 20.0);
                return oneCost != null
                        ? Component.translatable("adjustability.panel.cost", Toggler.costText(oneCost), cooldown)
                        : Component.translatable("adjustability.panel.cooldown", cooldown);
        }

        boolean contains(double mouseX, double mouseY)
        {
                int offset = PanelLayout.BRACKET_OFFSET;
                return mouseX >= this.x - offset && mouseX < this.x + this.content.width() + offset
                        && mouseY >= this.y - offset && mouseY < this.y + this.content.height() + offset;
        }

        /**
         * A left click inside the panel: on a dormant row, asks the server to make it active; on a single enchantment,
         * to switch it on or off; on the singles' heading, folds or unfolds the section.
         */
        void click(double mouseX, double mouseY)
        {
                if (this.sectionHeaderAt(mouseX, mouseY).isPresent())
                {
                        collapsedOverride = !collapsed(AdjustabilityConfig.player().panel().layout().singles());
                        return;
                }
                this.rowAt(mouseX, mouseY).filter(this::clickable)
                        .flatMap(row -> Optional.ofNullable(row.enchantment()).flatMap(Holder::unwrapKey)
                                .map(key -> new ToggleEnchantmentPayload(this.serverContainerId, this.serverSlot, key,
                                        row.none() || this.single(row) && row.active())))
                        .ifPresent(AdjustClient::send);
        }

        private boolean single(Row row)
        {
                return row.group() == this.singlesGroup;
        }

        /** Dormant rows switch on; single enchantments switch either way. */
        private boolean clickable(Row row)
        {
                return !row.active() || this.single(row);
        }

        private Optional<Header> sectionHeaderAt(double mouseX, double mouseY)
        {
                if (mouseX < this.x || mouseX >= this.x + this.content.width())
                {
                        return Optional.empty();
                }
                int height = PanelLayout.headerHeight(Minecraft.getInstance().font);
                return this.content.headers().stream().filter(Header::section).filter(header ->
                        mouseY >= this.y + header.y() - 1 && mouseY < this.y + header.y() + height - 1).findFirst();
        }

        /** Centre of the singles' heading, in screen coordinates; for the client GameTest. */
        public Optional<int[]> singlesHeadingCentre()
        {
                int height = PanelLayout.headerHeight(Minecraft.getInstance().font);
                return this.content.headers().stream().filter(Header::section).findFirst()
                        .map(header -> new int[]{this.x + this.content.width() / 2, this.y + header.y() + height / 2});
        }

        /** The panel's left, top, width and height, in screen coordinates (brackets not included); for client tests. */
        public int[] bounds()
        {
                return new int[]{this.x, this.y, this.content.width(), this.content.height()};
        }

        /** Centre of an enchantment's row, in screen coordinates; for the client GameTest. */
        public Optional<int[]> rowCentre(Holder<Enchantment> enchantment)
        {
                return this.content.rows().stream()
                        .filter(row -> !row.none() && enchantment.equals(row.enchantment()))
                        .findFirst().map(this::centre);
        }

        /** Centre of the None row of the group {@code anyInGroup} belongs to; for the client GameTest. */
        public Optional<int[]> noneRowCentre(Holder<Enchantment> anyInGroup)
        {
                Optional<Integer> group = this.content.rows().stream()
                        .filter(row -> anyInGroup.equals(row.enchantment())).map(Row::group).findFirst();
                return group.flatMap(index -> this.content.rows().stream()
                        .filter(row -> row.none() && row.group() == index).findFirst().map(this::centre));
        }

        private int[] centre(Row row)
        {
                return new int[]{this.x + this.content.width() / 2, this.y + row.y() + rowHeight() / 2};
        }

        private Optional<Row> rowAt(double mouseX, double mouseY)
        {
                if (mouseX < this.x || mouseX >= this.x + this.content.width())
                {
                        return Optional.empty();
                }
                int rowHeight = rowHeight();
                return this.content.rows().stream().filter(row -> mouseY >= this.y + row.y() - 1
                        && mouseY < this.y + row.y() + rowHeight - 1).findFirst();
        }

        private static int rowHeight()
        {
                return AdjustabilityConfig.player().panel().layout().rowHeight();
        }

        private ItemStack item()
        {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player == null)
                {
                        return ItemStack.EMPTY;
                }
                AbstractContainerMenu menu = minecraft.player.containerMenu;
                if (menu.containerId != this.containerId || this.slot < 0 || this.slot >= menu.slots.size())
                {
                        return ItemStack.EMPTY;
                }
                return menu.getSlot(this.slot).getItem();
        }
}
