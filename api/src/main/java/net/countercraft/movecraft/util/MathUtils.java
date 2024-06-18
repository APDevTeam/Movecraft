/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.util;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

public class MathUtils {



    /**
     * checks if <code>location</code> is within the bounding box <code>box</code> restricted by minimum values on x and z
     * @param box the bounding box to check within
     * @param minX the minimum x coordinate to search
     * @param minZ the minimum z coordinate to search
     * @param location the location to check
     * @return True if the player is within the given bounding box
     */
    @Contract(pure=true)
    public static boolean playerIsWithinBoundingPolygon(@NotNull final int[][][] box, final int minX, final int minZ, @NotNull final MovecraftLocation location) {
        if (location.getX() >= minX && location.getX() < (minX + box.length)) {
            // PLayer is within correct X boundary
            if (location.getZ() >= minZ && location.getZ() < (minZ + box[location.getX() - minX].length)) {
                // Player is within valid Z boundary
                int minY, maxY;
                try {
                    minY = box[location.getX() - minX][location.getZ() - minZ][0];
                    maxY = box[location.getX() - minX][location.getZ() - minZ][1];
                } catch (NullPointerException e) {
                    return false;
                }
                return location.getY() >= minY && location.getY() <= (maxY + 2);
            }
        }
        return false;
    }

    /**
     * checks if the given bukkit <code>location</code> is within <code>hitbox</code>
     * @param hitBox the bounding box to check within
     * @param location the location to check
     * @return True if the player is within the given bounding box
     */
    @Contract(pure=true)
    public static boolean locationInHitBox(@NotNull final HitBox hitBox, @NotNull final Location location) {
        return hitBox.inBounds(location.getX(),location.getY(),location.getZ());
    }

    /**
     * Checks if a given <code>Location</code> is within some distance, <code>distance</code>, from a given <code>HitBox</code>
     * @param hitBox the hitbox to check
     * @param location the location to check
     * @return True if <code>location</code> is less or equal to 3 blocks from <code>craft</code>
     */
    @Contract(pure=true)
    public static boolean locationNearHitBox(@NotNull final HitBox hitBox, @NotNull final Location location, double distance) {
        return !hitBox.isEmpty() &&
                location.getX() >= hitBox.getMinX() - distance &&
                location.getZ() >= hitBox.getMinZ() - distance &&
                location.getX() <= hitBox.getMaxX() + distance &&
                location.getZ() <= hitBox.getMaxZ() + distance &&
                location.getY() >= hitBox.getMinY() - distance &&
                location.getY() <= hitBox.getMaxY() + distance;
    }

    /**
     * Checks if a given <code>Location</code> is within 3 blocks from a given <code>Craft</code>
     * @param craft the craft to check
     * @param location the location to check
     * @return True if <code>location</code> is less or equal to 3 blocks from <code>craft</code>
     */
    @Contract(pure=true)
    public static boolean locIsNearCraftFast(@NotNull final Craft craft, @NotNull final MovecraftLocation location) {
        // optimized to be as fast as possible, it checks the easy ones first, then the more computationally intensive later
        return locationNearHitBox(craft.getHitBox(), location.toBukkit(craft.getWorld()), 3);
    }

    /**
     * Creates a <code>MovecraftLocation</code> representation of a bukkit <code>Location</code> object aligned to the block grid
     * @param bukkitLocation the location to convert
     * @return a new <code>MovecraftLocation</code> representing the given location
     */
    @NotNull
    @Contract(pure=true)
    public static MovecraftLocation bukkit2MovecraftLoc(@NotNull final Location bukkitLocation) {
        return new MovecraftLocation(bukkitLocation.getBlockX(), bukkitLocation.getBlockY(), bukkitLocation.getBlockZ());
    }

    /**
     * Rotates a MovecraftLocation towards a supplied <code>Rotation</code>.
     * The resulting MovecraftRotation is based on a center of (0,0,0).
     * @param rotation the direction to rotate
     * @param movecraftLocation the location to rotate
     * @return a rotated Movecraft location
     */
    @NotNull
    @Contract(pure=true)
    public static MovecraftLocation rotateVec(@NotNull final MovecraftRotation rotation, @NotNull final MovecraftLocation movecraftLocation) {
        double theta;
        if (rotation == MovecraftRotation.CLOCKWISE) {
            theta = 0.5 * Math.PI;
        } else {
            theta = -1 * 0.5 * Math.PI;
        }

        int x = (int) Math.round((movecraftLocation.getX() * Math.cos(theta)) + (movecraftLocation.getZ() * (-1 * Math.sin(theta))));
        int z = (int) Math.round((movecraftLocation.getX() * Math.sin(theta)) + (movecraftLocation.getZ() * Math.cos(theta)));

        return new MovecraftLocation(x, movecraftLocation.getY(), z);
    }

