package io.github.safetimamovic.adjustability.network;

import io.github.safetimamovic.adjustability.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: "step the held item's {@code component}-th group of conflicting enchantments to its next choice"
 * (Cycle key). The server works out the groups itself; see {@code ToggleNetwork}.
 */
public record CycleEnchantmentPayload(int component) implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<CycleEnchantmentPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("cycle_enchantment"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CycleEnchantmentPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, CycleEnchantmentPayload::component,
                        CycleEnchantmentPayload::new);

        @Override
        public CustomPacketPayload.Type<CycleEnchantmentPayload> type()
        {
                return TYPE;
        }
}
