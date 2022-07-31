package net.countercraft.movecraft.util.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractTeleport {

    public static boolean initialize() {
        return false;
    }

    public static void teleport(Player player, @NotNull Location location, float yawChange, float pitchChange) { }
}
