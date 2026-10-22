package io.github.safetimamovic.adjustability;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import io.github.safetimamovic.adjustability.network.CycleEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.ToggleEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.ToggleNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import java.util.function.Supplier;
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
                        public <T> void register(ResourceKey<? extends Registry<T>> registry, Identifier id,
                                Supplier<T> value)
                        {
                                Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY
                                        .getValueOrThrow((ResourceKey) registry);
                                Registry.register(target, id, value.get());
                        }
                });
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, CreatorConfig.SPEC,
                        CreatorConfig.FILE_NAME);
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, PlayerConfig.SPEC,
                        PlayerConfig.FILE_NAME);

                PayloadTypeRegistry.serverboundPlay().register(ToggleEnchantmentPayload.TYPE,
                        ToggleEnchantmentPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(ToggleEnchantmentPayload.TYPE,
                        (payload, context) -> ToggleNetwork.onToggle(context.player(), payload));
                PayloadTypeRegistry.serverboundPlay().register(CycleEnchantmentPayload.TYPE,
                        CycleEnchantmentPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(CycleEnchantmentPayload.TYPE,
                        (payload, context) -> ToggleNetwork.onCycle(context.player(), payload));

                CommandRegistrationCallback.EVENT.register(
                        (dispatcher, buildContext, environment) -> AdjustabilityCommand.register(dispatcher,
                                buildContext));
        }
}
