package net.countercraft.movecraft.utils;

import at.pavlov.cannons.API.CannonsAPI;
import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

public class CannonsUtils {
    public static Set<Cannon> getCannonsInHitBox(HitBox hitBox, World world) {
        Set<Cannon> foundCannons = new HashSet<>();
        for (Cannon can : CannonsAPI.getCannonsInBox(hitBox.getMidPoint().toBukkit(world), hitBox.getXLength(), hitBox.getYLength(), hitBox.getZLength())) {
            for (Location barrelLoc : can.getCannonDesign().getBarrelBlocks(can)) {
                if (!hitBox.contains(MathUtils.bukkit2MovecraftLoc(barrelLoc))) {
                    continue;
                }
                foundCannons.add(can);
                break;
            }
        }
        return foundCannons;
    }

    public static Set<Cannon> getCannonsOnCraft(Craft c) {
        return getCannonsInHitBox(c.getHitBox(), c.getWorld());
    }
}
