package io.github.safetimamovic.adjustability;

import net.fabricmc.api.ClientModInitializer;

/** Client-only setup (key mappings, tooltip lines, the Adjust screen) goes here, so dedicated servers never load it. */
public class AdjustabilityFabricClient implements ClientModInitializer
{
        @Override
        public void onInitializeClient()
        {
        }
}
