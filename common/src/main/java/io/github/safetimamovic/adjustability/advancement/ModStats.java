package io.github.safetimamovic.adjustability.advancement;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.ModRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

/** Our statistics, shown under Statistics → General. */
public final class ModStats
{
        /** Switches a player made (to an enchantment or to none). */
        public static final Identifier SWITCHES = Constants.id("switches");

        private ModStats()
        {
        }

        public static void register(ModRegistries.Registrar registrar)
        {
                registrar.register(Registries.CUSTOM_STAT, SWITCHES, () -> SWITCHES);
        }

        /** Counts one switch and returns the player's new total. */
        public static int countSwitch(ServerPlayer player)
        {
                player.awardStat(SWITCHES);
                return player.getStats().getValue(Stats.CUSTOM.get(SWITCHES, StatFormatter.DEFAULT));
        }
}
