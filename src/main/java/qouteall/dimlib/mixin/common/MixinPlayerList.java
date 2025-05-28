package qouteall.dimlib.mixin.common;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.dimlib.network.DimLibNetworkHandler;
import qouteall.dimlib.network.DimSyncPacket;

@Mixin(PlayerList.class)
public class MixinPlayerList {
    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundChangeDifficultyPacket;<init>(Lnet/minecraft/world/Difficulty;Z)V"))
    private void onConnectionEstablished(Connection netManager, ServerPlayer player, CallbackInfo ci) {
        DimLibNetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                DimSyncPacket.createPacket(player.server)
        );
    }
}
