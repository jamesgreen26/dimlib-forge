package qouteall.dimlib.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import qouteall.dimlib.DimLibEntry;

public class DimLibNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            DimSyncPacket.DIM_SYNC_CHANNEL,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        INSTANCE.messageBuilder(DimSyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DimSyncPacket::write)
                .decoder(DimSyncPacket::new)
                .consumerMainThread(DimSyncPacket::handle)
                .add();
    }
} 