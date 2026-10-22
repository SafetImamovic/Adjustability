package io.github.safetimamovic.adjustability.advancement;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.ModRegistries;
import net.minecraft.core.registries.Registries;

/** Our advancement criteria. */
public final class ModTriggers
{
        public static final SwitchedTrigger SWITCHED = new SwitchedTrigger();

        private ModTriggers()
        {
        }

        public static void register(ModRegistries.Registrar registrar)
        {
                registrar.register(Registries.TRIGGER_TYPE, Constants.id("switched"), () -> SWITCHED);
        }
}
