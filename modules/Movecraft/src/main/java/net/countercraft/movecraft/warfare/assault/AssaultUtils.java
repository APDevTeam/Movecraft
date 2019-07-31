package net.countercraft.movecraft.warfare.assault;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.WorldEdit7UpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.WorldEditUpdateCommand;
import net.countercraft.movecraft.utils.LegacyUtils;
import net.countercraft.movecraft.utils.LegacyWEUtils;
import net.countercraft.movecraft.utils.WorldguardUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AssaultUtils {
    private static long lastDamagesUpdate = 0;
    final static Material[] fragileBlocks = Settings.IsLegacy ? new Material[]{LegacyUtils.BED_BLOCK, LegacyUtils.PISTON_EXTENSION, Material.TORCH, Material.REDSTONE_WIRE, LegacyUtils.SIGN_POST, LegacyUtils.WOOD_DOOR, Material.LADDER, Material.getMaterial("WALL_SIGN"),
            Material.BIRCH_SIGN,Material.OAK_SIGN,Material.DARK_OAK_SIGN,Material.JUNGLE_SIGN,Material.SPRUCE_SIGN,Material.ACACIA_SIGN,Material.BIRCH_WALL_SIGN
            ,Material.OAK_WALL_SIGN,Material.DARK_OAK_WALL_SIGN, Material.JUNGLE_WALL_SIGN, Material.SPRUCE_WALL_SIGN, Material.ACACIA_WALL_SIGN, Material.LEVER, LegacyUtils.STONE_PLATE, LegacyUtils.IRON_DOOR_BLOCK, LegacyUtils.WOOD_PLATE, LegacyUtils.REDSTONE_TORCH_OFF, LegacyUtils.REDSTONE_TORCH_ON, Material.STONE_BUTTON, LegacyUtils.DIODE_BLOCK_OFF, LegacyUtils.DIODE_BLOCK_ON, LegacyUtils.TRAP_DOOR, Material.TRIPWIRE_HOOK,
            Material.TRIPWIRE, LegacyUtils.WOOD_BUTTON, LegacyUtils.GOLD_PLATE, LegacyUtils.IRON_PLATE, LegacyUtils.REDSTONE_COMPARATOR_OFF, LegacyUtils.REDSTONE_COMPARATOR_ON, Material.DAYLIGHT_DETECTOR, LegacyUtils.CARPET, LegacyUtils.DAYLIGHT_DETECTOR_INVERTED, Material.SPRUCE_DOOR, Material.BIRCH_DOOR, Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR}
            :
            new Material[]
                    //Beds
                    {Material.CYAN_BED, Material.BLACK_BED, Material.BLUE_BED,
                            Material.BROWN_BED, Material.GRAY_BED, Material.GREEN_BED, Material.LIGHT_BLUE_BED, Material.LIGHT_GRAY_BED, Material.LIME_BED, Material.MAGENTA_BED,
                            Material.ORANGE_BED, Material.PINK_BED, Material.PURPLE_BED, Material.RED_BED, Material.WHITE_BED, Material.YELLOW_BED,
                            //Redstone components
                            Material.REDSTONE_WIRE, Material.REPEATER, Material.COMPARATOR, Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH, Material.TRIPWIRE_HOOK,
                            //Pressure plates
                            Material.STONE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE, Material.DARK_OAK_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.JUNGLE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE,
                            //Buttons
                            Material.STONE_BUTTON, Material.BIRCH_BUTTON, Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON, Material.JUNGLE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON,
                            //Doors
                            Material.SPRUCE_DOOR, Material.BIRCH_DOOR, Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR, Material.OAK_DOOR, Material.IRON_DOOR,
                            //Trapdoors
                            Material.IRON_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.BIRCH_TRAPDOOR};
    public static boolean areDefendersOnline(ProtectedRegion tRegion) {
        int numOnline = 0;

        if(Settings.AssaultRequiredOwnersOnline > 0) {
            for (UUID playerID : tRegion.getOwners().getUniqueIds()) {
                if (Bukkit.getPlayer(playerID) != null) {
                    numOnline++;

                    if(numOnline > Settings.AssaultRequiredOwnersOnline) {
                        break;
                    }
                }
            }

            if (numOnline < Settings.AssaultRequiredOwnersOnline) {
                return false;
            }
        }

        numOnline = 0;
        if(Settings.AssaultRequiredOwnersOnline > 0) {
            for (UUID playerID : tRegion.getMembers().getUniqueIds()) {
                if (Bukkit.getPlayer(playerID) != null) {
                    numOnline++;

                    if(numOnline > Settings.AssaultRequiredDefendersOnline) {
                        return true;
                    }
                }
            }

            if (numOnline < Settings.AssaultRequiredDefendersOnline) {
                return false;
            }
        }
        return numOnline >= Settings.AssaultRequiredDefendersOnline;
    }

    public static double getCostToAssault(ProtectedRegion tRegion) {
        return getAssaultBalance(tRegion) * Settings.AssaultCostPercent;
    }

    public static double getMaxDamages(ProtectedRegion tRegion) {

        return getAssaultBalance(tRegion) * Settings.AssaultDamagesCapPercent;
    }

    private static double getAssaultBalance(ProtectedRegion tRegion) {
        return getOwnerBalance(tRegion) + getMemberBalance(tRegion);
    }

    private static double getOwnerBalance(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getOwners().getUniqueIds());
        double total = 0.0;
        for (UUID playerID : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerID);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > Settings.AssaultMaxBalance)
                    total += Settings.AssaultMaxBalance;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * (Settings.AssaultOwnerWeightPercent / 100.0);
    }

    private static double getMemberBalance(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getMembers().getUniqueIds());
        double total = 0.0;
        for (UUID playerID : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerID);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > Settings.AssaultMaxBalance)
                    total += Settings.AssaultMaxBalance;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * (Settings.AssaultMemberWeightPercent / 100.0);
    }

    public static boolean repairRegion(World world, String regionName) {
        Clipboard clipboard = Movecraft.getInstance().getMovecraftRepair().loadRegionRepairStateClipboard(regionName,world);
        if (clipboard == null){
            return false;
        }
        int minX = clipboard.getMinimumPoint().getX();
        int minY = clipboard.getMinimumPoint().getY();
        int minZ = clipboard.getMinimumPoint().getZ();
        int maxX = clipboard.getMaximumPoint().getX();
        int maxY = clipboard.getMaximumPoint().getY();
        int maxZ = clipboard.getMaximumPoint().getZ();
        LinkedList<UpdateCommand> updateCommands = new LinkedList<>();
        for (int x = minX; x <= maxX ; x++){
            for (int y = minY; y <= maxY; y++){
                for (int z = minZ ; z <= maxZ ; z++){
                    Material type;
                    if (Settings.IsLegacy){
                        Vector position = new Vector(x,y,z);
                        com.sk89q.worldedit.blocks.BaseBlock bb = LegacyWEUtils.getBlock(clipboard,position);
                        type = LegacyUtils.getMaterial(bb.getType());
                        byte data = (byte) bb.getData();
                        if (type.name().endsWith("AIR")) {
                            continue;
                        }
                        Block b = world.getBlockAt(x, y, z);
                        if (b.getType() != type){
                            updateCommands.add(new WorldEditUpdateCommand(bb,world,new MovecraftLocation(x,y,z),type,data));
                        }
                    } else {
                        BlockVector3 position = BlockVector3.at(x, y, z);
                        BaseBlock bb = clipboard.getFullBlock(position);
                        type = BukkitAdapter.adapt(bb.getBlockType());
                        BlockData bdata = BukkitAdapter.adapt(bb);
                        if (type.name().endsWith("AIR")) {
                            continue;
                        }
                        Block b = world.getBlockAt(x, y, z);
                        if (b.getType() != type){
                            updateCommands.add(new WorldEdit7UpdateCommand(bb,world,new MovecraftLocation(x,y,z),type));
                        }
                    }
                }
            }
        }
        if (!updateCommands.isEmpty()){
            MapUpdateManager.getInstance().scheduleUpdates(updateCommands);
        }
        return true;
    }

    public static void processAssault(EntityExplodeEvent e){
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
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.SOUTH).getType()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.DOWN).getType()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.UP).getType()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.EAST).getType()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.WEST).getType()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.NORTH).getType()) >= 0 ) {
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
}
