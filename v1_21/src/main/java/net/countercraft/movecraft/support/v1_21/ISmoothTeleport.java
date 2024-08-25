package net.countercraft.movecraft.support.v1_21;

import io.papermc.paper.entity.TeleportFlag;
import net.countercraft.movecraft.SmoothTeleport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ISmoothTeleport extends SmoothTeleport {
    public void teleport(@NotNull Player player, @NotNull Location location) {
        Bukkit.getServer().getLogger().info("Teleporting " + player.getName() + " to " + location);
        player.teleport(
                location,
                TeleportFlag.Relative.X,
                TeleportFlag.Relative.Y,
                TeleportFlag.Relative.Z,
                TeleportFlag.Relative.PITCH,
                TeleportFlag.Relative.YAW
        );
    }
}
