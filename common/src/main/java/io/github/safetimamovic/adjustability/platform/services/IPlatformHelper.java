package io.github.safetimamovic.adjustability.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper
{

        /**
         * Gets the name of the current platform
         *
         * @return The name of the current platform.
         */
        String getPlatformName();

        /**
         * Checks if a mod with the given id is loaded.
         *
         * @param modId The mod to check if it is loaded.
         * @return True if the mod is loaded, false otherwise.
         */
        boolean isModLoaded(String modId);

        /**
         * Check if the game is currently in a development environment.
         *
         * @return True if in a development environment, false otherwise.
         */
        boolean isDevelopmentEnvironment();

        /**
         * The game's config folder, where the loader keeps mods' config files.
         *
         * @return The config folder.
         */
        Path configDirectory();

        /**
         * Adjustability's version, as the loader read it from the mod's metadata.
         *
         * @return The mod's version, such as {@code 0.4.2}.
         */
        String modVersion();

        /**
         * Gets the name of the environment type as a string.
         *
         * @return The name of the environment type.
         */
        default String getEnvironmentName()
        {

                return isDevelopmentEnvironment() ? "development" : "production";
        }
}
