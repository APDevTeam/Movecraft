package net.countercraft.movecraft;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class SmoothTeleport {
    public abstract void teleport(Player player, @NotNull Location location, float yawChange, float pitchChange);
}
