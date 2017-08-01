package net.countercraft.movecraft.warfare.assault;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 *Procces assaults every 20 ticks
 */
public class AssaultManager extends BukkitRunnable {
    private final List<Assault> assaults = new CopyOnWriteArrayList<>();
    private final Plugin movecraft;

    public AssaultManager(Plugin movecraft) {
        this.movecraft = movecraft;
    }

    @Override
    public void run() {
        for (Iterator<Assault> iterator = assaults.iterator(); iterator.hasNext(); ) {
            Assault assault = iterator.next();
            if (!assault.getRunning().get()) {
                assaults.remove(assault);
                continue;
            }
            new AssaultTask(assault).runTask(movecraft);
        }
    }

    public List<Assault> getAssaults() {
        return assaults;
    }
}
