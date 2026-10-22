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
        public record PanelStyle(
                PanelActivation activation,
                PanelSide side,
                int background,
                int borderTop,
                int borderBottom,
                boolean showBrackets,
                int bracket,
                int bracketLength,
                int highlight,
                int activeMark,
                boolean showCost)
        {
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
