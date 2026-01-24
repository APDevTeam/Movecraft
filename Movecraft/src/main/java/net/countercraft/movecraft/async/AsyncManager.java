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

package net.countercraft.movecraft.async;

import com.google.common.collect.Lists;
import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.craft.*;
import net.countercraft.movecraft.craft.controller.AbstractRotationController;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.InitiateTranslateEvent;
import net.countercraft.movecraft.events.RunnableRegistrationEvent;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.BlockCreateCommand;
import net.countercraft.movecraft.mapUpdater.update.ExplosionUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.MutableHitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Deprecated
public class AsyncManager extends BukkitRunnable {

    private static final double ARCSIN_ONE_HALF = 0.523598776;

    private Map<Integer, List<Runnable>> tickFunctions;
    private int maxInterval;
    private int currentTick = 1;

    private final Map<AsyncTask, Craft> ownershipMap = new HashMap<>();
    private final BlockingQueue<AsyncTask> finishedAlgorithms = new LinkedBlockingQueue<>();
    private final Set<Craft> clearanceSet = new HashSet<>();
    private final Map<Craft, Integer> cooldownCache = new WeakHashMap<>();

    public AsyncManager() {

    }

    private Map<Integer, List<Runnable>> getTickFunctions() {
        if (this.tickFunctions != null) {
            return this.tickFunctions;
        }
        this.tickFunctions = new HashMap<>();

        RunnableRegistrationEvent registrationEvent = new RunnableRegistrationEvent(this.tickFunctions);
        Bukkit.getPluginManager().callEvent(registrationEvent);
        int maxIntervalTmp = 0;
        for (Integer i : this.tickFunctions.keySet()) {
            if (i > maxIntervalTmp) {
                maxIntervalTmp = i;
            }
        }
        this.maxInterval = maxIntervalTmp;

        return this.tickFunctions;
    }

    public void submitTask(AsyncTask task, Craft c) {
        if (c.isNotProcessing()) {
            c.setProcessing(true);
            ownershipMap.put(task, c);
            task.runTask(Movecraft.getInstance());
        }
    }

    public void submitCompletedTask(AsyncTask task) {
        finishedAlgorithms.add(task);
    }

    private void processAlgorithmQueue() {
        int runLength = 10;
        int queueLength = finishedAlgorithms.size();

        runLength = Math.min(runLength, queueLength);

        for (int i = 0; i < runLength; i++) {
            boolean sentMapUpdate = false;
            AsyncTask poll = finishedAlgorithms.poll();
            Craft c = ownershipMap.get(poll);

            if (poll instanceof TranslationTask) {
                // Process translation task

                TranslationTask task = (TranslationTask) poll;
                sentMapUpdate = processTranslation(task, c);

            } else if (poll instanceof RotationTask) {
                // Process rotation task
                RotationTask task = (RotationTask) poll;
                sentMapUpdate = processRotation(task, c);

            }

            ownershipMap.remove(poll);

            // only mark the craft as having finished updating if you didn't
            // send any updates to the map updater. Otherwise the map updater
            // will mark the crafts once it is done with them.
            if (!sentMapUpdate) {
                clear(c);
            }
        }
    }

    /**
     * Processes translation task for its corresponding craft
     * @param task the task to process
     * @param c the craft this task belongs to
     * @return true if translation task succeded to process, otherwise false
     */
    private boolean processTranslation(@NotNull final TranslationTask task, @NotNull final Craft c) {

        // Check that the craft hasn't been sneakily unpiloted

        if (task.failed()) {
            // The craft translation failed
            if (!(c instanceof SinkingCraft))
                c.getAudience().sendMessage(Component.text(task.getFailMessage()));

            if (task.isCollisionExplosion()) {
                c.setHitBox(task.getNewHitBox());
                c.setFluidLocations(task.getNewFluidList());
                MapUpdateManager.getInstance().scheduleUpdates(task.getUpdates());
                CraftManager.getInstance().addReleaseTask(c);
                return true;
            }
            return false;
        }
        // The craft is clear to move, perform the block updates
        MapUpdateManager.getInstance().scheduleUpdates(task.getUpdates());

        c.setHitBox(task.getNewHitBox());
        c.setFluidLocations(task.getNewFluidList());
        return true;
    }

