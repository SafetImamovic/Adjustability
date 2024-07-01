package io.github.safetimamovic.adjustability;

import io.github.safetimamovic.adjustability.custom_tooltip.CustomTooltip;
import io.github.safetimamovic.adjustability.enchantments_logic.AnvilHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * The <code>Adjustability</code> class is the <strong>main</strong> class of the mod.
 * It serves as the entry point and the main configuration class.
 */
@Mod( Adjustability.MODID )
public class Adjustability
{
	public static final String MODID = "adjustability";
	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * The constructor is called when the mod is loaded.
	 * <ul>
	 *      <li>
	 *          <code>FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup)</code> registers the commonSetup method to be called during the mod's common setup phase.
	 *      </li>
	 *      <li>
	 *          An IEventBus instance modEventBus is obtained, which is used to handle mod-specific events.
	 *      </li>
	 *      <li>
	 *          <code>modEventBus.addListener(this::commonSetup)</code> ensures that the commonSetup method is correctly registered with the mod event bus.
	 *      </li>
	 *      <li>
	 *          <code>MinecraftForge.EVENT_BUS.register(this)</code> registers this class with the global event bus to handle other game events of interest.
	 *      </li>
	 * </ul>
	 */
	public Adjustability ()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		// Register the commonSetup method for modloading
		modEventBus.addListener(this::commonSetup);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
	}

	/**
	 * <code>commonSetup</code> is a method that handles common setup tasks for the mod.
	 * Registers instances of:
	 * <ol>
	 *     <li>
	 *         <code>AnvilHandler</code>
	 *     </li>
	 *     <li>
	 *         <code>CustomTooltip</code>
	 *     </li>
	 * </ol>
	 *  with the global event bus, meaning these classes will handle specific events.
	 *
	 * @param event is passed by the <strong>Forge Event Bus</strong> when it triggers the common setup phase of mod loading.
	 */
	private void commonSetup (final FMLCommonSetupEvent event)
	{
		MinecraftForge.EVENT_BUS.register(new AnvilHandler());
		MinecraftForge.EVENT_BUS.register(new CustomTooltip());
	}
}
