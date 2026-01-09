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
import net.countercraft.movecraft.TrackedLocation;
import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import net.countercraft.movecraft.craft.datatag.CraftDataTagRegistry;
import net.countercraft.movecraft.craft.type.CraftProperties;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.events.CraftStopCruiseEvent;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.MutableHitBox;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.naming.Name;
import java.util.*;
import java.util.function.BiConsumer;

public interface Craft {
    // TODO: Unify with RECENT_CONTACTS tag and use a object to also store distance and direction
    CraftDataTagKey<List<UUID>> CONTACTS = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "contacts"), craft -> new ArrayList<>(0));
    CraftDataTagKey<Double> FUEL = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "fuel"), craft -> 0D);
    // TODO: Use NamespacedKeys!
    @Deprecated(forRemoval = true)
    CraftDataTagKey<Counter<Material>> MATERIALS = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "materials"), craft -> new Counter<>());
    CraftDataTagKey<Counter<NamespacedKey>> BLOCKS = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "blocks"), craft -> new Counter<>());
    CraftDataTagKey<Counter<RequiredBlockEntry>> FLYBLOCKS = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "flyblocks"), craft -> new Counter<>());
    CraftDataTagKey<Counter<RequiredBlockEntry>> MOVEBLOCKS = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "moveblocks"), craft -> new Counter<>());
    CraftDataTagKey<Integer> NON_NEGLIGIBLE_BLOCKS = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "non-negligible-blocks"), Craft::getOrigBlockCount);
    CraftDataTagKey<Integer> NON_NEGLIGIBLE_SOLID_BLOCKS = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "non-negligible-solid-blocks"), Craft::getOrigBlockCount);
    CraftDataTagKey<MovecraftLocation> CRAFT_ORIGIN = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "craft-origin"), craft -> craft.getHitBox().getMidPoint());

    // Java disallows private or protected fields in interfaces, this is a workaround
    class Hidden {
        // Concurrent so we don't have problems when accessing async (useful for addon plugins that want to do stuff async, for example NPC crafts with complex off-thread pathfinding)
        protected static final Map<UUID, Craft> uuidToCraft = Collections.synchronizedMap(new WeakHashMap<>());
    }

    public static Craft getCraftByUUID(final UUID uuid) {
        return Hidden.uuidToCraft.getOrDefault(uuid, null);
    }

    public default UUID getUUID() {
        return null;
    }

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
    @Deprecated(forRemoval = true)
    // Use getCraftProperties() instead!
    CraftType getType();

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

    /**
     * Attempts to translate the blocks controlled by the craft by a vector. If a world argument is supplied, the blocks will be transformed to a different world.
     * @param world The world to move to
     * @param dv The translation vector
     * @return The discrete (integer) translation vector performed
     */
    Vector translate(World world, Vector dv);

    @Deprecated
    void translate(int dx, int dy, int dz);

    /**
     * Attempts to rotate the blocks controlled by the craft.
     * @param rotation The direction to rotate the craft
     * @param originPoint the origin point of the rotation
     */
    default boolean rotate(MovecraftRotation rotation, MovecraftLocation originPoint) {
        return this.rotate(rotation, originPoint, null);
    }
    boolean rotate(MovecraftRotation rotation, MovecraftLocation originPoint, BiConsumer<Craft, MovecraftRotation> rotationProcessor);

    @Deprecated
    default boolean rotate(MovecraftRotation rotation, MovecraftLocation originPoint, boolean isSubCraft) {
        return rotate(rotation, originPoint, isSubCraft, null);
    }
    boolean rotate(MovecraftRotation rotation, MovecraftLocation originPoint, boolean isSubCraft, BiConsumer<Craft, MovecraftRotation> rotationProcessor);

    /**
     * Gets the cruising state of the craft.
     * @return The cruse state of the craft
     */
    boolean getCruising();

    /**
     * Sets the craft to cruise or not cruise.
     * @param cruising the desired cruise state
     */
    default void setCruising(boolean cruising) {
        setCruising(cruising, CraftStopCruiseEvent.Reason.UNKNOWN);
    }

    void setCruising(boolean cruising, CraftStopCruiseEvent.Reason reason);

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

    void setMaxBurningFuel(double value);

    double getMaxBurningFuel();

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
    default String getNameRaw() {
            Component compname = this.getName();
            return PlainTextComponentSerializer.plainText().serialize(compname);
    }

    Component getName();

    default void setName(@NotNull String name) {
        this.setName(Component.text(name));
    }

    void setName(Component name);

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

    <T> void setDataTag(@NotNull final CraftDataTagKey<T> tagKey, final T data);

    <T> T getDataTag(@NotNull final CraftDataTagKey<T> tagKey);

    <T> boolean hasDataTag(@NotNull final CraftDataTagKey<T> tagKey);

    void resetDataTag(@NotNull final CraftDataTagKey<?> tagKey);

    public default void markTileStateWithUUID(TileState tile) {
        // Add the marker
        tile.getPersistentDataContainer().set(
                MathUtils.KEY_CRAFT_UUID,
                PersistentDataType.STRING,
                this.getUUID().toString()
        );
    }

    public default void removeUUIDMarkFromTile(TileState tile) {
        tile.getPersistentDataContainer().remove(MathUtils.KEY_CRAFT_UUID);
    }

    Map<NamespacedKey, Set<TrackedLocation>> getTrackedLocations();

    public default void setCruiseCooldownMultiplier(double value) {
        // Do nothing by default
    }

    public default double getCruiseCooldownMultiplier() {
        return 1;
    }

    public default MovecraftLocation getCraftOrigin() {
        return this.getDataTag(CRAFT_ORIGIN);
    }

    public CraftProperties getCraftProperties();
}
