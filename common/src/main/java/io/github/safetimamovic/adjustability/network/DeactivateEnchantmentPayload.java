package io.github.safetimamovic.adjustability.network;

import io.github.safetimamovic.adjustability.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/** Client request to switch off one active enchantment, preserving its compatible group members. */
public record DeactivateEnchantmentPayload(int containerId, int slot, ResourceKey<Enchantment> target)
        implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<DeactivateEnchantmentPayload> TYPE =
                new CustomPacketPayload.Type<>(Constants.id("deactivate_enchantment"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DeactivateEnchantmentPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, DeactivateEnchantmentPayload::containerId,
                        ByteBufCodecs.VAR_INT, DeactivateEnchantmentPayload::slot,
                        ResourceKey.streamCodec(Registries.ENCHANTMENT), DeactivateEnchantmentPayload::target,
                        DeactivateEnchantmentPayload::new);

        @Override
        public CustomPacketPayload.Type<DeactivateEnchantmentPayload> type()
        {
                return TYPE;
        }
}
