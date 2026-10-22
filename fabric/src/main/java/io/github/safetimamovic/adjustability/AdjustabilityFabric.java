package io.github.safetimamovic.adjustability;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.neoforged.fml.config.ModConfig;

public class AdjustabilityFabric implements ModInitializer
{
        @Override
        public void onInitialize()
        {
                Adjustability.init();
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, CreatorConfig.SPEC,
                        CreatorConfig.FILE_NAME);
                CommandRegistrationCallback.EVENT.register(
                        (dispatcher, buildContext, environment) -> AdjustabilityCommand.register(dispatcher,
                                buildContext));
        }
}
