package net.countercraft.movecraft.repair;

import net.countercraft.movecraft.Movecraft;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
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

    public List<Repair> getRepairs() {
        return repairs;
    }
}
