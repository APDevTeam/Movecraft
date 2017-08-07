package net.countercraft.movecraft.mapUpdater.update;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import net.countercraft.movecraft.Movecraft;
import org.bukkit.Location;

public class ExplosionUpdateCommand implements UpdateCommand{
    private final Location explosionLocation;
    private final float explosionStrength;

    public ExplosionUpdateCommand(Location explosionLocation, float explosionStrength) {
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
        if (explosionStrength < -10) { // don't bother with tiny explosions

            //Location loc = new Location(explosionLocation.getWorld(), explosionLocation.getX() + 0.5, explosionLocation.getY() + 0.5, explosionLocation.getZ());
            this.createExplosion(explosionLocation.add(.5,.5,.5), explosionStrength / -100.0F);

        }

    }

    private void createExplosion(Location loc, float explosionPower) {
        boolean explosionBlocked = false;
        if (Movecraft.getInstance().getWorldGuardPlugin() != null) {
            ApplicableRegionSet set = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(loc.getWorld()).getApplicableRegions(loc);
            if (!set.allows(DefaultFlag.OTHER_EXPLOSION)) {
                explosionBlocked = true;
            }
        }
        if (!explosionBlocked)
            loc.getWorld().createExplosion(loc.getX() + 0.5, loc.getY() + 0.5, loc.getZ() + 0.5, explosionPower);
        return;
    }
}