    /**
     * Processes rotation task for its corresponding craft
     * @param task the task to process
     * @param c the craft this task belongs to
     * @return true if translation task succeded to process, otherwise false
     */
    private boolean processRotation(@NotNull final RotationTask task, @NotNull final Craft c) {
        // Check that the craft hasn't been sneakily unpiloted
        if (!(c instanceof PilotedCraft) && !task.getIsSubCraft())
            return false;

        if (task.isFailed()) {
            // The craft translation failed, don't try to notify
            // them if there is no pilot
            c.getAudience().sendMessage(Component.text(task.getFailMessage()));
            return false;
        }


        MapUpdateManager.getInstance().scheduleUpdates(task.getUpdates());

        c.setHitBox(task.getNewHitBox());
        c.setFluidLocations(task.getNewFluidList());
        return true;
    }

    public static void processCruise() {
        for (Craft craft : CraftManager.getInstance()) {
            if (craft == null || !craft.isNotProcessing() || !craft.getCruising())
                continue;

            long ticksElapsed = (System.currentTimeMillis() - craft.getLastCruiseUpdate()) / 50;
            World w = craft.getWorld();
            // if the craft should go slower underwater, make
            // time pass more slowly there
            if (craft.getCraftProperties().get(PropertyKeys.HALF_SPEED_UNDERWATER)
                    && craft.getHitBox().getMinY() < w.getSeaLevel())
                ticksElapsed >>= 1;
            // check direct controls to modify movement
            boolean bankLeft = false;
            boolean bankRight = false;
            boolean dive = false;
            boolean rise = false;

            if (craft instanceof PlayerCraft && ((PlayerCraft) craft).getPilotLocked() && ((PlayerCraft)craft).getPilot() != null) {
                Player pilot = ((PlayerCraft) craft).getPilot();
                if (pilot.isSneaking()) {
                    if (pilot.getInventory().getItem(EquipmentSlot.OFF_HAND) != null) {
                        dive = pilot.getInventory().getItem(EquipmentSlot.OFF_HAND).getType().isEmpty();
                    } else {
                        dive = true;
                    }
                    rise = !dive;
                }
                if (pilot.getInventory().getHeldItemSlot() == 3)
                    bankLeft = true;
                if (pilot.getInventory().getHeldItemSlot() == 5)
                    bankRight = true;
            }

            int cruiseSkipBlocks = craft.getCraftProperties().get(PropertyKeys.CRUISE_SKIP_BLOCKS, w);

            int tickCoolDown;
            if (Movecraft.getInstance().getAsyncManager().cooldownCache.containsKey(craft)) {
                tickCoolDown = Movecraft.getInstance().getAsyncManager().cooldownCache.get(craft);
            }
            else {
                tickCoolDown = craft.getTickCooldown();
                Movecraft.getInstance().getAsyncManager().cooldownCache.put(craft,tickCoolDown);
            }
            // Account for banking and diving in speed calculations by changing the tickCoolDown
            if (!craft.getCruiseDirection().isVertical()) {
                if (bankLeft || bankRight) {
                    if (!(dive || rise)) {
                        tickCoolDown *= (Math.sqrt(Math.pow(1 + cruiseSkipBlocks, 2)
                                + Math.pow(cruiseSkipBlocks >> 1, 2)) / (1 + cruiseSkipBlocks));
                    }
                    else {
                        tickCoolDown *= (Math.sqrt(Math.pow(1 + cruiseSkipBlocks, 2)
                                + Math.pow(cruiseSkipBlocks >> 1, 2) + 1) / (1 + cruiseSkipBlocks));
                    }
                }
                else if (dive || rise) {
                    tickCoolDown *= (Math.sqrt(Math.pow(1 + cruiseSkipBlocks, 2) + 1) / (1 + cruiseSkipBlocks));
                }
            }

            int vertCruiseSkipBlocks = craft.getCraftProperties().get(PropertyKeys.VERT_CRUISE_SKIP_BLOCKS).get(craft.getWorld());
            CruiseDirection direction = craft.getCruiseDirection().clone();
            int jumpDistance = 1 + (direction.isVertical() ? vertCruiseSkipBlocks : cruiseSkipBlocks);

            if (craft.getCruiseCooldownMultiplier() != 1 && craft.getCruiseCooldownMultiplier() != 0) {
                tickCoolDown *= craft.getCruiseCooldownMultiplier();
            }

            if (Math.abs(ticksElapsed) < tickCoolDown)
                continue;

            Movecraft.getInstance().getAsyncManager().cooldownCache.remove(craft);

            if (bankRight)
                direction.rotateAroundY(ARCSIN_ONE_HALF);
            if (bankLeft)
                direction.rotateAroundY(-ARCSIN_ONE_HALF);
            if (rise)
                direction.rise2D(ARCSIN_ONE_HALF);
            if (dive)
                direction.rise2D(-ARCSIN_ONE_HALF);

            Vector cruiseVector = direction.multiply(jumpDistance);

            if (craft.getCraftProperties().get(PropertyKeys.GEAR_SHIFT_AFFECT_CRUISE_SKIP_BLOCKS)) {
                final int gearshift = craft.getCurrentGear();
                cruiseVector.multiply(gearshift);
            }
            if (craft.getHitBox().getMinY() <= w.getSeaLevel() && cruiseVector.getY() < 1.0d) {
                cruiseVector.setY(-1);
            }
            if (craft.getCraftProperties().get(PropertyKeys.CRUISE_ON_PILOT)) {
                cruiseVector.setY(craft.getCraftProperties().get(PropertyKeys.CRUISE_ON_PILOT_VERT_MOVE));
            }

            // TODO: GEAR_SHIFT respection?
            InitiateTranslateEvent initiateTranslateEvent = new InitiateTranslateEvent(craft, cruiseVector.clone().multiply(cruiseVector.length()));
            Bukkit.getServer().getPluginManager().callEvent(initiateTranslateEvent);
            if (initiateTranslateEvent.isCancelled()) {
                continue;
            }
            final AbstractRotationController rotationController = craft.getCraftProperties().get(PropertyKeys.ROTATION_CONTROLLER);
            if (rotationController != null) {
                rotationController.onInitiateTranslation(initiateTranslateEvent);
                if (initiateTranslateEvent.isCancelled()) {
                    continue;
                }
            }
            cruiseVector = initiateTranslateEvent.getTranslationDirection();
            Vector discreteTranslation = craft.translate(craft.getWorld(), cruiseVector);
            craft.setLastTranslation(new MovecraftLocation((int) discreteTranslation.getX(), (int) discreteTranslation.getY(), (int) discreteTranslation.getZ()));
            craft.setLastCruiseUpdate(System.currentTimeMillis());
        }
    }

