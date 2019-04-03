package net.countercraft.movecraft.sign;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRepair;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.WorldEditUpdateCommand;
import net.countercraft.movecraft.repair.RepairUtils;
import net.countercraft.movecraft.utils.WorldguardUtils;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class RegionDamagedSign implements Listener {
    private String HEADER = ChatColor.RED + "REGION DAMAGED!";

    @EventHandler
    public void onSignRigtClick(PlayerInteractEvent event){
        Player p = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK){
            return;
        }
        if (!event.getClickedBlock().getType().equals(Material.WALL_SIGN)){
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!sign.getLine(0).equals(HEADER)){
            return;
        }
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
        if (!repairRegion(regionName, event.getClickedBlock().getWorld())) {
            Bukkit.getServer().broadcastMessage(String.format("REPAIR OF %s FAILED, CONTACT AN ADMIN", regionName));
            return;
        }
        RegionManager manager = WorldguardUtils.getRegionManager(event.getClickedBlock().getWorld());
        ProtectedRegion aRegion = manager.getRegion(regionName);
        for (String ownerName : owners) {
            if (ownerName.length() > 16) {
                aRegion.getOwners().addPlayer(UUID.fromString(ownerName));
            } else {

                if (Bukkit.getPlayer(ownerName) != null){//Cannot add names directly as bug will allow free assaults
                    aRegion.getOwners().addPlayer(Bukkit.getPlayer(ownerName).getUniqueId());
                } else {
                    aRegion.getOwners().addPlayer(Bukkit.getOfflinePlayer(ownerName).getUniqueId());
                }
                //aRegion.getOwners().addPlayer(ownerName);

            }
            //event.getPlayer().sendMessage(ownerName);
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
        return;
    }
    public static boolean repairRegion(String regionName, World world){
        MovecraftRepair movecraftRepair = Movecraft.getInstance().getMovecraftRepair();
        Clipboard clipboard = movecraftRepair.loadRegionRepairStateClipboard(Movecraft.getInstance(), regionName, world);
        if (clipboard == null){
            return false;
        }
        int minX = clipboard.getMinimumPoint().getBlockX();
        int minY = clipboard.getMinimumPoint().getBlockY();
        int minZ = clipboard.getMinimumPoint().getBlockZ();
        int maxX = clipboard.getMaximumPoint().getBlockX();
        int maxY = clipboard.getMaximumPoint().getBlockY();
        int maxZ = clipboard.getMaximumPoint().getBlockZ();

        for (int x = minX; x <= maxX; x++){
            for (int y = minY; y <= maxY; y++){
                for (int z = minZ; z <= maxZ; z++){
                    if (Settings.IsLegacy){
                        RepairUtils.legacyRepairRegion(clipboard, world,x,y,z);
                    } else {
                        BlockVector3 pos = BlockVector3.at(x,y,z);
                        MovecraftLocation location = new MovecraftLocation(x,y,z);
                        BaseBlock block = clipboard.getFullBlock(pos);
                        Location bukkitLoc = location.toBukkit(world);
                        if (bukkitLoc.getBlock().isLiquid() || bukkitLoc.getBlock().getType().name().endsWith("AIR"))
                            continue;
                        if (!Settings.AssaultDestroyableBlocks.contains(bukkitLoc.getBlock().getType()))
                            continue;
                        Material type = BukkitAdapter.adapt(block.getBlockType());
                        WorldEditUpdateCommand weUp = new WorldEditUpdateCommand(block, world, location, type);
                        MapUpdateManager.getInstance().scheduleUpdate(weUp);
                    }
                }
            }
        }
        return true;
    }
}
