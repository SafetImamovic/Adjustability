package io.github.safetimamovic.adjustability;

import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustabilityClientCommand;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only setup (key mappings, the Adjust screen, client commands), so dedicated servers never load it. */
@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class AdjustabilityNeoForgeClient
{
        public AdjustabilityNeoForgeClient(IEventBus modEventBus, ModContainer container)
        {
                AdjustClient.init(ClientPacketDistributor::sendToServer);
                container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
                AdjustabilityClientCommand.init(parent -> new ConfigurationScreen(container, parent));
                NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> AdjustabilityClientCommand
                        .register(event.getDispatcher()));
                modEventBus.addListener((RegisterKeyMappingsEvent event) ->
                {
                        event.registerCategory(AdjustKeys.CATEGORY);
                        event.register(AdjustKeys.ADJUST);
                        event.register(AdjustKeys.CYCLE);
                });
                NeoForge.EVENT_BUS.addListener(
                        (ClientTickEvent.Post event) -> AdjustClient.tick(Minecraft.getInstance()));
        }
}
