package io.github.safetimamovic.adjustability.config;

import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Settings, read through this class only. Two files: the creator file ({@link CreatorConfig}, server side, per world,
 * synced to clients) for the gameplay rules, and the player file ({@link PlayerConfig}, client side, editable in game)
 * for personal preferences. Which enchantments and items take part is not configured here but with tags.
 */
public final class AdjustabilityConfig
{
        private AdjustabilityConfig()
        {
        }

        /** Current anvil settings: the server's values once synced, the defaults before that. */
        public static AnvilConfig anvil()
        {
                return CreatorConfig.readAnvil();
        }

        /** Current settings for switching enchantments. */
        public static ToggleConfig toggle()
        {
                return CreatorConfig.readToggle();
        }

        /** This player's own settings; the defaults on a dedicated server. */
        public static PlayerSettings player()
        {
                return PlayerConfig.read();
        }

        /** Which enchantments conflict, and the level cost of switching within groups. */
        public static ExclusivityConfig exclusivity()
        {
                return CreatorConfig.readExclusivity();
        }

        /** Whether items take part unless denied, or only when allowed. */
        public static ItemMode itemMode()
        {
                return CreatorConfig.readItemMode();
        }

        /** Current grindstone settings. */
        public static GrindstoneConfig grindstone()
        {
                return CreatorConfig.readGrindstone();
        }

        /**
         * @param maxDormantPerItem        0 means no limit
         * @param maxConflictsPerComponent 0 means no limit
         */
        public record AnvilConfig(
                boolean enabled,
                boolean combineBooks,
                int dormantCostPerLevel,
                boolean allowTooExpensive,
                int maxDormantPerItem,
                int maxConflictsPerComponent)
        {
        }

        public record GrindstoneConfig(boolean removeDormant)
        {
        }

        public record ToggleConfig(
                boolean enabled,
                boolean autoFill,
                int cooldownTicks,
                ToggleCost costType,
                int costAmount,
                Identifier costItem,
                boolean allowInWorldCycle,
                boolean allowCommand,
                boolean allowNone,
                boolean allowSwitchOff,
                ServerPanelActivation panelActivation,
                boolean playSound)
        {
                /** The cost of a switch in a group no {@code group_costs} entry covers. */
                public Cost defaultCost()
                {
                        return new Cost(this.costType, this.costAmount, this.costItem);
                }
        }

        /**
         * What one switch costs.
         *
         * @param item only used with {@link ToggleCost#ITEM}
         */
        public record Cost(ToggleCost type, int amount, Identifier item)
        {
        }

        /** Raw entries; {@code core/Exclusivity} parses them. */
        public record ExclusivityConfig(
                boolean useVanilla,
                List<? extends String> groups,
                List<? extends String> compatible,
                List<? extends String> groupCosts)
        {
        }

        public enum ItemMode
        {
                DENYLIST,
                ALLOWLIST
        }

        public enum ToggleCost
        {
                NONE,
                XP_LEVELS,
                XP_POINTS,
                ITEM,
                DURABILITY
        }

        public record PlayerSettings(boolean showDormant, boolean showHint, PanelStyle panel)
        {
        }

        /** The Adjust panel's look and how its key works; colors are ARGB. */
        public record PanelStyle(PanelActivation activation, PanelSide side, Frame frame, Marks marks, Text text,
                Layout layout)
        {
        }

        /** The panel's background, border, corner brackets and the hovered row's highlight. */
        public record Frame(
                BorderStyle border,
                int background,
                int borderTop,
                int borderBottom,
                boolean showBrackets,
                int bracket,
                int bracketLength,
                int highlight)
        {
        }

        /**
         * The mark in front of each row.
         *
         * @param style a row mark's id, like {@code adjustability:circle}; mods can add their own
         */
        public record Marks(String style, int active, int dormant, boolean animate)
        {
        }

        /** The title (item name), the rows' text and the cost line at the bottom. */
        public record Text(
                int title,
                int active,
                int dormant,
                int footer,
                boolean shadow,
                boolean showTitle,
                boolean showIcon,
                boolean showCost)
        {
        }

        /** Spacing, how groups of conflicting enchantments are told apart, and the order of the rows. */
        public record Layout(
                int padding,
                int rowHeight,
                GroupStyle groups,
                int separator,
                RowOrder order,
                NonePosition none,
                SinglesSection singles)
        {
        }

        /**
         * GRADIENT: a tooltip-like border fading from the top color to the bottom one. SOLID: the top color all around.
         * NONE: only the background. SPRITE: the {@code adjustability:panel} GUI sprite, which resource packs can
         * replace (it includes the background).
         */
        public enum BorderStyle
        {
                GRADIENT,
                SOLID,
                NONE,
                SPRITE
        }

        /**
         * Between groups of conflicting enchantments. LINE: a gap with a thin line. GAP: only the gap. NONE: nothing.
         * HEADER: a heading with the group's name above each group.
         */
        public enum GroupStyle
        {
                LINE,
                GAP,
                NONE,
                HEADER
        }

        /** REGISTRY: the game's order. ALPHABETICAL: by name. ACTIVE_FIRST: the active ones, then the game's order. */
        public enum RowOrder
        {
                REGISTRY,
                ALPHABETICAL,
                ACTIVE_FIRST
        }

        /**
         * The section of enchantments that conflict with nothing, each switched on or off on its own (when the server
         * allows it). EXPANDED or COLLAPSED: how it starts; a click on its heading folds it. HIDDEN: not shown.
         */
        public enum SinglesSection
        {
                EXPANDED,
                COLLAPSED,
                HIDDEN
        }

        /** Where a group's None row goes, when the server allows switching a group off. */
        public enum NonePosition
        {
                LAST,
                FIRST
        }

        /** The server's say on the Adjust key: PLAYER leaves it to each player's {@link PanelActivation}. */
        public enum ServerPanelActivation
        {
                PLAYER,
                TOGGLE,
                HOLD
        }

        /** TOGGLE: press the Adjust key to open the panel, again to close it. HOLD: open while the key is held. */
        public enum PanelActivation
        {
                TOGGLE,
                HOLD
        }

        public enum PanelSide
        {
                AUTO,
                RIGHT,
                LEFT
        }
}
