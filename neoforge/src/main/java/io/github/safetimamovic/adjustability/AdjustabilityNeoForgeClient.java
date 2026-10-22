package io.github.safetimamovic.adjustability;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Client-only setup (key mappings, tooltip lines, the Adjust screen) goes here, so dedicated servers never load it. */
@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class AdjustabilityNeoForgeClient
{
        public AdjustabilityNeoForgeClient(IEventBus modEventBus, ModContainer container)
        {
        }
}
