package io.github.safetimamovic.adjustability.api.client;

import io.github.safetimamovic.adjustability.client.RowMarks;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Adjustability's client-side API: ways to change how the Adjust panel looks. Call it from client code only (a Fabric
 * client entrypoint, NeoForge's {@code FMLClientSetupEvent} or a {@code Dist.CLIENT} class).
 *
 * <p>Experimental until 1.0: names may still change.
 */
@ApiStatus.Experimental
public final class AdjustabilityClientApi
{
        private AdjustabilityClientApi()
        {
        }

        /**
         * Adds a row mark players can pick with {@code mark_style}, by its id; it shows up in the config screens and
         * {@code /adjustability panel}. Use your mod's namespace.
         *
         * @throws IllegalArgumentException if the id is taken
         */
        public static void registerRowMark(Identifier id, RowMark mark)
        {
                RowMarks.register(id.toString(), mark);
        }

        /** The ids of every row mark, Adjustability's own first. */
        public static Set<String> rowMarks()
        {
                return RowMarks.ids();
        }
}