    static final BlockFace[] CHECK_DIRECTIONS = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    //Controls sinking crafts
    public static void processSinking() {
        //copy the crafts before iteration to prevent concurrent modifications
        List<Craft> crafts = Lists.newArrayList(CraftManager.getInstance());
        for (Craft craft : crafts) {
            if (!(craft instanceof SinkingCraft))
                continue;

            if (craft.getHitBox().isEmpty() /*|| craft.getHitBox().getMinY() < (craft.getWorld().getMinHeight() +5 )*/) {
                CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.SUNK, false);
                continue;
            }

            long ticksElapsed = (System.currentTimeMillis() - craft.getLastCruiseUpdate()) / 50;
            if (Math.abs(ticksElapsed) < craft.getCraftProperties().get(PropertyKeys.SINK_RATE_TICKS))
                continue;

            // Do this first so we stay in sync with the rate
            craft.setLastCruiseUpdate(System.currentTimeMillis());

            // TODO: Refactor into own submethod
            if (craft.getCraftProperties().get(PropertyKeys.USE_ALTERNATIVE_SINKING_PROCESS)) {
                // We stay afloat for some time, then we play a sound every now and then
                // Then we will start to sink
                // If we fall under a certain minimal percentage of our original size, we will sink normally
                final int originalSize = craft.getOrigBlockCount();

                final long craftAge = System.currentTimeMillis() - craft.getOrigPilotTime();
                final long stayAfloatDuration = (long) craft.getOrigBlockCount() * (long) craft.getCraftProperties().get(PropertyKeys.ALTERNATIVE_SINKING_TIME_BEFORE_DISINITEGRATION);
                if (stayAfloatDuration > 0 && craftAge <= stayAfloatDuration) {
                    // Craft is still afloat => Does not begin to sink or disintegrate yet
                    continue;
                }

                // craft config values
                final double maxRemainingPercentageBeforeSinking = craft.getCraftProperties().get(PropertyKeys.ALTERNATIVE_SINKING_SINK_MAX_REMAINING_PERCENTAGE);
                final double disintegrationChance = craft.getCraftProperties().get(PropertyKeys.ALTERNATIVE_SINKING_DISINTEGRATION_CHANCE);
                final double explosionChance = craft.getCraftProperties().get(PropertyKeys.ALTERNATIVE_SINKING_EXPLOSION_CHANCE);
                final int minDisintegrate = craft.getCraftProperties().get(PropertyKeys.ALTERNATIVE_SINKING_MIN_DISINTEGRATE_BLOCKS);
                final int maxDisintegrate = craft.getCraftProperties().get(PropertyKeys.ALTERNATIVE_SINKING_MAX_DISINTEGRATE_BLOCKS);
                final int minExplosions = craft.getCraftProperties().get(PropertyKeys.ALTERNATIVE_SINKING_MIN_EXPLOSIONS);
                final int maxExplosions = craft.getCraftProperties().get(PropertyKeys.ALTERNATIVE_SINKING_MAX_EXPLOSIONS);

                Set<MovecraftLocation> toRemove = new HashSet<>();
                List<MovecraftLocation> blocks = new ArrayList<>(craft.getHitBox().asSet());

                blocks.removeIf(movecraftLocation -> {
                    Location bukkitLocation = movecraftLocation.toBukkit(craft.getWorld());
                    if (bukkitLocation.getBlock().isEmpty()) {
                        return true;
                    }
                    Block block = bukkitLocation.getBlock();
                    boolean hasAirNextToIt = false;
                    for (BlockFace face : CHECK_DIRECTIONS) {
                        Block relative = block.getRelative(face);
                        if (relative.isEmpty()) {
                            hasAirNextToIt = true;
                            break;
                        }
                    }
                    return !hasAirNextToIt;
                });

                Collections.shuffle(blocks);

                List<UpdateCommand> updateCommands = new ArrayList<>();
                // Also cause a explosion every now and then
                if (RANDOM.nextDouble() <= explosionChance) {
                    int explosions = MathUtils.randomBetween(RANDOM, minExplosions, maxExplosions);
                    for (int i = 0; i < explosions && !blocks.isEmpty(); i++) {
                        MovecraftLocation movecraftLocation = blocks.removeFirst();
                        Location bukkitLocation = movecraftLocation.toBukkit(craft.getWorld());
                        if (bukkitLocation.getBlock().isEmpty()) {
                            continue;
                        }
                        updateCommands.add(new ExplosionUpdateCommand(bukkitLocation, 4.0F, false));
                    }
                }
                if (RANDOM.nextDouble() <= disintegrationChance) {
                    int disintegrations = MathUtils.randomBetween(RANDOM, minDisintegrate, maxDisintegrate);
                    // First: Play sound
                    // TODO: Change to configured sound instead
                    final String disintegrationSound = craft.getCraftProperties().get(PropertyKeys.ALTERNATIVE_SINKING_DISINTEGRATION_SOUND);
                    if (!(disintegrationSound == null || disintegrationSound.isBlank())) {
                        int radius = (int)(Math.max(Math.max(craft.getHitBox().getXLength(), craft.getHitBox().getYLength()), craft.getHitBox().getZLength()) * 1.5D);
                        Location centerLocation = craft.getCraftOrigin().toBukkit(craft.getWorld());
                        for (Player player : craft.getWorld().getNearbyPlayers(centerLocation,radius)) {
                            player.playSound(centerLocation, disintegrationSound, 2.0F, 1.0F);
                        }
                    }
                    // Second: disintegrate the ship
                    for (int i = 0; i < disintegrations && !blocks.isEmpty(); i++) {
                        MovecraftLocation movecraftLocation = blocks.removeFirst();
                        toRemove.add(movecraftLocation);
                        updateCommands.add(new BlockCreateCommand(movecraftLocation, Material.AIR.createBlockData(), craft));
                    }
                }

                MapUpdateManager.getInstance().scheduleUpdates(updateCommands);

                // TODO: Is this truly necessary?
                if (toRemove.size() > 0) {
                    HitBox hitBox = craft.getHitBox();
                    // If we already use a mutable hitbox, we can simply modify that instead!
                    if (hitBox instanceof MutableHitBox mutableHitBox) {
                        mutableHitBox.removeAll(toRemove);
                    } else {
                        SetHitBox newHitBox = new SetHitBox(hitBox);
                        newHitBox.removeAll(toRemove);
                        craft.setHitBox(newHitBox);
                    }
                }

                int nonNegligibleBlocks = craft.getDataTag(Craft.NON_NEGLIGIBLE_BLOCKS);
                int nonNegligibleSolidBlocks = craft.getDataTag(Craft.NON_NEGLIGIBLE_SOLID_BLOCKS);
                final int currentSize = (craft.getCraftProperties().get(PropertyKeys.BLOCKED_BY_WATER) ? nonNegligibleBlocks : nonNegligibleSolidBlocks);
                final double remainingSizePercentage = ((double)currentSize) / ((double)originalSize);

                // We are still not damaged enough to actually sink, so postpone the uninevitable
                if (maxRemainingPercentageBeforeSinking <= remainingSizePercentage) {
                    continue;
                }
            }

            // The hitbox can be modified here, so check again
            if (craft.getHitBox().isEmpty()) {
                CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.SUNK, false);
                continue;
            } else if (craft.getHitBox().getMinY() == craft.getWorld().getMinHeight()) {
                removeBottomLayer(craft);
            }

