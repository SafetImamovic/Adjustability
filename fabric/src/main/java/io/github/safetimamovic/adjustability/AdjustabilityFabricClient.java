package io.github.safetimamovic.adjustability;

import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustabilityClientCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/** Client-only setup (key mappings, the Adjust panel, client commands), so dedicated servers never load it. */
public class AdjustabilityFabricClient implements ClientModInitializer
{
        @Override
        public void onInitializeClient()
        {
                // canSend: servers without the mod don't know the payload.
                AdjustClient.init(payload ->
                {
                        if (ClientPlayNetworking.canSend(payload.type()))
                        {
                                ClientPlayNetworking.send(payload);
                        }
                });
                // Opened from Mod Menu, through Forge Config API Port.
                ConfigScreenFactoryRegistry.INSTANCE.register(Constants.MOD_ID, ConfigurationScreen::new);
                AdjustabilityClientCommand.init(parent -> new ConfigurationScreen(Constants.MOD_ID, parent));
                ClientCommandRegistrationCallback.EVENT
                        .register((dispatcher, buildContext) -> AdjustabilityClientCommand.register(dispatcher));
                KeyMapping.Category.register(AdjustKeys.CATEGORY.id());
                KeyMappingHelper.registerKeyMapping(AdjustKeys.ADJUST);
                KeyMappingHelper.registerKeyMapping(AdjustKeys.CYCLE);
                ClientTickEvents.END_CLIENT_TICK.register(AdjustClient::tick);
        }
}
