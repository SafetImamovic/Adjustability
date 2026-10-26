package io.github.safetimamovic.adjustability.platform;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.platform.services.IPlatformHelper;
import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

public class NeoForgePlatformHelper implements IPlatformHelper
{

        @Override
        public String getPlatformName()
        {

                return "NeoForge";
        }

        @Override
        public boolean isModLoaded(String modId)
        {

                return ModList.get().isLoaded(modId);
        }

        @Override
        public boolean isDevelopmentEnvironment()
        {

                //? if >=26.1 {
                return !FMLLoader.getCurrent().isProduction();
                //?} else
                /*return !FMLLoader.isProduction();*/
        }

        @Override
        public Path configDirectory()
        {
                return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public String modVersion()
        {
                return ModList.get().getModContainerById(Constants.MOD_ID).orElseThrow().getModInfo().getVersion()
                        .toString();
        }
}
