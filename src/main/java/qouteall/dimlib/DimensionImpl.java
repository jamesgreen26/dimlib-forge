package qouteall.dimlib;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.dimlib.ducks.IMappedRegistry;

import java.util.HashSet;

public class DimensionImpl {

    public static final Logger LOGGER = LoggerFactory.getLogger(DimensionImpl.class);

    public static final HashSet<String> STABLE_NAMESPACES = new HashSet<>();
    public static boolean suppressExperimentalWarning = false;

    public static void directlyRegisterLevelStem(
            MinecraftServer server, ResourceLocation dimensionId, LevelStem levelStem
    ) {
        RegistryAccess.Frozen registryAccess = server.registryAccess();

        WorldData worldData = server.getWorldData();
        WorldOptions worldOptions = worldData.worldGenOptions();

        MappedRegistry<LevelStem> levelStems = (MappedRegistry<LevelStem>)
                registryAccess.registryOrThrow(Registries.LEVEL_STEM);

        if (!levelStems.containsKey(dimensionId)) {
            // the vanilla freezing mechanism is used for validating dangling object references
            // for this API, that thing won't happen
            boolean oldIsFrozen = ((IMappedRegistry) levelStems).dimlib_getIsFrozen();
            ((IMappedRegistry) levelStems).dimlib_setIsFrozen(false);

            try {
                levelStems.register(
                        ResourceKey.create(Registries.LEVEL_STEM, dimensionId),
                        levelStem,
                        Lifecycle.stable()
                );
            } finally {
                ((IMappedRegistry) levelStems).dimlib_setIsFrozen(oldIsFrozen);
            }
        } else {
            LOGGER.error(
                    "The dimension {} already exists",
                    dimensionId,
                    new Throwable()
            );
        }
    }

    public static MappedRegistry<LevelStem> getDimensionRegistry(MinecraftServer server) {
        return ((MappedRegistry<LevelStem>)
                server.registryAccess().registryOrThrow(Registries.LEVEL_STEM)
        );
    }
}
