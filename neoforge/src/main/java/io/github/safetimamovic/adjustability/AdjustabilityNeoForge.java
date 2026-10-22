package io.github.safetimamovic.adjustability;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class AdjustabilityNeoForge
{
        public AdjustabilityNeoForge(IEventBus modEventBus, ModContainer container)
        {
                Adjustability.init();
        }
}
