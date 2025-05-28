package qouteall.dimlib.mixin.common;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.dimlib.network.DimSyncPacket;

@Mixin(PlayerList.class)
public class MixinPlayerList {
    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundChangeDifficultyPacket;<init>(Lnet/minecraft/world/Difficulty;Z)V"))
    private void onConnectionEstablished(Connection netManager, ServerPlayer player, CallbackInfo ci) {
        player.connection.send(
                ServerPlayNetworking.createS2CPacket(
                        DimSyncPacket.DIM_SYNC_CHANNEL,
                        DimSyncPacket.createBuf(player.server)
                ));
    }
}
