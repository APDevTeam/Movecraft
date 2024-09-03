package net.countercraft.movecraft.util;

import net.countercraft.movecraft.SmoothTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitTeleport extends SmoothTeleport {
    @Override
    public void teleport(@NotNull Player player, @NotNull Location location) {
        player.teleport(location);
    }
}
