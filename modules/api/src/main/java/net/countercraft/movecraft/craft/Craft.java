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

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.countercraft.movecraft.utils.HashHitBox;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Craft {
    @NotNull protected final CraftType type;
    //protected int[][][] hitBox;
    //protected MovecraftLocation[] blockList;
    @NotNull protected HashHitBox hitBox;
    @NotNull protected final HashHitBox collapsedHitBox;

    @NotNull protected World w;
    @NotNull private final AtomicBoolean processing = new AtomicBoolean();
    private int maxHeightLimit;
    private boolean cruising;
    private boolean sinking;
    private boolean disabled;
    private byte cruiseDirection;
    private long lastCruiseUpdate;
    private long lastBlockCheck;
    private long lastRotateTime=0;
    private long lastGravityOnRotateTime=0;
    private long origPilotTime;
    private int lastDX, lastDY, lastDZ;
    private double burningFuel;
    private boolean pilotLocked;
    private double pilotLockedX;
    private double pilotLockedY;
    private int origBlockCount;
    private double pilotLockedZ;
    @Nullable private Player notificationPlayer;
    @Nullable private Player cannonDirector;
    @Nullable private Player AADirector;
    private float meanMoveTime;
    private int numMoves;
    @NotNull private final Map<MovecraftLocation,Material> phaseBlocks = new HashMap<>();
    @NotNull private final HashMap<UUID, Location> crewSigns = new HashMap<>();
    @NotNull private String name = "";

    public Craft(@NotNull CraftType type, @NotNull World world) {
        this.type = type;
        this.w = world;
        this.hitBox = new HashHitBox();
        this.collapsedHitBox = new HashHitBox();
        if (type.getMaxHeightLimit() > w.getMaxHeight() - 1) {
            this.maxHeightLimit = w.getMaxHeight() - 1;
        } else {
            this.maxHeightLimit = type.getMaxHeightLimit();
        }
        this.pilotLocked = false;
        this.pilotLockedX = 0.0;
        this.pilotLockedY = 0.0;
        this.pilotLockedZ = 0.0;
        this.cannonDirector = null;
        this.AADirector = null;
        this.lastCruiseUpdate = System.currentTimeMillis() - 10000;
        this.cruising = false;
        this.sinking = false;
        this.disabled = false;
        this.origPilotTime = System.currentTimeMillis();
        numMoves = 0;
    }

    public boolean isNotProcessing() {
        return !processing.get();
    }

    public void setProcessing(boolean processing) {
        this.processing.set(processing);
    }

    @NotNull
    public HashHitBox getHitBox() {
        return hitBox;
    }

    public void setHitBox(@NotNull HashHitBox hitBox){
        this.hitBox = hitBox;
    }

    @NotNull
    public CraftType getType() {
        return type;
    }

    @NotNull
    public World getW() {
        return w;
    }

    public abstract void detect(Player player, Player notificationPlayer, MovecraftLocation startPoint);

    public abstract void translate(int dx, int dy, int dz);

    public abstract void rotate(Rotation rotation, MovecraftLocation originPoint);

    public abstract void rotate(Rotation rotation, MovecraftLocation originPoint, boolean isSubCraft);

    public boolean getCruising() {
        return cruising;
    }

    public void setCruising(boolean cruising) {
        if(notificationPlayer!=null){
            notificationPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Cruising " + (cruising ? "enabled" : "disabled")));
        }
        this.cruising = cruising;
    }

    public boolean getSinking() {
        return sinking;
    }

    /*public void setSinking(boolean sinking) {
        this.sinking = sinking;
    }*/

    public void sink(){
        CraftSinkEvent event = new CraftSinkEvent(this);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()){
            return;
        }
        this.sinking = true;

    }

    public boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public byte getCruiseDirection() {
        return cruiseDirection;
    }

    public void setCruiseDirection(byte cruiseDirection) {
        this.cruiseDirection = cruiseDirection;
    }

    public void setLastCruiseUpdate(long update) {
        this.lastCruiseUpdate = update;
    }

    public long getLastCruiseUpdate() {
        return lastCruiseUpdate;
    }

    public long getLastBlockCheck() {
        return lastBlockCheck;
    }

    public void setLastBlockCheck(long update) {
        this.lastBlockCheck = update;
    }

    public int getLastDX() {
        return lastDX;
    }

    public void setLastDX(int dX) {
        this.lastDX = dX;
    }

    public int getLastDY() {
        return lastDY;
    }

    public void setLastDY(int dY) {
        this.lastDY = dY;
    }

    public int getLastDZ() {
        return lastDZ;
    }

    public void setLastDZ(int dZ) {
        this.lastDZ = dZ;
    }

    public boolean getPilotLocked() {
        return pilotLocked;
    }

    public void setPilotLocked(boolean pilotLocked) {
        this.pilotLocked = pilotLocked;
    }

    public double getPilotLockedX() {
        return pilotLockedX;
    }

    public void setPilotLockedX(double pilotLockedX) {
        this.pilotLockedX = pilotLockedX;
    }

    public double getPilotLockedY() {
        return pilotLockedY;
    }

    public void setPilotLockedY(double pilotLockedY) {
        this.pilotLockedY = pilotLockedY;
    }

    public double getPilotLockedZ() {
        return pilotLockedZ;
    }

    public void setPilotLockedZ(double pilotLockedZ) {
        this.pilotLockedZ = pilotLockedZ;
    }

    public double getBurningFuel() {
        return burningFuel;
    }

    public void setBurningFuel(double burningFuel) {
        this.burningFuel = burningFuel;
    }

    public int getOrigBlockCount() {
        return origBlockCount;
    }

    public void setOrigBlockCount(int origBlockCount) {
        this.origBlockCount = origBlockCount;
    }

    @Nullable
    public Player getNotificationPlayer() {
        return notificationPlayer;
    }

    public void setNotificationPlayer(@Nullable Player notificationPlayer) {
        this.notificationPlayer = notificationPlayer;
    }

    @Nullable
    public Player getCannonDirector() {
        return cannonDirector;
    }

    public void setCannonDirector(@Nullable Player cannonDirector) {
        this.cannonDirector = cannonDirector;
    }

    @Nullable
    public Player getAADirector() {
        return AADirector;
    }

    public void setAADirector(@Nullable Player AADirector) {
        this.AADirector = AADirector;
    }

    public long getOrigPilotTime() {
        return origPilotTime;
    }

    public float getMeanMoveTime() {
        return meanMoveTime;
    }

    public void addMoveTime(float moveTime){
        meanMoveTime = (meanMoveTime*numMoves + moveTime)/(++numMoves);
    }

    public int getTickCooldown() {
        if(sinking)
            return type.getSinkRateTicks();
        double chestPenalty = 0;
        for(MovecraftLocation location : hitBox){
            if(location.toBukkit(w).getBlock().getType()==Material.CHEST)
                chestPenalty++;
        }
        chestPenalty*=type.getChestPenalty();
        if(meanMoveTime==0)
            return type.getCruiseTickCooldown()+(int)chestPenalty;
        if(!cruising)
            return type.getTickCooldown()+(int)chestPenalty;
        if(type.getDynamicFlyBlockSpeedFactor()!=0){
            double count = 0;
            Material flyBlockMaterial = Material.getMaterial(type.getDynamicFlyBlock());
            for(MovecraftLocation location : hitBox){
                if(location.toBukkit(w).getBlock().getType()==flyBlockMaterial)
                    count++;
            }
            return Math.max((int) (20 / (type.getCruiseTickCooldown() * (1  + type.getDynamicFlyBlockSpeedFactor() * (count /hitBox.size() - .5)))), 1);
            //return  Math.max((int)(type.getCruiseTickCooldown()* (1 - count /hitBox.size()) +chestPenalty),1);
        }

        if(type.getDynamicLagSpeedFactor()==0)
            return type.getCruiseTickCooldown()+(int)chestPenalty;
        //TODO: modify skip blocks by an equal proportion to this, than add another modifier based on dynamic speed factor
        return Math.max((int)(type.getCruiseTickCooldown()*meanMoveTime*20/type.getDynamicLagSpeedFactor() +chestPenalty),1);
    }

    /**
     * gets the speed of a craft in blocks per second.
     * @return the speed of the craft
     */
    public double getSpeed(){
        return 20*type.getCruiseSkipBlocks()/(double)getTickCooldown();
    }

    public long getLastRotateTime() {
        return lastRotateTime;
    }

    public void setLastRotateTime(long lastRotateTime) {
        this.lastRotateTime = lastRotateTime;
    }

    public long getLastGravityOnRotateTime() {
        return lastGravityOnRotateTime;
    }

    public void setLastGravityOnRotateTime(long lastGravityOnRotateTime) {
        this.lastGravityOnRotateTime = lastGravityOnRotateTime;
    }

    public int getWaterLine(){
        //TODO: Remove this temporary system in favor of passthrough blocks
        // Find the waterline from the surrounding terrain or from the static level in the craft type
        int waterLine = 0;
        if (type.getStaticWaterLevel() != 0 || hitBox.isEmpty()) {
            return type.getStaticWaterLevel();
        }

        // figure out the water level by examining blocks next to the outer boundaries of the craft
        for (int posY = hitBox.getMaxY() + 1; posY >= hitBox.getMinY() - 1; posY--) {
            int numWater = 0;
            int numAir = 0;
            int posX;
            int posZ;
            posZ = hitBox.getMinZ() - 1;
            for (posX = hitBox.getMinX() - 1; posX <= hitBox.getMaxX() + 1; posX++) {
                int typeID = w.getBlockAt(posX, posY, posZ).getTypeId();
                if (typeID == 9)
                    numWater++;
                if (typeID == 0)
                    numAir++;
            }
            posZ = hitBox.getMaxZ() + 1;
            for (posX = hitBox.getMinX() - 1; posX <= hitBox.getMaxX() + 1; posX++) {
                int typeID = w.getBlockAt(posX, posY, posZ).getTypeId();
                if (typeID == 9)
                    numWater++;
                if (typeID == 0)
                    numAir++;
            }
            posX = hitBox.getMinX() - 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                int typeID = w.getBlockAt(posX, posY, posZ).getTypeId();
                if (typeID == 9)
                    numWater++;
                if (typeID == 0)
                    numAir++;
            }
            posX = hitBox.getMaxX() + 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                int typeID = w.getBlockAt(posX, posY, posZ).getTypeId();
                if (typeID == 9)
                    numWater++;
                if (typeID == 0)
                    numAir++;
            }
            if (numWater > numAir) {
                return posY;
            }
        }
        return waterLine;
    }

    @NotNull
    public Map<MovecraftLocation,Material> getPhaseBlocks(){
        return phaseBlocks;
    }

    @NotNull
    public Map<UUID, Location> getCrewSigns(){
        return crewSigns;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public HashHitBox getCollapsedHitBox() {
        return collapsedHitBox;
    }
}