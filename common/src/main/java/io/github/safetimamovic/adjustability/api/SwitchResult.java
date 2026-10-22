package io.github.safetimamovic.adjustability.api;

import java.util.Locale;
import org.jetbrains.annotations.ApiStatus;

/** How a request to make a dormant enchantment active went. */
@ApiStatus.Experimental
public enum SwitchResult
{
        /** Switched: the enchantment is active now. */
        DONE,
        /** Switching is turned off on this server ({@code [toggle] enabled}, or the Cycle key's own setting). */
        DISABLED,
        /** The item can't hold dormant enchantments, or has nothing to switch. */
        NOTHING_TO_SWITCH,
        /** The enchantment isn't dormant on the item. */
        NOT_DORMANT,
        /** A never-dormant enchantment (a curse, by default) would have been switched off. */
        WOULD_SWITCH_OFF_CURSE,
        /** The player switched too recently ({@code [toggle] cooldown_ticks}). */
        COOLDOWN,
        /** The player can't pay the switch's cost. */
        CANT_AFFORD,
        /** An {@link AdjustabilityListener} refused it. */
        CANCELLED,
        /** "None" for a group, but the server doesn't allow it ({@code [toggle] allow_none}). */
        NONE_NOT_ALLOWED;

        /** For translation keys: {@code adjustability.toggle.<key>}. */
        public String key()
        {
                return this.name().toLowerCase(Locale.ROOT);
        }
}
