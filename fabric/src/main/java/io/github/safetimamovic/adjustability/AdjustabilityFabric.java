package io.github.safetimamovic.adjustability;

import net.fabricmc.api.ModInitializer;

public class AdjustabilityFabric implements ModInitializer
{
        @Override
        public void onInitialize()
        {
                Adjustability.init();
        }
}
