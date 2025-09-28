package net.countercraft.movecraft.support.v1_21_5;

import io.papermc.paper.entity.TeleportFlag;
import net.countercraft.movecraft.SmoothTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ISmoothTeleport extends SmoothTeleport {
    public void teleport(@NotNull Player player, @NotNull Location location) {
        player.teleport(
                location,
                TeleportFlag.Relative.VELOCITY_X,//x
                TeleportFlag.Relative.VELOCITY_Y,//y
                TeleportFlag.Relative.VELOCITY_Z,//z
                TeleportFlag.Relative.VELOCITY_ROTATION,//pitch
                TeleportFlag.Relative.VELOCITY_ROTATION,//yaw
                TeleportFlag.EntityState.RETAIN_OPEN_INVENTORY,
                TeleportFlag.EntityState.RETAIN_VEHICLE,
                TeleportFlag.EntityState.RETAIN_PASSENGERS
        );
    }
}
