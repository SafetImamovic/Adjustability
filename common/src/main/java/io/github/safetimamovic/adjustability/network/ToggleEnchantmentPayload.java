package io.github.safetimamovic.adjustability.network;

import io.github.safetimamovic.adjustability.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Client → server: "make {@code target} active on the item in slot {@code slot} of my open menu" (Adjust screen). The
 * server re-checks the menu, the slot and everything about the switch; see {@code ToggleNetwork}.
 *
 * @param containerId the open menu's id, so a click meant for a menu that just closed does nothing
 * @param slot        index into that menu's slots
 */
public record ToggleEnchantmentPayload(int containerId, int slot, ResourceKey<Enchantment> target)
        implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<ToggleEnchantmentPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("toggle_enchantment"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleEnchantmentPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, ToggleEnchantmentPayload::containerId,
                        ByteBufCodecs.VAR_INT, ToggleEnchantmentPayload::slot,
                        ResourceKey.streamCodec(Registries.ENCHANTMENT), ToggleEnchantmentPayload::target,
                        ToggleEnchantmentPayload::new);

        @Override
        public CustomPacketPayload.Type<ToggleEnchantmentPayload> type()
        {
                return TYPE;
        }
}