    @NotNull
    @Deprecated
    public static double[] rotateVec(@NotNull MovecraftRotation rotation, double x, double z) {
        double theta;
        if (rotation == MovecraftRotation.CLOCKWISE) {
            theta = 0.5 * Math.PI;
        } else {
            theta = -1 * 0.5 * Math.PI;
        }

        double newX = Math.round((x * Math.cos(theta)) + (z * (-1 * Math.sin(theta))));
        double newZ = Math.round((x * Math.sin(theta)) + (z * Math.cos(theta)));

        return new double[]{newX, newZ};
    }

    @NotNull
    @Deprecated
    public static double[] rotateVecNoRound(@NotNull MovecraftRotation r, double x, double z) {
        double theta;
        if (r == MovecraftRotation.CLOCKWISE) {
            theta = 0.5 * Math.PI;
        } else {
            theta = -1 * 0.5 * Math.PI;
        }

        double newX = (x * Math.cos(theta)) + (z * (-1 * Math.sin(theta)));
        double newZ = (x * Math.sin(theta)) + (z * Math.cos(theta));

        return new double[]{newX, newZ};
    }

    @Deprecated
    public static int positiveMod(int mod, int divisor) {
        if (mod < 0) {
            mod += divisor;
        }
        return mod;
    }

    /**
     * Checks if a <link>MovecraftLocation</link> is within the border of the given <link>World</link>
     * @param world the world to check in
     * @param location the location in the given <link>World</link>
     * @return true if location is within the world border, false otherwise
     */
    @Contract(pure = true)
    public static boolean withinWorldBorder(@NotNull World world, @NotNull MovecraftLocation location) {
        WorldBorder border = world.getWorldBorder();
        int radius = (int) (border.getSize() / 2.0);
        //The visible border will always end at 29,999,984 blocks, despite being larger
        int minX = border.getCenter().getBlockX() - radius;
        int maxX = border.getCenter().getBlockX() + radius;
        int minZ = border.getCenter().getBlockZ() - radius;
        int maxZ = border.getCenter().getBlockZ() + radius;
        return Math.abs(location.getX()) < 29999984 &&
                Math.abs(location.getZ()) < 29999984 &&
                location.getX() >= minX &&
                location.getX() <= maxX &&
                location.getZ() >= minZ &&
                location.getZ() <= maxZ;
    }

    @NotNull
    public static OptionalInt parseInt(@NotNull String encoded){
        try {
            return OptionalInt.of(Integer.parseInt(encoded));
        }catch(NumberFormatException e){
            return OptionalInt.empty();
        }
    }

    @Nullable
    public static Craft fastNearestCraftToLoc(@NotNull Set<Craft> crafts, @NotNull Location loc) {
        Craft result = null;
        long closestDistSquared = Long.MAX_VALUE;

        for (Craft i : crafts) {
            if (i.getHitBox().isEmpty())
                continue;
            if (i.getWorld() != loc.getWorld())
                continue;

            int midX = (i.getHitBox().getMaxX() + i.getHitBox().getMinX()) >> 1;
            // don't check Y because it is slow
            int midZ = (i.getHitBox().getMaxZ() + i.getHitBox().getMinZ()) >> 1;
            long distSquared = (long) (Math.pow(midX -  (int) loc.getX(), 2) + Math.pow(midZ - (int) loc.getZ(), 2));
            if (distSquared < closestDistSquared) {
                closestDistSquared = distSquared;
                result = i;
            }
        }
        return result;
    }

    @Nullable
    public static Set<Craft> craftsNearLocFast(@NotNull Set<Craft> crafts, @NotNull Location loc) {
        Set<Craft> result = new HashSet<>(crafts.size(), 1);
        MovecraftLocation location = new MovecraftLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        for (Craft i : crafts) {
            if (i.getHitBox().isEmpty() || i.getWorld() != loc.getWorld() || !locIsNearCraftFast(i, location))
                continue;

            result.add(i);
        }
        return result;
    }
}
