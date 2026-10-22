package io.github.safetimamovic.adjustability;

import io.github.safetimamovic.adjustability.platform.Services;

/**
 * Common entrypoint. Shared between all loaders, so it may only touch vanilla code and libraries
 * vanilla ships. Loader-specific behavior goes through {@link Services}.
 */
public final class Adjustability
{

        private Adjustability()
        {
        }

        public static void init()
        {
                Constants.LOG.info("Adjustability initializing on {} ({})",
                        Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
        }
}
