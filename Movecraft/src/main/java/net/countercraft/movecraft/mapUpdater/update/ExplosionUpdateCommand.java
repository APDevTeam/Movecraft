package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.events.ExplosionEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;

public class ExplosionUpdateCommand extends UpdateCommand {
    private final Location explosionLocation;
    private final float explosionStrength;

    public ExplosionUpdateCommand(Location explosionLocation, float explosionStrength) throws IllegalArgumentException {
        if(explosionStrength < 0){
            throw new IllegalArgumentException("Explosion strength cannot be negative");
        }
        this.explosionLocation = explosionLocation;
        this.explosionStrength = explosionStrength;
    }

    public Location getLocation() {
        return explosionLocation;
    }

    public float getStrength() {
        return explosionStrength;
    }

    @Override
    public void doUpdate() {
        ExplosionEvent e = new ExplosionEvent(explosionLocation, explosionStrength);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if(e.isCancelled())
            return;

        if (Settings.Debug) {
            Bukkit.broadcastMessage("Explosion strength: " + explosionStrength + " at " + explosionLocation.toVector().toString());
        }

        this.createExplosion(explosionLocation.add(.5,.5,.5), explosionStrength);
    }

    private void createExplosion(Location loc, float explosionPower) {
        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), explosionPower);
    }

    @Override
    public int hashCode() {
        return Objects.hash(explosionLocation, explosionStrength);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ExplosionUpdateCommand)){
            return false;
        }
        ExplosionUpdateCommand other = (ExplosionUpdateCommand) obj;
        return this.explosionLocation.equals(other.explosionLocation) &&
                this.explosionStrength == other.explosionStrength;
    }
}
