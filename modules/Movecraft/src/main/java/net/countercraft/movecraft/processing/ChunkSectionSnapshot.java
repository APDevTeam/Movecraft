package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Material;

public class ChunkSectionSnapshot {
    private final Material[] materials;

    public ChunkSectionSnapshot(Material[] materials) {
        this.materials = materials;
    }

    public Material getMaterial(MovecraftLocation location){
        return this.getMaterial(location.getX(), location.getY(), location.getZ());
    }

    public Material getMaterial(int x, int y, int z){
        return this.materials[x + 16 * y + 16 * 16 * z];
    }
}
