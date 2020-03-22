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

package net.countercraft.movecraft;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Block aligned coordinate triplet.
 */
final public class MovecraftLocation implements Comparable<MovecraftLocation> {
    private final int x, y, z;

    public MovecraftLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    /**
     * Returns a MovecraftLocation that has undergone the given translation.
     * <p>
     * This does not change the MovecraftLocation that it is called upon and that should be accounted for in terms of Garbage Collection.
     *
     * @param dx - X translation
     * @param dy - Y translation
     * @param dz - Z translation
     * @return New MovecraftLocation shifted by specified amount
     */
    public MovecraftLocation translate(int dx, int dy, int dz) {
        return new MovecraftLocation(x + dx, y + dy, z + dz);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MovecraftLocation) {
            MovecraftLocation location = (MovecraftLocation) o;
            return location.x==this.x && location.y==this.y && location.z == this.z;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (x ^ (z << 12)) ^ (y << 24);
    }

    public MovecraftLocation add(MovecraftLocation l) {
        return new MovecraftLocation(getX() + l.getX(), getY() + l.getY(), getZ() + l.getZ());
    }

    public MovecraftLocation subtract(MovecraftLocation l) {
        return new MovecraftLocation(getX() - l.getX(), getY() - l.getY(), getZ() - l.getZ());
    }

    /**
     *
     * Gives the euclidean distance between this MovecraftLocation and another MovecraftLocation
     *
     * @param other the MovecraftLocation distant from this one
     * @return the euclidean distance between this and the other MovecraftLocation
     */
    public int distanceSquared(MovecraftLocation other) {
        int diffx = this.x - other.x;
        int diffy = this.y - other.y;
        int diffz = this.z - other.z;
        return diffx * diffx + diffy * diffy + diffz * diffz;
    }

    /**
     *
     * Gives the direct distance between this MovecraftLocation and another MovecraftLocation
     *
     * @param other the MovecraftLocation distant from this one
     * @return the direct distance between this and the other MovecraftLocation
     */
    public double distance(MovecraftLocation other) {
        return Math.sqrt(distanceSquared(other));
    }
    public Location toBukkit(World world){
        return new Location(world, this.x, this.y, this.z);
    }

    public static Location toBukkit(World world, MovecraftLocation location){
        return new Location(world, location.x, location.y, location.z);
    }
    public static MovecraftLocation fromVector(Vector vector){
        return new MovecraftLocation(vector.getBlockX(),vector.getBlockY(),vector.getBlockZ());
    }

    @Override
    public String toString(){
        return "(" + x + "," + y + "," + z +")";
    }

    @Override
    public int compareTo(@NotNull MovecraftLocation o) {
        return getX() - o.getX() + getY() - o.getY() + getZ() - o.getZ();
    }
}
