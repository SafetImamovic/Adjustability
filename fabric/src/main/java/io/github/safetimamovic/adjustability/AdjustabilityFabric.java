package io.github.safetimamovic.adjustability;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.config.ModConfig;

public class AdjustabilityFabric implements ModInitializer
{
        @Override
        public void onInitialize()
        {
                Adjustability.init();
                ModRegistries.registerAll(new ModRegistries.Registrar()
                {
                        @Override
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        public <T> void register(ResourceKey<? extends Registry<T>> registry, Identifier id, T value)
                        {
                                Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY
                                        .getValueOrThrow((ResourceKey) registry);
                                Registry.register(target, id, value);
                        }
                });
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, CreatorConfig.SPEC,
                        CreatorConfig.FILE_NAME);
                CommandRegistrationCallback.EVENT.register(
                        (dispatcher, buildContext, environment) -> AdjustabilityCommand.register(dispatcher,
                                buildContext));
        }
}
