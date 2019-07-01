package net.countercraft.movecraft.warfare.siege;

import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SiegePreparationTask extends SiegeTask {


    public SiegePreparationTask(Siege siege) {
        super(siege);
    }

    @Override
    public void run() {
        int timePassed = ((int)(System.currentTimeMillis() - siege.getStartTime())); //time passed in milliseconds
        int timePassedInSeconds = timePassed / 1000;
        if (timePassedInSeconds >= siege.getDelayBeforeStart()){
            siege.setStage(SiegeStage.IN_PROGRESS);
        }
        if ((siege.getDelayBeforeStart() - timePassedInSeconds) % Settings.SiegeTaskSeconds != 0 || timePassed < 3000){
             return;
        }
        int timeLeft = siege.getDelayBeforeStart() - timePassedInSeconds;
        broadcastSiegePreparation(Bukkit.getPlayer(siege.getPlayerUUID()), siege.getName(), timeLeft);
    }

    private void broadcastSiegePreparation(Player player, String siegeName, int timeLeft){
        String playerName = "";
        if (player != null){
            playerName = player.getDisplayName();
        }
        Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin ", playerName, siegeName) + formatMinutes(timeLeft));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
        }
    }

    private String formatMinutes(int seconds) {
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
