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
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.BlockCreateCommand;
import net.countercraft.movecraft.mapUpdater.update.EntityUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.ExplosionUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
    private final Map<AsyncTask, Craft> ownershipMap = new HashMap<>();
    private final BlockingQueue<AsyncTask> finishedAlgorithms = new LinkedBlockingQueue<>();
    private final Set<Craft> clearanceSet = new HashSet<>();
    private final Map<Craft, Integer> cooldownCache = new WeakHashMap<>();

    public AsyncManager() {}

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

    private void processCruise() {
        for (Craft craft : CraftManager.getInstance()) {
            if (craft == null || !craft.isNotProcessing() || !craft.getCruising())
                continue;

            long ticksElapsed = (System.currentTimeMillis() - craft.getLastCruiseUpdate()) / 50;
            World w = craft.getWorld();
            // if the craft should go slower underwater, make
            // time pass more slowly there
            if (craft.getType().getBoolProperty(CraftType.HALF_SPEED_UNDERWATER)
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
            int tickCoolDown;
            if (cooldownCache.containsKey(craft)) {
                tickCoolDown = cooldownCache.get(craft);
            }
            else {
                tickCoolDown = craft.getTickCooldown();
                cooldownCache.put(craft,tickCoolDown);
            }

            // Account for banking and diving in speed calculations by changing the tickCoolDown
            int cruiseSkipBlocks = (int) craft.getType().getPerWorldProperty(
                    CraftType.PER_WORLD_CRUISE_SKIP_BLOCKS, w);
            if (craft.getCruiseDirection() != CruiseDirection.UP
                    && craft.getCruiseDirection() != CruiseDirection.DOWN) {
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

            if (craft.getCruiseCooldownMultiplier() != 1 && craft.getCruiseCooldownMultiplier() != 0) {
                tickCoolDown *= craft.getCruiseCooldownMultiplier();
            }

            if (Math.abs(ticksElapsed) < tickCoolDown)
                continue;

            cooldownCache.remove(craft);
            int dx = 0;
            int dz = 0;
            int dy = 0;

            int vertCruiseSkipBlocks = (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_VERT_CRUISE_SKIP_BLOCKS, craft.getWorld());

            // ascend
            if (craft.getCruiseDirection() == CruiseDirection.UP)
                dy = 1 + vertCruiseSkipBlocks;
            // descend
            if (craft.getCruiseDirection() == CruiseDirection.DOWN) {
                dy = -1 - vertCruiseSkipBlocks;
                if (craft.getHitBox().getMinY() <= w.getSeaLevel())
                    dy = -1;
            }
            else if (dive || rise) {
                dy = -((cruiseSkipBlocks + 1) >> 1);
                if (craft.getHitBox().getMinY() <= w.getSeaLevel())
                    dy = -1;

                if (rise) {
                    dy *= -1;
                }
            }
            // TODO: This could be a switch case
            // ship faces west
            if (craft.getCruiseDirection() == CruiseDirection.WEST) {
                dx = -1 - cruiseSkipBlocks;
                if (bankRight)
                    dz = (-1 - cruiseSkipBlocks) >> 1;
                if (bankLeft)
                    dz = (1 + cruiseSkipBlocks) >> 1;
            }
            // ship faces east
            if (craft.getCruiseDirection() == CruiseDirection.EAST) {
                dx = 1 + cruiseSkipBlocks;
                if (bankLeft)
                    dz = (-1 - cruiseSkipBlocks) >> 1;
                if (bankRight)
                    dz = (1 + cruiseSkipBlocks) >> 1;
            }
            // ship faces north
            if (craft.getCruiseDirection() == CruiseDirection.SOUTH) {
                dz = 1 + cruiseSkipBlocks;
                if (bankRight)
                    dx = (-1 - cruiseSkipBlocks) >> 1;
                if (bankLeft)
                    dx = (1 + cruiseSkipBlocks) >> 1;
            }
            // ship faces south
            if (craft.getCruiseDirection() == CruiseDirection.NORTH) {
                dz = -1 - cruiseSkipBlocks;
                if (bankLeft)
                    dx = (-1 - cruiseSkipBlocks) >> 1;
                if (bankRight)
                    dx = (1 + cruiseSkipBlocks) >> 1;
            }
            if (craft.getType().getBoolProperty(CraftType.CRUISE_ON_PILOT)) {
                dy = craft.getType().getIntProperty(CraftType.CRUISE_ON_PILOT_VERT_MOVE);
            }
            if (craft.getType().getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_CRUISE_SKIP_BLOCKS)) {
                final int gearshift = craft.getCurrentGear();
                dx *= gearshift;
                dy *= gearshift;
                dz *= gearshift;
            }
            craft.translate(dx, dy, dz);
            craft.setLastTranslation(new MovecraftLocation(dx, dy, dz));
            craft.setLastCruiseUpdate(System.currentTimeMillis());
        }
    }

    //Controls sinking crafts
    private void processSinking() {
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
            if (Math.abs(ticksElapsed) < craft.getType().getIntProperty(CraftType.SINK_RATE_TICKS))
                continue;

            // Do this first so we stay in sync with the rate
            craft.setLastCruiseUpdate(System.currentTimeMillis());

            // TODO: Refactor into own submethod
            if (craft.getType().getBoolProperty(CraftType.USE_ALTERNATIVE_SINKING_PROCESS)) {
                // We stay afloat for some time, then we play a sound every now and then
                // Then we will start to sink
                // If we fall under a certain minimal percentage of our original size, we will sink normally
                final int originalSize = craft.getOrigBlockCount();

                final long craftAge = System.currentTimeMillis() - craft.getOrigPilotTime();
                final long stayAfloatDuration = (long) craft.getOrigBlockCount() * (long) craft.getType().getIntProperty(CraftType.ALTERNATIVE_SINKING_TIME_BEFORE_DISINTEGRATION);
                if (stayAfloatDuration > 0 && craftAge <= stayAfloatDuration) {
                    // Craft is still afloat => Does not begin to sink or disintegrate yet
                    continue;
                }

                // craft config values
                final double maxRemainingPercentageBeforeSinking = craft.getType().getDoubleProperty(CraftType.ALTERNATIVE_SINKING_SINK_MAX_REMAINING_PERCENTAGE);
                final double disintegrationChance = craft.getType().getDoubleProperty(CraftType.ALTERNATIVE_SINKING_DISINTEGRATION_CHANCE);
                final double explosionChance = craft.getType().getDoubleProperty(CraftType.ALTERNATIVE_SINKING_EXPLOSION_CHANCE);
                final int minDisintegrate = craft.getType().getIntProperty(CraftType.ALTERNATIVE_SINKING_MIN_DISINTEGRATE_BLOCKS);
                final int maxDisintegrate = craft.getType().getIntProperty(CraftType.ALTERNATIVE_SINKING_MAX_DISINTEGRATE_BLOCKS);
                final int minExplosions = craft.getType().getIntProperty(CraftType.ALTERNATIVE_SINKING_MIN_EXPLOSIONS);
                final int maxExplosions = craft.getType().getIntProperty(CraftType.ALTERNATIVE_SINKING_MAX_EXPLOSIONS);

                Set<MovecraftLocation> toRemove = new HashSet<>();
                List<MovecraftLocation> blocks = new ArrayList<>(craft.getHitBox().asSet());
                Collections.shuffle(blocks);
                List<UpdateCommand> updateCommands = new ArrayList<>();
                // Also cause a explosion every now and then
                boolean anyExplosion = false;
                if (RANDOM.nextDouble() <= explosionChance) {
                    int explosions = MathUtils.randomBetween(RANDOM, minExplosions, maxExplosions);
                    for (int i = 0; i < explosions && !blocks.isEmpty(); i++) {
                        MovecraftLocation movecraftLocation = blocks.removeFirst();
                        toRemove.add(movecraftLocation);
                        Location bukkitLocation = movecraftLocation.toBukkit(craft.getWorld());
                        if (bukkitLocation.getBlock().isEmpty()) {
                            continue;
                        }
                        anyExplosion |= true;

                        updateCommands.add(new ExplosionUpdateCommand(bukkitLocation, 4.0F, false));
                    }
                }
                if (RANDOM.nextDouble() <= disintegrationChance) {
                    int disintegrations = MathUtils.randomBetween(RANDOM, minDisintegrate, maxDisintegrate);
                    // First: Play sound
                    final String disintegrationSound = craft.getType().getStringProperty(CraftType.ALTERNATIVE_SINKING_DISINTEGRATION_SOUND);
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

                if (toRemove.size() > 0) {
                    BitmapHitBox newHitBox = new BitmapHitBox(craft.getHitBox());
                    newHitBox.removeAll(toRemove);
                    craft.setHitBox(newHitBox);
                }

                int nonNegligibleBlocks = craft.getDataTag(Craft.NON_NEGLIGIBLE_BLOCKS);
                int nonNegligibleSolidBlocks = craft.getDataTag(Craft.NON_NEGLIGIBLE_SOLID_BLOCKS);
                final int currentSize = (craft.getType().getBoolProperty(CraftType.BLOCKED_BY_WATER) ? nonNegligibleBlocks : nonNegligibleSolidBlocks);
                final double remainingSizePercentage = ((double)currentSize) / ((double)originalSize);

                // We are still not damaged enough to actually sink, so postpone the uninevitable
                if (anyExplosion || (maxRemainingPercentageBeforeSinking <= remainingSizePercentage)) {
                    continue;
                }
            }

            if (craft.getHitBox().getMinY() == craft.getWorld().getMinHeight()) {
                removeBottomLayer(craft);
            }

            int dx = 0;
            int dz = 0;
            if (craft.getType().getBoolProperty(CraftType.KEEP_MOVING_ON_SINK)) {
                dx = craft.getLastTranslation().getX();
                dz = craft.getLastTranslation().getZ();
            }
            craft.translate(dx, -1, dz);
        }
    }

    static final Random RANDOM = new Random();

    private void removeBottomLayer(Craft craft) {
        if (craft.getHitBox().isEmpty()) {
            return;
        }

        int bottomY = craft.getHitBox().getMinY();
        World world = craft.getWorld();

        final double chance = craft.getType().getDoubleProperty(CraftType.FALL_OUT_OF_WORLD_BLOCK_CHANCE);

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

                updateCommands.add(new BlockCreateCommand(world, movecraftLocation, Material.VOID_AIR));

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

        processCruise();
        processSinking();
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
