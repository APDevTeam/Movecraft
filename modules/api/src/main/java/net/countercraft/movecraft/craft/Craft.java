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
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.MutableHitBox;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public interface Craft {

    @Deprecated
    boolean isNotProcessing();

    @Deprecated
    void setProcessing(boolean processing);

    @NotNull
    HitBox getHitBox();

    void setHitBox(@NotNull HitBox hitBox);

    @NotNull
    CraftType getType();

    @Deprecated(forRemoval = true) @NotNull
    default World getW(){
        return this.getWorld();
    }

    @NotNull
    MovecraftWorld getMovecraftWorld();

    @NotNull
    World getWorld();

    @Deprecated(forRemoval = true)
    default void setW(@NotNull World world){
        this.setWorld(world);
    }

    void setWorld(@NotNull World world);

    void translate(World world, int dx, int dy, int dz);

    @Deprecated
    void translate(int dx, int dy, int dz);

    void rotate(Rotation rotation, MovecraftLocation originPoint);

    @Deprecated
    void rotate(Rotation rotation, MovecraftLocation originPoint, boolean isSubCraft);

    boolean getCruising();

    void setCruising(boolean cruising);

    @Deprecated
    boolean getSinking();

    @Deprecated
    void sink();


    /**
     * Gets the crafts that have made contact with this craft
     * @return a set of crafts on contact with this craft
     */
    Set<Craft> getContacts();

    boolean getDisabled();

    void setDisabled(boolean disabled);

    CruiseDirection getCruiseDirection();

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

    @Nullable @Deprecated(forRemoval = true)
    Player getNotificationPlayer();

    @Deprecated
    void setNotificationPlayer(@Nullable Player notificationPlayer);

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
}
