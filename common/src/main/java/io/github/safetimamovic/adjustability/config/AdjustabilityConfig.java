package io.github.safetimamovic.adjustability.config;

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
                boolean allowCommand)
        {
        }

        public enum ToggleCost
        {
                NONE,
                XP_LEVELS,
                XP_POINTS,
                ITEM,
                DURABILITY
        }

        public record PlayerSettings(boolean showDormant, boolean showHint)
        {
        }
}
