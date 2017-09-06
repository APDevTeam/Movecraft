package net.countercraft.movecraft.warfare.siege;

import org.bukkit.scheduler.BukkitRunnable;

public abstract class SiegeTask extends BukkitRunnable {
    protected final Siege siege;

    protected SiegeTask(Siege siege) {
        this.siege = siege;
    }

    @Override
    public abstract void run();
}
