package net.countercraft.movecraft.util.teleport;

import net.countercraft.movecraft.Movecraft;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Code taken with permission from MicleBrick
 * https://www.spigotmc.org/threads/teleport-player-smoothly.317416/
 * Used for 1.17.1
 */
public class MojangMappedTeleport extends AbstractTeleport {
    public static boolean initialize() {
        return true;
    }

    public static void teleport(Player player, @NotNull Location location, float yawChange, float pitchChange) {
        Movecraft.getInstance().getWorldHandler().addPlayerLocation(player,
                location.getX() - player.getLocation().getX(),
                location.getY() - player.getLocation().getY(),
                location.getZ() - player.getLocation().getZ(),
                yawChange, pitchChange
        );
    }
}
