package net.countercraft.movecraft.utils;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.HashSet;
import java.util.Set;

public class WorldEditUtils {
    public static Vector toWeVector(org.bukkit.util.Vector bukkitVector){
        return new Vector(bukkitVector.getBlockX(),bukkitVector.getBlockY(),bukkitVector.getBlockZ());
    }

    public static BlockVector3 toBlockVector(org.bukkit.util.Vector bukkitVector){
        return BlockVector3.at(bukkitVector.getBlockX(),bukkitVector.getBlockY(),bukkitVector.getBlockZ());
    }

    public static HashSet<Vector> toWeVectorSet(Set<org.bukkit.util.Vector> bukkitVectorSet){
        HashSet<Vector> ret = new HashSet<>();
        for (org.bukkit.util.Vector bukkitVector : bukkitVectorSet){
            ret.add(toWeVector(bukkitVector));
        }
        return ret;
    }

    public static HashSet<BlockVector3> toBlockVectorSet(Set<org.bukkit.util.Vector> bukkitVectorSet){
        HashSet<BlockVector3> ret = new HashSet<>();
        for (org.bukkit.util.Vector bukkitVector : bukkitVectorSet){
            ret.add(toBlockVector(bukkitVector));
        }
        return ret;
    }
}
