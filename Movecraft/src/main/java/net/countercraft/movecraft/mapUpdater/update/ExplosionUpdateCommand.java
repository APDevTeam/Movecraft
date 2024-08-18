package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.events.ExplosionEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;

public class ExplosionUpdateCommand extends UpdateCommand {
    private final Location explosionLocation;
    private final float explosionStrength;
    private final boolean incendiary;

    public ExplosionUpdateCommand(Location explosionLocation, float explosionStrength, boolean incendiary) throws IllegalArgumentException {
        if(explosionStrength < 0){
            throw new IllegalArgumentException("Explosion strength cannot be negative");
        }
        this.explosionLocation = explosionLocation;
        this.explosionStrength = explosionStrength;
        this.incendiary = incendiary;
    }

    public Location getLocation() {
        return explosionLocation;
    }

    public float getStrength() {
        return explosionStrength;
    }

    public boolean isIncendiary() {
        return incendiary;
    }

    @Override
    public void doUpdate() {
        ExplosionEvent e = new ExplosionEvent(explosionLocation, explosionStrength, incendiary);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if(e.isCancelled())
            return;

        if (Settings.Debug) {
            Bukkit.broadcastMessage("Explosion strength: " + explosionStrength + " at " + explosionLocation.toVector().toString());
        }

        this.createExplosion(explosionLocation.add(.5,.5,.5), explosionStrength, incendiary);
    }

    private void createExplosion(Location loc, float explosionPower, boolean incendiary) {
        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), explosionPower, incendiary);
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
                this.explosionStrength == other.explosionStrength &&
                this.incendiary == other.incendiary;
    }
}
