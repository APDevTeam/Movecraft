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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.LegacyUtils;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.WorldguardUtils;
import net.countercraft.movecraft.warfare.assault.Assault;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BlockListener implements Listener {


    private long lastDamagesUpdate;
    final Material[] fragileBlocks = Settings.IsLegacy ? new Material[]{

    }
            :
            new Material[]
                    //Beds
                    {
                            //Redstone components


                            };

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        if (!Settings.RestrictSiBsToRegions ||
                e.getBlockPlaced().getType() != Material.CHEST ||
                !e.getItemInHand().hasItemMeta() ||
                !e.getItemInHand().getItemMeta().hasLore()) {
            return;
        }
        List<String> loreList = e.getItemInHand().getItemMeta().getLore();
        for (String lore : loreList) {
            if (!lore.contains("SiB")) {
                continue;
            }
            if (lore.toLowerCase().contains("merchant") || lore.toLowerCase().contains("mm")) {
                return;
            }
            Location loc = e.getBlockPlaced().getLocation();
            ApplicableRegionSet regions;
            if (Settings.IsLegacy){
                regions = LegacyUtils.getApplicableRegions(LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), loc.getWorld()), loc);//.getApplicableRegions();
            } else {
                regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld())).getApplicableRegions(BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            }
            if (regions.size() == 0) {
                e.getPlayer().sendMessage(I18nSupport.getInternationalisedString("SIB MUST BE PLACED IN REGION"));
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getBlock().getType().name().endsWith("WALL_SIGN")) {
            Sign s = (Sign) e.getBlock().getState();
            if (s.getLine(0).equalsIgnoreCase(ChatColor.RED + "REGION DAMAGED!")) {
                e.setCancelled(true);
                return;
            }
        }
        if (Settings.ProtectPilotedCrafts) {
            MovecraftLocation mloc = MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
            CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld());
            for (Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())) {
                if (craft == null || craft.getDisabled()) {
                    continue;
                }
                for (MovecraftLocation tloc : craft.getHitBox()) {
                    if (tloc.equals(mloc)) {
                        e.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Player - Block part of piloted craft"));
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    // prevent items from dropping from moving crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(final ItemSpawnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(e.getLocation().getWorld())) {
            if ((!tcraft.isNotProcessing()) && MathUtils.locationInHitBox(tcraft.getHitBox(), e.getLocation())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // prevent water and lava from spreading on moving crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Block block = e.getToBlock();
        if (block.getType() != Material.WATER && block.getType() != Material.LAVA) {
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            if ((!tcraft.isNotProcessing()) && MathUtils.locIsNearCraftFast(tcraft, MathUtils.bukkit2MovecraftLoc(block.getLocation()))) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // process certain redstone on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstoneEvent(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        CraftManager.getInstance().getCraftsInWorld(block.getWorld());
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (Settings.IsLegacy) {
                if (MathUtils.locIsNearCraftFast(tcraft, mloc) &&
                        tcraft.getCruising() && (block.getType() == LegacyUtils.PISTON_STICKY_BASE ||
                        block.getType() == LegacyUtils.PISTON_BASE || block.getType() == Material.DISPENSER &&
                        !tcraft.isNotProcessing())) {
                    event.setNewCurrent(event.getOldCurrent()); // don't allow piston movement on cruising crafts
                    return;
                }
            } else {
                if (MathUtils.locIsNearCraftFast(tcraft, mloc) &&
                        tcraft.getCruising() && (block.getType() == Material.STICKY_PISTON ||
                        block.getType() == Material.PISTON || block.getType() == Material.DISPENSER &&
                        !tcraft.isNotProcessing())) {
                    event.setNewCurrent(event.getOldCurrent()); // don't allow piston movement on cruising crafts
                    return;
                }
            }
        }
    }

    public void onPistonRetract(BlockPistonRetractEvent event){

    }
    // prevent pistons on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonEvent(BlockPistonExtendEvent event) {
        Block block = event.getBlock();

        CraftManager.getInstance().getCraftsInWorld(block.getWorld());
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (MathUtils.locIsNearCraftFast(tcraft, mloc) && tcraft.getCruising() && !tcraft.isNotProcessing()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // prevent hoppers on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHopperEvent(InventoryMoveItemEvent event) {
        if (!(event.getSource().getHolder() instanceof Hopper)) {
            return;
        }
        Hopper block = (Hopper) event.getSource().getHolder();
        CraftManager.getInstance().getCraftsInWorld(block.getWorld());
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (MathUtils.locIsNearCraftFast(tcraft, mloc) && tcraft.getCruising() && !tcraft.isNotProcessing()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // prevent fragile items from dropping on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPhysics(BlockPhysicsEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (!MathUtils.locIsNearCraftFast(tcraft, mloc)) {
                continue;
            }

            if (isFragileBlock(block)) {
                BlockFace face = BlockFace.DOWN;
                boolean faceAlwaysDown = false;
                if (Settings.IsLegacy) {
                    MaterialData m = block.getState().getData();
                    if (block.getType() == LegacyUtils.REDSTONE_COMPARATOR_ON || block.getType() == LegacyUtils.REDSTONE_COMPARATOR_OFF || block.getType() == LegacyUtils.DIODE_BLOCK_ON|| block.getType() == LegacyUtils.DIODE_BLOCK_OFF)
                        faceAlwaysDown = true;
                    if (m instanceof Attachable && !faceAlwaysDown) {
                        face = ((Attachable) m).getAttachedFace();
                    }
                } else {
                    BlockData data = block.getBlockData();
                    if (block.getType() == Material.REPEATER || block.getType() == Material.COMPARATOR)
                        faceAlwaysDown = true;
                    if (data instanceof Directional && !faceAlwaysDown)
                        face = ((Directional) data).getFacing().getOppositeFace();
                }
                if (!event.getBlock().getRelative(face).getType().isSolid()) {
//						if(event.getEventName().equals("BlockPhysicsEvent")) {
                    event.setCancelled(true);
                    return;
                }
            }

        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final Craft adjacentCraft = adjacentCraft(event.getBlock().getLocation());
        // replace blocks with fire occasionally, to prevent fast craft from simply ignoring fire
        if (Settings.FireballPenetration && event.getCause() == BlockIgniteEvent.IgniteCause.FIREBALL) {
            Block testBlock = event.getBlock().getRelative(-1, 0, 0);
            if (!testBlock.getType().isBurnable())
                testBlock = event.getBlock().getRelative(1, 0, 0);
            if (!testBlock.getType().isBurnable())
                testBlock = event.getBlock().getRelative(0, 0, -1);
            if (!testBlock.getType().isBurnable())
                testBlock = event.getBlock().getRelative(0, 0, 1);
            if (!testBlock.getType().isBurnable()) {
                return;
            }
            // check to see if fire spread is allowed, don't check if worldguard integration is not enabled
            if (Movecraft.getInstance().getWorldGuardPlugin() != null && (Settings.WorldGuardBlockMoveOnBuildPerm || Settings.WorldGuardBlockSinkOnPVPPerm) && !WorldguardUtils.allowFireSpread(testBlock.getLocation())) {
                    return;

            }
            testBlock.setType(org.bukkit.Material.AIR);
        } else if (adjacentCraft != null) {

            adjacentCraft.getHitBox().add(MathUtils.bukkit2MovecraftLoc(event.getBlock().getLocation()));
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispense(BlockDispenseEvent e) {
        CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld());
        for (Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())) {
            if (craft != null &&
                    !craft.isNotProcessing() &&
                    MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation()))) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onFlow(BlockFromToEvent e){
        if(Settings.DisableSpillProtection)
            return;
        if(!e.getBlock().isLiquid())
            return;
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
        MovecraftLocation toLoc = MathUtils.bukkit2MovecraftLoc(e.getToBlock().getLocation());
        for(Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())){
            if(craft.getHitBox().contains((loc)) && !craft.getFluidLocations().contains(toLoc)) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void explodeEvent(EntityExplodeEvent e) {
        if (Settings.DurabilityOverride != null) {
            e.blockList().removeIf(b -> Settings.DurabilityOverride.containsKey(b.getType()) &&
                    (new Random(b.getX() + b.getY() + b.getZ() + (System.currentTimeMillis() >> 12)))
                            .nextInt(100) < Settings.DurabilityOverride.get(b.getType()));
        }
        if (Settings.AssaultEnable) {
            processAssault(e);
        }
        if (e.getEntity() == null)
            return;
        for (Player p : e.getEntity().getWorld().getPlayers()) {
            Entity tnt = e.getEntity();

            if (e.getEntityType() == EntityType.PRIMED_TNT && Settings.TracerRateTicks != 0) {
                long minDistSquared = 60 * 60;
                long maxDistSquared = Bukkit.getServer().getViewDistance() * 16;
                maxDistSquared = maxDistSquared - 16;
                maxDistSquared = maxDistSquared * maxDistSquared;
                // is the TNT within the view distance (rendered world) of the player, yet further than 60 blocks?
                if (p.getLocation().distanceSquared(tnt.getLocation()) < maxDistSquared && p.getLocation().distanceSquared(tnt.getLocation()) >= minDistSquared) {  // we use squared because its faster
                    final Location loc = tnt.getLocation();
                    final Player fp = p;
                    final World fw = e.getEntity().getWorld();
                    // then make a glowstone to look like the explosion, place it a little later so it isn't right in the middle of the volley
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            fp.sendBlockChange(loc, Material.GLOWSTONE, (byte) 0);
                        }
                    }.runTaskLater(Movecraft.getInstance(), 5);
                    // then remove it
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            fp.sendBlockChange(loc, Material.AIR, (byte) 0);
                        }
                    }.runTaskLater(Movecraft.getInstance(), 160);
                }
            }
        }

        for (Block b : e.blockList()){
            if (b.getState() instanceof Sign){
                Sign sign = (Sign) b.getState();
                if (sign.getLine(0).equals(ChatColor.RED + "REGION DAMAGED!")){

                }
            }
        }
    }

    private void processAssault(EntityExplodeEvent e){
        List<Assault> assaults = Movecraft.getInstance().getAssaultManager() != null ? Movecraft.getInstance().getAssaultManager().getAssaults() : null;
        if (assaults == null || assaults.size() == 0) {
            return;
        }
        WorldGuardPlugin worldGuard = Movecraft.getInstance().getWorldGuardPlugin();
        for (final Assault assault : assaults) {
            Iterator<Block> i = e.blockList().iterator();
            while (i.hasNext()) {
                Block b = i.next();
                if (b.getWorld() != assault.getWorld())
                    continue;
                ApplicableRegionSet regions = WorldguardUtils.getRegionsAt(b.getLocation());
                boolean isInAssaultRegion = false;
                for (com.sk89q.worldguard.protection.regions.ProtectedRegion tregion : regions.getRegions()) {
                    if (assault.getRegionName().equals(tregion.getId())) {
                        isInAssaultRegion = true;
                    }
                }
                if (!isInAssaultRegion)
                    continue;
                // first see if it is outside the destroyable area
                org.bukkit.util.Vector min = assault.getMinPos();
                org.bukkit.util.Vector max = assault.getMaxPos();

                if (b.getLocation().getBlockX() < min.getBlockX() ||
                        b.getLocation().getBlockX() > max.getBlockX() ||
                        b.getLocation().getBlockZ() < min.getBlockZ() ||
                        b.getLocation().getBlockZ() > max.getBlockZ() ||
                        !Settings.AssaultDestroyableBlocks.contains(b.getType()) ||
                        isFragileBlock(b.getRelative(BlockFace.SOUTH)) ||
                        isFragileBlock(b.getRelative(BlockFace.DOWN)) ||
                        isFragileBlock(b.getRelative(BlockFace.UP)) ||
                        isFragileBlock(b.getRelative(BlockFace.EAST)) ||
                        isFragileBlock(b.getRelative(BlockFace.WEST)) ||
                        isFragileBlock(b.getRelative(BlockFace.NORTH)) ) {
                    i.remove();
                }


                // whether or not you actually destroyed the block, add to damages
                long damages = assault.getDamages() + Settings.AssaultDamagesPerBlock;
                if (damages < assault.getMaxDamages()) {
                    assault.setDamages(damages);
                } else {
                    assault.setDamages(assault.getMaxDamages());
                }

                // notify nearby players of the damages, do this 1 second later so all damages from this volley will be included
                if (System.currentTimeMillis() < lastDamagesUpdate + 4000) {
                    continue;
                }
                final Location floc = b.getLocation();
                final World fworld = b.getWorld();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        long fdamages = assault.getDamages();
                        for (Player p : fworld.getPlayers()) {
                            if (Math.round(p.getLocation().getBlockX() / 1000.0) == Math.round(floc.getBlockX() / 1000.0) &&
                                    Math.round(p.getLocation().getBlockZ() / 1000.0) == Math.round(floc.getBlockZ() / 1000.0)) {
                                p.sendMessage("Damages: " + fdamages);

                            }
                        }
                    }
                }.runTaskLater(Movecraft.getInstance(), 20);
                lastDamagesUpdate = System.currentTimeMillis();

            }
        }

    }

    @Nullable
    private Craft adjacentCraft(@NotNull Location location) {
        for (Craft craft : CraftManager.getInstance().getCraftsInWorld(location.getWorld())) {
            if (!MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(location))) {
                continue;
            }
            return craft;
        }
        return null;
    }

    private boolean pistonFacingLocation(Location loc) {
        final Vector[] SHIFTS = {new Vector(0,1,0), new Vector(0,-1,0),
                new Vector(1,0,0), new Vector(-1,0,0),
                new Vector(0,0,1), new Vector(0,0,-1)};
        for (Vector shift : SHIFTS) {
            final Location test = loc.add(shift);
            if (!(test.getBlock().getState().getData() instanceof PistonBaseMaterial)) {
                continue;
            }
            PistonBaseMaterial piston = (PistonBaseMaterial) test.getBlock().getState().getData();
            if (!test.getBlock().getRelative(piston.getFacing()).getLocation().equals(loc)) {
                continue;
            }
            return true;
        }
        return false;
    }
    private boolean isFragileBlock(Block block) {
        Material type = block.getType();
        BlockState state = block.getState();
        return type.name().endsWith("_BED") ||
                state instanceof Sign ||
                type.name().endsWith("DOOR") ||
                type.name().endsWith("BUTTON") ||
                type.name().endsWith("_PLATE") ||
                type == Material.REDSTONE_WIRE ||
                type.name().endsWith("TORCH") ||
                type == Material.TRIPWIRE ||
                type == Material.TRIPWIRE_HOOK ||
                type == Material.LADDER ||
                type == Material.LEVER ||
                type == Material.DAYLIGHT_DETECTOR ||
                (Settings.IsLegacy ?
                        (type == LegacyUtils.BED_BLOCK||
                        type == LegacyUtils.PISTON_EXTENSION||
                        type == LegacyUtils.SIGN_POST||
                        type == LegacyUtils.WOOD_DOOR||
                        type == LegacyUtils.WALL_SIGN||
                        type == LegacyUtils.IRON_DOOR_BLOCK||
                        type == LegacyUtils.REDSTONE_TORCH_OFF||
                        type == LegacyUtils.REDSTONE_TORCH_ON||
                        type == LegacyUtils.DIODE_BLOCK_OFF||
                        type == LegacyUtils.DIODE_BLOCK_ON||
                        type == LegacyUtils.TRAP_DOOR ||
                        type == LegacyUtils.REDSTONE_COMPARATOR_OFF||
                        type == LegacyUtils.REDSTONE_COMPARATOR_ON ||
                        type == LegacyUtils.CARPET||
                        type == LegacyUtils.DAYLIGHT_DETECTOR_INVERTED)
        :
                        (type == Material.REPEATER ||
                        type == Material.COMPARATOR )
        );

    }
}
