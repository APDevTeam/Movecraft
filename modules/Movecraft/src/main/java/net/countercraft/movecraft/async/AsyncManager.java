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
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.BlockCreateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.BitmapHitBox;
import net.countercraft.movecraft.utils.CollectionUtils;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.Pair;
import net.countercraft.movecraft.utils.SolidHitBox;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Deprecated
public class AsyncManager extends BukkitRunnable {
    //private static AsyncManager instance = new AsyncManager();
    private final HashMap<AsyncTask, Craft> ownershipMap = new HashMap<>();
    private final HashMap<Craft, HashMap<Craft, Long>> recentContactTracking = new HashMap<>();
    private final BlockingQueue<AsyncTask> finishedAlgorithms = new LinkedBlockingQueue<>();
    private final HashSet<Craft> clearanceSet = new HashSet<>();
    private final HashMap<HitBox, Long> wrecks = new HashMap<>();
    private final HashMap<HitBox, World> wreckWorlds = new HashMap<>();
    private final HashMap<HitBox, Map<Location, Pair<Material, Byte>>> wreckPhases = new HashMap<>();
    private final WeakHashMap<World, Set<MovecraftLocation>> processedFadeLocs = new WeakHashMap<>();
    private final Map<Craft, Integer> cooldownCache = new WeakHashMap<>();

    private long lastFadeCheck = 0;
    private long lastContactCheck = 0;

    public AsyncManager() {}

   /* public static AsyncManager getInstance() {
        return instance;
    }*/

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

    public void addWreck(Craft craft){
        if(craft.getCollapsedHitBox().isEmpty() || Settings.FadeWrecksAfter == 0){
            return;
        }
        wrecks.put(craft.getCollapsedHitBox(), System.currentTimeMillis());
        wreckWorlds.put(craft.getCollapsedHitBox(), craft.getW());
        wreckPhases.put(craft.getCollapsedHitBox(), craft.getPhaseBlocks());
    }

