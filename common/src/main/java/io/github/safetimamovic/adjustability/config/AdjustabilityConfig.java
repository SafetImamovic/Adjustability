package io.github.safetimamovic.adjustability.config;

/**
 * Settings, read through this class only. The creator file ({@link CreatorConfig}, server side, per world, synced to
 * clients) holds the gameplay rules. Which enchantments and items take part is not configured here but with tags.
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
}
