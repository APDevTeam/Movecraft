package net.countercraft.movecraft.warfare.siege;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SiegePreparationTask extends SiegeTask {

    public SiegePreparationTask(Siege siege) {
        super(siege);
    }

    @Override
    public void run() {
        Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                , Bukkit.getPlayer(siege.getPlayerUUID()).getDisplayName(), siege.getName(), (siege.getDelayBeforeStart() / 60) / 4 * 3));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
        }

    }
}
