package io.github.safetimamovic.adjustability;

import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustabilityClientCommand;
import io.github.safetimamovic.adjustability.client.ConfigScreens;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//? if >=26.1 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else
/*import net.neoforged.neoforge.network.PacketDistributor;*/
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/** Client-only setup (key mappings, the Adjust panel, client commands), so dedicated servers never load it. */
@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class AdjustabilityNeoForgeClient
{
        public AdjustabilityNeoForgeClient(IEventBus modEventBus, ModContainer container)
        {
                //? if >=26.1 {
                AdjustClient.init(ClientPacketDistributor::sendToServer);
                //?} else
                /*AdjustClient.init(payload -> PacketDistributor.sendToServer(payload));*/
                UnaryOperator<@Nullable Screen> configScreen = ConfigScreens
                        .withYacl(parent -> new ConfigurationScreen(container, parent));
                container.registerExtensionPoint(IConfigScreenFactory.class,
                        (IConfigScreenFactory) (mod, parent) -> configScreen.apply(parent));
                AdjustabilityClientCommand.init(configScreen);
                NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> AdjustabilityClientCommand
                        .register(event.getDispatcher()));
                modEventBus.addListener((RegisterKeyMappingsEvent event) ->
                {
                        //? if >=26.1
                        event.registerCategory(AdjustKeys.CATEGORY);
                        event.register(AdjustKeys.ADJUST);
                        event.register(AdjustKeys.CYCLE);
                });
                NeoForge.EVENT_BUS.addListener(
                        (ClientTickEvent.Post event) -> AdjustClient.tick(Minecraft.getInstance()));
        }
}
