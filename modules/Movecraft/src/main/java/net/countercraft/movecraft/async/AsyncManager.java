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

import at.pavlov.cannons.cannon.Cannon;
import com.google.common.collect.Lists;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.BlockCreateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
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
            task.runTaskAsynchronously(Movecraft.getInstance());
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
            final BitmapHitBox invertedHitBox = new BitmapHitBox(c.getHitBox().boundingHitBox()).difference(c.getHitBox());

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

        CraftManager.getInstance().addCraft(c, p);
    }

    /**
     * Processes translation task for its corresponding craft
     * @param task the task to process
     * @param c the craft this task belongs to
     * @return true if translation task succeded to process, otherwise false
     */
    private boolean processTranslation(@NotNull final TranslationTask task, @NotNull final Craft c) {
        Player notifyP = c.getNotificationPlayer();

        // Check that the craft hasn't been sneakily unpiloted

        if (task.failed()) {
            // The craft translation failed
            if (notifyP != null && !c.getSinking())
                notifyP.sendMessage(task.getFailMessage());

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
        // get list of cannons before sending map updates, to avoid
        // conflicts
        if (Movecraft.getInstance().getCannonsPlugin() != null && c.getNotificationPlayer() != null) {
            // convert blocklist to location list
            List<Location> shipLocations = new ArrayList<>();
            for (MovecraftLocation loc : c.getHitBox()) {
                Location tloc = new Location(c.getW(), loc.getX(), loc.getY(), loc.getZ());
                shipLocations.add(tloc);
            }
            HashSet<Cannon> shipCannons = Movecraft.getInstance().getCannonsPlugin().getCannonsAPI()
                    .getCannons(shipLocations, c.getNotificationPlayer().getUniqueId(), true);
            // move any cannons that were present
            for (Cannon can : shipCannons) {
                can.move(new Vector(task.getDx(), task.getDy(), task.getDz()));
            }
        }
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
        Player notifyP = c.getNotificationPlayer();
        // Check that the craft hasn't been sneakily unpiloted
        if (notifyP == null && !task.getIsSubCraft())  {
            return false;
        }

        if (task.isFailed()) {
            // The craft translation failed, don't try to notify
            // them if there is no pilot
            if (notifyP != null)
                notifyP.sendMessage(task.getFailMessage());
            else
                Movecraft.getInstance().getLogger().log(Level.INFO,
                        I18nSupport.getInternationalisedString("Rotation - NULL Player Rotation Failed")+ ": " + task.getFailMessage());
            return false;
        }

        // get list of cannons before sending map updates, to
        // avoid conflicts
        HashSet<Cannon> shipCannons = null;
        if (Movecraft.getInstance().getCannonsPlugin() != null && c.getNotificationPlayer() != null) {
            // convert blocklist to location list
            List<Location> shipLocations = new ArrayList<>();
            for (MovecraftLocation loc : c.getHitBox()) {
                shipLocations.add(loc.toBukkit(c.getW()));
            }
            shipCannons = Movecraft.getInstance().getCannonsPlugin().getCannonsAPI()
                    .getCannons(shipLocations, c.getNotificationPlayer().getUniqueId(), true);
        }

        MapUpdateManager.getInstance().scheduleUpdates(task.getUpdates());



        c.setHitBox(task.getNewHitBox());
        c.setFluidLocations(task.getNewFluidList());

        // rotate any cannons that were present
        if (Movecraft.getInstance().getCannonsPlugin() != null && shipCannons != null) {
            Location tloc = task.getOriginPoint().toBukkit(task.getCraft().getW());
            for (Cannon can : shipCannons) {
                if (task.getRotation() == Rotation.CLOCKWISE)
                    can.rotateRight(tloc.toVector());
                if (task.getRotation() == Rotation.ANTICLOCKWISE)
                    can.rotateLeft(tloc.toVector());
            }
        }



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
            if (pcraft.getPilotLocked()) {
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
            if(Settings.Debug) {
                Movecraft.getInstance().getLogger().info("TickCoolDown: " + tickCoolDown);
            }
            if(pcraft.getCruiseDirection() != 0x42 && pcraft.getCruiseDirection() != 0x43) {
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
            if(Settings.Debug) {
                Movecraft.getInstance().getLogger().info("New TickCoolDown: " + tickCoolDown);
                Movecraft.getInstance().getLogger().info("Direction:" + (bankLeft ? " Banking Left" : "") + (bankRight ? " Banking Right" : "") + (dive ? " Diving" : ""));
            }

            if (Math.abs(ticksElapsed) < tickCoolDown) {
                continue;
            }
            cooldownCache.remove(pcraft);
            int dx = 0;
            int dz = 0;
            int dy = 0;

            // ascend
            if (pcraft.getCruiseDirection() == 0x42) {
                dy = 1 + pcraft.getType().getVertCruiseSkipBlocks();
            }
            // descend
            if (pcraft.getCruiseDirection() == 0x43) {
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
            if (pcraft.getCruiseDirection() == 0x5) {
                dx = -1 - pcraft.getType().getCruiseSkipBlocks(w);
                if (bankRight) {
                    dz = (-1 - pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
                if (bankLeft) {
                    dz = (1 + pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
            }
            // ship faces east
            if (pcraft.getCruiseDirection() == 0x4) {
                dx = 1 + pcraft.getType().getCruiseSkipBlocks(w);
                if (bankLeft) {
                    dz = (-1 - pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
                if (bankRight) {
                    dz = (1 + pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
            }
            // ship faces north
            if (pcraft.getCruiseDirection() == 0x2) {
                dz = 1 + pcraft.getType().getCruiseSkipBlocks(w);
                if (bankRight) {
                    dx = (-1 - pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
                if (bankLeft) {
                    dx = (1 + pcraft.getType().getCruiseSkipBlocks(w)) >> 1;
                }
            }
            // ship faces south
            if (pcraft.getCruiseDirection() == 0x3) {
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
            int totalNonAirBlocks = 0;
            int totalNonAirWaterBlocks = 0;
            HashMap<List<Integer>, Integer> foundFlyBlocks = new HashMap<>();
            HashMap<List<Integer>, Integer> foundMoveBlocks = new HashMap<>();
            // go through each block in the blocklist, and
            // if its in the FlyBlocks, total up the number
            // of them
            for (MovecraftLocation l : pcraft.getHitBox()) {
                int blockID = w.getBlockAt(l.getX(), l.getY(), l.getZ()).getTypeId();
                int dataID = (int) w.getBlockAt(l.getX(), l.getY(), l.getZ()).getData();
                int shiftedID = (blockID << 4) + dataID + 10000;
                for (List<Integer> flyBlockDef : pcraft.getType().getFlyBlocks().keySet()) {
                    if (flyBlockDef.contains(blockID) || flyBlockDef.contains(shiftedID)) {
                        foundFlyBlocks.merge(flyBlockDef, 1, (a, b) -> a + b);
                    }
                }
                for (List<Integer> moveBlockDef : pcraft.getType().getMoveBlocks().keySet()) {
                    if (moveBlockDef.contains(blockID) || moveBlockDef.contains(shiftedID)) {
                        foundMoveBlocks.merge(moveBlockDef, 1, (a, b) -> a + b);
                    }
                }

                if (blockID != 0 && blockID != 51) {
                    totalNonAirBlocks++;
                }
                if (blockID != 0 && blockID != 8 && blockID != 9) {
                    totalNonAirWaterBlocks++;
                }
            }

            // now see if any of the resulting percentagesit
            // are below the threshold specified in
            // SinkPercent
            boolean isSinking = false;

            for (List<Integer> i : pcraft.getType().getFlyBlocks().keySet()) {
                int numfound = 0;
                if (foundFlyBlocks.get(i) != null) {
                    numfound = foundFlyBlocks.get(i);
                }
                double percent = ((double) numfound / (double) totalNonAirBlocks) * 100.0;
                double flyPercent = pcraft.getType().getFlyBlocks().get(i).get(0);
                double sinkPercent = flyPercent * pcraft.getType().getSinkPercent() / 100.0;
                if (percent < sinkPercent) {
                    isSinking = true;
                }

            }
            for (List<Integer> i : pcraft.getType().getMoveBlocks().keySet()) {
                int numfound = 0;
                if (foundMoveBlocks.get(i) != null) {
                    numfound = foundMoveBlocks.get(i);
                }
                double percent = ((double) numfound / (double) totalNonAirBlocks) * 100.0;
                double movePercent = pcraft.getType().getMoveBlocks().get(i).get(0);
                double disablePercent = movePercent * pcraft.getType().getSinkPercent() / 100.0;
                if (percent < disablePercent && !pcraft.getDisabled() && pcraft.isNotProcessing()) {
                    pcraft.setDisabled(true);
                    if (pcraft.getNotificationPlayer() != null) {
                        Location loc = pcraft.getNotificationPlayer().getLocation();
                        pcraft.getW().playSound(loc, Sound.ENTITY_IRONGOLEM_DEATH, 5.0f, 5.0f);
                    }
                }
            }

            // And check the overallsinkpercent
            if (pcraft.getType().getOverallSinkPercent() != 0.0) {
                double percent;
                if (pcraft.getType().blockedByWater()) {
                    percent = (double) totalNonAirBlocks
                            / (double) pcraft.getOrigBlockCount();
                } else {
                    percent = (double) totalNonAirWaterBlocks
                            / (double) pcraft.getOrigBlockCount();
                }
                if (percent * 100.0 < pcraft.getType().getOverallSinkPercent()) {
                    isSinking = true;
                }
            }

            if (totalNonAirBlocks == 0) {
                isSinking = true;
            }

            // if the craft is sinking, let the player
            // know and release the craft. Otherwise
            // update the time for the next check
            if (isSinking && pcraft.isNotProcessing()) {
                Player notifyP = pcraft.getNotificationPlayer();
                if (notifyP != null) {
                    notifyP.sendMessage(I18nSupport.getInternationalisedString("Player - Craft is sinking"));
                }
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
                for (Craft ccraft : CraftManager.getInstance().getCraftsInWorld(w)) {
                    if (CraftManager.getInstance().getPlayerFromCraft(ccraft) == null) {
                        continue;
                    }
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
                        String notification = I18nSupport.getInternationalisedString("Contact - New Contact") + ": ";

                        if (tcraft.getName().length() >= 1){
                            notification += tcraft.getName();
                            notification += ChatColor.RESET;
                            notification += " (";
                        }
                        notification += tcraft.getType().getCraftName();
                        if (tcraft.getName().length() >= 1){
                            notification += ")";
                        }
                        notification += " " + I18nSupport.getInternationalisedString("Contact - Commanded By")+" ";
                        if (tcraft.getNotificationPlayer() != null) {
                            notification += tcraft.getNotificationPlayer().getDisplayName();
                        } else {
                            notification += "NULL";
                        }
                        notification += ", " + I18nSupport.getInternationalisedString("Contact - Size") + ": ";
                        notification += tcraft.getOrigBlockCount();
                        notification += ", " + I18nSupport.getInternationalisedString("Contact - Range") + ": ";
                        notification += (int) Math.sqrt(distsquared);
                        notification += " " + I18nSupport.getInternationalisedString("Contact - To The") + " ";
                        if (Math.abs(diffx) > Math.abs(diffz))
                            if (diffx < 0)
                                notification += I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - East");
                            else
                                notification += I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - West");
                        else if (diffz < 0)
                            notification += I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - South");
                        else
                            notification += I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - North");

                        notification += ".";
                        ccraft.getNotificationPlayer().sendMessage(notification);
                        w.playSound(ccraft.getNotificationPlayer().getLocation(), ccraft.getType().getCollisionSound(), 1.0f, 2.0f);


                        long timestamp = System.currentTimeMillis();
                        recentContactTracking.get(ccraft).put(tcraft, timestamp);

                    }


                }
            }

            lastContactCheck = System.currentTimeMillis();
        }
    }

    //Removed for refactor
    /*private void processScheduledBlockChanges() {
        for (World w : Bukkit.getWorlds()) {
            if (w != null && CraftManager.getInstance().getCraftsInWorld(w) != null) {
                ArrayList<BlockTranslateCommand> updateCommands = new ArrayList<>();
                for (Craft pcraft : CraftManager.getInstance().getCraftsInWorld(w)) {
                    HashMap<BlockTranslateCommand, Long> scheduledBlockChanges = pcraft.getScheduledBlockChanges();
                    if (scheduledBlockChanges != null) {
                        Iterator<BlockTranslateCommand> mucI = scheduledBlockChanges.keySet().iterator();
                        boolean madeChanges = false;
                        while (mucI.hasNext()) {
                            BlockTranslateCommand muc = mucI.next();
                            if ((pcraft.getScheduledBlockChanges().get(muc) < System.currentTimeMillis()) && (pcraft.isNotProcessing())) {
                                int cx = muc.getNewBlockLocation().getX() >> 4;
                                int cz = muc.getNewBlockLocation().getZ() >> 4;
                                if (!w.isChunkLoaded(cx, cz)) {
                                    w.loadChunk(cx, cz);
                                }
                                if (w.getBlockAt(muc.getNewBlockLocation().getX(), muc.getNewBlockLocation().getY(), muc.getNewBlockLocation().getZ()).getType() == muc.getType()) {
                                    // if the block you will be updating later has changed type, something went horribly wrong: it burned away, was flooded, or was destroyed. Don't update it
                                    updateCommands.add(muc);
                                }
                                mucI.remove();
                                madeChanges = true;
                            }
                        }
                        if (madeChanges) {
                            pcraft.setScheduledBlockChanges(scheduledBlockChanges);
                        }
                    }
                }
                if (updateCommands.size() > 0) {
                    MapUpdateManager.getInstance().scheduleUpdates(updateCommands.toArray(new BlockTranslateCommand[1]));
                }
            }
        }
    }*/

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
