package net.countercraft.movecraft.repair;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RepairTask extends BukkitRunnable {
    private final Repair repair;

    public RepairTask(Repair repair){
        this.repair = repair;
    }
    @Override
    public void run() {
        long ticksFromStart = repair.getTicksSinceStart();
        Player p = Bukkit.getPlayer(repair.getPlayerUUID());
        if (p != null) {
            repair.getProgressBar().addPlayer(p);
        }
        long secsFromStart = ticksFromStart / 20;
        long durationInSecs = repair.getDurationInTicks() / 20;
        double repairProgress = (double) ticksFromStart / (double) repair.getDurationInTicks();
        if (repairProgress >= 0.0 && repairProgress <= 1.0) {
            repair.getProgressBar().setProgress(repairProgress);
        }
        repair.getProgressBar().setTitle(repair.getName() + ": " + secsFromStart + "/" + durationInSecs);

        if (p != null && ticksFromStart % 1200 == 0){
            p.sendMessage(I18nSupport.getInternationalisedString("Repair - Repairs underway") + ": " + secsFromStart + "/" + durationInSecs);
        }
        ticksFromStart++;
        repair.setTicksSinceStart(ticksFromStart);
        if (ticksFromStart % Settings.RepairTicksPerBlock == 0){
            //To avoid any issues during the repair, such as fragile blocks missing, or guns firing by accident, place the blocks in correct order
            if (!repair.getUpdateCommands().isEmpty()){ //first, all the solid blocks
                MapUpdateManager.getInstance().scheduleUpdate(repair.getUpdateCommands().pop());
                return;
            }
            if (!repair.getFragileBlockUpdateCommands().isEmpty()){//Then all the fragile blocks requiring support from blocks to the side of or above it
                MapUpdateManager.getInstance().scheduleUpdate(repair.getFragileBlockUpdateCommands().pop());
                return;
            }

        }
        //When the time is up and there are no more blocks to place, finish the repair.
        if ((ticksFromStart >= repair.getDurationInTicks()) && repair.getUpdateCommands().isEmpty() && repair.getFragileBlockUpdateCommands().isEmpty()){
            if (p != null && repair.getRunning().get()) {

                p.sendMessage(I18nSupport.getInternationalisedString("Repair - Repairs complete"));
                CraftManager.getInstance().removeCraft(repair.getCraft());
            }
            Movecraft.getInstance().getLogger().info(I18nSupport.getInternationalisedString("Repair - Repair Complete Console"));
            repair.getProgressBar().setVisible(false);
            repair.getRunning().set(false);
        }
    }
    public Repair getRepair(){
        return repair;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RepairTask){
            RepairTask task = (RepairTask) obj;
            return this.getRepair() == task.getRepair();
        }
        return false;
    }
}
