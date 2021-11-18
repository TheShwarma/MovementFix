package com.guncolony.movementfixguncolony.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MovementFixMixin {
    @Final @Shadow private MinecraftClient client;

    @Shadow @Final private ClientConnection connection;

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"), cancellable = true)
    public void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo callback) {
        // if the player is teleported to an identical position, it can mean that
        // the only modification is yaw and pitch. In that case, carefully
        // apply them. The issue in vanilla is probably a result of rounding.
        if (
                packet.getX() == 0 && packet.getY() == 0 && packet.getZ() == 0
        ) {
            // left over from the original code
            NetworkThreadUtils.forceMainThread(packet, (ClientPlayNetworkHandler) (Object) this, client);
            var player = client.player;
            var yaw = player.getYaw() + packet.getYaw();
            var pitch = MathHelper.clamp(player.getPitch() + packet.getPitch(), -90, 90);

            player.setYaw(yaw);
            player.setPitch(pitch);
            player.prevYaw = packet.getYaw();
            player.prevPitch = packet.getPitch();
            // confirm that the message was received
            this.connection.send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
            this.connection.send(new PlayerMoveC2SPacket.Full(player.getX(), player.getY(), player.getZ(),
                    yaw, pitch, false));
            callback.cancel();
        }
    }
}