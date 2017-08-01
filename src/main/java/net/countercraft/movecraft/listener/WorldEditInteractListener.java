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

package net.countercraft.movecraft.listener;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.DispenserBlock;
import com.sk89q.worldedit.blocks.SignBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateManager;
import net.countercraft.movecraft.utils.MovecraftLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

//import com.sk89q.worldedit.blocks.BaseBlock;
//import com.sk89q.worldedit.world.DataException;

public class WorldEditInteractListener implements Listener {
    private static final Map<Player, Long> timeMap = new HashMap<Player, Long>();
    private static final Map<Player, Long> repairRightClickTimeMap = new HashMap<Player, Long>();

    public boolean repairRegion(World w, String regionName) {
        if (w == null || regionName == null)
            return false;
        String repairStateName = Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/RegionRepairStates";
        repairStateName += "/";
        repairStateName += regionName.replaceAll("\\s+", "_");
        repairStateName += ".schematic";
        File file = new File(repairStateName);
        if (!file.exists()) {
            return false;
        }
        SchematicFormat sf = SchematicFormat.getFormat(file);
        CuboidClipboard cc;
        try {
            cc = sf.load(file);
        } catch (com.sk89q.worldedit.data.DataException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        ArrayList<MapUpdateCommand> updateCommands = new ArrayList<MapUpdateCommand>();
        int minx = cc.getOrigin().getBlockX();
        int miny = cc.getOrigin().getBlockY();
        int minz = cc.getOrigin().getBlockZ();
        int maxx = minx + cc.getWidth();
        int maxy = miny + cc.getHeight();
        int maxz = minz + cc.getLength();
        for (int x = minx; x < maxx; x++) {
            for (int y = miny; y < maxy; y++) {
                for (int z = minz; z < maxz; z++) {
                    Vector ccloc = new Vector(x - minx, y - miny, z - minz);
                    com.sk89q.worldedit.blocks.BaseBlock bb = cc.getBlock(ccloc);
                    if (!bb.isAir()) { // most blocks will be air, quickly move on to the next. This loop will run 16 million times, needs to be fast
                        if (Settings.AssaultDestroyableBlocks.contains(bb.getId())) {
                            if (w.getChunkAt(x >> 4, z >> 4).isLoaded() == false)
                                w.loadChunk(x >> 4, z >> 4);
                            if (w.getBlockAt(x, y, z).isEmpty() || w.getBlockAt(x, y, z).isLiquid()) {
                                MovecraftLocation moveloc = new MovecraftLocation(x, y, z);
                                MapUpdateCommand updateCom = new MapUpdateCommand(moveloc, bb.getType(), (byte) bb.getData(), bb, null);
                                updateCommands.add(updateCom);
                            }
                        }
                    }
                }
            }
        }
        if (!updateCommands.isEmpty()) {
            MapUpdateManager.getInstance().addWorldUpdate(w,
                    updateCommands.toArray(new MapUpdateCommand[1]), null, null);
        }
        return true;
    }

    @EventHandler
    public void WEOnPlayerInteract(PlayerInteractEvent event) {

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Material m = event.getClickedBlock().getType();
            if (m.equals(Material.SIGN_POST) || m.equals(Material.WALL_SIGN)) {
                Sign sign = (Sign) event.getClickedBlock().getState();
                String signText = org.bukkit.ChatColor.stripColor(sign.getLine(0));

                if (signText == null) {
                    return;
                }
            }
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Material m = event.getClickedBlock().getType();
            if (m.equals(Material.SIGN_POST) || m.equals(Material.WALL_SIGN)) {
                WEOnSignRightClick(event);
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Material m = event.getClickedBlock().getType();
            if (m.equals(Material.SIGN_POST) || m.equals(Material.WALL_SIGN)) {
                if (event.getClickedBlock() == null) {
                    return;
                }
                Sign sign = (Sign) event.getClickedBlock().getState();
                String signText = org.bukkit.ChatColor.stripColor(sign.getLine(0));

                if (signText == null) {
                    return;
                }

                if (org.bukkit.ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Repair:")) { // left click the Repair sign, and it saves the state
                    if (Settings.RepairTicksPerBlock == 0) {
                        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Repair functionality is disabled or WorldEdit was not detected"));
                        return;
                    }
                    Craft pCraft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
                    if (pCraft == null) {
                        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
                        return;
                    }

                    String repairStateName = Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/RepairStates";
                    File file = new File(repairStateName);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    repairStateName += "/";
                    repairStateName += event.getPlayer().getName();
                    repairStateName += sign.getLine(1).replaceAll("\\s+", "_");
                    repairStateName += ".schematic";
                    file = new File(repairStateName);

                    Vector size = new Vector(pCraft.getMaxX() - pCraft.getMinX(), (pCraft.getMaxY() - pCraft.getMinY()) + 1, pCraft.getMaxZ() - pCraft.getMinZ());
                    Vector origin = new Vector(sign.getX(), sign.getY(), sign.getZ());
                    Vector offset = new Vector(pCraft.getMinX() - sign.getX(), pCraft.getMinY() - sign.getY(), pCraft.getMinZ() - sign.getZ());
                    CuboidClipboard cc = new CuboidClipboard(size, origin, offset);
                    final int[] ignoredBlocks = new int[]{64, 140, 144, 176, 177};  // BLOCKS THAT CAN'T BE PARTIALLY RECONSTRUCTED

                    for (MovecraftLocation loc : pCraft.getBlockList()) {
                        Vector ccpos = new Vector(loc.getX() - pCraft.getMinX(), loc.getY() - pCraft.getMinY(), loc.getZ() - pCraft.getMinZ());
                        Block b = sign.getWorld().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
                        boolean isIgnored = (Arrays.binarySearch(ignoredBlocks, b.getTypeId()) >= 0);
                        if (!isIgnored) {
                            com.sk89q.worldedit.blocks.BaseBlock bb;
                            BlockState state = b.getState();
                            if (state instanceof Sign) {
                                Sign s = (Sign) state;
                                SignBlock sb = new SignBlock(b.getTypeId(), b.getData(), s.getLines());
                                bb = sb;
                            } else if (state instanceof Dispenser) {
                                LocalWorld lw = BukkitUtil.getLocalWorld(event.getPlayer().getWorld());
                                Vector vectorPos = new Vector(loc.getX(), loc.getY(), loc.getZ());
                                bb = lw.getBlock(vectorPos);
                                DispenserBlock dispBlock = new DispenserBlock(bb.getData());
                                BaseItemStack[] bItemStack = new BaseItemStack[9];
                                int numTNT = 0;
                                int numFireballs = 0;
                                int numWater = 0;
                                InventoryHolder inventoryHolder = (InventoryHolder) state;
                                if (inventoryHolder.getInventory().contains(46)) {
                                    HashMap<Integer, ? extends ItemStack> foundItems = inventoryHolder.getInventory().all(46);
                                    // count how many were in the chest
                                    for (ItemStack istack : foundItems.values()) {
                                        numTNT += istack.getAmount();
                                    }
                                }
                                if (inventoryHolder.getInventory().contains(385)) {
                                    HashMap<Integer, ? extends ItemStack> foundItems = inventoryHolder.getInventory().all(385);
                                    // count how many were in the chest
                                    for (ItemStack istack : foundItems.values()) {
                                        numFireballs += istack.getAmount();
                                    }
                                }
                                if (inventoryHolder.getInventory().contains(326)) {
                                    HashMap<Integer, ? extends ItemStack> foundItems = inventoryHolder.getInventory().all(326);
                                    // count how many were in the chest
                                    for (ItemStack istack : foundItems.values()) {
                                        numWater += istack.getAmount();
                                    }
                                }
                                int index = 0;
                                while (numTNT > 64) {
                                    bItemStack[index] = new BaseItemStack(46, 64);
                                    index++;
                                    numTNT -= 64;
                                }
                                if (numTNT > 0)
                                    bItemStack[index] = new BaseItemStack(46, numTNT);
                                while (numFireballs > 64) {
                                    bItemStack[index] = new BaseItemStack(385, 64);
                                    index++;
                                    numFireballs -= 64;
                                }
                                if (numFireballs > 0)
                                    bItemStack[index] = new BaseItemStack(385, numFireballs);
                                while (numWater > 0) {
                                    bItemStack[index] = new BaseItemStack(326, 1);
                                    index++;
                                    numWater--;
                                }
                                dispBlock.setItems(bItemStack);
                                bb = dispBlock;
                            } else {
                                bb = new com.sk89q.worldedit.blocks.BaseBlock(b.getTypeId(), b.getData());
                            }
                            cc.setBlock(ccpos, bb);
                        }
                    }
                    try {
                        cc.saveSchematic(file);
                    } catch (Exception e) {
                        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Could not save file"));
                        e.printStackTrace();
                        return;
                    }
                    event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("State saved"));
                    event.setCancelled(true);
                }
            }
        }
    }

