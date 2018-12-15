package net.countercraft.movecraft.warfare.assault;

import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.listener.WorldEditInteractListener;
import net.countercraft.movecraft.utils.ChatUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.UUID;

public class AssaultTask extends BukkitRunnable {
    private final Assault assault;

    public AssaultTask(Assault assault) {
        this.assault = assault;
    }


    @Override
    public void run() {
        //in-case the server is lagging and a new assault task is started at the exact time on ends
        if (!assault.getRunning().get())
            return;

        //track assault progress in progress and damage bars
        assault.getDamageBar().setVisible(true);
        assault.getProgressBar().setVisible(true);
        double dmgProgress = (double) assault.getDamages() / (double) assault.getMaxDamages();
        double timeProgress = (double) ((System.currentTimeMillis() - assault.getStartTime()) / 1000)/ (double) Settings.AssaultDuration;
        //Bukkit.broadcastMessage(String.format("Damage: %d, Max damage: %d, Start time: %d, passed time: %d", assault.getDamages(), assault.getMaxDamages(), assault.getStartTime(), System.currentTimeMillis()));
        Location aLoc = assault.getWorld().getBlockAt(assault.getMinPos().getBlockX() + assault.getMaxPos().getBlockX() / 2, assault.getMinPos().getBlockY() + assault.getMaxPos().getBlockY() / 2, assault.getMinPos().getBlockZ() + assault.getMaxPos().getBlockZ() / 2).getLocation();
        for (Player p : Bukkit.getOnlinePlayers()){
            assault.getProgressBar().addPlayer(p);
            if (p.getLocation().getX() - aLoc.getX() <= 1500.0 && p.getLocation().getX() - aLoc.getX() >= -1500.0 &&
            p.getLocation().getZ() - aLoc.getZ() <= 1500.0 && p.getLocation().getZ() - aLoc.getZ() >= -1500.0){
                assault.getDamageBar().addPlayer(p);
            } else {
                assault.getDamageBar().removePlayer(p);
            }
        }

        if (assault.getDamages() >= assault.getMaxDamages() / 10 * 9){ //When 9/10 of Max damages is inflicted, set progress bar green
            assault.getProgressBar().setColor(BarColor.GREEN);
        } else if (assault.getDamages() >= assault.getMaxDamages() / 2){ //When half of the max damages is inflicted, set progress bar yellow
            assault.getProgressBar().setColor(BarColor.YELLOW);
        } else { //Otherwise, keep it red
            assault.getProgressBar().setColor(BarColor.RED);
        }

        if (dmgProgress >= 0.0 && dmgProgress <= 1.0){
            assault.getDamageBar().setProgress(dmgProgress);
        }
        if (timeProgress >= 0.0 && timeProgress <= 1.0){
            assault.getProgressBar().setProgress(timeProgress);
        }
        long timeLeft = Settings.AssaultDuration * 1000 - (System.currentTimeMillis() - assault.getStartTime());
        int hours = (int) (timeLeft / 1000) / 3600;
        int minutes = (int) (timeLeft / 1000) / 60 - hours * 60;
        int seconds = (int) (timeLeft / 1000) - minutes * 60;
        DecimalFormat df = new DecimalFormat("00");
        assault.getDamageBar().setTitle(String.format("%s damages: %d", assault.getRegionName(),assault.getDamages()));
        assault.getProgressBar().setTitle(assault.getRegionName() + " time left: " + df.format(hours) + ":" + df.format(minutes) + ":" + df.format(seconds));
        if (assault.getDamages() >= assault.getMaxDamages()) {
            assault.getDamageBar().setVisible(false);
            assault.getProgressBar().setVisible(false);
            // assault was successful
            assault.getRunning().set(false);
            World w = assault.getWorld();
            Bukkit.getServer().broadcastMessage(String.format("The assault of %s was successful!", assault.getRegionName()));
            ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(w).getRegion(assault.getRegionName());
            assert tRegion != null;
            tRegion.setFlag(DefaultFlag.TNT, StateFlag.State.DENY);

            //first, find a position for the repair beacon
            int beaconX = assault.getMinPos().getBlockX();
            int beaconZ = assault.getMinPos().getBlockZ();
            int beaconY = w.getHighestBlockAt(beaconX, beaconZ).getY();
            int x, y, z;
            for (x = beaconX; x < beaconX + 5; x++)
                for (z = beaconZ; z < beaconZ + 5; z++)
                    if (!w.isChunkLoaded(x >> 4, z >> z))
                        w.loadChunk(x >> 4, z >> 4);
            boolean empty = false;
            while (!empty && beaconY < 250) {
                empty = true;
                beaconY++;
                for (x = beaconX; x < beaconX + 5; x++) {
                    for (y = beaconY; y < beaconY + 4; y++) {
                        for (z = beaconZ; z < beaconZ + 5; z++) {
                            if (!w.getBlockAt(x, y, z).isEmpty())
                                empty = false;
                        }
                    }
                }
            }

            //now make the beacon
            y = beaconY;
            for (x = beaconX + 1; x < beaconX + 4; x++)
                for (z = beaconZ + 1; z < beaconZ + 4; z++)
                    w.getBlockAt(x, y, z).setType(Material.BEDROCK);
            y = beaconY + 1;
            for (x = beaconX; x < beaconX + 5; x++)
                for (z = beaconZ; z < beaconZ + 5; z++)
                    if (x == beaconX || z == beaconZ || x == beaconX + 4 || z == beaconZ + 4)
                        w.getBlockAt(x, y, z).setType(Material.BEDROCK);
                    else
                        w.getBlockAt(x, y, z).setType(Material.IRON_BLOCK);
            y = beaconY + 2;
            for (x = beaconX + 1; x < beaconX + 4; x++)
                for (z = beaconZ + 1; z < beaconZ + 4; z++)
                    w.getBlockAt(x, y, z).setType(Material.BEDROCK);
            w.getBlockAt(beaconX + 2, beaconY + 2, beaconZ + 2).setType(Material.BEACON);
            w.getBlockAt(beaconX + 2, beaconY + 3, beaconZ + 2).setType(Material.BEDROCK);
            // finally the sign on the beacon
            w.getBlockAt(beaconX + 2, beaconY + 3, beaconZ + 1).setType(Material.WALL_SIGN);
            Sign s = (Sign) w.getBlockAt(beaconX + 2, beaconY + 3, beaconZ + 1).getState();
            s.setLine(0, ChatColor.RED + "REGION DAMAGED!");
            s.setLine(1, "Region:" + assault.getRegionName());
            s.setLine(2, "Damage:" + assault.getDamages());
            s.setLine(3, "Owner:" + getRegionOwnerList(tRegion));
            s.update();
            tRegion.getOwners().clear();
        } else {
            // assault was not successful
            if (System.currentTimeMillis() - assault.getStartTime() > Settings.AssaultDuration * 1000) {
                assault.getDamageBar().setVisible(false);
                assault.getProgressBar().setVisible(false);
                // assault has failed to reach damage cap within required time
                assault.getRunning().set(false);
                Bukkit.getServer().broadcastMessage(String.format("The assault of %s has failed!", assault.getRegionName()));
                ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(assault.getWorld()).getRegion(assault.getRegionName());
                assert tRegion != null;
                tRegion.setFlag(DefaultFlag.TNT, StateFlag.State.DENY);
                // repair the damages that have occurred so far
                if (!new WorldEditInteractListener().repairRegion(assault.getWorld(), assault.getRegionName())) {
                    Bukkit.getServer().broadcastMessage(String.format("REPAIR OF %s FAILED, CONTACT AN ADMIN", assault.getRegionName().toUpperCase()));
                }
            }
        }


    }


    private static String getRegionOwnerList(ProtectedRegion tRegion) {
        StringBuilder output = new StringBuilder();
        if (tRegion == null)
            return "";
        boolean first = true;
        if (tRegion.getOwners().getUniqueIds().size() > 0) {
            for (UUID uid : tRegion.getOwners().getUniqueIds()) {
                if (!first)
                    output.append(",");
                else
                    first = false;
                OfflinePlayer offP = Bukkit.getOfflinePlayer(uid);
                if (offP.getName() == null)
                    output.append(uid.toString());
                else
                    output.append(offP.getName());
            }
        }
        if (tRegion.getOwners().getPlayers().size() > 0) {
            for (String player : tRegion.getOwners().getPlayers()) {
                if (!first)
                    output.append(",");
                else
                    first = false;
                output.append(player);
            }
        }
        return output.toString();
    }
}
