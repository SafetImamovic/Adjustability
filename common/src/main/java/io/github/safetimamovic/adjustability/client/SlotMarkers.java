package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.config.PlayerConfig;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.component.CustomData;

/** Each screen keeps its slot checks until their immutable custom data changes. */
public final class SlotMarkers
{
        private final Map<Slot, Cached> slots = new IdentityHashMap<>();

        private record Cached(CustomData data, boolean marked)
        {
        }

        public void draw(GuiGraphicsExtractor graphics, Slot slot)
        {
                if (!PlayerConfig.slotMarker() || !slot.hasItem())
                {
                        return;
                }
                CustomData data = slot.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                Cached cached = this.slots.get(slot);
                if (cached == null || cached.data() != data)
                {
                        cached = new Cached(data, DormantEnchantments.has(slot.getItem()));
                        this.slots.put(slot, cached);
                }
                if (cached.marked())
                {
                        int color = PlayerConfig.slotMarkerColor();
                        graphics.fill(slot.x + 12, slot.y, slot.x + 16, slot.y + 1, color);
                        graphics.fill(slot.x + 15, slot.y + 1, slot.x + 16, slot.y + 4, color);
                }
        }
}
