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
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.CollectionUtils;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.detection.DetectionTaskData;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.api.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.BlockCreateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.TownyUtils;
import net.countercraft.movecraft.utils.WGCustomFlagsUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public class AsyncManager extends BukkitRunnable {
    //private static AsyncManager instance = new AsyncManager();
    private final HashMap<AsyncTask, Craft> ownershipMap = new HashMap<>();
    private final HashMap<TNTPrimed, Double> TNTTracking = new HashMap<>();
    private final HashMap<Craft, HashMap<Craft, Long>> recentContactTracking = new HashMap<>();
    private final BlockingQueue<AsyncTask> finishedAlgorithms = new LinkedBlockingQueue<>();
    private final HashSet<Craft> clearanceSet = new HashSet<>();
    private HashMap<org.bukkit.entity.SmallFireball, Long> FireballTracking = new HashMap<>();
    private long lastTracerUpdate = 0;
    private long lastFireballCheck = 0;
    private long lastTNTContactCheck = 0;
    private long lastFadeCheck = 0;
    private long lastContactCheck = 0;
    private HashSet<Material> transparent = null;

    public AsyncManager() {
        transparent = new HashSet<>();
        transparent.add(Material.AIR);
        transparent.add(Material.GLASS);
        transparent.add(Material.THIN_GLASS);
        transparent.add(Material.STAINED_GLASS);
        transparent.add(Material.STAINED_GLASS_PANE);
        transparent.add(Material.IRON_FENCE);
        transparent.add(Material.REDSTONE_WIRE);
        transparent.add(Material.IRON_TRAPDOOR);
        transparent.add(Material.TRAP_DOOR);
        transparent.add(Material.NETHER_BRICK_STAIRS);
        transparent.add(Material.LEVER);
        transparent.add(Material.STONE_BUTTON);
        transparent.add(Material.WOOD_BUTTON);
        transparent.add(Material.STEP);
        transparent.add(Material.SMOOTH_STAIRS);
        transparent.add(Material.SIGN);
        transparent.add(Material.SIGN_POST);
        transparent.add(Material.WALL_SIGN);
    }

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
                DetectionTaskData data = task.getData();

                Player p = data.getPlayer();
                Player notifyP = data.getNotificationPlayer();
                Craft pCraft = CraftManager.getInstance().getCraftByPlayer(p);

                if (pCraft != null && p != null) {
                    // Player is already controlling a craft
                    notifyP.sendMessage(I18nSupport.getInternationalisedString("Detection - Failed - Already commanding a craft"));
                } else {
                    if (data.failed()) {
                        if (notifyP != null)
                            notifyP.sendMessage(data.getFailMessage());
                        else
                            Movecraft.getInstance().getLogger().log(Level.INFO,
                                    "NULL Player Craft Detection failed:" + data.getFailMessage());

                    } else {
                        Craft[] craftsInWorld = CraftManager.getInstance().getCraftsInWorld(c.getW());
                        boolean failed = false;
                        boolean isSubcraft = false;

                        if (craftsInWorld != null) {
                            for (Craft craft : craftsInWorld) {
                                if(craft.getHitBox().intersects(new HitBox(Arrays.asList(data.getBlockList())))){
                                    isSubcraft = true;
                                    if (c.getType().getCruiseOnPilot() || p != null) {
                                        if (craft.getType() == c.getType()
                                                || craft.getHitBox().size() <= data.getBlockList().length) {
                                            notifyP.sendMessage(I18nSupport.getInternationalisedString(
                                                    "Detection - Failed Craft is already being controlled"));
                                            failed = true;
                                        } else {
                                            // if this is a different type than
                                            // the overlapping craft, and is
                                            // smaller, this must be a child
                                            // craft, like a fighter on a
                                            // carrier
                                            if (!craft.isNotProcessing()) {
                                                failed = true;
                                                notifyP.sendMessage(I18nSupport.getInternationalisedString("Parent Craft is busy"));
                                            }
                                            craft.setHitBox(new HitBox(CollectionUtils.filter(craft.getHitBox(),Arrays.asList(data.getBlockList()))));
                                            craft.setOrigBlockCount(craft.getOrigBlockCount() - data.getBlockList().length);
                                        }
                                    }
                                }


                            }
                        }
                        if (c.getType().getMustBeSubcraft() && !isSubcraft) {
                            failed = true;
                            notifyP.sendMessage(I18nSupport.getInternationalisedString("Craft must be part of another craft"));
                        }
                        if (!failed) {
                            c.setHitBox(new HitBox(Arrays.asList(task.getData().getBlockList())));
                            c.setOrigBlockCount(data.getBlockList().length);
                            c.setNotificationPlayer(notifyP);
<<<<<<< HEAD
                            c.setUniqueName();
                            if (c.getType().getDynamicFlyBlockSpeedFactor() != 0.0) {
                                //c.setCurTickCooldown(c.getType().getCruiseTickCooldown());
                                //c.setMaxSpeed(c.getCurSpeed() + (data.dynamicFlyBlockSpeedMultiplier * c.getCurSpeed()));
                            } else {
                                //c.setCurTickCooldown(c.getType().getCruiseTickCooldown());
                                //c.setMaxSpeed(c.getCurSpeed());
                            }

=======
>>>>>>> upstream/master
                            if (notifyP != null) {
                                notifyP.sendMessage(I18nSupport
                                        .getInternationalisedString("Detection - Successfully piloted craft")
                                        + " Size: " + c.getHitBox().size());
                                Movecraft.getInstance().getLogger().log(Level.INFO,
                                        String.format(
                                                I18nSupport.getInternationalisedString(
                                                        "Detection - Success - Log Output"),
                                                notifyP.getName(), c.getType().getCraftName(), c.getHitBox().size(),
                                                c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
                            } else {
                                Movecraft.getInstance().getLogger().log(Level.INFO,
                                        String.format(
                                                I18nSupport.getInternationalisedString(
                                                        "Detection - Success - Log Output"),
                                                "NULL PLAYER", c.getType().getCraftName(), c.getHitBox().size(),
                                                c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
                            }
                            CraftManager.getInstance().addCraft(c, p);
                        }
                    }
                }

            } else if (poll instanceof TranslationTask) {
                // Process translation task

                TranslationTask task = (TranslationTask) poll;
                Player notifyP = c.getNotificationPlayer();

                // Check that the craft hasn't been sneakily unpiloted
                // if ( p != null ) { cruiseOnPilot crafts don't have player
                // pilots

                if (task.failed()) {
                    // The craft translation failed
                    if (notifyP != null && !c.getSinking())
                        notifyP.sendMessage(task.getFailMessage());

                    if (task.collisionExplosion()) {
                        c.setHitBox(task.getNewHitBox());
                        //c.setBlockList(task.getData().getBlockList());
                        //boolean failed = MapUpdateManager.getInstance().addWorldUpdate(c.getW(), updates, null, null, exUpdates);
                        MapUpdateManager mapUpdateManager= MapUpdateManager.getInstance();
                        for(UpdateCommand updateCommand : task.getUpdates())
                            mapUpdateManager.scheduleUpdate(updateCommand);
                        sentMapUpdate = true;

                    }
                } else {
                    // The craft is clear to move, perform the block updates
                    MapUpdateManager.getInstance().scheduleUpdates(task.getUpdates());
                    // get list of cannons before sending map updates, to avoid
                    // conflicts
                    HashSet<Cannon> shipCannons = null;
                    if (Movecraft.getInstance().getCannonsPlugin() != null && c.getNotificationPlayer() != null) {
                        // convert blocklist to location list
                        List<Location> shipLocations = new ArrayList<>();
                        for (MovecraftLocation loc : c.getHitBox()) {
                            Location tloc = new Location(c.getW(), loc.getX(), loc.getY(), loc.getZ());
                            shipLocations.add(tloc);
                        }
                        
                        shipCannons = Movecraft.getInstance().getCannonsPlugin().getCannonsAPI().getCannons(shipLocations, notifyP, true);
                        		//.getCannons(shipLocations, c.getNotificationPlayer().getUniqueId(), true);
                    }

                    sentMapUpdate = true;
                    //c.setBlockList(task.getData().getBlockList());
                    //c.setMinX(task.getData().getMinX());
                    //c.setMinZ(task.getData().getMinZ());
                    //c.setHitBox(task.getData().getHitbox());
                    c.setHitBox(task.getNewHitBox());

                    // move any cannons that were present
                    if (Movecraft.getInstance().getCannonsPlugin() != null && shipCannons != null) {
                        for (Cannon can : shipCannons) {
                            can.move(new Vector(task.getDx(), task.getDy(), task.getDz()));
                        }
                    }


                }

            } else if (poll instanceof RotationTask) {
                // Process rotation task
                RotationTask task = (RotationTask) poll;
                Player notifyP = c.getNotificationPlayer();

                // Check that the craft hasn't been sneakily unpiloted
                if (notifyP != null || task.getIsSubCraft()) {

                    if (task.isFailed()) {
                        // The craft translation failed, don't try to notify
                        // them if there is no pilot
                        if (notifyP != null)
                            notifyP.sendMessage(task.getFailMessage());
                        else
                            Movecraft.getInstance().getLogger().log(Level.INFO,
                                    "NULL Player Rotation Failed: " + task.getFailMessage());
                    } else {
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
                                    .getCannons(shipLocations, notifyP, true);
                        }

                        MapUpdateManager.getInstance().scheduleUpdates(task.getUpdates());


                        sentMapUpdate = true;

                        /*c.setBlockList(task.getBlockList());
                        c.setMinX(task.getMinX());
                        c.setMinZ(task.getMinZ());
                        c.setHitBox(task.getHitbox());*/
                        c.setHitBox(task.getNewHitBox());

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

                    }
                }
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

    private void processCruise() {
        for (Craft pcraft : CraftManager.getInstance().getCraftList()) {
            if (pcraft == null || !pcraft.isNotProcessing() || !pcraft.getCruising()) {
                continue;
            }
            long ticksElapsed = (System.currentTimeMillis() - pcraft.getLastCruiseUpdate()) / 50;
            World w = pcraft.getW();
            // if the craft should go slower underwater, make
            // time pass more slowly there
<<<<<<< HEAD
            if (pcraft.getType().getHalfSpeedUnderwater() && pcraft.getMinY() < w.getSeaLevel())
=======
            if (pcraft.getType().getHalfSpeedUnderwater() && pcraft.getHitBox().getMinY() < w.getSeaLevel())
>>>>>>> upstream/master
                ticksElapsed >>= 1;
            // check direct controls to modify movement
            boolean bankLeft = false;
            boolean bankRight = false;
            boolean dive = false;
<<<<<<< HEAD
            boolean climb = false;
=======
>>>>>>> upstream/master
            if (pcraft.getPilotLocked()) {
                if (pcraft.getNotificationPlayer().isSneaking())
                    dive = true;
                if (pcraft.getNotificationPlayer().getInventory().getHeldItemSlot() == 3)
                    bankLeft = true;
                if (pcraft.getNotificationPlayer().getInventory().getHeldItemSlot() == 5)
                    bankRight = true;
<<<<<<< HEAD
                if (pcraft.getNotificationPlayer().getInventory().getHeldItemSlot() == 4)
                	climb = true;
=======
>>>>>>> upstream/master
            }

            if (Math.abs(ticksElapsed) < pcraft.getTickCooldown()) {
                return;
            }
            int dx = 0;
            int dz = 0;
            int dy = 0;

            // ascend
            if (pcraft.getCruiseDirection() == 0x42) {
                dy = 1 + pcraft.getType().getVertCruiseSkipBlocks();
            }
            // descend
            if (pcraft.getCruiseDirection() == 0x43) {
                dy = 0 - 1 - pcraft.getType().getVertCruiseSkipBlocks();
<<<<<<< HEAD
                if (pcraft.getMinY() <= w.getSeaLevel()) {
=======
                if (pcraft.getHitBox().getMinY() <= w.getSeaLevel()) {
>>>>>>> upstream/master
                    dy = -1;
                }
            } else if (dive) {
                dy = 0 - ((pcraft.getType().getCruiseSkipBlocks() + 1) >> 1);
<<<<<<< HEAD
                if (pcraft.getMinY() <= w.getSeaLevel()) {
                    dy = -1;
                }
            } else if (climb) {
            	dy = 0 + ((pcraft.getType().getCruiseSkipBlocks() + 1) >> 1);
            	
=======
                if (pcraft.getHitBox().getMinY() <= w.getSeaLevel()) {
                    dy = -1;
                }
>>>>>>> upstream/master
            }
            // ship faces west
            if (pcraft.getCruiseDirection() == 0x5) {
                dx = 0 - 1 - pcraft.getType().getCruiseSkipBlocks();
                if (bankRight) {
                    dz = (0 - 1 - pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
                if (bankLeft) {
                    dz = (1 + pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
            }
            // ship faces east
            if (pcraft.getCruiseDirection() == 0x4) {
                dx = 1 + pcraft.getType().getCruiseSkipBlocks();
                if (bankLeft) {
                    dz = (0 - 1 - pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
                if (bankRight) {
                    dz = (1 + pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
            }
            // ship faces north
            if (pcraft.getCruiseDirection() == 0x2) {
                dz = 1 + pcraft.getType().getCruiseSkipBlocks();
                if (bankRight) {
                    dx = (0 - 1 - pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
                if (bankLeft) {
                    dx = (1 + pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
            }
            // ship faces south
            if (pcraft.getCruiseDirection() == 0x3) {
                dz = 0 - 1 - pcraft.getType().getCruiseSkipBlocks();
                if (bankLeft) {
                    dx = (0 - 1 - pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
                if (bankRight) {
                    dx = (1 + pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
            }
            if (pcraft.getType().getCruiseOnPilot()) {
                dy = pcraft.getType().getCruiseOnPilotVertMove();
            }
            pcraft.translate(dx, dy, dz);
            pcraft.setLastDX(dx);
            pcraft.setLastDZ(dz);
            if (pcraft.getLastCruiseUpdate() != -1) {
                pcraft.setLastCruisUpdate(System.currentTimeMillis());
            } else {
                pcraft.setLastCruisUpdate(System.currentTimeMillis() - 30000);
            }
        }
    }

    private boolean isRegionBlockedPVP(MovecraftLocation loc, World w) {
        if (Movecraft.getInstance().getWorldGuardPlugin() == null)
            return false;
        if (!Settings.WorldGuardBlockSinkOnPVPPerm)
            return false;

        Location nativeLoc = new Location(w, loc.getX(), loc.getY(), loc.getZ());
        ApplicableRegionSet set = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(w)
                .getApplicableRegions(nativeLoc);
        return !set.allows(DefaultFlag.PVP);
    }

    private boolean isRegionFlagSinkAllowed(MovecraftLocation loc, World w) {
        if (Movecraft.getInstance().getWorldGuardPlugin() != null
                && Movecraft.getInstance().getWGCustomFlagsPlugin() != null && Settings.WGCustomFlagsUseSinkFlag) {
            Location nativeLoc = new Location(w, loc.getX(), loc.getY(), loc.getZ());
            WGCustomFlagsUtils WGCFU = new WGCustomFlagsUtils();
            return WGCFU.validateFlag(nativeLoc, Movecraft.FLAG_SINK);
        } else {
            return true;
        }
    }

    private Location isTownyPlotPVPEnabled(MovecraftLocation loc, World w, Set<TownBlock> townBlockSet) {
        Location plugLoc = new Location(w, loc.getX(), loc.getY(), loc.getZ());
        TownBlock townBlock = TownyUtils.getTownBlock(plugLoc);
        if (townBlock != null && !townBlockSet.contains(townBlock)) {
            if (TownyUtils.validatePVP(townBlock)) {
                townBlockSet.add(townBlock);
                return null;
            } else {
                return plugLoc;
            }
        } else {
            return null;
        }
    }

    private void processSinking() {
        for (World w : Bukkit.getWorlds()) {
            if (w == null || CraftManager.getInstance().getCraftsInWorld(w) == null) {
                continue;
            }
            TownyWorld townyWorld;
            boolean townyEnabled = false;
            if (Movecraft.getInstance().getTownyPlugin() != null && Settings.TownyBlockSinkOnNoPVP) {
                townyWorld = TownyUtils.getTownyWorld(w);
                if (townyWorld != null) {
                    townyEnabled = townyWorld.isUsingTowny();
                }
            }
            // check every few seconds for every craft to see if it should
            // be sinking or disabled
            for (Craft pcraft : CraftManager.getInstance().getCraftsInWorld(w)) {
                Set<TownBlock> townBlockSet = new HashSet<>();
                if (pcraft != null && !pcraft.getSinking()) {
                    if (pcraft.getType().getSinkPercent() != 0.0 && pcraft.isNotProcessing()) {
                        long ticksElapsed = (System.currentTimeMillis() - pcraft.getLastBlockCheck()) / 50;

                        if (ticksElapsed > Settings.SinkCheckTicks) {
                            int totalNonAirBlocks = 0;
                            int totalNonAirWaterBlocks = 0;
                            HashMap<ArrayList<Integer>, Integer> foundFlyBlocks = new HashMap<>();
                            HashMap<ArrayList<Integer>, Integer> foundMoveBlocks = new HashMap<>();
                            boolean regionPVPBlocked = false;
                            boolean sinkingForbiddenByFlag = false;
                            boolean sinkingForbiddenByTowny = false;
                            // go through each block in the blocklist, and
                            // if its in the FlyBlocks, total up the number
                            // of them
                            Location townyLoc = null;
                            for (MovecraftLocation l : pcraft.getHitBox()) {
                                if (isRegionBlockedPVP(l, w))
                                    regionPVPBlocked = true;
                                if (!isRegionFlagSinkAllowed(l, w))
                                    sinkingForbiddenByFlag = true;
                                if (townyLoc == null && townyEnabled && Settings.TownyBlockSinkOnNoPVP) {
                                    townyLoc = isTownyPlotPVPEnabled(l, w, townBlockSet);
                                    if (townyLoc != null) {
                                        sinkingForbiddenByTowny = true;
                                    }
                                }
                                Integer blockID = w.getBlockAt(l.getX(), l.getY(), l.getZ()).getTypeId();
                                Integer dataID = (int) w.getBlockAt(l.getX(), l.getY(), l.getZ()).getData();
                                Integer shiftedID = (blockID << 4) + dataID + 10000;
                                for (ArrayList<Integer> flyBlockDef : pcraft.getType().getFlyBlocks().keySet()) {
                                    if (flyBlockDef.contains(blockID) || flyBlockDef.contains(shiftedID)) {
                                        foundFlyBlocks.merge(flyBlockDef, 1, (a, b) -> a + b);
                                    }
                                }
                                if (pcraft.getType().getMoveBlocks() != null) {
                                    for (ArrayList<Integer> moveBlockDef : pcraft.getType().getMoveBlocks().keySet()) {
                                        if (moveBlockDef.contains(blockID) || moveBlockDef.contains(shiftedID)) {
                                            foundMoveBlocks.merge(moveBlockDef, 1, (a, b) -> a + b);
                                        }
                                    }
                                }

                                if (blockID != 0) {
                                    totalNonAirBlocks++;
                                }
                                if (blockID != 0 && blockID != 8 && blockID != 9) {
                                    totalNonAirWaterBlocks++;
                                }
                            }

                            // now see if any of the resulting percentages
                            // are below the threshold specified in
                            // SinkPercent
                            boolean isSinking = false;

                            for (ArrayList<Integer> i : pcraft.getType().getFlyBlocks().keySet()) {
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
                            if (pcraft.getType().getMoveBlocks() != null) {
                                for (ArrayList<Integer> i : pcraft.getType().getMoveBlocks().keySet()) {
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

                            if (isSinking && (regionPVPBlocked || sinkingForbiddenByFlag || sinkingForbiddenByTowny)
                                    && pcraft.isNotProcessing()) {
                                Player notifyP = pcraft.getNotificationPlayer();
                                if (notifyP != null)
                                    if (regionPVPBlocked) {
                                        notifyP.sendMessage(I18nSupport.getInternationalisedString(
                                                "Player- Craft should sink but PVP is not allowed in this WorldGuard region"));
                                    } else if (sinkingForbiddenByFlag) {
                                        notifyP.sendMessage(I18nSupport.getInternationalisedString(
                                                "WGCustomFlags - Sinking a craft is not allowed in this WorldGuard region"));
                                    } else {
                                        if (townyLoc != null) {
                                            notifyP.sendMessage(String.format(
                                                    I18nSupport.getInternationalisedString(
                                                            "Towny - Sinking a craft is not allowed in this town plot")
                                                            + " @ %d,%d,%d", townyLoc.getBlockX(), townyLoc.getBlockY(),
                                                    townyLoc.getBlockZ()));
                                        } else {
                                            notifyP.sendMessage(
                                                    I18nSupport.getInternationalisedString(
                                                            "Towny - Sinking a craft is not allowed in this town plot"));
                                        }

                                    }
                                pcraft.setCruising(false);
                                CraftManager.getInstance().removeCraft(pcraft);
                            } else {
                                // if the craft is sinking, let the player
                                // know and release the craft. Otherwise
                                // update the time for the next check
                                if (isSinking && pcraft.isNotProcessing()) {
                                    Player notifyP = pcraft.getNotificationPlayer();
                                    if (notifyP != null)
                                        notifyP.sendMessage(I18nSupport
                                                .getInternationalisedString("Player- Craft is sinking"));
                                    pcraft.setCruising(false);
                                    pcraft.setSinking(true);
                                    CraftManager.getInstance().removePlayerFromCraft(pcraft);
                                    final Craft releaseCraft = pcraft;
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            CraftManager.getInstance().removeCraft(releaseCraft);
                                        }
                                    }.runTaskLater(Movecraft.getInstance(), (20 * 600));
                                } else {
                                    pcraft.setLastBlockCheck(System.currentTimeMillis());
                                }
                            }
                        }
                    }
                }
            }

            // sink all the sinking ships
            if (CraftManager.getInstance().getCraftsInWorld(w) != null) {
                for (Craft pcraft : CraftManager.getInstance().getCraftsInWorld(w)) {
                    if (pcraft != null && pcraft.getSinking()) {
                        if (pcraft.getHitBox().size() == 0) {
                            CraftManager.getInstance().removeCraft(pcraft);
                        }
                        if (pcraft.getHitBox().getMinY() < 5) {
                            CraftManager.getInstance().removeCraft(pcraft);
                        }
                        long ticksElapsed = (System.currentTimeMillis() - pcraft.getLastCruiseUpdate()) / 50;
                        if (Math.abs(ticksElapsed) >= pcraft.getType().getSinkRateTicks()) {
                            int dx = 0;
                            int dz = 0;
                            if (pcraft.getType().getKeepMovingOnSink()) {
                                dx = pcraft.getLastDX();
                                dz = pcraft.getLastDZ();
                            }
                            pcraft.translate(dx, -1, dz);
                            if (pcraft.getLastCruiseUpdate() != -1) {
                                pcraft.setLastCruisUpdate(System.currentTimeMillis());
                            } else {
                                pcraft.setLastCruisUpdate(System.currentTimeMillis() - 30000);
                            }
                        }
                    }
                }
            }
        }
    }

    private void processTracers() {
        if (Settings.TracerRateTicks == 0)
            return;
        long ticksElapsed = (System.currentTimeMillis() - lastTracerUpdate) / 50;
        if (ticksElapsed > Settings.TracerRateTicks) {
            for (World w : Bukkit.getWorlds()) {
                if (w != null) {
                    for (TNTPrimed tnt : w.getEntitiesByClass(TNTPrimed.class)) {
                        if (tnt.getVelocity().lengthSquared() > 0.25) {
                            for (Player p : w.getPlayers()) {
                                // is the TNT within the view distance (rendered
                                // world) of the player?
                                long maxDistSquared = Bukkit.getServer().getViewDistance() * 16;
                                maxDistSquared = maxDistSquared - 16;
                                maxDistSquared = maxDistSquared * maxDistSquared;

                                if (p.getLocation().distanceSquared(tnt.getLocation()) < maxDistSquared) { // we
                                    // use
                                    // squared
                                    // because
                                    // its
                                    // faster
                                    final Location loc = tnt.getLocation();
                                    final Player fp = p;
                                    // then make a cobweb to look like smoke,
                                    // place it a little later so it isn't right
                                    // in the middle of the volley
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            fp.sendBlockChange(loc, 30, (byte) 0);
                                        }
                                    }.runTaskLater(Movecraft.getInstance(), 5);
                                    // then remove it
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            // fp.sendBlockChange(loc,
                                            // fw.getBlockAt(loc).getType(),
                                            // fw.getBlockAt(loc).getData());
                                            fp.sendBlockChange(loc, 0, (byte) 0);
                                        }
                                    }.runTaskLater(Movecraft.getInstance(), 160);
                                }
                            }
                        }
                    }
                }
            }
            lastTracerUpdate = System.currentTimeMillis();
        }
    }

    private void processFireballs() {
        long ticksElapsed = (System.currentTimeMillis() - lastFireballCheck) / 50;

        if (ticksElapsed > 3) {
            for (World w : Bukkit.getWorlds()) {
                if (w != null) {
                    for (org.bukkit.entity.SmallFireball fireball : w
                            .getEntitiesByClass(org.bukkit.entity.SmallFireball.class)) {
                        if (!(fireball.getShooter() instanceof org.bukkit.entity.LivingEntity)) { // means
                            // it
                            // was
                            // launched
                            // by
                            // a
                            // dispenser
                            if (w.getPlayers().size() > 0) {
                                Player p = w.getPlayers().get(0);

                                if (!FireballTracking.containsKey(fireball)) {
                                    Craft c = fastNearestCraftToLoc(fireball.getLocation());
                                    if (c != null) {
                                        int distX = c.getHitBox().getMinX() + c.getHitBox().getMaxX();
                                        distX = distX >> 1;
                                        distX = Math.abs(distX - fireball.getLocation().getBlockX());
                                        int distY = c.getHitBox().getMinY() + c.getHitBox().getMaxY();
                                        distY = distY >> 1;
                                        distY = Math.abs(distY - fireball.getLocation().getBlockY());
                                        int distZ = c.getHitBox().getMinZ() + c.getHitBox().getMaxZ();
                                        distZ = distZ >> 1;
                                        distZ = Math.abs(distZ - fireball.getLocation().getBlockZ());
                                        boolean inRange = (distX < 50) && (distY < 50) && (distZ < 50);
                                        if ((c.getAADirector() != null) && inRange) {
                                            p = c.getAADirector();
                                            if (p.getItemInHand().getTypeId() == Settings.PilotTool) {
                                                Vector fv = fireball.getVelocity();
                                                double speed = fv.length(); // store the speed to add it back in later, since all the values we will be using are "normalized", IE: have a speed of 1
                                                fv = fv.normalize(); // you normalize it for comparison with the new direction to see if we are trying to steer too far
                                                Block targetBlock = p.getTargetBlock(transparent, 120);
                                                Vector targetVector;
                                                if (targetBlock == null) { // the player is looking at nothing, shoot in that general direction
                                                    targetVector = p.getLocation().getDirection();
                                                } else { // shoot directly at the block the player is looking at (IE: with convergence)
                                                    targetVector = targetBlock.getLocation().toVector().subtract(fireball.getLocation().toVector());
                                                    targetVector = targetVector.normalize();
                                                }
                                                if (targetVector.getX() - fv.getX() > 0.5) {
                                                    fv.setX(fv.getX() + 0.5);
                                                } else if (targetVector.getX() - fv.getX() < -0.5) {
                                                    fv.setX(fv.getX() - 0.5);
                                                } else {
                                                    fv.setX(targetVector.getX());
                                                }
                                                if (targetVector.getY() - fv.getY() > 0.5) {
                                                    fv.setY(fv.getY() + 0.5);
                                                } else if (targetVector.getY() - fv.getY() < -0.5) {
                                                    fv.setY(fv.getY() - 0.5);
                                                } else {
                                                    fv.setY(targetVector.getY());
                                                }
                                                if (targetVector.getZ() - fv.getZ() > 0.5) {
                                                    fv.setZ(fv.getZ() + 0.5);
                                                } else if (targetVector.getZ() - fv.getZ() < -0.5) {
                                                    fv.setZ(fv.getZ() - 0.5);
                                                } else {
                                                    fv.setZ(targetVector.getZ());
                                                }
                                                fv = fv.multiply(speed); // put the original speed back in, but now along a different trajectory
                                                fireball.setVelocity(fv);
                                                fireball.setDirection(fv);
                                            }
                                        } else {
                                            p = c.getNotificationPlayer();
                                        }
                                    }
                                    // give it a living shooter, then set the
                                    // fireball to be deleted
                                    fireball.setShooter(p);
                                    FireballTracking.put(fireball, System.currentTimeMillis());
                                }
                            }
                        }
                    }
                }
            }

            int timelimit = 20 * Settings.FireballLifespan * 50;
            // then, removed any exploded TNT from tracking
            Iterator<org.bukkit.entity.SmallFireball> fireballI = FireballTracking.keySet().iterator();
            while (fireballI.hasNext()) {
                org.bukkit.entity.SmallFireball fireball = fireballI.next();
                if (fireball != null)
                    if (System.currentTimeMillis() - FireballTracking.get(fireball) > timelimit) {
                        fireball.remove();
                        fireballI.remove();
                    }
            }

            lastFireballCheck = System.currentTimeMillis();
        }
    }

    private Craft fastNearestCraftToLoc(Location loc) {
        Craft ret = null;
        long closestDistSquared = 1000000000L;
        Craft[] craftsList = CraftManager.getInstance().getCraftsInWorld(loc.getWorld());
        if (craftsList != null) {
            for (Craft i : craftsList) {
                int midX = (i.getHitBox().getMaxX() + i.getHitBox().getMinX()) >> 1;
//				int midY=(i.getMaxY()+i.getMinY())>>1; don't check Y because it is slow
                int midZ = (i.getHitBox().getMaxZ() + i.getHitBox().getMinZ()) >> 1;
                long distSquared = Math.abs(midX - (int) loc.getX());
//				distSquared+=Math.abs(midY-(int)loc.getY());
                distSquared += Math.abs(midZ - (int) loc.getZ());
                if (distSquared < closestDistSquared) {
                    closestDistSquared = distSquared;
                    ret = i;
                }
            }
        }
        return ret;
    }

    private void processTNTContactExplosives() {
        long ticksElapsed = (System.currentTimeMillis() - lastTNTContactCheck) / 50;
        if (ticksElapsed > 0) {
            // see if there is any new rapid moving TNT in the worlds
            for (World w : Bukkit.getWorlds()) {
                if (w != null) {
                    for (TNTPrimed tnt : w.getEntitiesByClass(TNTPrimed.class)) {
                        if ((tnt.getVelocity().lengthSquared() > 0.35)) {
                            if (!TNTTracking.containsKey(tnt)) {
                                Craft c = fastNearestCraftToLoc(tnt.getLocation());
                                if (c != null) {
                                    int distX = c.getHitBox().getMinX() + c.getHitBox().getMaxX();
                                    distX = distX >> 1;
                                    distX = Math.abs(distX - tnt.getLocation().getBlockX());
                                    int distY = c.getHitBox().getMinY() + c.getHitBox().getMaxY();
                                    distY = distY >> 1;
                                    distY = Math.abs(distY - tnt.getLocation().getBlockY());
                                    int distZ = c.getHitBox().getMinZ() + c.getHitBox().getMaxZ();
                                    distZ = distZ >> 1;
                                    distZ = Math.abs(distZ - tnt.getLocation().getBlockZ());
                                    boolean inRange = (distX < 100) && (distY < 100) && (distZ < 100);
                                    if ((c.getCannonDirector() != null) && inRange) {
                                        Player p = c.getCannonDirector();
                                        if (p.getInventory().getItemInMainHand().getTypeId() == Settings.PilotTool) {
                                            Vector tv = tnt.getVelocity();
                                            double speed = tv.length(); // store the speed to add it back in later, since all the values we will be using are "normalized", IE: have a speed of 1
                                            tv = tv.normalize(); // you normalize it for comparison with the new direction to see if we are trying to steer too far
                                            Block targetBlock = p.getTargetBlock(transparent, 120);
                                            Vector targetVector;
                                            if (targetBlock == null) { // the player is looking at nothing, shoot in that general direction
                                                targetVector = p.getLocation().getDirection();
                                            } else { // shoot directly at the block the player is looking at (IE: with convergence)
                                                targetVector = targetBlock.getLocation().toVector().subtract(tnt.getLocation().toVector());
                                                targetVector = targetVector.normalize();
                                            }
                                            if (targetVector.getX() - tv.getX() > 0.7) {
                                                tv.setX(tv.getX() + 0.7);
                                            } else if (targetVector.getX() - tv.getX() < -0.7) {
                                                tv.setX(tv.getX() - 0.7);
                                            } else {
                                                tv.setX(targetVector.getX());
                                            }
                                            if (targetVector.getZ() - tv.getZ() > 0.7) {
                                                tv.setZ(tv.getZ() + 0.7);
                                            } else if (targetVector.getZ() - tv.getZ() < -0.7) {
                                                tv.setZ(tv.getZ() - 0.7);
                                            } else {
                                                tv.setZ(targetVector.getZ());
                                            }
                                            tv = tv.multiply(speed); // put the original speed back in, but now along a different trajectory
                                            tv.setY(tnt.getVelocity().getY()); // you leave the original Y (or vertical axis) trajectory as it was
                                            tnt.setVelocity(tv);
                                        }
                                    }
                                }
                                TNTTracking.put(tnt, tnt.getVelocity().lengthSquared());
                            }
                        }
                    }
                }
            }

            // then, removed any exploded TNT from tracking
            TNTTracking.keySet().removeIf(tnt -> tnt.getFuseTicks() <= 0);

            // now check to see if any has abruptly changed velocity, and should
            // explode
            for (TNTPrimed tnt : TNTTracking.keySet()) {
                double vel = tnt.getVelocity().lengthSquared();
                if (vel < TNTTracking.get(tnt) / 10.0) {
                    tnt.setFuseTicks(0);
                } else {
                    // update the tracking with the new velocity so gradual
                    // changes do not make TNT explode
                    TNTTracking.put(tnt, vel);
                }
            }

            lastTNTContactCheck = System.currentTimeMillis();
        }
    }

    private void processFadingBlocks() {
        if (Settings.FadeWrecksAfter == 0)
            return;
        long ticksElapsed = (System.currentTimeMillis() - lastFadeCheck) / 50;
        if (ticksElapsed > 20) {
            for (World w : Bukkit.getWorlds()) {
                if (w != null) {
                    ArrayList<UpdateCommand> updateCommands = new ArrayList<>();
                    CopyOnWriteArrayList<MovecraftLocation> locations = null;

                    // I know this is horrible, but I honestly don't see another
                    // way to do this...
                    int numTries = 0;
                    while ((locations == null) && (numTries < 100)) {
                        try {
                            locations = new CopyOnWriteArrayList<>(
                                    Movecraft.getInstance().blockFadeTimeMap.keySet());
                        } catch (java.util.ConcurrentModificationException e) {
                            numTries++;
                        } catch (java.lang.NegativeArraySizeException e) {
                            Movecraft.getInstance().blockFadeTimeMap = new HashMap<>();
                            Movecraft.getInstance().blockFadeTypeMap = new HashMap<>();
                            Movecraft.getInstance().blockFadeWaterMap = new HashMap<>();
                            Movecraft.getInstance().blockFadeWorldMap = new HashMap<>();
                            locations = new CopyOnWriteArrayList<>(
                                    Movecraft.getInstance().blockFadeTimeMap.keySet());
                        }
                    }

                    for (MovecraftLocation loc : locations) {
                        if (Movecraft.getInstance().blockFadeWorldMap.get(loc) == w) {
                            Long time = Movecraft.getInstance().blockFadeTimeMap.get(loc);
                            Integer type = Movecraft.getInstance().blockFadeTypeMap.get(loc);
                            Boolean water = Movecraft.getInstance().blockFadeWaterMap.get(loc);
                            if (time != null && type != null && water != null) {
                                long secsElapsed = (System.currentTimeMillis()
                                        - Movecraft.getInstance().blockFadeTimeMap.get(loc)) / 1000;
                                // has enough time passed to fade the block?
                                boolean timeElapsed;
                                // make containers take longer to fade so their loot can be recovered
                                if (w.getBlockTypeIdAt(loc.getX(), loc.getY(), loc.getZ()) == 54) {
                                    timeElapsed = secsElapsed > Settings.FadeWrecksAfter * 5;
                                } else if (w.getBlockTypeIdAt(loc.getX(), loc.getY(), loc.getZ()) == 146) {
                                    timeElapsed = secsElapsed > Settings.FadeWrecksAfter * 5;
                                } else if (w.getBlockTypeIdAt(loc.getX(), loc.getY(), loc.getZ()) == 158) {
                                    timeElapsed = secsElapsed > Settings.FadeWrecksAfter * 5;
                                } else if (w.getBlockTypeIdAt(loc.getX(), loc.getY(), loc.getZ()) == 23) {
                                    timeElapsed = secsElapsed > Settings.FadeWrecksAfter * 5;
                                } else {
                                    timeElapsed = secsElapsed > Settings.FadeWrecksAfter;
                                }
                                if (timeElapsed) {
                                    // load the chunk if it hasn't been already
                                    int cx = loc.getX() >> 4;
                                    int cz = loc.getZ() >> 4;
                                    if (!w.isChunkLoaded(cx, cz)) {
                                        w.loadChunk(cx, cz);
                                    }
                                    // check to see if the block type has
                                    // changed, if so don't fade it
                                    if (w.getBlockTypeIdAt(loc.getX(), loc.getY(),
                                            loc.getZ()) == Movecraft.getInstance().blockFadeTypeMap.get(loc)) {
                                        // should it become water? if not, then
                                        // air
                                        if (Movecraft.getInstance().blockFadeWaterMap.get(loc)) {
                                            BlockCreateCommand updateCom = new BlockCreateCommand(w, loc, Material.STATIONARY_WATER, (byte) 0);
                                            updateCommands.add(updateCom);
                                        } else {
                                            BlockCreateCommand updateCom = new BlockCreateCommand(w, loc, Material.AIR, (byte) 0);
                                            updateCommands.add(updateCom);
                                        }
                                    }
                                    Movecraft.getInstance().blockFadeTimeMap.remove(loc);
                                    Movecraft.getInstance().blockFadeTypeMap.remove(loc);
                                    Movecraft.getInstance().blockFadeWorldMap.remove(loc);
                                    Movecraft.getInstance().blockFadeWaterMap.remove(loc);
                                }
                            }
                        }
                    }
                    if (updateCommands.size() > 0) {
                        MapUpdateManager.getInstance().scheduleUpdates(updateCommands);
                    }
                }
            }

            lastFadeCheck = System.currentTimeMillis();
        }
    }

    private void processDetection() {
        long ticksElapsed = (System.currentTimeMillis() - lastContactCheck) / 50;
        if (ticksElapsed > 21) {
            for (World w : Bukkit.getWorlds()) {
                if (w != null && CraftManager.getInstance().getCraftsInWorld(w) != null) {
                    for (Craft ccraft : CraftManager.getInstance().getCraftsInWorld(w)) {
                        if (CraftManager.getInstance().getPlayerFromCraft(ccraft) != null) {
                            if (!recentContactTracking.containsKey(ccraft)) {
                                recentContactTracking.put(ccraft, new HashMap<>());
                            }
                            for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(w)) {
                                long cposx = ccraft.getHitBox().getMaxX() + ccraft.getHitBox().getMinX();
                                long cposy = ccraft.getHitBox().getMaxY() + ccraft.getHitBox().getMinY();
                                long cposz = ccraft.getHitBox().getMaxZ() + ccraft.getHitBox().getMinZ();
                                cposx = cposx >> 1;
                                cposy = cposy >> 1;
                                cposz = cposz >> 1;
                                long tposx = tcraft.getHitBox().getMaxX() + tcraft.getHitBox().getMinX();
                                long tposy = tcraft.getHitBox().getMaxY() + tcraft.getHitBox().getMinY();
                                long tposz = tcraft.getHitBox().getMaxZ() + tcraft.getHitBox().getMinZ();
                                tposx = tposx >> 1;
                                tposy = tposy >> 1;
                                tposz = tposz >> 1;
                                long diffx = cposx - tposx;
                                long diffy = cposy - tposy;
                                long diffz = cposz - tposz;
                                long distsquared = Math.abs(diffx) * Math.abs(diffx);
                                distsquared += Math.abs(diffy) * Math.abs(diffy);
                                distsquared += Math.abs(diffz) * Math.abs(diffz);
                                long detectionRange;
                                if (tposy > 65) {
                                    detectionRange = (long) (Math.sqrt(tcraft.getOrigBlockCount())
                                            * tcraft.getType().getDetectionMultiplier());
                                } else {
                                    detectionRange = (long) (Math.sqrt(tcraft.getOrigBlockCount())
                                            * tcraft.getType().getUnderwaterDetectionMultiplier());
                                }
                                if (distsquared < detectionRange * detectionRange
                                        && tcraft.getNotificationPlayer() != ccraft.getNotificationPlayer()) {
                                    // craft has been detected

                                    // has the craft not been seen in the last
                                    // minute, or is completely new?
                                    if (recentContactTracking.get(ccraft).get(tcraft) == null
                                            || System.currentTimeMillis()
                                            - recentContactTracking.get(ccraft).get(tcraft) > 60000) {
                                        String notification = "New contact: ";
                                        notification += tcraft.getType().getCraftName();
                                        notification += " commanded by ";
                                        if (tcraft.getNotificationPlayer() != null) {
                                            notification += tcraft.getNotificationPlayer().getDisplayName();
                                        } else {
                                            notification += "NULL";
                                        }
                                        if (tcraft.getUniqueCraftName() != null) {
                                        	notification += "name: ";
                                        	notification += tcraft.getUniqueCraftName();
                                        }
                                        notification += ", size: ";
                                        notification += tcraft.getOrigBlockCount();
                                        notification += ", range: ";
                                        notification += (int) Math.sqrt(distsquared);
                                        notification += " to the";
                                        if (Math.abs(diffx) > Math.abs(diffz))
                                            if (diffx < 0)
                                                notification += " east.";
                                            else
                                                notification += " west.";
                                        else if (diffz < 0)
                                            notification += " south.";
                                        else
                                            notification += " north.";

                                        ccraft.getNotificationPlayer().sendMessage(notification);
                                        w.playSound(ccraft.getNotificationPlayer().getLocation(),
                                                Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
/*										final World sw = w;
                                        final Player sp = ccraft.getNotificationPlayer();
										BukkitTask replaysound = new BukkitRunnable() {
											@Override
											public void run() {
												sw.playSound(sp.getLocation(), Sound.BLOCK_ANVIL_LAND, 10.0f, 2.0f);
											}
										}.runTaskLater(Movecraft.getInstance(), (5));*/

                                    }

                                    long timestamp = System.currentTimeMillis();
                                    recentContactTracking.get(ccraft).put(tcraft, timestamp);
                                }
                            }
                        }
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
        processSinking();
        processTracers();
        processFireballs();
        processTNTContactExplosives();
        processFadingBlocks();
        processDetection();
        processAlgorithmQueue();
        //processScheduledBlockChanges();
//		if(Settings.CompatibilityMode==false)
//			FastBlockChanger.getInstance().run();

        // now cleanup craft that are bugged and have not moved in the past 60 seconds, but have no pilot or are still processing
        for (Craft pcraft : CraftManager.getInstance().getCraftList()) {
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
