package io.github.safetimamovic.adjustability.effect;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.ModRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Our sound events. {@code assets/adjustability/sounds.json} points them at vanilla sounds, so a resource pack can swap
 * them without touching anything else.
 */
public final class ModSounds
{
        /** A successful switch, including to none. */
        public static final SoundEvent SWITCH = SoundEvent.createVariableRangeEvent(Constants.id("item.switch"));

        private ModSounds()
        {
        }

        public static void register(ModRegistries.Registrar registrar)
        {
                registrar.register(Registries.SOUND_EVENT, SWITCH.location(), () -> SWITCH);
        }
}
