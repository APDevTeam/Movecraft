package net.countercraft.movecraft.warfare.siege;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.countercraft.movecraft.localisation.I18nSupport;

public abstract class SiegeTask extends BukkitRunnable {
    protected final Siege siege;

    protected SiegeTask(Siege siege) {
        this.siege = siege;
    }

    @Override
    public abstract void run();

    protected void addPlayersToProgressBar() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            siege.getProgressBar().addPlayer(p);
        }
    }

    public String formatMinutes(int seconds) {
        if (seconds < 60) {
            if (seconds > 0) {
                return String.format(I18nSupport.getInternationalisedString("Siege - Ending in X seconds"), seconds);
            } else {
                return I18nSupport.getInternationalisedString("Siege - Ending Soon");
            }
        }

        int minutes = seconds / 60;
        if (minutes == 1) {
            return I18nSupport.getInternationalisedString("Siege - Ending In 1 Minute");
        }
        else {

                return String.format(I18nSupport.getInternationalisedString("Siege - Ending In X Minutes"), minutes);


        }
    }
}
