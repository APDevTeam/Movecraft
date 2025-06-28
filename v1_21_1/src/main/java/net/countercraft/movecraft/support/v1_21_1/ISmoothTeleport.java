package net.countercraft.movecraft.support.v1_21_1;

import io.papermc.paper.entity.TeleportFlag;
import net.countercraft.movecraft.SmoothTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ISmoothTeleport extends SmoothTeleport {
    public void teleport(@NotNull Player player, @NotNull Location location) {
        player.teleport(
                location,
                TeleportFlag.Relative.X,//x
                TeleportFlag.Relative.Y,//y
                TeleportFlag.Relative.Z,//z
                TeleportFlag.Relative.PITCH,//pitch
                TeleportFlag.Relative.YAW,//yaw
                TeleportFlag.EntityState.RETAIN_OPEN_INVENTORY,
                TeleportFlag.EntityState.RETAIN_VEHICLE,
                TeleportFlag.EntityState.RETAIN_PASSENGERS
        );
    }
}
