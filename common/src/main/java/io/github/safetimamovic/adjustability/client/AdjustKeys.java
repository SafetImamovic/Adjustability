package io.github.safetimamovic.adjustability.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.safetimamovic.adjustability.Constants;
import net.minecraft.client.KeyMapping;

/** Key mappings, in their own Controls category; each loader registers them. */
public final class AdjustKeys
{
        public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Constants.id("controls"));

        /** While hovering an item in an inventory: pins the Adjust panel next to it. */
        public static final KeyMapping ADJUST = new KeyMapping(name("adjust"), InputConstants.KEY_LALT, CATEGORY);

        /** Outside screens: steps the held item to its next choice. Unbound by default. */
        public static final KeyMapping CYCLE = new KeyMapping(name("cycle"), InputConstants.UNKNOWN.getValue(),
                CATEGORY);

        private AdjustKeys()
        {
        }

        private static String name(String key)
        {
                return "key." + Constants.MOD_ID + "." + key;
        }
}
