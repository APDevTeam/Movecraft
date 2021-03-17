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
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MutableHitBox;
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

    boolean isNotProcessing();

    void setProcessing(boolean processing);

    @NotNull
    HitBox getHitBox();

    void setHitBox(@NotNull HitBox hitBox);

    @NotNull
    CraftType getType();

    @NotNull @Deprecated
    World getW();

    @NotNull
    default World getWorld(){
        return getW();
    }

    @Deprecated
    void setW(World world);

    default void setWorld(World world){
        setW(world);
    }

    void detect(Player player, Player notificationPlayer, MovecraftLocation startPoint);

    void translate(World world, int dx, int dy, int dz);

    @Deprecated
    void translate(int dx, int dy, int dz);

    void rotate(Rotation rotation, MovecraftLocation originPoint);

    void rotate(Rotation rotation, MovecraftLocation originPoint, boolean isSubCraft);

    boolean getCruising();

    void setCruising(boolean cruising);

    boolean getSinking();

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

    int getLastDX();

    void setLastDX(int dX);

    int getLastDY();

    void setLastDY(int dY);

    int getLastDZ();

    void setLastDZ(int dZ);

    double getBurningFuel();

    void setBurningFuel(double burningFuel);

    int getOrigBlockCount();

    void setOrigBlockCount(int origBlockCount);

    @Nullable @Deprecated
    Player getNotificationPlayer();

    @Deprecated
    void setNotificationPlayer(@Nullable Player notificationPlayer);

    long getOrigPilotTime();

    float getMeanCruiseTime();

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
