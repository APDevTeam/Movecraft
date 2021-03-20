package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.BitmapHitBox;
import net.countercraft.movecraft.utils.Counter;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MutableHitBox;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.countercraft.movecraft.utils.SignUtils.getFacing;

public abstract class BaseCraft implements Craft{
    @NotNull
    protected final CraftType type;
    @NotNull protected HitBox hitBox;
    @NotNull protected final MutableHitBox collapsedHitBox;
    @NotNull protected MutableHitBox fluidLocations;
    @NotNull protected final Counter<Material> materials;
    @NotNull protected World w;
    @NotNull private final AtomicBoolean processing = new AtomicBoolean();
    private int maxHeightLimit;
    private boolean cruising;
    private boolean sinking;
    private boolean disabled;
    private CruiseDirection cruiseDirection;
    private long lastCruiseUpdate;
    private long lastBlockCheck;
    private long lastRotateTime=0;
    private final long origPilotTime;
    private long lastTeleportTime;
    private int lastDX, lastDY, lastDZ;
    private int currentGear = 1;
    private double burningFuel;
    private boolean pilotLocked;
    private double pilotLockedX;
    private double pilotLockedY;
    private int origBlockCount;
    private double pilotLockedZ;
    @Nullable
    private Player notificationPlayer;
    @NotNull private Audience audience;
    private float meanCruiseTime;
    private int numMoves;
    @NotNull private final Map<Location, BlockData> phaseBlocks = new HashMap<>();
    @NotNull private String name = "";

    public BaseCraft(@NotNull CraftType type, @NotNull World world) {
        this.type = type;
        this.w = world;
        this.hitBox = new BitmapHitBox();
        this.collapsedHitBox = new BitmapHitBox();
        this.fluidLocations = new BitmapHitBox();
        if (type.getMaxHeightLimit(w) > w.getMaxHeight() - 1) {
            this.maxHeightLimit = w.getMaxHeight() - 1;
        } else {
            this.maxHeightLimit = type.getMaxHeightLimit(w);
        }
        this.pilotLocked = false;
        this.pilotLockedX = 0.0;
        this.pilotLockedY = 0.0;
        this.pilotLockedZ = 0.0;
        this.lastCruiseUpdate = System.currentTimeMillis() - 10000;
        this.cruising = false;
        this.sinking = false;
        this.disabled = false;
        this.origPilotTime = System.currentTimeMillis();
        numMoves = 0;
        materials = new Counter<>();
        audience = Audience.empty();
    }

    public boolean isNotProcessing() {
        return !processing.get();
    }

    public void setProcessing(boolean processing) {
        this.processing.set(processing);
    }

    @NotNull
    public HitBox getHitBox() {
        return hitBox;
    }