    private void processAlgorithmQueue() {
        int runLength = 10;
        int queueLength = finishedAlgorithms.size();

        runLength = Math.min(runLength, queueLength);

        for (int i = 0; i < runLength; i++) {
            boolean sentMapUpdate = false;
            AsyncTask poll = finishedAlgorithms.poll();
            Craft c = ownershipMap.get(poll);

            if (poll instanceof DetectionTask) {
                // Process detection task

                DetectionTask task = (DetectionTask) poll;

                processDetection(task);

            } else if (poll instanceof TranslationTask) {
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

    private void processDetection(DetectionTask task){
        Player p = task.getPlayer();
        Player notifyP = task.getNotificationPlayer();
        Craft pCraft = CraftManager.getInstance().getCraftByPlayer(notifyP);
        Craft c = task.getCraft();
        boolean failed = task.failed();
        if (pCraft != null && p != null) {
            // Player is already controlling a craft
            notifyP.sendMessage(I18nSupport.getInternationalisedString("Detection - Failed - Already commanding a craft"));
            return;
        }
        if (failed) {
            notifyP.sendMessage(task.getFailMessage());
            return;
        }
        Set<Craft> craftsInWorld = CraftManager.getInstance().getCraftsInWorld(c.getW());

        boolean isSubcraft = false;

        if (c.getType().getCruiseOnPilot() || p != null) {
            for (Craft craft : craftsInWorld) {
                if (craft.getHitBox().intersection(task.getHitBox()).isEmpty()) {
                    continue;
                }
                if (craft.getType() == c.getType()
                        || craft.getHitBox().size() <= task.getHitBox().size()) {
                    notifyP.sendMessage(I18nSupport.getInternationalisedString(
                            "Detection - Failed Craft is already being controlled"));
                    return;
                }
                if (!craft.isNotProcessing()) {
                    notifyP.sendMessage(I18nSupport.getInternationalisedString("Detection - Parent Craft is busy"));
                    return;
                }
                isSubcraft = true;
                craft.setHitBox(craft.getHitBox().difference(task.getHitBox()));
                craft.setOrigBlockCount(craft.getOrigBlockCount() - task.getHitBox().size());
            }
        }
        if (c.getType().getMustBeSubcraft() && !isSubcraft) {
            notifyP.sendMessage(I18nSupport.getInternationalisedString("Craft must be part of another craft"));
            return;
        }
        c.setHitBox(task.getHitBox());
        c.setFluidLocations(task.getFluidBox());
        c.setOrigBlockCount(task.getHitBox().size());
        c.setNotificationPlayer(notifyP);
        final int waterLine = c.getWaterLine();
        if (!c.getType().blockedByWater() && c.getHitBox().getMinY() <= waterLine) {
            //The subtraction of the set of coordinates in the HitBox cube and the HitBox itself
            final HitBox invertedHitBox = new BitmapHitBox(c.getHitBox().boundingHitBox()).difference(c.getHitBox());

            //A set of locations that are confirmed to be "exterior" locations
            final BitmapHitBox confirmed = new BitmapHitBox();
            final BitmapHitBox entireHitbox = new BitmapHitBox(c.getHitBox());

            //place phased blocks
            final Set<Location> overlap = new HashSet<>(c.getPhaseBlocks().keySet());
            overlap.retainAll(c.getHitBox().asSet().stream().map(l -> l.toBukkit(c.getW())).collect(Collectors.toSet()));
            final int minX = c.getHitBox().getMinX();
            final int maxX = c.getHitBox().getMaxX();
            final int minY = c.getHitBox().getMinY();
            final int maxY = overlap.isEmpty() ? c.getHitBox().getMaxY() : Collections.max(overlap, Comparator.comparingInt(Location::getBlockY)).getBlockY();
            final int minZ = c.getHitBox().getMinZ();
            final int maxZ = c.getHitBox().getMaxZ();
            final HitBox[] surfaces = {
                    new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(minX, maxY, maxZ)),
                    new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(maxX, maxY, minZ)),
                    new SolidHitBox(new MovecraftLocation(maxX, minY, maxZ), new MovecraftLocation(minX, maxY, maxZ)),
                    new SolidHitBox(new MovecraftLocation(maxX, minY, maxZ), new MovecraftLocation(maxX, maxY, minZ)),
                    new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(maxX, minY, maxZ))};
            final BitmapHitBox validExterior = new BitmapHitBox();
            for (HitBox hitBox : surfaces) {
                validExterior.addAll(new BitmapHitBox(hitBox).difference(c.getHitBox()));
            }

            //Check to see which locations in the from set are actually outside of the craft
            //use a modified BFS for multiple origin elements
            BitmapHitBox visited = new BitmapHitBox();
            Queue<MovecraftLocation> queue = Lists.newLinkedList(validExterior);
            while (!queue.isEmpty()) {
                MovecraftLocation node = queue.poll();
                if (visited.contains(node))
                    continue;
                visited.add(node);
                //If the node is already a valid member of the exterior of the HitBox, continued search is unitary.
                for (MovecraftLocation neighbor : CollectionUtils.neighbors(invertedHitBox, node)) {
                    queue.add(neighbor);
                }
            }
            confirmed.addAll(visited);
            entireHitbox.addAll(invertedHitBox.difference(confirmed));

            for (MovecraftLocation location : entireHitbox) {
                if (location.getY() <= waterLine) {
                    c.getPhaseBlocks().put(location.toBukkit(c.getW()), new Pair<>(Material.WATER, (byte) 0));
                }
            }
        }
        final CraftDetectEvent event = new CraftDetectEvent(c);
        Bukkit.getPluginManager().callEvent(event);
        failed = event.isCancelled();
        if (failed) {
            notifyP.sendMessage(event.getFailMessage());
            return;
        }
        notifyP.sendMessage(I18nSupport.getInternationalisedString("Detection - Successfully piloted craft")
                + " Size: " + c.getHitBox().size());
        Movecraft.getInstance().getLogger().info(String.format(
                I18nSupport.getInternationalisedString("Detection - Success - Log Output"),
                notifyP.getName(), c.getType().getCraftName(), c.getHitBox().size(),
                c.getHitBox().getMinX(), c.getHitBox().getMinZ()));

        CraftManager.getInstance().addCraft(c);
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
            if (!c.getSinking())
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
        if (c.getNotificationPlayer() == null && !task.getIsSubCraft())  {
            return false;
        }

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
        for (Craft pcraft : CraftManager.getInstance()) {
            if (pcraft == null || !pcraft.isNotProcessing() || !pcraft.getCruising()) {
                continue;
            }
            long ticksElapsed = (System.currentTimeMillis() - pcraft.getLastCruiseUpdate()) / 50;
            World w = pcraft.getW();
            // if the craft should go slower underwater, make
            // time pass more slowly there
            if (pcraft.getType().getHalfSpeedUnderwater() && pcraft.getHitBox().getMinY() < w.getSeaLevel())
                ticksElapsed >>= 1;
            // check direct controls to modify movement
            boolean bankLeft = false;
            boolean bankRight = false;
            boolean dive = false;
            if (pcraft instanceof PlayerCraft && ((PlayerCraft) pcraft).getPilotLocked() && pcraft.getNotificationPlayer() != null && pcraft.getNotificationPlayer().isOnline()) {
                if (pcraft.getNotificationPlayer().isSneaking())
                    dive = true;
                if (pcraft.getNotificationPlayer().getInventory().getHeldItemSlot() == 3)
                    bankLeft = true;
                if (pcraft.getNotificationPlayer().getInventory().getHeldItemSlot() == 5)
                    bankRight = true;
            }
            int tickCoolDown;
            if(cooldownCache.containsKey(pcraft)){
                tickCoolDown = cooldownCache.get(pcraft);
            } else {
                tickCoolDown = pcraft.getTickCooldown();
                cooldownCache.put(pcraft,tickCoolDown);
            }

            // Account for banking and diving in speed calculations by changing the tickCoolDown
            if(pcraft.getCruiseDirection() != CruiseDirection.UP && pcraft.getCruiseDirection() != CruiseDirection.DOWN) {
                if (bankLeft || bankRight) {
                    if (!dive) {
                        tickCoolDown *= (Math.sqrt(Math.pow(1 + pcraft.getType().getCruiseSkipBlocks(w), 2) + Math.pow(pcraft.getType().getCruiseSkipBlocks(w) >> 1, 2)) / (1 + pcraft.getType().getCruiseSkipBlocks(w)));
                    } else {
                        tickCoolDown *= (Math.sqrt(Math.pow(1 + pcraft.getType().getCruiseSkipBlocks(w), 2) + Math.pow(pcraft.getType().getCruiseSkipBlocks(w) >> 1, 2) + 1) / (1 + pcraft.getType().getCruiseSkipBlocks(w)));
                    }
                } else if (dive) {
                    tickCoolDown *= (Math.sqrt(Math.pow(1 + pcraft.getType().getCruiseSkipBlocks(w), 2) + 1) / (1 + pcraft.getType().getCruiseSkipBlocks(w)));
                }
            }

            if (Math.abs(ticksElapsed) < tickCoolDown) {
                continue;
            }
            cooldownCache.remove(pcraft);
            int dx = 0;
            int dz = 0;
            int dy = 0;

            // ascend
            if (pcraft.getCruiseDirection() == CruiseDirection.UP) {
                dy = 1 + pcraft.getType().getVertCruiseSkipBlocks();
            }
            // descend
            if (pcraft.getCruiseDirection() == CruiseDirection.DOWN) {
                dy = -1 - pcraft.getType().getVertCruiseSkipBlocks();
                if (pcraft.getHitBox().getMinY() <= w.getSeaLevel()) {
                    dy = -1;
                }
            } else if (dive) {
                dy = -((pcraft.getType().getCruiseSkipBlocks(w) + 1) >> 1);
                if (pcraft.getHitBox().getMinY() <= w.getSeaLevel()) {
                    dy = -1;
                }
            }
            // ship faces west
            if (pcraft.getCruiseDirection() == CruiseDirection.WEST) {
                dx = -1 - pcraft.getType().getCruiseSkipBlocks(w);
                if (bankRight) {
                    dz = (-1 - pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
                if (bankLeft) {
                    dz = (1 + pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
            }
            // ship faces east
            if (pcraft.getCruiseDirection() == CruiseDirection.EAST) {
                dx = 1 + pcraft.getType().getCruiseSkipBlocks(w);
                if (bankLeft) {
                    dz = (-1 - pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
                if (bankRight) {
                    dz = (1 + pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
            }
            // ship faces north
            if (pcraft.getCruiseDirection() == CruiseDirection.SOUTH) {
                dz = 1 + pcraft.getType().getCruiseSkipBlocks(w);
                if (bankRight) {
                    dx = (-1 - pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
                if (bankLeft) {
                    dx = (1 + pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
            }
            // ship faces south
            if (pcraft.getCruiseDirection() == CruiseDirection.NORTH) {
                dz = -1 - pcraft.getType().getCruiseSkipBlocks(w);
                if (bankLeft) {
                    dx = (-1 - pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
                if (bankRight) {
                    dx = (1 + pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
            }
            if (pcraft.getType().getCruiseOnPilot()) {
                dy = pcraft.getType().getCruiseOnPilotVertMove();
            }
            if (pcraft.getType().getGearShiftsAffectCruiseSkipBlocks()) {
                final int gearshift = pcraft.getCurrentGear();
                dx *= gearshift;
                dy *= gearshift;
                dz *= gearshift;
            }
            pcraft.translate(dx, dy, dz);
            pcraft.setLastDX(dx);
            pcraft.setLastDZ(dz);
            if (pcraft.getLastCruiseUpdate() != -1) {
                pcraft.setLastCruiseUpdate(System.currentTimeMillis());
            } else {
                pcraft.setLastCruiseUpdate(System.currentTimeMillis() - 30000);
            }
        }
    }

    private void detectSinking(){
        List<Craft> crafts = Lists.newArrayList(CraftManager.getInstance());
        for(Craft pcraft : crafts) {
            if (pcraft.getSinking()) {
                continue;
            }
            if (pcraft.getType().getSinkPercent() == 0.0 || !pcraft.isNotProcessing()) {
                continue;
            }
            long ticksElapsed = (System.currentTimeMillis() - pcraft.getLastBlockCheck()) / 50;

            if (ticksElapsed <= Settings.SinkCheckTicks) {
                continue;
            }
            final World w = pcraft.getW();
            int totalNonNegligibleBlocks = 0;
            int totalNonNegligibleWaterBlocks = 0;
            HashMap<List<Material>, Integer> foundFlyBlocks = new HashMap<>();
            HashMap<List<Material>, Integer> foundMoveBlocks = new HashMap<>();
            // go through each block in the blocklist, and
            // if its in the FlyBlocks, total up the number
            // of them
            for (MovecraftLocation l : pcraft.getHitBox()) {
                Material blockID = w.getBlockAt(l.getX(), l.getY(), l.getZ()).getType();
                for (List<Material> flyBlockDef : pcraft.getType().getFlyBlocks().keySet()) {
                    if (flyBlockDef.contains(blockID)) {
                        foundFlyBlocks.merge(flyBlockDef, 1, (a, b) -> a + b);
                    }
                }
                for (List<Material> moveBlockDef : pcraft.getType().getMoveBlocks().keySet()) {
                    if (moveBlockDef.contains(blockID)) {
                        foundMoveBlocks.merge(moveBlockDef, 1, (a, b) -> a + b);
                    }
                }

                if (blockID != Material.AIR && blockID != Material.FIRE) {
                    totalNonNegligibleBlocks++;
                }
                if (blockID != Material.AIR && blockID != Material.FIRE && blockID != Material.WATER) {
                    totalNonNegligibleWaterBlocks++;
                }
            }

            // now see if any of the resulting percentagesit
            // are below the threshold specified in
            // SinkPercent
            boolean isSinking = false;

            for (List<Material> i : pcraft.getType().getFlyBlocks().keySet()) {
                int numfound = 0;
                if (foundFlyBlocks.get(i) != null) {
                    numfound = foundFlyBlocks.get(i);
                }
                double percent = ((double) numfound / (double) totalNonNegligibleBlocks) * 100.0;
                double flyPercent = pcraft.getType().getFlyBlocks().get(i).get(0);
                double sinkPercent = flyPercent * pcraft.getType().getSinkPercent() / 100.0;
                if (percent < sinkPercent) {
                    isSinking = true;
                }

            }
            for (List<Material> i : pcraft.getType().getMoveBlocks().keySet()) {
                int numfound = 0;
                if (foundMoveBlocks.get(i) != null) {
                    numfound = foundMoveBlocks.get(i);
                }
                double percent = ((double) numfound / (double) totalNonNegligibleBlocks) * 100.0;
                double movePercent = pcraft.getType().getMoveBlocks().get(i).get(0);
                double disablePercent = movePercent * pcraft.getType().getSinkPercent() / 100.0;
                if (percent < disablePercent && !pcraft.getDisabled() && pcraft.isNotProcessing()) {
                    pcraft.setDisabled(true);
                    pcraft.getAudience().playSound(Sound.sound(Key.key("entity.iron_golem.death"), Sound.Source.NEUTRAL, 5.0f, 5.0f));
                }
            }

            // And check the overallsinkpercent
            if (pcraft.getType().getOverallSinkPercent() != 0.0) {
                double percent;
                if (pcraft.getType().blockedByWater()) {
                    percent = (double) totalNonNegligibleBlocks
                            / (double) pcraft.getOrigBlockCount();
                } else {
                    percent = (double) totalNonNegligibleWaterBlocks
                            / (double) pcraft.getOrigBlockCount();
                }
                if (percent * 100.0 < pcraft.getType().getOverallSinkPercent()) {
                    isSinking = true;
                }
            }

            if (totalNonNegligibleBlocks == 0) {
                isSinking = true;
            }

            // if the craft is sinking, let the player
            // know and release the craft. Otherwise
            // update the time for the next check
            if (isSinking && pcraft.isNotProcessing()) {
                pcraft.getAudience().sendMessage(I18nSupport.getInternationalisedComponent("Player - Craft is sinking"));
                pcraft.setCruising(false);
                pcraft.sink();
                CraftManager.getInstance().removePlayerFromCraft(pcraft);
            } else {
                pcraft.setLastBlockCheck(System.currentTimeMillis());
            }
        }
    }

    //Controls sinking crafts
    private void processSinking() {
        //copy the crafts before iteration to prevent concurrent modifications
        List<Craft> crafts = Lists.newArrayList(CraftManager.getInstance());
        for(Craft craft : crafts){
            if (craft == null || !craft.getSinking()) {
                continue;
            }
            if (craft.getHitBox().isEmpty() || craft.getHitBox().getMinY() < 5) {
                CraftManager.getInstance().removeCraft(craft, CraftReleaseEvent.Reason.SUNK);
                continue;
            }
            long ticksElapsed = (System.currentTimeMillis() - craft.getLastCruiseUpdate()) / 50;
            if (Math.abs(ticksElapsed) < craft.getType().getSinkRateTicks()) {
                continue;
            }
            int dx = 0;
            int dz = 0;
            if (craft.getType().getKeepMovingOnSink()) {
                dx = craft.getLastDX();
                dz = craft.getLastDZ();
            }
            craft.translate(dx, -1, dz);
            craft.setLastCruiseUpdate(System.currentTimeMillis() - (craft.getLastCruiseUpdate() != -1 ? 0 : 30000));
        }
    }

    private void processFadingBlocks() {
        if (Settings.FadeWrecksAfter == 0)
            return;
        long ticksElapsed = (System.currentTimeMillis() - lastFadeCheck) / 50;
        if (ticksElapsed <= Settings.FadeTickCooldown) {
            return;
        }
        List<HitBox> processed = new ArrayList<>();
        for(Map.Entry<HitBox, Long> entry : wrecks.entrySet()){
            if (Settings.FadeWrecksAfter * 1000 > System.currentTimeMillis() - entry.getValue()) {
                continue;
            }
            final HitBox hitBox = entry.getKey();
            final Map<Location, Pair<Material, Byte>> phaseBlocks = wreckPhases.get(hitBox);
            final World world = wreckWorlds.get(hitBox);
            ArrayList<UpdateCommand> commands = new ArrayList<>();
            int fadedBlocks = 0;
            if (!processedFadeLocs.containsKey(world)) {
                processedFadeLocs.put(world, new HashSet<>());
            }
            int maxFadeBlocks = (int) (hitBox.size() *  (Settings.FadePercentageOfWreckPerCycle / 100.0));
            //Iterate hitbox as a set to get more random locations
            for (MovecraftLocation location : hitBox.asSet()){
                if (processedFadeLocs.get(world).contains(location)) {
                    continue;
                }

                if (fadedBlocks >= maxFadeBlocks) {
                    break;
                }
                final Location bLoc = location.toBukkit(world);
                if ((Settings.FadeWrecksAfter + Settings.ExtraFadeTimePerBlock.getOrDefault(bLoc.getBlock().getType(), 0))* 1000 > System.currentTimeMillis() - entry.getValue()) {
                    continue;
                }
                fadedBlocks++;
                processedFadeLocs.get(world).add(location);
                Pair<Material, Byte> phaseBlock = phaseBlocks.getOrDefault(bLoc, new Pair<>(Material.AIR, (byte) 0));
                commands.add(new BlockCreateCommand(world, location, phaseBlock.getLeft(), phaseBlock.getRight()));
                
            }
            MapUpdateManager.getInstance().scheduleUpdates(commands);
            if (!processedFadeLocs.get(world).containsAll(hitBox.asSet())) {
                continue;
            }
            processed.add(hitBox);
            processedFadeLocs.get(world).removeAll(hitBox.asSet());
        }
        for(HitBox hitBox : processed){
            wrecks.remove(hitBox);
            wreckPhases.remove(hitBox);
            wreckWorlds.remove(hitBox);
        }

        lastFadeCheck = System.currentTimeMillis();
    }

    private void processDetection() {
        long ticksElapsed = (System.currentTimeMillis() - lastContactCheck) / 50;
        if (ticksElapsed > 21) {
            for (World w : Bukkit.getWorlds()) {
                if (w == null) {
                    continue;
                }
                for (Craft ccraft : CraftManager.getInstance().getPlayerCraftsInWorld(w)) {
                    if (!recentContactTracking.containsKey(ccraft)) {
                        recentContactTracking.put(ccraft, new HashMap<>());
                    }
                    for (Craft tcraft : ccraft.getContacts()) {
                        MovecraftLocation ccenter = ccraft.getHitBox().getMidPoint();
                        MovecraftLocation tcenter = tcraft.getHitBox().getMidPoint();
                        int diffx = ccenter.getX() - tcenter.getX();
                        int diffz = ccenter.getZ() - tcenter.getZ();
                        int distsquared = ccenter.distanceSquared(tcenter);
                        // craft has been detected

                        // has the craft not been seen in the last
                        // minute, or is completely new?
                        if (System.currentTimeMillis() - recentContactTracking.get(ccraft).getOrDefault(tcraft, 0L) <= 60000) {
                            continue;
                        }

                        Component notification = I18nSupport.getInternationalisedComponent("Contact - New Contact").append(Component.text( ": "));

                        if (tcraft.getName().length() >= 1){
                            notification = notification.append(Component.text(tcraft.getName() + " ("));
                        }
                        notification = notification.append(Component.text(tcraft.getType().getCraftName()));
                        if (tcraft.getName().length() >= 1){
                            notification = notification.append(Component.text(")"));
                        }
                        notification = notification.append(Component.text(" "))
                                .append(I18nSupport.getInternationalisedComponent("Contact - Commanded By"))
                                .append(Component.text(" "));
                        if (tcraft.getNotificationPlayer() != null) {
                            notification = notification.append(Component.text(tcraft.getNotificationPlayer().getDisplayName()));
                        } else {
                            notification = notification.append(Component.text("NULL"));
                        }
                        notification = notification.append(Component.text(", "))
                                .append(I18nSupport.getInternationalisedComponent("Contact - Size"))
                                .append(Component.text( ": "))
                                .append(Component.text(tcraft.getOrigBlockCount()))
                                .append(Component.text(", "))
                                .append(I18nSupport.getInternationalisedComponent("Contact - Range"))
                                .append(Component.text(": "))
                                .append(Component.text((int) Math.sqrt(distsquared)))
                                .append(Component.text(" "))
                                .append(I18nSupport.getInternationalisedComponent("Contact - To The"))
                                .append(Component.text(" "));
                        if (Math.abs(diffx) > Math.abs(diffz)) {
                            if (diffx < 0) {
                                notification = notification.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - East"));
                            } else {
                                notification = notification.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - West"));
                            }
                        }
                        else if (diffz < 0) {
                            notification = notification.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - South"));
                        }
                        else {
                            notification = notification.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - North"));
                        }

                        notification = notification.append(Component.text("."));

                        ccraft.getAudience().sendMessage(notification);
                        ccraft.getAudience().playSound(Sound.sound(Key.key(ccraft.getType().getCollisionSound().getKey().getKey()), Sound.Source.NEUTRAL, 1.0f, 2.0f));


                        long timestamp = System.currentTimeMillis();
                        recentContactTracking.get(ccraft).put(tcraft, timestamp);
                    }
                }
            }

            lastContactCheck = System.currentTimeMillis();
        }
    }

    public void run() {
        clearAll();

        processCruise();
        detectSinking();
        processSinking();
        processFadingBlocks();
        processDetection();
        processAlgorithmQueue();
        //processScheduledBlockChanges();
//		if(Settings.CompatibilityMode==false)
//			FastBlockChanger.getInstance().run();

        // now cleanup craft that are bugged and have not moved in the past 60 seconds, but have no pilot or are still processing
        for (Craft pcraft : CraftManager.getInstance()) {
            if (CraftManager.getInstance().getPlayerFromCraft(pcraft) == null) {
                if (pcraft.getLastCruiseUpdate() < System.currentTimeMillis() - 60000) {
                    CraftManager.getInstance().forceRemoveCraft(pcraft);
                }
            }
            if (!pcraft.isNotProcessing()) {
                if (pcraft.getCruising()) {
                    if (pcraft.getLastCruiseUpdate() < System.currentTimeMillis() - 5000) {
                        pcraft.setProcessing(false);
                    }
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
