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

package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
    public static MovecraftLocation rotateVec(@NotNull final Rotation rotation, @NotNull final MovecraftLocation movecraftLocation) {
        double theta;
        if (rotation == Rotation.CLOCKWISE) {
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
    public static double[] rotateVec(@NotNull Rotation rotation, double x, double z) {
        double theta;
        if (rotation == Rotation.CLOCKWISE) {
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
    public static double[] rotateVecNoRound(@NotNull Rotation r, double x, double z) {
        double theta;
        if (r == Rotation.CLOCKWISE) {
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
}
