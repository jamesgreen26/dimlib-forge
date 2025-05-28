package qouteall.dimlib;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.dimlib.config.DimLibConfig;
import qouteall.dimlib.ducks.IMinecraftServer;
import qouteall.dimlib.network.DimLibNetworkHandler;

@Mod(DimLibEntry.MODID)
public class DimLibEntry {
    public static final Logger LOGGER = LoggerFactory.getLogger(DimLibEntry.class);
	public static final String MODID = "dimlib";

	public DimLibEntry(FMLJavaModLoadingContext context) {
		LOGGER.info("DimLib initializing");

		DynamicDimensionsImpl.init();

		DimensionTemplate.init();

		MinecraftForge.EVENT_BUS.register(this);
		context.getModEventBus().addListener(this::onClientSetup);
		context.getModEventBus().addListener(this::onCommonSetup);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			((IMinecraftServer) server).dimlib_processTasks();
		});

		MidnightConfig.init(
				MODID, DimLibConfig.class
		);
	}

	public void onCommonSetup(FMLCommonSetupEvent event) {
		DimLibNetworkHandler.register();
	}

	public void onClientSetup(FMLClientSetupEvent event) {
		LOGGER.info("client setup");
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		LOGGER.info("registering commands");
		DimsCommand.register(event.getDispatcher());
	}
}