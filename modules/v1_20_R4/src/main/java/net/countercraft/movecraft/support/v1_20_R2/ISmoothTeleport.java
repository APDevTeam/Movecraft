package net.countercraft.movecraft.support.v1_20_R4;

import net.countercraft.movecraft.SmoothTeleport;
import net.countercraft.movecraft.util.ReflectUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Code derived from code taken with permission from MicleBrick
 * https://www.spigotmc.org/threads/teleport-player-smoothly.317416/
 * Used for 1.20.4
 */
public class ISmoothTeleport extends SmoothTeleport {
    private final Field teleportPosField;
    private final Field teleportAwaitField;
    private final Field awaitingTeleportTimeField;
    private final Field tickCountField;

    public ISmoothTeleport() throws NoSuchFieldException, ClassNotFoundException {
        teleportPosField = ReflectUtils.getField(ServerGamePacketListenerImpl.class, "B"); // awaitingPositionFromClient
        teleportAwaitField = ReflectUtils.getField(ServerGamePacketListenerImpl.class, "C"); // awaitingTeleport
        awaitingTeleportTimeField = ReflectUtils.getField(ServerGamePacketListenerImpl.class, "D"); // awaitingTeleportTime
        tickCountField = ReflectUtils.getField(ServerGamePacketListenerImpl.class, "k"); // tickCount
    }

    public void teleport(Player player, @NotNull Location location, float yawChange, float pitchChange) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        ServerPlayer handle = (ServerPlayer) ReflectUtils.getHandle(player);

        try {
            handle.absMoveTo(x, y, z, handle.getXRot(), handle.getYRot());
            ServerGamePacketListenerImpl connection = handle.connection;
            teleportPosField.set(connection, new Vec3(x, y, z));
            int teleportAwait = teleportAwaitField.getInt(connection) + 1;
            if (teleportAwait == Integer.MAX_VALUE)
                teleportAwait = 0;
            teleportAwaitField.setInt(connection, teleportAwait);
            awaitingTeleportTimeField.set(connection, tickCountField.get(connection));

            ClientboundPlayerPositionPacket packet = new ClientboundPlayerPositionPacket(x, y, z, yawChange, pitchChange, Set.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT), teleportAwait);
            connection.send(packet);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
