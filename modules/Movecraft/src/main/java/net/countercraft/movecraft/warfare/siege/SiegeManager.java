package net.countercraft.movecraft.warfare.siege;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SiegeManager extends BukkitRunnable {
    private final List<Siege> sieges = new CopyOnWriteArrayList<>();
    private final Plugin movecraft;


    public SiegeManager(Plugin movecraft) {
        this.movecraft = movecraft;
    }

    @Override
    public void run() {
        for (Siege siege : sieges) {
            new SiegePaymentTask(siege).runTask(movecraft);
            if (siege.getStage().get() == SiegeStage.IN_PROGRESS) {
                new SiegeProgressTask(siege).runTask(movecraft);
            } else if (siege.getStage().get() == SiegeStage.PREPERATION) {
                new SiegePreparationTask(siege).runTask(movecraft);
            }

        }
    }

    public List<Siege> getSieges() {
        return sieges;
    }
}
