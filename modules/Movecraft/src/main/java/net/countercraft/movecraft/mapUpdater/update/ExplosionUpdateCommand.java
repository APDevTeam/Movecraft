package net.countercraft.movecraft.mapUpdater.update;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
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
        //if (explosionStrength > 0) { // don't bother with tiny explosions
        //Location loc = new Lo cation(explosionLocation.getWorld(), explosionLocation.getX() + 0.5, explosionLocation.getY() + 0.5, explosionLocation.getZ());
        if (Settings.Debug){
            Bukkit.broadcastMessage("Explosion strength: " + explosionStrength + " at " + explosionLocation.toVector().toString());
        }
        this.createExplosion(explosionLocation.add(.5,.5,.5), explosionStrength);
        //}

    }

    private void createExplosion(Location loc, float explosionPower) {
        if (Movecraft.getInstance().getWorldGuardPlugin() != null) {
            ApplicableRegionSet set = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(loc.getWorld()).getApplicableRegions(loc);
            if (!set.allows(DefaultFlag.OTHER_EXPLOSION)) {
               return;
            }
        }
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
