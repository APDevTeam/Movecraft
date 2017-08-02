package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.utils.MovecraftLocation;

public class ExplosionUpdateCommand {
    private final MovecraftLocation explosionLocation;
    private final float explosionStrength;

    public ExplosionUpdateCommand(MovecraftLocation explosionLocation, float explosionStrength) {
        this.explosionLocation = explosionLocation;
        this.explosionStrength = explosionStrength;
    }

    public MovecraftLocation getLocation() {
        return explosionLocation;
    }

    public float getStrength() {
        return explosionStrength;
    }
}
