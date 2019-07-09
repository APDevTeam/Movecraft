package net.countercraft.movecraft.repair;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RepairManager extends BukkitRunnable {
    private List<Repair> repairs = new CopyOnWriteArrayList<>();

    public RepairManager(){

    }
    @Override
    public void run() {
        for (Repair repair : repairs){
            if (repair.getRunning().get()) {
                new RepairTask(repair).runTask(Movecraft.getInstance());
            }
        }
    }

    public int convertOldCraftRepairStates(){
        //Check in the old RepairStates folder
        File repairStateDir = new File(Movecraft.getInstance().getDataFolder(),"RepairStates");
        if (!repairStateDir.exists()){
            return 0;
        }
        int convertedRepairStates = 0;
        File[] repairStates = repairStateDir.listFiles();
        if (repairStateDir.exists() && (repairStates != null || repairStates.length > 0)){
            for (File rs : repairStates){
                if (!rs.getName().contains(".schematic")){
                    continue;
                }
                String fileName = rs.getName();
                fileName = fileName.replace(".schematic", "");
                OfflinePlayer owner = null;

                for (OfflinePlayer op : Bukkit.getOfflinePlayers()){
                    if (fileName.startsWith(op.getName())){
                        owner = op;
                        fileName = fileName.replace(op.getName(),"");
                        break;
                    }
                }
                if (owner == null){
                    Movecraft.getInstance().getLogger().severe(String.format(I18nSupport.getInternationalisedString("Repair - Invalid Player Name"), fileName));
                    continue;
                }
                File playerDir = new File(repairStateDir, owner.getUniqueId().toString());
                if (!playerDir.exists()) {
                    playerDir.mkdirs();
                }
                File dest = new File(playerDir, fileName + ".schematic");
                if (rs.renameTo(dest)){
                    convertedRepairStates++;
                }

            }
        }
        return convertedRepairStates;
    }

    public List<Repair> getRepairs() {
        return repairs;
    }
}
