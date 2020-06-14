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
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.countercraft.movecraft.utils.BitmapHitBox;
import net.countercraft.movecraft.utils.Counter;
import net.countercraft.movecraft.utils.Pair;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;



public abstract class Craft {
    @NotNull protected final CraftType type;
    //protected int[][][] hitBox;
    //protected MovecraftLocation[] blockList;
    @NotNull protected BitmapHitBox hitBox;
    @NotNull protected final BitmapHitBox collapsedHitBox;
    @NotNull protected BitmapHitBox fluidLocations;
    @NotNull protected final Counter<Material> materials;
    @NotNull protected World world;
    @NotNull private final AtomicBoolean processing = new AtomicBoolean();
    private int maxHeightLimit;
    private boolean cruising;
    private boolean sinking;
    private boolean disabled;
    @NotNull private BlockFace cruiseDirection;
    private long lastCruiseUpdate;
    private long lastBlockCheck;
    private long lastRotateTime=0;
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
    private float meanCruiseTime;
    private int numMoves;
    @NotNull private final Map<Location, Pair<Material, Object>> phaseBlocks = new HashMap<>();
    @NotNull private final HashMap<UUID, Location> crewSigns = new HashMap<>();
    @NotNull private String name = "";
    private boolean translating;

    public Craft(@NotNull CraftType type, @NotNull World world) {
        this.type = type;
        this.world = world;
        this.hitBox = new BitmapHitBox();
        this.collapsedHitBox = new BitmapHitBox();
        this.fluidLocations = new BitmapHitBox();
        if (type.getMaxHeightLimit(world) > world.getMaxHeight() - 1) {
            this.maxHeightLimit = world.getMaxHeight() - 1;
        } else {
            this.maxHeightLimit = type.getMaxHeightLimit(world);
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
        materials = new Counter<>();
    }

    public boolean isNotProcessing() {
        return !processing.get();
    }

    public void setProcessing(boolean processing) {
        this.processing.set(processing);
    }

    @NotNull
    public BitmapHitBox getHitBox() {
        return hitBox;
    }

    public void setHitBox(@NotNull BitmapHitBox hitBox){
        this.hitBox = hitBox;
    }

    @NotNull
    public CraftType getType() {
        return type;
    }

    @NotNull
    @Deprecated
    public World getW() {
        return world;
    }

    @NotNull
    public World getWorld() {
        return world;
    }

    @Deprecated
    public void setW(World world) {
        this.world = world;
        if (type.getMaxHeightLimit(world) > world.getMaxHeight() - 1) {
            this.maxHeightLimit = world.getMaxHeight() - 1;
        } else {
            this.maxHeightLimit = type.getMaxHeightLimit(world);
        }
    }

    public void setWorld(@NotNull World world) {
        this.world = world;
        if (type.getMaxHeightLimit(world) > world.getMaxHeight() - 1) {
            this.maxHeightLimit = world.getMaxHeight() - 1;
        } else {
            this.maxHeightLimit = type.getMaxHeightLimit(world);
        }
    }

    public abstract void detect(Player player, Player notificationPlayer, MovecraftLocation startPoint);

    public abstract void translate(World world, int dx, int dy, int dz);

    @Deprecated
    public void translate(int dx, int dy, int dz) {
        translate(world, dx, dy, dz);
    }

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


    /**
     * Gets the crafts that have made contact with this craft
     * @return a set of crafts on contact with this craft
     */
    public abstract Set<Craft> getContacts();

    public boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @NotNull
    public BlockFace getCruiseDirection() {
        return cruiseDirection;
    }

    public void setCruiseDirection(@NotNull BlockFace cruiseDirection) {
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

    public float getMeanCruiseTime() {
        return meanCruiseTime;
    }

    public void addCruiseTime(float cruiseTime){
        meanCruiseTime = (meanCruiseTime *numMoves + cruiseTime)/(++numMoves);
    }

    public int getTickCooldown() {
        if(sinking)
            return type.getSinkRateTicks();

//        Counter<Material> counter = new Counter<>();
//        Map<Material, Integer> counter = new HashMap<>();
        if(materials.isEmpty()){
            for(MovecraftLocation location : hitBox){
                materials.add(location.toBukkit(world).getBlock().getType());
            }
        }

        int chestPenalty = (int)((materials.get(Material.CHEST) + materials.get(Material.TRAPPED_CHEST)) * type.getChestPenalty());
        if(!cruising)
            return type.getTickCooldown(world) + chestPenalty;

        // Ascent or Descent
        if(cruiseDirection == BlockFace.UP || cruiseDirection == BlockFace.DOWN) {
            return type.getVertCruiseTickCooldown(world) + chestPenalty;
        }

        // Dynamic Fly Block Speed
        if(type.getDynamicFlyBlockSpeedFactor() != 0) {
            double count = 0.0, woolRatio = 0.0;
            for (Material flyBlockMaterial : type.getDynamicFlyBlocks()) {
                count += materials.get(flyBlockMaterial);
                woolRatio += count / hitBox.size();
            }
            return Math.max((int) Math.round((20.0 * (type.getCruiseSkipBlocks(world) + 1)) / ((type.getDynamicFlyBlockSpeedFactor() * 1.5) * (woolRatio - .5) + (20.0 / type.getCruiseTickCooldown(world)) + 1)), 1);
        }

        if(type.getDynamicLagSpeedFactor() == 0.0 || type.getDynamicLagPowerFactor() == 0.0 || Math.abs(type.getDynamicLagPowerFactor()) > 1.0)
            return type.getCruiseTickCooldown(world) + chestPenalty;
        if(meanCruiseTime == 0)
            return type.getCruiseTickCooldown(world) + chestPenalty;

        if(Settings.Debug) {
            Bukkit.getLogger().info("Skip: " + type.getCruiseSkipBlocks(world));
            Bukkit.getLogger().info("Tick: " + type.getCruiseTickCooldown(world));
            Bukkit.getLogger().info("SpeedFactor: " + type.getDynamicLagSpeedFactor());
            Bukkit.getLogger().info("PowerFactor: " + type.getDynamicLagPowerFactor());
            Bukkit.getLogger().info("MinSpeed: " + type.getDynamicLagMinSpeed());
            Bukkit.getLogger().info("CruiseTime: " + getMeanCruiseTime() * 1000.0 + "ms");
        }

        // Dynamic Lag Speed
        double speed = 20.0 * (type.getCruiseSkipBlocks(world) + 1.0) / (float)type.getCruiseTickCooldown(world);
        speed -= type.getDynamicLagSpeedFactor() * Math.pow(getMeanCruiseTime() * 1000.0, type.getDynamicLagPowerFactor());
        speed = Math.max(type.getDynamicLagMinSpeed(), speed);
        return (int)Math.round((20.0 * (type.getCruiseSkipBlocks(world) + 1.0)) / speed);
            //In theory, the chest penalty is not needed for a DynamicLag craft.
    }

    /**
     * gets the speed of a craft in blocks per second.
     * @return the speed of the craft
     */
    public double getSpeed() {
        if(cruiseDirection == BlockFace.UP || cruiseDirection == BlockFace.DOWN) {
            return 20 * (type.getVertCruiseSkipBlocks(world) + 1) / (double) getTickCooldown();
        }
        else {
            return 20 * (type.getCruiseSkipBlocks(world) + 1) / (double) getTickCooldown();
        }
    }

    public long getLastRotateTime() {
        return lastRotateTime;
    }

    public void setLastRotateTime(long lastRotateTime) {
        this.lastRotateTime = lastRotateTime;
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
                Material type = world.getBlockAt(posX, posY, posZ).getType();
                if (type.name().endsWith("WATER"))
                    numWater++;
                if (type.name().endsWith("AIR"))
                    numAir++;
            }
            posZ = hitBox.getMaxZ() + 1;
            for (posX = hitBox.getMinX() - 1; posX <= hitBox.getMaxX() + 1; posX++) {
                Material type = world.getBlockAt(posX, posY, posZ).getType();
                if (type.name().endsWith("WATER"))
                    numWater++;
                if (type.name().endsWith("AIR"))
                    numAir++;
            }
            posX = hitBox.getMinX() - 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                Material type = world.getBlockAt(posX, posY, posZ).getType();
                if (type.name().endsWith("WATER"))
                    numWater++;
                if (type.name().endsWith("AIR"))
                    numAir++;
            }
            posX = hitBox.getMaxX() + 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                Material type = world.getBlockAt(posX, posY, posZ).getType();
                if (type.name().endsWith("WATER"))
                    numWater++;
                if (type.name().endsWith("AIR"))
                    numAir++;
            }
            if (numWater > numAir) {
                return posY;
            }
        }
        return waterLine;
    }

    @NotNull
    public Map<Location, Pair<Material, Object>> getPhaseBlocks(){
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
    public BitmapHitBox getCollapsedHitBox() {
        return collapsedHitBox;
    }

    public abstract void resetSigns(@NotNull final Sign clicked);

    @NotNull
    public BitmapHitBox getFluidLocations() {
        return fluidLocations;
    }

    public void setFluidLocations(@NotNull BitmapHitBox fluidLocations) {
        this.fluidLocations = fluidLocations;
    }

    public boolean isTranslating() {
        return translating;
    }

    public void setTranslating(boolean translating) {
        this.translating = translating;
    }
}
