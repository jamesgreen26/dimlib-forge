package qouteall.dimlib;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.dimlib.config.DimLibConfig;
import qouteall.dimlib.ducks.IMinecraftServer;
import qouteall.dimlib.network.DimSyncPacket;

@Mod(DimLibEntry.MODID)
public class DimLibEntry {
    public static final Logger LOGGER = LoggerFactory.getLogger(DimLibEntry.class);
	public static final String MODID = "dimlib";

	public DimLibEntry(FMLJavaModLoadingContext context) {
		LOGGER.info("DimLib initializing");

		DynamicDimensionsImpl.init();

		DimensionTemplate.init();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			((IMinecraftServer) server).dimlib_processTasks();
		});

		MidnightConfig.init(
				MODID, DimLibConfig.class
		);

		context.getModEventBus().addListener(this::onClientSetup);
		MinecraftForge.EVENT_BUS.register(this);
	}


	public void onClientSetup(FMLClientSetupEvent event) {
		LOGGER.info("client setup");
		ClientPlayNetworking.registerGlobalReceiver(
				DimSyncPacket.TYPE,
				(DimSyncPacket packet, LocalPlayer player, PacketSender responseSender) -> {
					packet.handle(player.connection);
				}
		);
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		LOGGER.info("registering commands");
		DimsCommand.register(event.getDispatcher());
	}
}