    private void WEOnSignRightClick(PlayerInteractEvent event) {
        Sign sign = (Sign) event.getClickedBlock().getState();
        String signText = org.bukkit.ChatColor.stripColor(sign.getLine(0));

        if (signText == null) {
            return;
        }

        // don't process commands if this is a pilot tool click
        if (event.getItem() != null && event.getItem().getTypeId() == Settings.PilotTool) {
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (c != null)
                return;
        }


        if (org.bukkit.ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Repair:")) {
            if (Settings.RepairTicksPerBlock == 0) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Repair functionality is disabled or WorldEdit was not detected"));
                return;
            }
            Craft pCraft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (pCraft == null) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
                return;
            }
            if (!event.getPlayer().hasPermission("movecraft." + pCraft.getType().getCraftName() + ".repair")) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return;
            }
            // load up the repair state

            String repairStateName = Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/RepairStates";
            repairStateName += "/";
            repairStateName += event.getPlayer().getName();
            repairStateName += sign.getLine(1).replaceAll("\\s+", "_");
            File file = new File(repairStateName);
            if (!file.exists()) {
                repairStateName += ".schematic";
                file = new File(repairStateName);
                if (!file.exists()) {
                    repairStateName = Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/RepairStates";
                    repairStateName += "/";
                    repairStateName += event.getPlayer().getName();
                    repairStateName += sign.getLine(1).replaceAll("_", " ");
                    file = new File(repairStateName);
                    if (!file.exists()) {
                        repairStateName += ".schematic";
                        file = new File(repairStateName);
                        if (!file.exists()) {
                            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("REPAIR STATE NOT FOUND"));
                            return;
                        } else {
                            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("RESAVE YOUR REPAIR STATE"));
                        }
                    } else {
                        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("RESAVE YOUR REPAIR STATE"));
                    }
                } else {
                    event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("RESAVE YOUR REPAIR STATE"));
                }
            }
            SchematicFormat sf = SchematicFormat.getFormat(file);
            CuboidClipboard cc;
            try {
                cc = sf.load(file);
            } catch (com.sk89q.worldedit.data.DataException e) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("REPAIR STATE NOT FOUND"));
                e.printStackTrace();
                return;
            } catch (IOException e) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("REPAIR STATE NOT FOUND"));
                e.printStackTrace();
                return;
            }

            // calculate how many and where the blocks need to be replaced
            Location worldLoc = new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ());
            int numdiffblocks = 0;
            HashMap<Integer, Integer> numMissingItems = new HashMap<Integer, Integer>(); // block type, number missing
            HashSet<Vector> locMissingBlocks = new HashSet<Vector>();
            for (int x = 0; x < cc.getWidth(); x++) {
                for (int y = 0; y < cc.getHeight(); y++) {
                    for (int z = 0; z < cc.getLength(); z++) {
                        Vector ccLoc = new Vector(x, y, z);
                        worldLoc.setX(sign.getX() + cc.getOffset().getBlockX() + x);
                        worldLoc.setY(sign.getY() + cc.getOffset().getBlockY() + y);
                        worldLoc.setZ(sign.getZ() + cc.getOffset().getBlockZ() + z);
                        Boolean isImportant = true;
                        if (!pCraft.getType().blockedByWater())
                            if (cc.getBlock(ccLoc).getId() == 8 || cc.getBlock(ccLoc).getId() == 9)
                                isImportant = false;
                        if (cc.getBlock(ccLoc).getId() == 0)
                            isImportant = false;
                        if (isImportant && worldLoc.getWorld().getBlockAt(worldLoc).getTypeId() != cc.getBlock(ccLoc).getId()) {
                            numdiffblocks++;
                            int itemToConsume = cc.getBlock(ccLoc).getId();
                            int qtyToConsume = 1;
                            //some blocks aren't represented by items with the same number as the block
                            if (itemToConsume == 63 || itemToConsume == 68) // signs
                                itemToConsume = 323;
                            if (itemToConsume == 93 || itemToConsume == 94) // repeaters
                                itemToConsume = 356;
                            if (itemToConsume == 149 || itemToConsume == 150) // comparators
                                itemToConsume = 404;
                            if (itemToConsume == 55) // redstone
                                itemToConsume = 331;
                            if (itemToConsume == 118) // cauldron
                                itemToConsume = 380;
                            if (itemToConsume == 124) // lit redstone lamp
                                itemToConsume = 123;
                            if (itemToConsume == 75) // lit redstone torch
                                itemToConsume = 76;
                            if (itemToConsume == 8 || itemToConsume == 9) { // don't require water to be in the chest
                                itemToConsume = 0;
                                qtyToConsume = 0;
                            }
                            if (itemToConsume == 10 || itemToConsume == 11) { // don't require lava either, yeah you could exploit this for free lava, so make sure you set a price per block
                                itemToConsume = 0;
                                qtyToConsume = 0;
                            }
                            if (itemToConsume == 26 || itemToConsume == 34 || itemToConsume == 71 || itemToConsume == 193 || itemToConsume == 194 || itemToConsume == 195 || itemToConsume == 196 || itemToConsume == 197) { // don't require doors, beds
                                itemToConsume = 0;
                                qtyToConsume = 0;
                            }
                            if (itemToConsume == 23) { // for dispensers, count the fireballs and tnt
                                BaseBlock bb = cc.getBlock(ccLoc);
                                DispenserBlock dispBlock = new DispenserBlock(bb.getData());
                                dispBlock.setNbtData(bb.getNbtData());
                                int numFireCharges = 0;
                                int numTNT = 0;
                                int numWater = 0;
                                for (BaseItemStack bi : dispBlock.getItems()) {
                                    if (bi != null) {
                                        if (bi.getType() == 46)
                                            numTNT += bi.getAmount();
                                        if (bi.getType() == 385)
                                            numFireCharges += bi.getAmount();
                                        if (bi.getType() == 326)
                                            numWater += bi.getAmount();
                                    }
                                }
                                if (numFireCharges > 0) {
                                    if (!numMissingItems.containsKey(385)) {
                                        numMissingItems.put(385, numFireCharges);
                                    } else {
                                        Integer num = numMissingItems.get(385);
                                        num += numFireCharges;
                                        numMissingItems.put(385, num);
                                    }
                                }
                                if (numTNT > 0) {
                                    if (!numMissingItems.containsKey(46)) {
                                        numMissingItems.put(46, numTNT);
                                    } else {
                                        Integer num = numMissingItems.get(46);
                                        num += numTNT;
                                        numMissingItems.put(46, num);
                                    }
                                }
                                if (numWater > 0) {
                                    if (!numMissingItems.containsKey(326)) {
                                        numMissingItems.put(326, numWater);
                                    } else {
                                        Integer num = numMissingItems.get(326);
                                        num += numWater;
                                        numMissingItems.put(46, num);
                                    }
                                }
                            }
                            if (itemToConsume == 43) { // for double slabs, require 2 slabs
                                itemToConsume = 44;
                                qtyToConsume = 2;
                            }
                            if (itemToConsume == 125) { // for double wood slabs, require 2 wood slabs
                                itemToConsume = 126;
                                qtyToConsume = 2;
                            }
                            if (itemToConsume == 181) { // for double red sandstone slabs, require 2 red sandstone slabs
                                itemToConsume = 182;
                                qtyToConsume = 2;
                            }

                            if (itemToConsume != 0) {
                                if (!numMissingItems.containsKey(itemToConsume)) {
                                    numMissingItems.put(itemToConsume, qtyToConsume);
                                } else {
                                    Integer num = numMissingItems.get(itemToConsume);
                                    num += qtyToConsume;
                                    numMissingItems.put(itemToConsume, num);
                                }
                            }
                            locMissingBlocks.add(ccLoc);
                        }
                        if ((worldLoc.getWorld().getBlockAt(worldLoc).getTypeId() == 23) && (cc.getBlock(ccLoc).getId() == 23)) { // check to see if ammo is missing from intact dispensers
                            BaseBlock bb = cc.getBlock(ccLoc);
                            DispenserBlock dispBlock = new DispenserBlock(bb.getData());
                            dispBlock.setNbtData(bb.getNbtData());
                            int numFireCharges = 0;
                            int numTNT = 0;
                            int numWater = 0;
                            boolean needsReplace = false;
                            for (BaseItemStack bi : dispBlock.getItems()) {
                                if (bi != null) {
                                    if (bi.getType() == 46)
                                        numTNT += bi.getAmount();
                                    if (bi.getType() == 385)
                                        numFireCharges += bi.getAmount();
                                    if (bi.getType() == 326)
                                        numWater += bi.getAmount();
                                }
                            }
                            Dispenser disp = (Dispenser) worldLoc.getWorld().getBlockAt(worldLoc).getState();
                            for (ItemStack di : disp.getInventory().getContents()) {
                                if (di != null) {
                                    if (di.getTypeId() == 46)
                                        numTNT -= di.getAmount();
                                    if (di.getTypeId() == 385)
                                        numFireCharges -= di.getAmount();
                                    if (di.getTypeId() == 326)
                                        numWater -= di.getAmount();
                                }
                            }
                            if (numFireCharges > 0) {
                                if (!numMissingItems.containsKey(385)) {
                                    numMissingItems.put(385, numFireCharges);
                                } else {
                                    Integer num = numMissingItems.get(385);
                                    num += numFireCharges;
                                    numMissingItems.put(385, num);
                                }
                                needsReplace = true;
                            }
                            if (numTNT > 0) {
                                if (!numMissingItems.containsKey(46)) {
                                    numMissingItems.put(46, numTNT);
                                } else {
                                    Integer num = numMissingItems.get(46);
                                    num += numTNT;
                                    numMissingItems.put(46, num);
                                }
                                needsReplace = true;
                            }
                            if (numWater > 0) {
                                if (!numMissingItems.containsKey(326)) {
                                    numMissingItems.put(326, numWater);
                                } else {
                                    Integer num = numMissingItems.get(326);
                                    num += numWater;
                                    numMissingItems.put(46, num);
                                }
                                needsReplace = true;
                            }
                            if (needsReplace) {
                                numdiffblocks++;
                                locMissingBlocks.add(ccLoc);
                            }
                        }
                    }
                }
            }

            // if this is the second click in the last 5 seconds, start the repair, otherwise give them the info on the repair
            Boolean secondClick = false;
            Long time = repairRightClickTimeMap.get(event.getPlayer());
            if (time != null) {
                long ticksElapsed = (System.currentTimeMillis() - time) / 50;
                if (ticksElapsed < 100) {
                    secondClick = true;
                }
            }
            if (secondClick) {
                // check all the chests for materials for the repair
                HashMap<Integer, ArrayList<InventoryHolder>> chestsToTakeFrom = new HashMap<Integer, ArrayList<InventoryHolder>>(); // typeid, list of chest inventories
                boolean enoughMaterial = true;
                for (Integer typeID : numMissingItems.keySet()) {
                    int remainingQty = numMissingItems.get(typeID);
                    ArrayList<InventoryHolder> chests = new ArrayList<InventoryHolder>();
                    for (MovecraftLocation loc : pCraft.getBlockList()) {
                        Block b = pCraft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
                        if ((b.getTypeId() == 54) || (b.getTypeId() == 146)) {
                            InventoryHolder inventoryHolder = (InventoryHolder) b.getState();
                            if (inventoryHolder.getInventory().contains(typeID) && remainingQty > 0) {
                                HashMap<Integer, ? extends ItemStack> foundItems = inventoryHolder.getInventory().all(typeID);
                                // count how many were in the chest
                                int numfound = 0;
                                for (ItemStack istack : foundItems.values()) {
                                    numfound += istack.getAmount();
                                }
                                remainingQty -= numfound;
                                chests.add(inventoryHolder);
                            }
                        }
                    }
                    if (remainingQty > 0) {
                        event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Need more of material") + ": %s - %d", Material.getMaterial(typeID).name().toLowerCase().replace("_", " "), remainingQty));
                        enoughMaterial = false;
                    } else {
                        chestsToTakeFrom.put(typeID, chests);
                    }
                }
                if (Movecraft.getInstance().getEconomy() != null && enoughMaterial) {
                    double moneyCost = numdiffblocks * Settings.RepairMoneyPerBlock;
                    if (Movecraft.getInstance().getEconomy().has(event.getPlayer(), moneyCost)) {
                        Movecraft.getInstance().getEconomy().withdrawPlayer(event.getPlayer(), moneyCost);
                    } else {
                        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("You do not have enough money"));
                        enoughMaterial = false;
                    }
                }
                if (enoughMaterial) {
                    // we know we have enough materials to make the repairs, so remove the materials from the chests
                    for (Integer typeID : numMissingItems.keySet()) {
                        int remainingQty = numMissingItems.get(typeID);
                        for (InventoryHolder inventoryHolder : chestsToTakeFrom.get(typeID)) {
                            HashMap<Integer, ? extends ItemStack> foundItems = inventoryHolder.getInventory().all(typeID);
                            for (ItemStack istack : foundItems.values()) {
                                if (istack.getAmount() <= remainingQty) {
                                    remainingQty -= istack.getAmount();
                                    inventoryHolder.getInventory().removeItem(istack);
                                } else {
                                    istack.setAmount(istack.getAmount() - remainingQty);
                                    remainingQty = 0;
                                }
                            }

                        }
                    }
                    double Cost = numdiffblocks * Settings.RepairMoneyPerBlock;
                    Bukkit.getLogger().info(event.getPlayer().toString() + " has begun a repair with the cost of " + String.valueOf(Cost));
                    ArrayList<MapUpdateCommand> updateCommands = new ArrayList<MapUpdateCommand>();
                    for (Vector ccloc : locMissingBlocks) {
                        com.sk89q.worldedit.blocks.BaseBlock bb = cc.getBlock(ccloc);
                        if (bb.getId() == 68 || bb.getId() == 63) { // I don't know why this is necessary. I'm pretty sure WE should be loading signs as signblocks, but it doesn't seem to
                            SignBlock sb = new SignBlock(bb.getId(), bb.getData());
                            sb.setNbtData(bb.getNbtData());
                            String[] textLines = sb.getText();
                            for (int i = 0; i < textLines.length; i++) {
                                String str = textLines[i];
                                if (str.length() >= 11)
                                    str = str.substring(9);
                                str = str.replaceAll("\"", "");
                                str = str.replace("\\\\", "\\");
                                str = str.replaceAll("\\}", "");
                                textLines[i] = str;
                            }
                            if (textLines[0].equals("\\  ||  /")) {
                                textLines[1] = "==      ==";
                                textLines[2] = "/  ||  \\";
                                textLines[3] = "";
                            }
                            sb.setText(textLines);
                            bb = sb;
                        }
                        MovecraftLocation moveloc = new MovecraftLocation(sign.getX() + cc.getOffset().getBlockX() + ccloc.getBlockX(), sign.getY() + cc.getOffset().getBlockY() + ccloc.getBlockY(), sign.getZ() + cc.getOffset().getBlockZ() + ccloc.getBlockZ());
                        MapUpdateCommand updateCom = new MapUpdateCommand(moveloc, bb.getType(), (byte) bb.getData(), bb, pCraft);
                        updateCommands.add(updateCom);
                    }
                    if (updateCommands.size() > 0) {
                        final Craft fpCraft = pCraft;
                        final MapUpdateCommand[] fUpdateCommands = updateCommands.toArray(new MapUpdateCommand[1]);
                        int durationInTicks = numdiffblocks * Settings.RepairTicksPerBlock;

                        // send out status updates every minute
                        for (int ticsFromStart = 0; ticsFromStart < durationInTicks; ticsFromStart += 1200) {
                            final Player fp = event.getPlayer();
                            final int fTics = ticsFromStart / 20;
                            final int fDur = durationInTicks / 20;
                            BukkitTask statusTask = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    fp.sendMessage(String.format(I18nSupport.getInternationalisedString("Repairs underway") + ": %d / %d", fTics, fDur));
                                }
                            }.runTaskLater(Movecraft.getInstance(), (ticsFromStart));

                        }

                        // keep craft piloted during the repair process so player can not move it
                        CraftManager.getInstance().removePlayerFromCraft(pCraft);
                        final Craft releaseCraft = pCraft;
                        final Player fp = event.getPlayer();
                        BukkitTask releaseTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                CraftManager.getInstance().removeCraft(releaseCraft);
                                fp.sendMessage(I18nSupport.getInternationalisedString("Repairs complete. You may now pilot the craft"));
                            }
                        }.runTaskLater(Movecraft.getInstance(), (durationInTicks + 20));

                        //do the actual repair
                        BukkitTask repairTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                MapUpdateManager.getInstance().addWorldUpdate(fpCraft.getW(), fUpdateCommands, null, null);
                            }
                        }.runTaskLater(Movecraft.getInstance(), (durationInTicks));

                    }
                }

            } else {
                // if this is the first time they have clicked the sign, show the summary of repair costs and requirements
                event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Total damaged blocks") + ": %d", numdiffblocks));
                float percent = (numdiffblocks * 100) / pCraft.getOrigBlockCount();
                event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Percentage of craft") + ": %.2f%%", percent));
                if (percent > 50) {
                    event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("This craft is too damaged and can not be repaired"));
                    return;
                }
                if (numdiffblocks != 0) {
                    event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("SUPPLIES NEEDED"));
                    for (Integer blockTypeInteger : numMissingItems.keySet()) {
                        event.getPlayer().sendMessage(String.format("%s : %d", Material.getMaterial(blockTypeInteger).name().toLowerCase().replace("_", " "), numMissingItems.get(blockTypeInteger)));
                    }
                    int durationInSeconds = numdiffblocks * Settings.RepairTicksPerBlock / 20;
                    event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Seconds to complete repair") + ": %d", durationInSeconds));
                    int moneyCost = (int) (numdiffblocks * Settings.RepairMoneyPerBlock);
                    event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Money to complete repair") + ": %d", moneyCost));
                    repairRightClickTimeMap.put(event.getPlayer(), System.currentTimeMillis());
                }
            }
        }
        if (sign.getLine(0).equalsIgnoreCase(ChatColor.RED + "REGION DAMAGED!")) {
            String regionName = sign.getLine(1).substring(7);
            Long damages = Long.parseLong(sign.getLine(2).substring(7));
            String[] owners = sign.getLine(3).substring(6).split(",");
            if (Movecraft.getInstance().getEconomy().has(event.getPlayer(), damages)) {
                Movecraft.getInstance().getEconomy().withdrawPlayer(event.getPlayer(), damages);
            } else {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("You do not have enough money"));
                return;
            }
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Repairing region"));
            if (repairRegion(event.getClickedBlock().getWorld(), regionName) == false) {
                Bukkit.getServer().broadcastMessage(String.format("REPAIR OF %s FAILED, CONTACT AN ADMIN", regionName));
            }
            ProtectedRegion aRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(event.getClickedBlock().getWorld()).getRegion(regionName);
            for (String ownerName : owners) {
                if (ownerName.length() > 16) {
                    aRegion.getOwners().addPlayer(UUID.fromString(ownerName));
                } else {
                    aRegion.getOwners().addPlayer(ownerName);
                }
            }
            int beaconX = sign.getX() - 2;
            int beaconY = sign.getY() - 3;
            int beaconZ = sign.getZ() - 1;
            for (int x = beaconX; x < beaconX + 5; x++) {
                for (int y = beaconY; y < beaconY + 4; y++) {
                    for (int z = beaconZ; z < beaconZ + 5; z++) {
                        event.getClickedBlock().getWorld().getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }

        }
    }

}
