package io.github.safetimamovic.adjustability;

//? if >=26.1 {
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
//?} else
/*import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;*/
import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustabilityClientCommand;
import io.github.safetimamovic.adjustability.client.ConfigScreens;
import java.util.function.UnaryOperator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
//?} else
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;*/
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.jspecify.annotations.Nullable;

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
                UnaryOperator<@Nullable Screen> configScreen = ConfigScreens
                        .withYacl(parent -> new ConfigurationScreen(Constants.MOD_ID, parent));
                ConfigScreenFactoryRegistry.INSTANCE.register(Constants.MOD_ID,
                        (modId, parent) -> configScreen.apply(parent));
                AdjustabilityClientCommand.init(configScreen);
                ClientCommandRegistrationCallback.EVENT
                        .register((dispatcher, buildContext) -> AdjustabilityClientCommand.register(dispatcher));
                //? if >=26.1 {
                KeyMapping.Category.register(AdjustKeys.CATEGORY.id());
                KeyMappingHelper.registerKeyMapping(AdjustKeys.ADJUST);
                KeyMappingHelper.registerKeyMapping(AdjustKeys.CYCLE);
                //?} else {
                /*KeyBindingHelper.registerKeyBinding(AdjustKeys.ADJUST);
                KeyBindingHelper.registerKeyBinding(AdjustKeys.CYCLE);
                *///?}
                ClientTickEvents.END_CLIENT_TICK.register(AdjustClient::tick);
        }
}
