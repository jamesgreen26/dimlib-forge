package qouteall.dimlib.network;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.Validate;
import qouteall.dimlib.DimLibEntry;
import qouteall.dimlib.client.ClientDimensionInfo;
import qouteall.dimlib.mixin.client.IClientPacketListener;

import java.util.function.Supplier;

import static qouteall.dimlib.DimLibEntry.MODID;

public record DimSyncPacket(
        CompoundTag dimIdToTypeIdTag
) {
    public static final ResourceLocation DIM_SYNC_CHANNEL = ResourceLocation.fromNamespaceAndPath(MODID, "dim_sync");

    public DimSyncPacket(FriendlyByteBuf buf) {
        this(buf.readNbt());
    }

    public static DimSyncPacket createPacket(MinecraftServer server) {
        RegistryAccess registryManager = server.registryAccess();
        Registry<DimensionType> dimensionTypes = registryManager.registryOrThrow(Registries.DIMENSION_TYPE);

        CompoundTag dimIdToDimTypeId = new CompoundTag();
        for (ServerLevel world : server.getAllLevels()) {
            ResourceKey<Level> dimId = world.dimension();

            DimensionType dimType = world.dimensionType();
            ResourceLocation dimTypeId = dimensionTypes.getKey(dimType);

            if (dimTypeId == null) {
                DimLibEntry.LOGGER.error("Cannot find dimension type for {}", dimId.location());
                DimLibEntry.LOGGER.error(
                        "Registered dimension types {}", dimensionTypes.keySet()
                );
                dimTypeId = BuiltinDimensionTypes.OVERWORLD.location();
            }

            dimIdToDimTypeId.putString(
                    dimId.location().toString(),
                    dimTypeId.toString()
            );
        }

        return new DimSyncPacket(dimIdToDimTypeId);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(dimIdToTypeIdTag);
    }

    public ImmutableMap<ResourceKey<Level>, ResourceKey<DimensionType>> toMap() {
        CompoundTag tag = dimIdToTypeIdTag();

        ImmutableMap.Builder<ResourceKey<Level>, ResourceKey<DimensionType>> builder =
                new ImmutableMap.Builder<>();

        for (String key : tag.getAllKeys()) {
            ResourceKey<Level> dimId = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(key)
            );
            String dimTypeId = tag.getString(key);
            ResourceKey<DimensionType> dimType = ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    ResourceLocation.parse(dimTypeId)
            );
            builder.put(dimId, dimType);
        }

        return builder.build();
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Validate.isTrue(
                    Minecraft.getInstance().isSameThread(),
                    "Not running in client thread"
            );

            DimLibEntry.LOGGER.info(
                    "Client received dimension info\n{}",
                    String.join("\n", dimIdToTypeIdTag.getAllKeys())
            );

            var dimIdToDimType = this.toMap();
            ClientDimensionInfo.accept(dimIdToDimType);
            ((IClientPacketListener) Minecraft.getInstance().getConnection()).ip_setLevels(dimIdToDimType.keySet());
        });
        context.setPacketHandled(true);
    }
}
