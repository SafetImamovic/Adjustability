package io.github.safetimamovic.adjustability.platform;

import io.github.safetimamovic.adjustability.platform.services.IPlatformHelper;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper
{

        @Override
        public String getPlatformName()
        {
                return "Fabric";
        }

        @Override
        public boolean isModLoaded(String modId)
        {

                return FabricLoader.getInstance().isModLoaded(modId);
        }

        @Override
        public boolean isDevelopmentEnvironment()
        {

                return FabricLoader.getInstance().isDevelopmentEnvironment();
        }

        @Override
        public Path configDirectory()
        {
                return FabricLoader.getInstance().getConfigDir();
        }
}
