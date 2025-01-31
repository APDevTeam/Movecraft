package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.TrackedLocation;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.datatag.CraftDataTagContainer;
import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import net.countercraft.movecraft.craft.datatag.CraftDataTagRegistry;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.CachedMovecraftWorld;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.TimingData;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.MutableHitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static net.countercraft.movecraft.util.SignUtils.getFacing;

public abstract class BaseCraft implements Craft {
    @NotNull
    protected final CraftType type;
    @NotNull
    protected final MutableHitBox collapsedHitBox;
    @NotNull
    private final AtomicBoolean processing = new AtomicBoolean();
    private final long origPilotTime;
    @NotNull
    private final Map<Location, BlockData> phaseBlocks = new HashMap<>();
    @NotNull
    protected HitBox hitBox;
    @NotNull
    protected MutableHitBox fluidLocations;
    @NotNull
    protected World w;
    @NotNull TimingData stats = new TimingData();
    private boolean cruising;
    private boolean disabled;
    private CruiseDirection cruiseDirection;
    private long lastCruiseUpdate;
    private long lastBlockCheck;
    private long lastRotateTime = 0;
    private long lastTeleportTime;
    private int currentGear = 1;
    private double burningFuel;
    private int origBlockCount;
    @NotNull
    private Audience audience;
    @NotNull
    private String name = "";
    @NotNull
    private MovecraftLocation lastTranslation = new MovecraftLocation(0, 0, 0);
    private Map<NamespacedKey, Set<TrackedLocation>> trackedLocations = new ConcurrentHashMap<>();

    @NotNull
    private final CraftDataTagContainer dataTagContainer;

    private final UUID uuid = UUID.randomUUID();

    public BaseCraft(@NotNull CraftType type, @NotNull World world) {
        Hidden.uuidToCraft.put(uuid, this);
        this.type = type;
        this.w = world;
        hitBox = new SetHitBox();
        collapsedHitBox = new SetHitBox();
        fluidLocations = new SetHitBox();
        lastCruiseUpdate = System.currentTimeMillis();
        cruising = false;
        disabled = false;
        origPilotTime = System.currentTimeMillis();
        audience = Audience.empty();
        dataTagContainer = new CraftDataTagContainer();
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

    public void setHitBox(@NotNull HitBox hitBox) {
        this.hitBox = hitBox;
    }

    @NotNull
    public CraftType getType() {
        return type;
    }

    @NotNull
    public MovecraftWorld getMovecraftWorld() {
        return CachedMovecraftWorld.of(w);
    }

    @NotNull
    public World getWorld() {
        if (WorldManager.INSTANCE.isRunning() && !Bukkit.isPrimaryThread()) {
            var exception = new Throwable("Invoking most methods on worlds while the world manager is running WILL cause deadlock.");
            Bukkit.getLogger().log(Level.SEVERE, exception, exception::getMessage);
        }
        return w;
    }

    public void setWorld(@NotNull World world) {
        this.w = world;
    }

    @Deprecated
    public void translate(int dx, int dy, int dz) {
        translate(w, dx, dy, dz);
    }

    @Override
    public void translate(@NotNull World world, int dx, int dy, int dz) {
        var v = type.getObjectProperty(CraftType.DISABLE_TELEPORT_TO_WORLDS);
        if (!(v instanceof Collection<?>))
            throw new IllegalStateException("DISABLE_TELEPORT_TO_WORLDS must be of type Collection");
        var disableTeleportToWorlds = ((Collection<?>) v);
        disableTeleportToWorlds.forEach(i -> {
            if (!(i instanceof String))
                throw new IllegalStateException("Values in DISABLE_TELEPORT_TO_WORLDS must be of type String");
        });

        // check to see if the craft is trying to move in a direction not permitted by the type
        if (!(this instanceof SinkingCraft)) { // sinking crafts can move in any direction
            if (!world.equals(w)
                    && !(getType().getBoolProperty(CraftType.CAN_SWITCH_WORLD)
                            || disableTeleportToWorlds.contains(world.getName())))
                world = w;
            if (!getType().getBoolProperty(CraftType.ALLOW_HORIZONTAL_MOVEMENT)) {
                dx = 0;
                dz = 0;
            }
            if (!getType().getBoolProperty(CraftType.ALLOW_VERTICAL_MOVEMENT))
                dy = 0;
        }
        if (dx == 0 && dy == 0 && dz == 0 && world.equals(w))
            return;

        if (!getType().getBoolProperty(CraftType.ALLOW_VERTICAL_TAKEOFF_AND_LANDING)
                && dy != 0 && dx == 0 && dz == 0
                && !(this instanceof SinkingCraft))
            return;

        Movecraft.getInstance().getAsyncManager().submitTask(new TranslationTask(this, world, dx, dy, dz), this);
    }

    @Override
    public void rotate(MovecraftRotation rotation, MovecraftLocation originPoint) {
        if (getLastRotateTime() + 1e9 > System.nanoTime()) {
            getAudience().sendMessage(I18nSupport.getInternationalisedComponent("Rotation - Turning Too Quickly"));
            return;
        }
        setLastRotateTime(System.nanoTime());
        Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(this, originPoint, rotation, getWorld()), this);
    }

