package io.github.safetimamovic.adjustability;

import io.github.safetimamovic.adjustability.gametest.AdjustabilityGameTests;
import io.github.safetimamovic.adjustability.gametest.TestAdjustableItem;
import io.github.safetimamovic.adjustability.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Everything the mod adds to vanilla registries. The objects are created once in common code; each loader passes its
 * own way of registering them (Fabric right away, NeoForge from its register event).
 */
public final class ModRegistries
{
        private ModRegistries()
        {
        }

        /**
         * Registers one value, created when the loader asks for it (NeoForge only allows creating items and the like
         * during its register event). NeoForge calls {@link #registerAll} once per registry and skips the other keys.
         */
        public interface Registrar
        {
                <T> void register(ResourceKey<? extends Registry<T>> registry, Identifier id, Supplier<T> value);
        }

        public static void registerAll(Registrar registrar)
        {
                AdjustabilityGameTests.register(registrar);
                // Only for the GameTests: never in a released game.
                if (Services.PLATFORM.isDevelopmentEnvironment())
                {
                        TestAdjustableItem.register(registrar);
                }
        }
}
