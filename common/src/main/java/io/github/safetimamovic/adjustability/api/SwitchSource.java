package io.github.safetimamovic.adjustability.api;

import org.jetbrains.annotations.ApiStatus;

/** What asked for a switch. */
@ApiStatus.Experimental
public enum SwitchSource
{
        /** A click in the Adjust panel. */
        PANEL,
        /** The Cycle key. */
        CYCLE_KEY,
        /** {@code /adjustability toggle}. */
        COMMAND,
        /** Another mod, through {@link AdjustabilityApi#activate}. */
        API
}