            int dx = 0;
            int dz = 0;
            if (craft.getCraftProperties().get(PropertyKeys.KEEP_MOVING_ON_SINK)) {
                dx = craft.getLastTranslation().getX();
                dz = craft.getLastTranslation().getZ();
            }
            craft.translate(dx, -1, dz);
        }
    }

    static final Random RANDOM = new Random();

    private static void removeBottomLayer(Craft craft) {
        if (craft.getHitBox().isEmpty()) {
            return;
        }

        int bottomY = craft.getHitBox().getMinY();
        World world = craft.getWorld();

        final double chance = craft.getCraftProperties().get(PropertyKeys.FALL_OUT_OF_WORLD_BLOCK_CHANCE);

        List<MovecraftLocation> toRemove = new ArrayList<>();
        Set<MovecraftLocation> oldHitbox = craft.getHitBox().asSet();

        List<UpdateCommand> updateCommands = new ArrayList<>();

        for (MovecraftLocation movecraftLocation : oldHitbox) {
            Location location = movecraftLocation.toBukkit(world);
            if (movecraftLocation.getY() == bottomY) {
                // TODO: Change this to use UpdateCommands too
                if (chance > 0.0D && RANDOM.nextDouble() <= chance) {
                    Block block = location.getBlock();
                    FallingBlock fallingBlock = world.spawnFallingBlock(location, block.getBlockData());
                    fallingBlock.setDropItem(false);
                    Vector velocity = new Vector(RANDOM.nextDouble() - 0.5D, fallingBlock.getVelocity().getY() / 2.0D, RANDOM.nextDouble() - 0.5D);
                    fallingBlock.setVelocity(velocity.normalize().multiply(0.5D));
                }

                updateCommands.add(new BlockCreateCommand(world, movecraftLocation, Material.AIR));

                toRemove.add(movecraftLocation);
            }
        }

        MapUpdateManager.getInstance().scheduleUpdates(updateCommands);

        // Recalculate hitbox
        BitmapHitBox newHitBox = new BitmapHitBox(oldHitbox);
        newHitBox.removeAll(toRemove);
        craft.setHitBox(newHitBox);
    }

    public void run() {
        clearAll();

        Map<Integer, List<Runnable>> functions = this.getTickFunctions();

        for (Map.Entry<Integer, List<Runnable>> entry : functions.entrySet()) {
            if (this.currentTick % entry.getKey() == 0) {
                for (Runnable function : entry.getValue()) {
                    try {
                        function.run();
                    } catch(Exception exception) {
                        // TODO: Log error!
                    }
                }
            }
        }
        // TODO: This is probably not correct!
        this.currentTick++;
        if (this.currentTick > this.maxInterval) {
            this.currentTick = 1;
        }

        processAlgorithmQueue();

        // now cleanup craft that are bugged and have not moved in the past 60 seconds,
        //  but have no pilot or are still processing
        for (Craft craft : CraftManager.getInstance()) {
            if (!(craft instanceof PilotedCraft)) {
                if (craft.getLastCruiseUpdate() < System.currentTimeMillis() - 60000)
                    CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.INACTIVE, true);
            }
            if (!craft.isNotProcessing()) {
                if (craft.getCruising()) {
                    if (craft.getLastCruiseUpdate() < System.currentTimeMillis() - 5000)
                        craft.setProcessing(false);
                }
            }
        }
    }

    private void clear(Craft c) {
        clearanceSet.add(c);
    }

    private void clearAll() {
        for (Craft c : clearanceSet) {
            c.setProcessing(false);
        }

        clearanceSet.clear();
    }
}