    public void setHitBox(@NotNull HitBox hitBox){
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

    public void setW(World world) {
        this.w = world;
        if (type.getMaxHeightLimit(w) > w.getMaxHeight() - 1) {
            this.maxHeightLimit = w.getMaxHeight() - 1;
        } else {
            this.maxHeightLimit = type.getMaxHeightLimit(w);
        }
    }

    @Override
    public void detect(@Nullable Player player, @NotNull Player notificationPlayer, MovecraftLocation startPoint) {
        this.setNotificationPlayer(notificationPlayer);
        this.setAudience(Movecraft.getAdventure().player(notificationPlayer));
        Movecraft.getInstance().getAsyncManager().submitTask(new DetectionTask(this, startPoint, player, notificationPlayer), this);
    }

    @Deprecated
    public void translate(int dx, int dy, int dz) {
        translate(w, dx, dy, dz);
    }

    @Override
    public void translate(@NotNull World world, int dx, int dy, int dz) {
        // check to see if the craft is trying to move in a direction not permitted by the type
        if (!world.equals(w) && !(this.getType().getCanSwitchWorld() || type.getDisableTeleportToWorlds().contains(world.getName())) && !this.getSinking()) {
            world = w;
        }
        if (!this.getType().allowHorizontalMovement() && !this.getSinking()) {
            dx = 0;
            dz = 0;
        }
        if (!this.getType().allowVerticalMovement() && !this.getSinking()) {
            dy = 0;
        }
        if (dx == 0 && dy == 0 && dz == 0 && world.equals(w)) {
            return;
        }

        if (!this.getType().allowVerticalTakeoffAndLanding() && dy != 0 && !this.getSinking()) {
            if (dx == 0 && dz == 0) {
                return;
            }
        }

        Movecraft.getInstance().getAsyncManager().submitTask(new TranslationTask(this, world, dx, dy, dz), this);
    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint) {
        if(getLastRotateTime()+1e9>System.nanoTime()){
            getAudience().sendMessage(I18nSupport.getInternationalisedComponent("Rotation - Turning Too Quickly"));
            return;
        }
        setLastRotateTime(System.nanoTime());
        Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(this, originPoint, rotation, this.getW()), this);
    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint, boolean isSubCraft) {
        Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(this, originPoint, rotation, this.getW(), isSubCraft), this);
    }

    /**
     * Gets the crafts that have made contact with this craft
     * @return a set of crafts on contact with this craft
     */
    @NotNull
    @Override
    public Set<Craft> getContacts() {
        final Set<Craft> contacts = new HashSet<>();
        for (Craft contact : CraftManager.getInstance().getCraftsInWorld(w)) {
            MovecraftLocation ccenter = this.getHitBox().getMidPoint();
            MovecraftLocation tcenter = contact.getHitBox().getMidPoint();
            int distsquared = ccenter.distanceSquared(tcenter);
            int detectionRange = (int) (contact.getOrigBlockCount() * (tcenter.getY() > 65 ? contact.getType().getDetectionMultiplier(contact.getW()) : contact.getType().getUnderwaterDetectionMultiplier(contact.getW())));
            detectionRange = detectionRange * 10;
            if (distsquared > detectionRange || contact.getNotificationPlayer() == this.getNotificationPlayer()) {
                continue;
            }
            contacts.add(contact);
        }
        return contacts;
    }

    @Override
    public void resetSigns(@NotNull Sign clicked) {
        for (final MovecraftLocation ml : hitBox) {
            final Block b = ml.toBukkit(w).getBlock();
            if (!(b.getState() instanceof Sign)) {
                continue;
            }
            final Sign sign = (Sign) b.getState();
            if (sign.equals(clicked)) {
                continue;
            }
            if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: ON")){
                sign.setLine(0, "Cruise: OFF");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: OFF")
                    && ChatColor.stripColor(clicked.getLine(0)).equalsIgnoreCase("Cruise: ON")
                    && getFacing(sign) == getFacing(clicked)) {
                sign.setLine(0,"Cruise: ON");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: ON")){
                sign.setLine(0, "Ascend: OFF");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: OFF")
                    && ChatColor.stripColor(clicked.getLine(0)).equalsIgnoreCase("Ascend: ON")){
                sign.setLine(0, "Ascend: ON");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: ON")){
                sign.setLine(0, "Descend: OFF");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: OFF")
                    && ChatColor.stripColor(clicked.getLine(0)).equalsIgnoreCase("Descend: ON")){
                sign.setLine(0, "Descend: ON");
            }
            sign.update();
        }
    }

    @Override
    public boolean getCruising() {
        return cruising;
    }

    @Override
    public void setCruising(boolean cruising) {
        audience.sendActionBar(Component.text().content("Cruising " + (cruising ? "enabled" : "disabled")));
        this.cruising = cruising;
    }

    @Override
    public boolean getSinking() {
        return sinking;
    }

    /*public void setSinking(boolean sinking) {
        this.sinking = sinking;
    }*/

    @Override
    public void sink(){
        CraftSinkEvent event = new CraftSinkEvent(this);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()){
            return;
        }
        this.sinking = true;
    }

    @Override
    public boolean getDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public CruiseDirection getCruiseDirection() {
        return cruiseDirection;
    }

    @Override
    public void setCruiseDirection(CruiseDirection cruiseDirection) {
        this.cruiseDirection = cruiseDirection;
    }

    @Override
    public void setLastCruiseUpdate(long update) {
        this.lastCruiseUpdate = update;
    }

    @Override
    public long getLastCruiseUpdate() {
        return lastCruiseUpdate;
    }

    @Override
    public long getLastBlockCheck() {
        return lastBlockCheck;
    }

    @Override
    public void setLastBlockCheck(long update) {
        this.lastBlockCheck = update;
    }

    @Override
    public int getLastDX() {
        return lastDX;
    }

    @Override
    public void setLastDX(int dX) {
        this.lastDX = dX;
    }

    @Override
    public int getLastDY() {
        return lastDY;
    }

    @Override
    public void setLastDY(int dY) {
        this.lastDY = dY;
    }

    @Override
    public int getLastDZ() {
        return lastDZ;
    }

    @Override
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

    @Override
    public double getBurningFuel() {
        return burningFuel;
    }

    @Override
    public void setBurningFuel(double burningFuel) {
        this.burningFuel = burningFuel;
    }

    @Override
    public int getOrigBlockCount() {
        return origBlockCount;
    }

    @Override
    public void setOrigBlockCount(int origBlockCount) {
        this.origBlockCount = origBlockCount;
    }

    @Nullable @Deprecated
    public Player getNotificationPlayer() {
        return notificationPlayer;
    }

    @Deprecated
    public void setNotificationPlayer(@Nullable Player notificationPlayer) {
        this.notificationPlayer = notificationPlayer;
    }

    @Override
    public long getOrigPilotTime() {
        return origPilotTime;
    }

    @Override
    public float getMeanCruiseTime() {
        return meanCruiseTime;
    }

    @Override
    public void addCruiseTime(float cruiseTime){
        meanCruiseTime = (meanCruiseTime *numMoves + cruiseTime)/(++numMoves);
    }

    @Override
    public int getTickCooldown() {
        if(sinking)
            return type.getSinkRateTicks();

//        Counter<Material> counter = new Counter<>();
//        Map<Material, Integer> counter = new HashMap<>();
        if(materials.isEmpty()){
            for(MovecraftLocation location : hitBox){
                materials.add(location.toBukkit(w).getBlock().getType());
            }
        }

        int chestPenalty = (int)((materials.get(Material.CHEST) + materials.get(Material.TRAPPED_CHEST)) * type.getChestPenalty());
        if(!cruising)
            return (type.getTickCooldown(w) + chestPenalty) * (type.getGearShiftsAffectTickCooldown() ? currentGear : 1);

        // Ascent or Descent
        if(cruiseDirection == CruiseDirection.UP || cruiseDirection == CruiseDirection.DOWN) {
            return (type.getVertCruiseTickCooldown(w) + chestPenalty) * (type.getGearShiftsAffectTickCooldown() ? currentGear : 1);
        }

        // Dynamic Fly Block Speed
        if(type.getDynamicFlyBlockSpeedFactor() != 0){
            Material flyBlockMaterial = type.getDynamicFlyBlock();
            double count = materials.get(flyBlockMaterial);
            double woolRatio = count / hitBox.size();
            return Math.max((int)Math.round((20.0 * (type.getCruiseSkipBlocks(w) + 1)) / ((type.getDynamicFlyBlockSpeedFactor() * 1.5) * (woolRatio - .5) + (20.0 / (type.getCruiseTickCooldown(w) )) + 1)) * (type.getGearShiftsAffectTickCooldown() ? currentGear : 1), 1);
        }

        if(type.getDynamicLagSpeedFactor() == 0.0 || type.getDynamicLagPowerFactor() == 0.0 || Math.abs(type.getDynamicLagPowerFactor()) > 1.0)
            return (type.getCruiseTickCooldown(w) + chestPenalty) * (type.getGearShiftsAffectTickCooldown() ? currentGear : 1);
        if(numMoves == 0)
            return (int) Math.round(20.0 * ((type.getCruiseSkipBlocks(w) + 1.0) / type.getDynamicLagMinSpeed()) * (type.getGearShiftsAffectTickCooldown() ? currentGear : 1));

        if(Settings.Debug) {
            Bukkit.getLogger().info("Skip: " + type.getCruiseSkipBlocks(w));
            Bukkit.getLogger().info("Tick: " + type.getCruiseTickCooldown(w));
            Bukkit.getLogger().info("SpeedFactor: " + type.getDynamicLagSpeedFactor());
            Bukkit.getLogger().info("PowerFactor: " + type.getDynamicLagPowerFactor());
            Bukkit.getLogger().info("MinSpeed: " + type.getDynamicLagMinSpeed());
            Bukkit.getLogger().info("CruiseTime: " + getMeanCruiseTime() * 1000.0 + "ms");
        }

        // Dynamic Lag Speed
        double speed = 20.0 * (type.getCruiseSkipBlocks(w) + 1.0) / (float)type.getCruiseTickCooldown(w);
        speed -= type.getDynamicLagSpeedFactor() * Math.pow(getMeanCruiseTime() * 1000.0, type.getDynamicLagPowerFactor());
        speed = Math.max(type.getDynamicLagMinSpeed(), speed);
        return (int)Math.round((20.0 * (type.getCruiseSkipBlocks(w) + 1.0)) / speed) * (type.getGearShiftsAffectTickCooldown() ? currentGear : 1);
        //In theory, the chest penalty is not needed for a DynamicLag craft.
    }

    /**
     * gets the speed of a craft in blocks per second.
     * @return the speed of the craft
     */
    @Override
    public double getSpeed() {
        if(cruiseDirection == CruiseDirection.UP || cruiseDirection == CruiseDirection.DOWN) {
            return 20 * (type.getVertCruiseSkipBlocks(w) + 1) / (double) getTickCooldown();
        }
        else {
            return 20 * (type.getCruiseSkipBlocks(w) + 1) / (double) getTickCooldown();
        }
    }

    @Override
    public long getLastRotateTime() {
        return lastRotateTime;
    }

    @Override
    public void setLastRotateTime(long lastRotateTime) {
        this.lastRotateTime = lastRotateTime;
    }

    @Override
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
                Material material = w.getBlockAt(posX, posY, posZ).getType();
                if (material == Material.WATER)
                    numWater++;
                if (material == Material.AIR)
                    numAir++;
            }
            posZ = hitBox.getMaxZ() + 1;
            for (posX = hitBox.getMinX() - 1; posX <= hitBox.getMaxX() + 1; posX++) {
                Material material = w.getBlockAt(posX, posY, posZ).getType();
                if (material == Material.WATER)
                    numWater++;
                if (material == Material.AIR)
                    numAir++;
            }
            posX = hitBox.getMinX() - 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                Material material = w.getBlockAt(posX, posY, posZ).getType();
                if (material == Material.WATER)
                    numWater++;
                if (material == Material.AIR)
                    numAir++;
            }
            posX = hitBox.getMaxX() + 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                Material material = w.getBlockAt(posX, posY, posZ).getType();
                if (material == Material.WATER)
                    numWater++;
                if (material == Material.AIR)
                    numAir++;
            }
            if (numWater > numAir) {
                return posY;
            }
        }
        return waterLine;
    }

    @Override
    public @NotNull Map<Location, BlockData> getPhaseBlocks(){
        return phaseBlocks;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Override
    @NotNull
    public MutableHitBox getCollapsedHitBox() {
        return collapsedHitBox;
    }

    @Override
    @NotNull
    public MutableHitBox getFluidLocations() {
        return fluidLocations;
    }

    @Override
    public void setFluidLocations(@NotNull MutableHitBox fluidLocations) {
        this.fluidLocations = fluidLocations;
    }

    @Override
    public long getLastTeleportTime() {
        return lastTeleportTime;
    }

    @Override
    public void setLastTeleportTime(long lastTeleportTime) {
        this.lastTeleportTime = lastTeleportTime;
    }

    @Override
    public int getCurrentGear() {
        return currentGear;
    }

    @Override
    public void setCurrentGear(int currentGear) {
        if (currentGear > type.getGearShifts()) {
            this.currentGear = type.getGearShifts();
        }
        this.currentGear = Math.max(currentGear, 1);
    }

    @Override
    @NotNull
    public Audience getAudience(){
        return audience;
    }

    @Override
    public void setAudience(@NotNull Audience audience){
        this.audience = audience;
    }
}
