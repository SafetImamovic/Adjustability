package io.github.safetimamovic.adjustability;

//? if >=26.1 {
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
//?} else
/*import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;*/
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import io.github.safetimamovic.adjustability.network.CycleEnchantmentPayload;
import io.github.safetimamovic.adjustability.network.DeactivateEnchantmentPayload;
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
                                //? if >=26.1 {
                                Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY
                                        .getValueOrThrow((ResourceKey) registry);
                                //?} else
                                /*Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY.getOrThrow((ResourceKey) registry);*/
                                Registry.register(target, id, value.get());
                        }
                });
                //? if >=26.1 {
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, CreatorConfig.SPEC,
                        CreatorConfig.FILE_NAME);
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, PlayerConfig.SPEC,
                        PlayerConfig.FILE_NAME);
                var payloads = PayloadTypeRegistry.serverboundPlay();
                //?} else {
                /*NeoForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, CreatorConfig.SPEC,
                        CreatorConfig.FILE_NAME);
                NeoForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, PlayerConfig.SPEC,
                        PlayerConfig.FILE_NAME);
                var payloads = PayloadTypeRegistry.playC2S();
                *///?}

                payloads.register(ToggleEnchantmentPayload.TYPE, ToggleEnchantmentPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(ToggleEnchantmentPayload.TYPE,
                        (payload, context) -> ToggleNetwork.onToggle(context.player(), payload));
                payloads.register(DeactivateEnchantmentPayload.TYPE, DeactivateEnchantmentPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(DeactivateEnchantmentPayload.TYPE,
                        (payload, context) -> ToggleNetwork.onDeactivate(context.player(), payload));
                payloads.register(CycleEnchantmentPayload.TYPE, CycleEnchantmentPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(CycleEnchantmentPayload.TYPE,
                        (payload, context) -> ToggleNetwork.onCycle(context.player(), payload));

                CommandRegistrationCallback.EVENT.register(
                        (dispatcher, buildContext, environment) -> AdjustabilityCommand.register(dispatcher,
                                buildContext));
        }
}
