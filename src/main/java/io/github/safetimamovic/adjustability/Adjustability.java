package io.github.safetimamovic.adjustability;
import io.github.safetimamovic.adjustability.enchantments_logic.AnvilHandler;

import com.mojang.logging.LogUtils;
import io.github.safetimamovic.adjustability.render_core.CustomTooltip;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Adjustability.MODID)
public class Adjustability
{
    public static final String MODID = "adjustability";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Adjustability()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        MinecraftForge.EVENT_BUS.register(new AnvilHandler());
        MinecraftForge.EVENT_BUS.register(new CustomTooltip());
    }
}