package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.utils.WorldguardUtils;
import org.bukkit.Location;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityExplodeEvent;

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
        this.createExplosion(explosionLocation.add(.5,.5,.5), explosionStrength);
        //}

    }

    private void createExplosion(Location loc, float explosionPower) {
        if (Movecraft.getInstance().getWorldGuardPlugin() != null) {
            if (!WorldguardUtils.explosionsPermitted(loc)){
                return;
            }
        }
        loc.getWorld().createExplosion(loc.getX() + 0.5, loc.getY() + 0.5, loc.getZ() + 0.5, explosionPower);
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
