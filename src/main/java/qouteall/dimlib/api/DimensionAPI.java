package qouteall.dimlib.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.dimlib.DimensionImpl;
import qouteall.dimlib.DimensionTemplate;
import qouteall.dimlib.DynamicDimensionsImpl;
import qouteall.dimlib.client.ClientDimensionInfo;
import qouteall.dimlib.ducks.IMinecraftServer;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class DimensionAPI {
    private static final Logger LOGGER = LogManager.getLogger();


    /**
     * Add a new dimension.
     * Can be used both when server is running or during [broken link].
     * Note: Should not register a dimension that already exists.
     * The added dimension's config will be saved into `level.dat`
     */
    public static void addDimension(
            MinecraftServer server,
            ResourceLocation dimensionId,
            LevelStem levelStem
    ) {
        if (((IMinecraftServer) server).dimlib_getIsFinishedCreatingWorlds()) {
            addDimensionDynamically(server, dimensionId, levelStem);
        } else {
            if (((IMinecraftServer) server).dimlib_getCanDirectlyRegisterDimensions()) {
                DimensionImpl.directlyRegisterLevelStem(server, dimensionId, levelStem);
            } else {
                LOGGER.error(
                        "Cannot add dimension at this time {}", dimensionId, new Throwable()
                );
            }
        }
    }

    /**
     * Check if a dimension exists in registry.
     * This can be used when the server worlds are not yet initialized.
     */
    public static boolean dimensionExistsInRegistry(
            MinecraftServer server, ResourceLocation dimensionId
    ) {
        // if the server is not yet running, getLevel() doesn't work
        return DimensionImpl.getDimensionRegistry(server).containsKey(dimensionId);
    }

    /**
     * Similar to {@link DimensionAPI#addDimension(MinecraftServer, ResourceLocation, LevelStem)},
     * but will not add the dimension if it already exists.
     */
    public static void addDimensionIfNotExists(
            MinecraftServer server,
            ResourceLocation dimensionId,
            Supplier<LevelStem> levelStem
    ) {
        if (dimensionExistsInRegistry(server, dimensionId)) {
            return;
        }

        addDimension(server, dimensionId, levelStem.get());
    }

    /**
     * Add a new dimension when the server is running.
     * Can only be used when server is running.
     * The new dimension's config will be saved in the `level.dat` file.
     */
    public static void addDimensionDynamically(
            MinecraftServer server,
            ResourceLocation dimensionId,
            LevelStem levelStem
    ) {
        Validate.isTrue(server.isRunning(), "The server is not running");
        DynamicDimensionsImpl.addDimensionDynamically(server, dimensionId, levelStem);
    }

    /**
     * Remove a dimension dynamically.
     * Cannot be used during server initialization.
     * Cannot remove vanilla dimensions.
     * The removed dimension's config will be removed from the `level.dat` file.
     * Does not delete that dimension's world saving files.
     */
    public static void removeDimensionDynamically(ServerLevel world) {
        if (!world.getServer().isRunning()) {
            LOGGER.error(
                    "Cannot remove dimension at this time {}", world, new Throwable()
            );
            return;
        }

        DynamicDimensionsImpl.removeDimensionDynamically(world);
    }

    /**
     * Is the dimension still in the server.
     * Can be used when the server is running.
     */
    public static boolean isDimensionAlive(ServerLevel world) {
        return world.getServer().getLevel(world.dimension()) == world;
    }

    /**
     * Disable the "Worlds using Experimental Settings are not supported" warning screen.
     * This should be called during initialization.
     */
    public static void suppressExperimentalWarning() {
        DimensionImpl.suppressExperimentalWarning = true;
    }

    /**
     * Mark a namespace as "stable".
     * Then the dimensions with that namespace will not cause "Worlds using Experimental Settings are not supported" warning screen to appear (but other dimensions may do).
     * This is not needed if {@link DimensionAPI#suppressExperimentalWarning()} is called.
     * This should be called during initialization.
     */
    public static void suppressExperimentalWarningForNamespace(String namespace) {
        DimensionImpl.STABLE_NAMESPACES.add(namespace);
    }

    /**
     * @return The dimension ids. Can be called in client.
     */
    public static Set<ResourceKey<Level>> getClientDimensionIds() {
        return ClientDimensionInfo.getDimensionIds();
    }

    /**
     * @return The map from dimension id to dimension type id. Can be called in client.
     */
    public static Map<ResourceKey<Level>, ResourceKey<DimensionType>> getClientDimensionIdToTypeMap() {
        return ClientDimensionInfo.getDimensionIdToType();
    }

    /**
     * The dimension templates are used in `/dims add_dimension` command.
     * Should register during initialization.
     * The name should not contain spaces.
     */
    public static void registerDimensionTemplate(
            String name, DimensionTemplate dimensionTemplate
    ) {
        DimensionTemplate.registerDimensionTemplate(name, dimensionTemplate);
    }
}
