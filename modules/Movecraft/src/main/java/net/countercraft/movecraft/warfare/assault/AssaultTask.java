package net.countercraft.movecraft.warfare.assault;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.sign.RegionDamagedSign;
import net.countercraft.movecraft.utils.LegacyUtils;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

import static net.countercraft.movecraft.utils.ChatUtils.ERROR_PREFIX;

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
        if (assault.getDamages() >= assault.getMaxDamages()) {
            // assault was successful
            assault.getRunning().set(false);
            World w = assault.getWorld();
            Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Assault - Assault Successful"), assault.getRegionName()));
            ProtectedRegion tRegion;
            Flag flag;
            if (Settings.IsLegacy){
                tRegion = LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), w).getRegion(assault.getRegionName());
                flag = DefaultFlag.TNT;
            } else {
                tRegion = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w)).getRegion(assault.getRegionName());
                flag = Flags.TNT;
            }
            assert tRegion != null;
            tRegion.setFlag(flag, StateFlag.State.DENY);

            //first, find a position for the repair beacon
            int beaconX = assault.getMinPos().getBlockX();
            int beaconZ = assault.getMinPos().getBlockZ();
            int beaconY;
            for(beaconY = 255; beaconY > 0; beaconY--) {
                if(w.getBlockAt(beaconX, beaconY, beaconZ).getType().isOccluding()) {
                    beaconY++;
                    break;
                }
            }
            if(beaconY > 250) {
                Bukkit.getServer().broadcastMessage(ERROR_PREFIX + String.format(I18nSupport.getInternationalisedString("Assault - Beacon Placement Failed"), assault));
            }
            else {
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
                w.getBlockAt(beaconX + 2, beaconY + 3, beaconZ + 1).setType(Settings.is1_14 ? Material.OAK_WALL_SIGN : LegacyUtils.WALL_SIGN);
                Sign s = (Sign) w.getBlockAt(beaconX + 2, beaconY + 3, beaconZ + 1).getState();
                s.setLine(0, ChatColor.RED + I18nSupport.getInternationalisedString("Region Damaged"));
                s.setLine(1, I18nSupport.getInternationalisedString("Region Name")+":" + assault.getRegionName());
                s.setLine(2, I18nSupport.getInternationalisedString("Damages")+":" + assault.getMaxDamages());
                s.setLine(3, I18nSupport.getInternationalisedString("Region Owner")+":" + getRegionOwnerList(tRegion));
                s.update();
                tRegion.getOwners().clear();
            }
        } else {
            // assault was not successful
            if (System.currentTimeMillis() - assault.getStartTime() > Settings.AssaultDuration * 1000) {
                // assault has failed to reach damage cap within required time
                assault.getRunning().set(false);
                Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Assault - Assault Failed"), assault.getRegionName()));
                ProtectedRegion tRegion;
                if (Settings.IsLegacy){
                    tRegion = LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), assault.getWorld()).getRegion(assault.getRegionName());
                } else {
                    tRegion = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(assault.getWorld())).getRegion(assault.getRegionName());
                }
                assert tRegion != null;
                tRegion.setFlag(DefaultFlag.TNT, StateFlag.State.DENY);
                // repair the damages that have occurred so far
                if (!new RegionDamagedSign().repairRegion(assault.getWorld(), assault.getRegionName())) {
                    Bukkit.getServer().broadcastMessage(ERROR_PREFIX+String.format(I18nSupport.getInternationalisedString("Assault - Repair Failed"), assault.getRegionName().toUpperCase()));
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