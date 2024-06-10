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

package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.MutableHitBox;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface Craft {

    @Deprecated
    boolean isNotProcessing();

    @Deprecated
    void setProcessing(boolean processing);
    /**
     * Gets a HitBox representing the current locations that this craft controls
     *
     * @return the crafts current HitBox
     */
    @NotNull
    HitBox getHitBox();

    /**
     * Sets the HitBox representing the current locations that this craft controls
     */
    void setHitBox(@NotNull HitBox hitBox);
    /**
     * Gets the CraftType used to determine the Craft's behaviours
     *
     * @return the Craft's CraftType
     */
    @NotNull
    CraftType getType();


    Map<Object, Collection<Object>> trackedLocations = new HashMap<>();

    Map<String, Object> craftTags = new HashMap<>();

    /**
     * Attaches a Key-Value Pair to a Craft Object. 
     */
    void setDataTag(@NotNull String key, @Nullable Object data);

    /**
     * Gets the Value of and then Removes the Key, if Present (Otherwise Returns Null).
     */
    @Nullable
    Object removeDataTag(String key);

    /**
     * Gets a Tag Value from a Keys.
     */
    @Nullable
    Object getDataTag(String key);

    /**
     * Gets all Tag Values from all Keys.
     */
    @NotNull
    Collection<Object> getAllTagValues();

    /**
     * Checks if the Craft has a certain Key out of all Keys
     */
    @NotNull
    boolean hasDataKey(String key);

    /**
     * Checks if the Craft has a certain Value from all Keys
     */
    @NotNull
    boolean hasDataValue(Object value);

    /**
     * Translates the given HitBox by a MovecraftLocation (Which is Treated as a Vector)
     */
    @NotNull
    HitBox translateBox(HitBox box, MovecraftLocation vec);

    /**
     * Rotates the given HitBox around an Axis by a MovecraftRotation
     */
    @NotNull
    HitBox rotateBox(HitBox box, MovecraftLocation axis, MovecraftRotation rotation);

    /**
     * Checks if an Object (MovecraftLocation / HitBox) is currently being Tracked.
     */
    @NotNull
    boolean isTracking(@NotNull Object tracked);

    /**
     * Replaces/Updates a Key of Tracked MovecraftLocation or HitBox
     */
    void setTrackedLocs(@NotNull Collection<Object> tracked, @NotNull Object key);
    /**
     * Adds a Tracked MovecraftLocation or HitBox to a Key
     */
    void addTrackedLoc(@NotNull Object tracked, @NotNull Object key);
    /**
     * Removes a Tracked MovecraftLocation or HitBox from a Key
     */
    void removeTrackedLoc(@NotNull Object tracked, @NotNull Object key);
    /**
     * Gets all Tracked MovecraftLocations (Including from Tracked HitBoxes) from all Keys
     */
    Set<Object> getAllTrackedLocation();

    /**
     * Gets all Tracked MovecraftLocations (Including from Tracked HitBoxes) or HitBox from a particular key
     */
    @Nullable
    Set<MovecraftLocation> getTrackedLocs(@NotNull Object key);

    /**
     * Gets a HitBox representing the current locations that this craft controls
     *
     * @return the crafts current HitBox
     */

    
    @Deprecated(forRemoval = true) @NotNull
    default World getW(){
        return this.getWorld();
    }

    /**
     * Gets a MovecraftWorld representing the crafts current world. This can be saftely used during processing, as opposed to {@link #getWorld()}
     * @return The MovecraftWorld representation of the crafts current world
     */
    @NotNull
    MovecraftWorld getMovecraftWorld();

    /**
     * Gets the World object that this craft is currently located in. When processing, instead use {@link #getMovecraftWorld()}
     * @return The World of this craft
     */
    @NotNull
    World getWorld();

    @Deprecated(forRemoval = true)
    default void setW(@NotNull World world){
        this.setWorld(world);
    }

    /**
     * Sets the current world of the craft. This notably does not physically move the craft - for this {@link #translate(World, int, int, int)} should be used instead.
     * @param world the world the craft should now be considered in
     */
    void setWorld(@NotNull World world);

    /**
     * Attempts to translate the blocks controlled by the craft. If a world argument is supplied, the blocks will be transformed to a different world.
     * @param world The world to move to
     * @param dx The amount to shift in the x axis
     * @param dy The amount to shift in the y axis
     * @param dz The amount to shift in the z axis
     */
    void translate(World world, int dx, int dy, int dz);

    @Deprecated
    void translate(int dx, int dy, int dz);

    /**
     * Attempts to rotate the blocks controlled by the craft.
     * @param rotation The direction to rotate the craft
     * @param originPoint the origin point of the rotation
     */
    void rotate(MovecraftRotation rotation, MovecraftLocation originPoint);

    @Deprecated
    void rotate(MovecraftRotation rotation, MovecraftLocation originPoint, boolean isSubCraft);

    /**
     * Gets the cruising state of the craft.
     * @return The cruse state of the craft
     */
    boolean getCruising();

    /**
     * Sets the craft to cruise or not cruise.
     * @param cruising the desired cruise state
     */
    void setCruising(boolean cruising);

    /**
     * Gets the crafts that have made contact with this craft
     * @return a set of crafts on contact with this craft
     */
    Set<Craft> getContacts();

    /**
     * Gets the disabled status of the craft
     * @return the disabled status of the craft
     */
    boolean getDisabled();

    /**
     * Sets the craft to be disabled or not
     * @param disabled the desired disabled state of the craft
     */
    void setDisabled(boolean disabled);

    /**
     * Gets the direction of cruise for the craft
     * @return The current CruiseDirection of the craft
     */
    CruiseDirection getCruiseDirection();

    /**
     * Sets the crafts cruise direction
     * @param cruiseDirection The desired cruise direction
     */
    void setCruiseDirection(CruiseDirection cruiseDirection);

    void setLastCruiseUpdate(long update);

    long getLastCruiseUpdate();

    long getLastBlockCheck();

    void setLastBlockCheck(long update);

    @NotNull MovecraftLocation getLastTranslation();

    void setLastTranslation(@NotNull MovecraftLocation lastTranslation);

    @Deprecated(forRemoval = true)
    default int getLastDX(){
        return getLastTranslation().getX();
    }

    @Deprecated(forRemoval = true)
    default void setLastDX(int dX){}

    @Deprecated(forRemoval = true)
    default int getLastDY(){
        return getLastTranslation().getY();
    }

    @Deprecated(forRemoval = true)
    default void setLastDY(int dY){}

    @Deprecated(forRemoval = true)
    default int getLastDZ(){
        return getLastTranslation().getZ();
    }

    @Deprecated(forRemoval = true)
    default void setLastDZ(int dZ){}

    double getBurningFuel();

    void setBurningFuel(double burningFuel);

    int getOrigBlockCount();

    void setOrigBlockCount(int origBlockCount);

    long getOrigPilotTime();

    double getMeanCruiseTime();

    void addCruiseTime(float cruiseTime);

    int getTickCooldown();

    /**
     * gets the speed of a craft in blocks per second.
     * @return the speed of the craft
     */
    double getSpeed();

    long getLastRotateTime();

    void setLastRotateTime(long lastRotateTime);

    int getWaterLine();

    @NotNull
    Map<Location, BlockData> getPhaseBlocks();

    @NotNull
    String getName();

    void setName(@NotNull String name);

    @NotNull
    MutableHitBox getCollapsedHitBox();

    @Deprecated(forRemoval = true)
    void resetSigns(@NotNull final Sign clicked);

    @NotNull
    MutableHitBox getFluidLocations();

    void setFluidLocations(@NotNull MutableHitBox fluidLocations);

    long getLastTeleportTime();

    void setLastTeleportTime(long lastTeleportTime);

    int getCurrentGear();

    void setCurrentGear(int currentGear);

    Audience getAudience();

    void setAudience(Audience audience);

    Counter<Material> getMaterials ();

    void updateMaterials (Counter<Material> materials);

    double getTotalFuel ();

    void setTotalFuel (double fuel);
}
