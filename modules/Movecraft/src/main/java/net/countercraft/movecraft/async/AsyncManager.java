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
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.detection.DetectionTaskData;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.BlockingQueue;
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
    private HashMap<SmallFireball, Long> FireballTracking = new HashMap<>();
    private HashMap<HitBox, Long> wrecks = new HashMap<>();
    private HashMap<HitBox, World> wreckWorlds = new HashMap<>();
    private HashMap<HitBox, Map<MovecraftLocation,Material>> wreckPhases = new HashMap<>();
    private long lastTracerUpdate = 0;
    private long lastFireballCheck = 0;
    private long lastTNTContactCheck = 0;
    private long lastFadeCheck = 0;
    private long lastContactCheck = 0;
    private HashSet<Material> transparent = null;

    public AsyncManager() {
        transparent = new HashSet<>();
        //These materials exists in both 1.12 and 1.13 APIs
        transparent.add(Material.AIR);
        transparent.add(Material.GLASS);
        transparent.add(Material.REDSTONE_WIRE);
        transparent.add(Material.IRON_TRAPDOOR);
        transparent.add(Material.NETHER_BRICK_STAIRS);
        transparent.add(Material.LEVER);
        transparent.add(Material.STONE_BUTTON);

        if (Settings.IsLegacy){
            transparent.add(LegacyUtils.THIN_GLASS);
            transparent.add(LegacyUtils.STAINED_GLASS);
            transparent.add(LegacyUtils.STAINED_GLASS_PANE);
            transparent.add(LegacyUtils.IRON_FENCE);
            transparent.add(LegacyUtils.TRAP_DOOR);
            transparent.add(LegacyUtils.WOOD_BUTTON);
            transparent.add(LegacyUtils.STEP);
            transparent.add(LegacyUtils.SMOOTH_STAIRS);
            transparent.add(LegacyUtils.SIGN_POST);
        } else {
            if (Settings.is1_14){
                transparent.add(Material.BIRCH_SIGN);
                transparent.add(Material.OAK_SIGN);
                transparent.add(Material.DARK_OAK_SIGN);
                transparent.add(Material.JUNGLE_SIGN);
                transparent.add(Material.SPRUCE_SIGN);
                transparent.add(Material.ACACIA_SIGN);
                transparent.add(Material.BIRCH_WALL_SIGN);
                transparent.add(Material.OAK_WALL_SIGN);
                transparent.add(Material.DARK_OAK_WALL_SIGN);
                transparent.add(Material.JUNGLE_WALL_SIGN);
                transparent.add(Material.SPRUCE_WALL_SIGN);
                transparent.add(Material.ACACIA_WALL_SIGN);
            } else {
                transparent.add(Material.getMaterial("SIGN"));
                transparent.add(Material.getMaterial("WALL_SIGN"));
            }
            transparent.add(Material.GLASS_PANE);
            transparent.add(Material.BLACK_STAINED_GLASS_PANE);
            transparent.add(Material.WHITE_STAINED_GLASS_PANE);
            transparent.add(Material.GRAY_STAINED_GLASS_PANE);
            transparent.add(Material.GREEN_STAINED_GLASS_PANE);
        }
    }
    public void addWreck(Craft craft){
        if(craft.getCollapsedHitBox().isEmpty() || Settings.FadeWrecksAfter == 0){
            return;
        }
        wrecks.put(craft.getCollapsedHitBox(), System.currentTimeMillis());
        wreckWorlds.put(craft.getCollapsedHitBox(), craft.getW());
        wreckPhases.put(craft.getCollapsedHitBox(), craft.getPhaseBlocks());
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
                            		I18nSupport.getInternationalisedString("Detection - NULL Player Detection Failed") + ": " + data.getFailMessage());

                    } else {
                        Set<Craft> craftsInWorld = CraftManager.getInstance().getCraftsInWorld(c.getW());
                        boolean failed = false;
                        boolean isSubcraft = false;

                        for (Craft craft : craftsInWorld) {
                            if(craft.getHitBox().intersects(data.getBlockList())){
                                isSubcraft = true;
                                if (c.getType().getCruiseOnPilot() || p != null) {
                                    if (craft.getType() == c.getType()
                                            || craft.getHitBox().size() <= data.getBlockList().size()) {
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
                                        craft.setHitBox(new HashHitBox(CollectionUtils.filter(craft.getHitBox(),data.getBlockList())));
                                        craft.setOrigBlockCount(craft.getOrigBlockCount() - data.getBlockList().size());
                                    }
                                }
                            }


                        }
                        if (c.getType().getMustBeSubcraft() && !isSubcraft) {
                            failed = true;
                            notifyP.sendMessage(I18nSupport.getInternationalisedString("Craft must be part of another craft"));
                        }
                        if (!failed) {
                            c.setHitBox(task.getData().getBlockList());
                            c.setOrigBlockCount(data.getBlockList().size());
                            c.setNotificationPlayer(notifyP);
                            final int waterLine = c.getWaterLine();
                            if(!c.getType().blockedByWater() && c.getHitBox().getMinY() <= waterLine){
                                for(MovecraftLocation location : c.getHitBox().boundingHitBox()){
                                    if(location.getY() <= waterLine){
                                        c.getPhaseBlocks().put(location, Material.WATER);
                                    }
                                }

                            }

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
                if(c!=null){
                    Bukkit.getPluginManager().callEvent(new CraftDetectEvent(c));
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

                    if (task.isCollisionExplosion()) {
                        c.setHitBox(task.getNewHitBox());
                        //c.setBlockList(task.getData().getBlockList());
                        //boolean failed = MapUpdateManager.getInstance().addWorldUpdate(c.getW(), updates, null, null, exUpdates);
                        MapUpdateManager.getInstance().scheduleUpdates(task.getUpdates());
                        sentMapUpdate = true;
                        CraftManager.getInstance().addReleaseTask(c);

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
                        shipCannons = Movecraft.getInstance().getCannonsPlugin().getCannonsAPI()
                                .getCannons(shipLocations, c.getNotificationPlayer().getUniqueId(), true);
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
                            		I18nSupport.getInternationalisedString("Rotation - NULL Player Rotation Failed")+ ": " + task.getFailMessage());
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
                                    .getCannons(shipLocations, c.getNotificationPlayer().getUniqueId(), true);
                        }

                        MapUpdateManager.getInstance().scheduleUpdates(task.getUpdates());


                        sentMapUpdate = true;
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
            boolean climb = pcraft.isClimbing();
            if (pcraft.getPilotLocked()) {
                if (pcraft.getNotificationPlayer().isSneaking())
                    dive = true;
                if (pcraft.getNotificationPlayer().getInventory().getHeldItemSlot() == 3)
                    bankLeft = true;
                if (pcraft.getNotificationPlayer().getInventory().getHeldItemSlot() == 5)
                    bankRight = true;

            }

            if (Math.abs(ticksElapsed) < pcraft.getTickCooldown()) {
                continue;
            }
            int dx = 0;
            int dz = 0;
            int dy = 0;

            // ascend
            if (pcraft.getCruiseDirection() == BlockFace.UP) {
                dy = 1 + pcraft.getType().getVertCruiseSkipBlocks();
            }
            else if (climb){
                dy = (1 + pcraft.getType().getCruiseSkipBlocks()) >> 1;
            }
            // descend
            if (pcraft.getCruiseDirection() == BlockFace.DOWN) {
                dy = 0 - 1 - pcraft.getType().getVertCruiseSkipBlocks();
                if (pcraft.getHitBox().getMinY() <= w.getSeaLevel()) {
                    dy = -1;
                }
            } else if (dive) {
                dy = 0 - ((pcraft.getType().getCruiseSkipBlocks() + 1) >> 1);
                if (pcraft.getHitBox().getMinY() <= w.getSeaLevel()) {
                    dy = -1;
                }
            }
            // ship faces west
            if (pcraft.getCruiseDirection() == BlockFace.WEST) {
                dx = 0 - 1 - pcraft.getType().getCruiseSkipBlocks();
                if (bankRight) {
                    dz = (0 - 1 - pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
                if (bankLeft) {
                    dz = (1 + pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }

            }
            // ship faces east
            if (pcraft.getCruiseDirection() == BlockFace.EAST) {
                dx = 1 + pcraft.getType().getCruiseSkipBlocks();
                if (bankLeft) {
                    dz = (0 - 1 - pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
                if (bankRight) {
                    dz = (1 + pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
            }
            // ship faces north
            if (pcraft.getCruiseDirection() == BlockFace.NORTH) {
                dz = 0 - 1 - pcraft.getType().getCruiseSkipBlocks();
                if (bankRight) {
                    dx = (0 - 1 - pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
                if (bankLeft) {
                    dx = (1 + pcraft.getType().getCruiseSkipBlocks()) >> 1;
                }
            }
            // ship faces south
            if (pcraft.getCruiseDirection() == BlockFace.SOUTH) {
                dz = 1 + pcraft.getType().getCruiseSkipBlocks();
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
            HashMap<Map<Material, List<Integer>>, Integer> foundFlyBlocks = new HashMap<>();
            HashMap<Map<Material, List<Integer>>, Integer> foundMoveBlocks = new HashMap<>();
            // go through each block in the blocklist, and
            // if its in the FlyBlocks, total up the number
            // of them
            for (MovecraftLocation l : pcraft.getHitBox()) {
                Material blockType = w.getBlockAt(l.getX(), l.getY(), l.getZ()).getType();
                byte dataID = w.getBlockAt(l.getX(), l.getY(), l.getZ()).getData();
                for (Map<Material, List<Integer>> flyBlockDef : pcraft.getType().getFlyBlocks().keySet()) {
                    if ((flyBlockDef.containsKey(blockType) && flyBlockDef.get(blockType).isEmpty())|| (flyBlockDef.containsKey(blockType) && flyBlockDef.get(blockType).contains(dataID))) {
                        foundFlyBlocks.merge(flyBlockDef, 1, (a, b) -> a + b);
                    }
                }
                for (Map<Material, List<Integer>> moveBlockDef : pcraft.getType().getMoveBlocks().keySet()) {
                    if ((moveBlockDef.containsKey(blockType) && moveBlockDef.get(blockType).isEmpty())|| (moveBlockDef.containsKey(blockType) && moveBlockDef.get(blockType).contains(dataID))) {
                        foundMoveBlocks.merge(moveBlockDef, 1, (a, b) -> a + b);
                    }
                }

                if (blockType != Material.AIR) {
                    totalNonAirBlocks++;
                }
                if (blockType != Material.AIR && blockType != Material.WATER && blockType != LegacyUtils.STATIONARY_WATER) {
                    totalNonAirWaterBlocks++;
                }
            }

            // now see if any of the resulting percentages
            // are below the threshold specified in
            // SinkPercent
            boolean isSinking = false;

            for (Map<Material, List<Integer>> i : pcraft.getType().getFlyBlocks().keySet()) {
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
            for (Map<Material, List<Integer>> i : pcraft.getType().getMoveBlocks().keySet()) {
                int numfound = 0;
                if (foundMoveBlocks.get(i) != null) {
                    numfound = foundMoveBlocks.get(i);
                }
                double percent = ((double) numfound / (double) totalNonAirBlocks) * 100.0;
                double movePercent = pcraft.getType().getMoveBlocks().get(i).get(0);
                double disablePercent = movePercent * pcraft.getType().getSinkPercent() / 100.0;
                if (percent < disablePercent && !pcraft.getDisabled() && pcraft.isNotProcessing()) {
                    Bukkit.broadcastMessage(String.valueOf(percent));
                    pcraft.setDisabled(true);
                    if (pcraft.getNotificationPlayer() != null) {
                        Location loc = pcraft.getNotificationPlayer().getLocation();
                        pcraft.getW().playSound(loc, Settings.IsLegacy ? LegacyUtils.ENITIY_IRONGOLEM_DEATH : Sound.ENTITY_IRON_GOLEM_DEATH , 5.0f, 5.0f);
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
            Player notifyP = pcraft.getNotificationPlayer();
            if (Movecraft.getInstance().getWorldGuardPlugin() != null){
                WorldGuardPlugin wgPlugin = Movecraft.getInstance().getWorldGuardPlugin();
                ProtectedRegion region = null;
                RegionManager regionManager;
                if (Settings.IsLegacy){
                    regionManager = LegacyUtils.getRegionManager(wgPlugin, pcraft.getW());
                } else {
                    RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                    regionManager = container.get(BukkitAdapter.adapt(pcraft.getW()));
                }
                for (MovecraftLocation location : pcraft.getHitBox()){
                    ApplicableRegionSet regions;
                    if (Settings.IsLegacy)
                        regions = LegacyUtils.getApplicableRegions(regionManager, location.toBukkit(pcraft.getW()));
                    else {
                        regions = regionManager.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
                    }
                    for (ProtectedRegion pr : regions.getRegions()){
                        if (WorldguardUtils.pvpAllowed(pr)){
                            region = pr;
                            break;
                        }
                    }
                    if (region != null){
                        break;
                    }
                }
                Flag pvpFlag = Settings.IsLegacy ? DefaultFlag.PVP : Flags.PVP;
                if (region != null && Settings.WorldGuardBlockSinkOnPVPPerm && region.getFlag(pvpFlag).equals(StateFlag.State.DENY)){
                    notifyP.sendMessage(I18nSupport.getInternationalisedString("Player- Craft should sink but PVP is not allowed in this WorldGuard region"));
                    isSinking = false;
                }
            }
            if (isSinking && pcraft.isNotProcessing()) {

                if (notifyP != null) {
                    notifyP.sendMessage(I18nSupport.getInternationalisedString("Player- Craft is sinking"));
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
                CraftManager.getInstance().removeCraft(craft);
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
                                            fp.sendBlockChange(loc, Settings.IsLegacy ? LegacyUtils.WEB :Material.COBWEB, (byte) 0);
                                        }
                                    }.runTaskLater(Movecraft.getInstance(), 5);
                                    // then remove it
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            // fp.sendBlockChange(loc,
                                            // fw.getBlockAt(loc).getType(),
                                            // fw.getBlockAt(loc).getData());
                                            fp.sendBlockChange(loc, Material.AIR, (byte) 0);
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
                                            if (p.getItemInHand().getType() == Settings.PilotTool) {
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

    private void processWaterlogging(){
        for (Craft c : CraftManager.getInstance()){

        }
    }

    private Craft fastNearestCraftToLoc(Location loc) {
        Craft ret = null;
        long closestDistSquared = 1000000000L;
        Set<Craft> craftsList = CraftManager.getInstance().getCraftsInWorld(loc.getWorld());
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
        return ret;
    }

    private void processTNTContactExplosives() {
        long ticksElapsed = (System.currentTimeMillis() - lastTNTContactCheck) / 50;

        if (ticksElapsed <= 0) {
            return;
        }
        // see if there is any new rapid moving TNT in the worlds
        for (World w : Bukkit.getWorlds()) {
            if (w == null) {
                continue;
            }
            for (TNTPrimed tnt : w.getEntitiesByClass(TNTPrimed.class)) {
                if (!(tnt.getVelocity().lengthSquared() > 0.35) || TNTTracking.containsKey(tnt)) {
                    continue;
                }
                Craft c = fastNearestCraftToLoc(tnt.getLocation());
                TNTTracking.put(tnt, tnt.getVelocity().lengthSquared());
                if (c == null) {
                    continue;
                }
                MovecraftLocation midpoint = c.getHitBox().getMidPoint();
                int distX  = Math.abs(midpoint.getX() - tnt.getLocation().getBlockX());
                int distY= Math.abs(midpoint.getY() - tnt.getLocation().getBlockY());
                int distZ = Math.abs(midpoint.getZ() - tnt.getLocation().getBlockZ());
                if (c.getCannonDirector() == null || distX >= 100 || distY >= 100 || distZ >= 100) {
                    continue;
                }
                Player p = c.getCannonDirector();
                if (p.getInventory().getItemInMainHand().getType() != Settings.PilotTool) {
                    continue;
                }
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
    private void processFadingBlocks() {
        if (Settings.FadeWrecksAfter == 0)
            return;
        long ticksElapsed = (System.currentTimeMillis() - lastFadeCheck) / 50;
        if (ticksElapsed <= 20) {
            return;
        }
        List<HitBox> processed = new ArrayList<>();
        final WorldHandler handler = Movecraft.getInstance().getWorldHandler();
        for(Map.Entry<HitBox, Long> entry : wrecks.entrySet()){
            if (entry.getValue() + Settings.FadeWrecksAfter <= System.currentTimeMillis()) {
                continue;
            }
            final HitBox hitBox = entry.getKey();
            final Map<MovecraftLocation, Material> phaseBlocks = wreckPhases.get(hitBox);
            final World world = wreckWorlds.get(hitBox);
            for (MovecraftLocation location : hitBox){
                handler.setBlockFast(location.toBukkit(world), phaseBlocks.getOrDefault(location, Material.AIR),(byte) 0);
            }
            processed.add(hitBox);
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
                if (w != null) {
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
                                        String notification = I18nSupport.getInternationalisedString("New Contact") + ": ";

                                        if (tcraft.getName() != null && tcraft.getName().length() >= 1){
                                            notification += tcraft.getName();
                                            notification += " (";
                                        }
                                        notification += tcraft.getType().getCraftName();
                                        if (tcraft.getName() != null && tcraft.getName().length() >= 1){
                                            notification += ")";
                                        }
                                        notification += " " + I18nSupport.getInternationalisedString("Commanded By")+" ";
                                        if (tcraft.getNotificationPlayer() != null) {
                                            notification += tcraft.getNotificationPlayer().getDisplayName();
                                        } else {
                                            notification += "NULL";
                                        }
                                        notification += ", " + I18nSupport.getInternationalisedString("Size") + ": ";
                                        notification += tcraft.getOrigBlockCount();
                                        notification += ", " + I18nSupport.getInternationalisedString("Range") + ": ";
                                        notification += (int) Math.sqrt(distsquared);
                                        notification += " " + I18nSupport.getInternationalisedString("To The") + " ";
                                        if (Math.abs(diffx) > Math.abs(diffz))
                                            if (diffx < 0)
                                                notification += I18nSupport.getInternationalisedString("east");
                                            else
                                                notification += I18nSupport.getInternationalisedString("west");
                                        else if (diffz < 0)
                                            notification += I18nSupport.getInternationalisedString("south");
                                        else
                                            notification += I18nSupport.getInternationalisedString("north");
                                        
                                        notification += ".";
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

    private void processGravity(){
        boolean onGround = false;
        for (Craft pCraft : CraftManager.getInstance()){
            if (!pCraft.getType().getUseGravity()){
                continue;
            }
            int minX = pCraft.getHitBox().getMinX() - 1;
            int maxX = pCraft.getHitBox().getMaxX() + 1;
            int minY = pCraft.getHitBox().getMinY() - 1;
            int minZ = pCraft.getHitBox().getMinX() - 1;
            int maxZ = pCraft.getHitBox().getMaxX() + 1;

            //Check if there are any solid blocks below the craft
            for (int x = minX ; x <= maxX ; x++){
                for (int z = minZ ; z <= maxZ ; z++){
                    if (pCraft.getW().getBlockAt(x,minY,z).getType().isSolid()){
                        onGround = true;
                        break;
                    }
                }
                if (onGround)
                    break;
            }
            if (onGround){
                return;
            }
            pCraft.translate(0,-1,0);

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
        processTracers();
        processFireballs();
        processTNTContactExplosives();
        if (!Settings.IsLegacy) {
            processWaterlogging();
        }
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
