package net.countercraft.movecraft.support.v1_20;

import io.papermc.paper.entity.TeleportFlag;
import net.countercraft.movecraft.SmoothTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ISmoothTeleport extends SmoothTeleport {
    public void teleport(@NotNull Player player, @NotNull Location location, float yawChange, float pitchChange) {
        location.setYaw(yawChange);
        location.setPitch(pitchChange);
        player.teleport(location, TeleportFlag.Relative.PITCH, TeleportFlag.Relative.YAW);
    }
}
