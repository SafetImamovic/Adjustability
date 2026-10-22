package io.github.safetimamovic.adjustability;

import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import io.github.safetimamovic.adjustability.network.CycleEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.ToggleEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.ToggleNetwork;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class AdjustabilityNeoForge
{
        public AdjustabilityNeoForge(IEventBus modEventBus, ModContainer container)
        {
                Adjustability.init();
                // SYNCED: NeoForge's synced-to-clients config (called SERVER on Fabric's port); same file names on both.
                container.registerConfig(ModConfig.Type.SYNCED, CreatorConfig.SPEC, CreatorConfig.FILE_NAME);
                container.registerConfig(ModConfig.Type.CLIENT, PlayerConfig.SPEC, PlayerConfig.FILE_NAME);
                // Fires once per registry; RegisterEvent.register ignores keys of the other registries.
                modEventBus.addListener((RegisterEvent event) -> ModRegistries.registerAll(new ModRegistries.Registrar()
                {
                        @Override
                        public <T> void register(ResourceKey<? extends Registry<T>> registry, Identifier id, T value)
                        {
                                event.register(registry, id, () -> value);
                        }
                }));
                modEventBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1")
                        .playToServer(ToggleEnchantmentPayload.TYPE, ToggleEnchantmentPayload.STREAM_CODEC,
                                (payload, context) -> ToggleNetwork.onToggle((ServerPlayer) context.player(), payload))
                        .playToServer(CycleEnchantmentPayload.TYPE, CycleEnchantmentPayload.STREAM_CODEC,
                                (payload, context) -> ToggleNetwork.onCycle((ServerPlayer) context.player(), payload)));
                NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> AdjustabilityCommand.register(
                        event.getDispatcher(), event.getBuildContext()));
        }
}
