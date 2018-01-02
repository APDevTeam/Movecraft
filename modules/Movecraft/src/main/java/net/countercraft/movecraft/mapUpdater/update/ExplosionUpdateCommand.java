package net.countercraft.movecraft.mapUpdater.update;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import net.countercraft.movecraft.Movecraft;
import org.bukkit.Location;

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
        Movecraft.getInstance().getLogger().info("Explosion of strength " + explosionStrength);
        //if (explosionStrength > 0) { // don't bother with tiny explosions

            //Location loc = new Location(explosionLocation.getWorld(), explosionLocation.getX() + 0.5, explosionLocation.getY() + 0.5, explosionLocation.getZ());
            this.createExplosion(explosionLocation.add(.5,.5,.5), explosionStrength / 100.0F);

        //}

    }

    private void createExplosion(Location loc, float explosionPower) {
        if (Movecraft.getInstance().getWorldGuardPlugin() != null) {
            ApplicableRegionSet set = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(loc.getWorld()).getApplicableRegions(loc);
            if (!set.allows(DefaultFlag.OTHER_EXPLOSION)) {
               return;
            }
        }
        loc.getWorld().createExplosion(loc.getX() + 0.5, loc.getY() + 0.5, loc.getZ() + 0.5, explosionPower);
    }
}
