package io.github.safetimamovic.adjustability;

import io.github.safetimamovic.adjustability.config.CreatorConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Constants.MOD_ID)
public class AdjustabilityNeoForge
{
        public AdjustabilityNeoForge(IEventBus modEventBus, ModContainer container)
        {
                Adjustability.init();
                // SYNCED: NeoForge's synced-to-clients config (called SERVER on Fabric's port); same file name on both.
                container.registerConfig(ModConfig.Type.SYNCED, CreatorConfig.SPEC, CreatorConfig.FILE_NAME);
        }
}