    @Override
    public void rotate(MovecraftRotation rotation, MovecraftLocation originPoint, boolean isSubCraft) {
        Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(this, originPoint, rotation, getWorld(), isSubCraft), this);
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
            if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: ON")) {
                sign.setLine(0, "Cruise: OFF");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: OFF")
                    && ChatColor.stripColor(clicked.getLine(0)).equalsIgnoreCase("Cruise: ON")
                    && getFacing(sign) == getFacing(clicked)) {
                sign.setLine(0, "Cruise: ON");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: ON")) {
                sign.setLine(0, "Ascend: OFF");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: OFF")
                    && ChatColor.stripColor(clicked.getLine(0)).equalsIgnoreCase("Ascend: ON")) {
                sign.setLine(0, "Ascend: ON");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: ON")) {
                sign.setLine(0, "Descend: OFF");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: OFF")
                    && ChatColor.stripColor(clicked.getLine(0)).equalsIgnoreCase("Descend: ON")) {
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
    public long getLastCruiseUpdate() {
        return lastCruiseUpdate;
    }

    @Override
    public void setLastCruiseUpdate(long update) {
        lastCruiseUpdate = update;
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
    @NotNull
    public MovecraftLocation getLastTranslation() {
        return this.lastTranslation;
    }

    @Override
    public void setLastTranslation(@NotNull MovecraftLocation lastTranslation) {
        this.lastTranslation = lastTranslation;
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

    @Override
    public long getOrigPilotTime() {
        return origPilotTime;
    }

    @Override
    public double getMeanCruiseTime() {
        return stats.getRecentAverage();
    }

    @Override
    public void addCruiseTime(float cruiseTime) {
        stats.accept(cruiseTime);
    }

    @Override
    public int getTickCooldown() {
        if (this instanceof SinkingCraft)
            return type.getIntProperty(CraftType.SINK_RATE_TICKS);

        Counter<Material> materials = getDataTag(Craft.MATERIALS);

        int chestPenalty = 0;
        if (!materials.isEmpty()) {
            for (Material m : Tags.CHESTS) {
                chestPenalty += materials.get(m);
            }
        }
        chestPenalty *= (int) type.getDoubleProperty(CraftType.CHEST_PENALTY);
        if (!cruising)
            return ((int) type.getPerWorldProperty(CraftType.PER_WORLD_TICK_COOLDOWN, w) + chestPenalty) * (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN) ? currentGear : 1);

        // Ascent or Descent
        if (cruiseDirection == CruiseDirection.UP || cruiseDirection == CruiseDirection.DOWN)
            return ((int) type.getPerWorldProperty(CraftType.PER_WORLD_VERT_CRUISE_TICK_COOLDOWN, w) + chestPenalty) * (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN) ? currentGear : 1);

        // Dynamic Fly Block Speed
        int cruiseTickCooldown = (int) type.getPerWorldProperty(CraftType.PER_WORLD_CRUISE_TICK_COOLDOWN, w);
        if (type.getDoubleProperty(CraftType.DYNAMIC_FLY_BLOCK_SPEED_FACTOR) != 0) {
            if (materials.isEmpty()) {
                return ((int) type.getPerWorldProperty(CraftType.PER_WORLD_TICK_COOLDOWN, w) + chestPenalty) * (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN) ? currentGear : 1);
            }
            EnumSet<Material> flyBlockMaterials = type.getMaterialSetProperty(CraftType.DYNAMIC_FLY_BLOCK);
            double count = 0;
            for (Material m : flyBlockMaterials) {
                count += materials.get(m);
            }
            double ratio = count / hitBox.size();
            double speed = (type.getDoubleProperty(CraftType.DYNAMIC_FLY_BLOCK_SPEED_FACTOR) * 1.5)
                    * (ratio - 0.5)
                        + (20.0 / cruiseTickCooldown) + 1;
            return Math.max((int) Math.round((20.0 * ((int) type.getPerWorldProperty(CraftType.PER_WORLD_CRUISE_SKIP_BLOCKS, w) + 1)) / speed) * (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN) ? currentGear : 1), 1);
        }

        if (type.getDoubleProperty(CraftType.DYNAMIC_LAG_SPEED_FACTOR) == 0.0
                || type.getDoubleProperty(CraftType.DYNAMIC_LAG_POWER_FACTOR) == 0.0
                || Math.abs(type.getDoubleProperty(CraftType.DYNAMIC_LAG_POWER_FACTOR)) > 1.0)
            return (cruiseTickCooldown + chestPenalty) * (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN) ? currentGear : 1);

        int cruiseSkipBlocks = (int) type.getPerWorldProperty(CraftType.PER_WORLD_CRUISE_SKIP_BLOCKS, w);
        if (stats.getCount() == 0) {
            if (Settings.Debug) {
                Bukkit.getLogger().info("First cruise: ");
                Bukkit.getLogger().info("\t- Skip: " + cruiseSkipBlocks);
                Bukkit.getLogger().info("\t- Tick: " + cruiseTickCooldown);
                Bukkit.getLogger().info("\t- MinSpeed: " + type.getDoubleProperty(CraftType.DYNAMIC_LAG_MIN_SPEED));
                Bukkit.getLogger().info("\t- Gearshifts: " + (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN) ? currentGear : 1));
                Bukkit.getLogger().info("\t- Cooldown: " + (int) Math.round(20.0 * ((cruiseSkipBlocks + 1.0) / type.getDoubleProperty(CraftType.DYNAMIC_LAG_MIN_SPEED)) * (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN) ? currentGear : 1)));
            }
            return (int) Math.round(20.0 * ((cruiseSkipBlocks + 1.0) / type.getDoubleProperty(CraftType.DYNAMIC_LAG_MIN_SPEED)) * (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN) ? currentGear : 1));
        }

        if (Settings.Debug) {
            Bukkit.getLogger().info("Cruise: ");
            Bukkit.getLogger().info("\t- Skip: " + cruiseSkipBlocks);
            Bukkit.getLogger().info("\t- Tick: " + cruiseTickCooldown);
            Bukkit.getLogger().info("\t- SpeedFactor: " + type.getDoubleProperty(CraftType.DYNAMIC_LAG_SPEED_FACTOR));
            Bukkit.getLogger().info("\t- PowerFactor: " + type.getDoubleProperty(CraftType.DYNAMIC_LAG_POWER_FACTOR));
            Bukkit.getLogger().info("\t- MinSpeed: " + type.getDoubleProperty(CraftType.DYNAMIC_LAG_MIN_SPEED));
            Bukkit.getLogger().info("\t- CruiseTime: " + getMeanCruiseTime() * 1000.0 + "ms");
        }

        // Dynamic Lag Speed
        double speed = 20.0 * (cruiseSkipBlocks + 1.0) / (float) cruiseTickCooldown;
        speed -= type.getDoubleProperty(CraftType.DYNAMIC_LAG_SPEED_FACTOR) * Math.pow(getMeanCruiseTime() * 1000.0, type.getDoubleProperty(CraftType.DYNAMIC_LAG_POWER_FACTOR));
        speed = Math.max(type.getDoubleProperty(CraftType.DYNAMIC_LAG_MIN_SPEED), speed);
        return (int) Math.round((20.0 * (cruiseSkipBlocks + 1.0)) / speed) * (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN) ? currentGear : 1);
        //In theory, the chest penalty is not needed for a DynamicLag craft.
    }

    /**
     * gets the speed of a craft in blocks per second.
     *
     * @return the speed of the craft
     */
    @Override
    public double getSpeed() {
        if (cruiseDirection == CruiseDirection.UP || cruiseDirection == CruiseDirection.DOWN) {
            return 20 * ((int) type.getPerWorldProperty(CraftType.PER_WORLD_VERT_CRUISE_SKIP_BLOCKS, w) + 1) / (double) getTickCooldown();
        }
        else {
            return 20 * ((int) type.getPerWorldProperty(CraftType.PER_WORLD_CRUISE_SKIP_BLOCKS, w) + 1) / (double) getTickCooldown();
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
    public int getWaterLine() {
        //TODO: Remove this temporary system in favor of passthrough blocks
        // Find the waterline from the surrounding terrain or from the static level in the craft type
        int waterLine = 0;
        if (type.getIntProperty(CraftType.STATIC_WATER_LEVEL) != 0 || hitBox.isEmpty()) {
            return type.getIntProperty(CraftType.STATIC_WATER_LEVEL);
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
                if (Tags.WATER.contains(material))
                    numWater++;
                if (material.isAir())
                    numAir++;
            }
            posZ = hitBox.getMaxZ() + 1;
            for (posX = hitBox.getMinX() - 1; posX <= hitBox.getMaxX() + 1; posX++) {
                Material material = w.getBlockAt(posX, posY, posZ).getType();
                if (Tags.WATER.contains(material))
                    numWater++;
                if (material.isAir())
                    numAir++;
            }
            posX = hitBox.getMinX() - 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                Material material = w.getBlockAt(posX, posY, posZ).getType();
                if (Tags.WATER.contains(material))
                    numWater++;
                if (material.isAir())
                    numAir++;
            }
            posX = hitBox.getMaxX() + 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                Material material = w.getBlockAt(posX, posY, posZ).getType();
                if (Tags.WATER.contains(material))
                    numWater++;
                if (material.isAir())
                    numAir++;
            }
            if (numWater > numAir) {
                return posY;
            }
        }
        return waterLine;
    }

    @Override
    public @NotNull Map<Location, BlockData> getPhaseBlocks() {
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
        this.currentGear = Math.min(Math.max(currentGear, 1), type.getIntProperty(CraftType.GEAR_SHIFTS));
    }

    @Override
    @NotNull
    public Audience getAudience() {
        return audience;
    }

    @Override
    public void setAudience(@NotNull Audience audience) {
        this.audience = audience;
    }

    @Override
    public <T> void setDataTag(final @NotNull CraftDataTagKey<T> tagKey, final T data) {
        dataTagContainer.set(tagKey, data);
    }

    @Override
    public <T> T getDataTag(final @NotNull CraftDataTagKey<T> tagKey) {
        return dataTagContainer.get(this, tagKey);
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BaseCraft))
            return false;

        return this.getUUID().equals(((BaseCraft) obj).getUUID());
    }

    @Override
    public int hashCode() {
        return this.getUUID().hashCode();
    }

    @Override
    public Map<NamespacedKey, Set<TrackedLocation>> getTrackedLocations() {return trackedLocations;}
}
