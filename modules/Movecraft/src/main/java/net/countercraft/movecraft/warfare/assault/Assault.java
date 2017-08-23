package net.countercraft.movecraft.warfare.assault;

import com.sk89q.worldedit.Vector;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents an assault
 */
public class Assault {
    private final String regionName;
    private final UUID starterUUID;
    private final long startTime;
    private long damages;
    private final long maxDamages;
    private final World world;
    private final Vector minPos, maxPos;
    private AtomicBoolean running = new AtomicBoolean(true);

    public Assault(String regionName, Player starter, World world, long startTime, long maxDamages, Vector minPos, Vector maxPos) {
        this.regionName = regionName;
        starterUUID = starter.getUniqueId();
        this.world = world;
        this.startTime = startTime;
        this.maxDamages = maxDamages;
        this.minPos = minPos;
        this.maxPos = maxPos;
    }

    public Vector getMaxPos() {
        return maxPos;
    }

    public Vector getMinPos() {
        return minPos;
    }

    public World getWorld() {
        return world;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDamages() {
        return damages;
    }

    public void setDamages(long damages) {
        this.damages = damages;
    }

    public long getMaxDamages() {
        return maxDamages;
    }

    public UUID getStarterUUID() {
        return starterUUID;
    }


    public String getRegionName() {
        return regionName;
    }

    public AtomicBoolean getRunning() {
        return running;
    }
}
