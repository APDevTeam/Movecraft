package net.countercraft.movecraft.warfare.siege;

import org.bukkit.scheduler.BukkitRunnable;

public abstract class SiegeTask extends BukkitRunnable {
    protected final Siege siege;

    protected SiegeTask(Siege siege) {
        this.siege = siege;
    }

    @Override
    public abstract void run();

    public String formatMinutes(int seconds) {
        if (seconds < 60) {
            return "soon";
        }

        int minutes = seconds / 60;
        if (minutes == 1) {
            return "in 1 minute";
        }
        else {
            return String.format("in %d minutes", minutes);
        }
    }
}
