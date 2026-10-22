package io.github.safetimamovic.adjustability;

import io.github.safetimamovic.adjustability.config.CreatorConfig;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class AdjustabilityNeoForge
{
        public AdjustabilityNeoForge(IEventBus modEventBus, ModContainer container)
        {
                Adjustability.init();
                // SYNCED: NeoForge's synced-to-clients config (called SERVER on Fabric's port); same file name on both.
                container.registerConfig(ModConfig.Type.SYNCED, CreatorConfig.SPEC, CreatorConfig.FILE_NAME);
                // Fires once per registry; RegisterEvent.register ignores keys of the other registries.
                modEventBus.addListener((RegisterEvent event) -> ModRegistries.registerAll(new ModRegistries.Registrar()
                {
                        @Override
                        public <T> void register(ResourceKey<? extends Registry<T>> registry, Identifier id, T value)
                        {
                                event.register(registry, id, () -> value);
                        }
                }));
                NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> AdjustabilityCommand.register(
                        event.getDispatcher(), event.getBuildContext()));
        }
}
