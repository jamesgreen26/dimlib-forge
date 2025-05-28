package qouteall.dimlib;

import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.MappedRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.dimlib.api.DimensionAPI;

public class DimsCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands
                .literal("dims")
                .requires(source -> source.hasPermission(2));

        builder.then(Commands
                .literal("clone_dimension")
                .then(Commands.argument("templateDimension", DimensionArgument.dimension())
                        .then(Commands.argument("newDimensionID", StringArgumentType.string())
                                .executes(context -> {
                                    ServerLevel templateDimension =
                                            DimensionArgument.getDimension(context, "templateDimension");
                                    String newDimensionId = StringArgumentType.getString(context, "newDimensionID");

                                    ResourceLocation newDimId = parseDimensionId(context, newDimensionId);

                                    if (newDimId == null) {
                                        return 0;
                                    }

                                    cloneDimension(
                                            templateDimension, newDimId
                                    );

                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Dynamically added dimension %s".formatted(newDimensionId)
                                    ), true);

                                    return 0;
                                })
                        )
                )
        );

        // to make q_misc_util be able to work server-only
        // we cannot use custom command argument type
        RequiredArgumentBuilder<CommandSourceStack, String> addDimensionCommandNode =
                Commands.argument("newDimensionId", StringArgumentType.string());

        for (var e : DimensionTemplate.DIMENSION_TEMPLATES.entrySet()) {
            String dimTemplateId = e.getKey();
            DimensionTemplate dimensionTemplate = e.getValue();
            addDimensionCommandNode.then(Commands.literal(dimTemplateId)
                    .executes(context -> {
                        return runAddDimension(context, dimensionTemplate);
                    })
            );
        }

        builder.then(Commands.literal("add_dimension")
                .then(addDimensionCommandNode)
        );

        builder.then(Commands
                .literal("remove_dimension")
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(context -> {
                            ServerLevel dimension =
                                    DimensionArgument.getDimension(context, "dimension");

                            DimensionAPI.removeDimensionDynamically(dimension);

                            context.getSource().sendSuccess(() -> Component.literal(
                                    ("Dynamically removed dimension %s . Its world file is not yet deleted. " +
                                            "Note: if the datapack config for that dimension exists, the dimension will be re-added after server restart.")
                                            .formatted(dimension.dimension().location())
                            ), true);

                            return 0;
                        })
                )
        );

        builder.then(Commands.literal("view_dim_config")
                .then(Commands.argument("dim", DimensionArgument.dimension())
                        .executes(context -> {
                            ServerLevel world =
                                    DimensionArgument.getDimension(context, "dim");

                            MappedRegistry<LevelStem> dimensionRegistry =
                                    DimensionImpl.getDimensionRegistry(world.getServer());

                            LevelStem levelStem = dimensionRegistry.get(world.dimension().location());

                            if (levelStem == null) {
                                context.getSource().sendFailure(
                                        Component.literal("Dimension config not found")
                                );
                                return 0;
                            }

                            DataResult<JsonElement> encoded = LevelStem.CODEC.encodeStart(
                                    RegistryOps.create(JsonOps.INSTANCE, world.registryAccess()),
                                    levelStem
                            );

                            if (encoded.result().isPresent()) {
                                String jsonStr = DimLibUtil.GSON.toJson(encoded.result().get());

                                context.getSource().sendSuccess(
                                        () -> Component.literal(jsonStr),
                                        true
                                );
                            } else {
                                context.getSource().sendFailure(Component.literal(
                                        encoded.error().toString()
                                ));
                            }

                            return 0;
                        })
                )
        );

        dispatcher.register(builder);
    }

    @Nullable
    private static ResourceLocation parseDimensionId(
            CommandContext<CommandSourceStack> context, String newDimensionId
    ) {
        ResourceLocation newDimId;
        try {
            newDimId = ResourceLocation.parse(newDimensionId);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Invalid dimension id"));
            return null;
        }

        MinecraftServer server = context.getSource().getServer();

        if (newDimId.getNamespace().equals("minecraft")) {
            context.getSource().sendFailure(
                    Component.literal("namespace cannot be minecraft")
            );
            return null;
        }

        if (DimensionAPI.dimensionExistsInRegistry(server, newDimId)) {
            context.getSource().sendFailure(
                    Component.literal("Dimension" + newDimId + " already exists")
            );
            return null;
        }

        return newDimId;
    }

    private static int runAddDimension(
            CommandContext<CommandSourceStack> context, DimensionTemplate template
    ) {
        String newDimensionId = StringArgumentType.getString(
                context, "newDimensionId"
        );

        ResourceLocation newDimId = parseDimensionId(context, newDimensionId);

        if (newDimId == null) {
            return 0;
        }

        MinecraftServer server = context.getSource().getServer();

        DimensionAPI.addDimensionDynamically(
                server,
                newDimId,
                template.createLevelStem(server)
        );

        return 0;
    }

    private static void cloneDimension(
            ServerLevel templateDimension, ResourceLocation newDimId
    ) {
        // may throw exception here

        ChunkGenerator generator = templateDimension.getChunkSource().getGenerator();

        DimensionAPI.addDimension(
                templateDimension.getServer(),
                newDimId,
                new LevelStem(
                        templateDimension.dimensionTypeRegistration(),
                        generator
                )
        );
    }

}
