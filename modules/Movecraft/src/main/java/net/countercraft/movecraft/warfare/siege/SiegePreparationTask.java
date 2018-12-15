package net.countercraft.movecraft.warfare.siege;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SiegePreparationTask extends SiegeTask {

    public SiegePreparationTask(Siege siege) {
        super(siege);
    }

    @Override
    public void run() {
        int timePassed = ((int)(System.currentTimeMillis() - siege.getStartTime())); //time passed in milliseconds
        int timePassedinSeconds = timePassed / 1000;
        if (timePassedinSeconds >= siege.getDelayBeforeStart()){
            siege.setStage(SiegeStage.IN_PROGRESS);
        }
        double progress = (double) timePassedinSeconds / (double) siege.getDelayBeforeStart();
        if (progress <= 1.0) {
            siege.getProgressBar().setProgress(progress);
        }
        //Bukkit.getServer().broadcastMessage(String.valueOf(progress) + ", " + String.valueOf(timePassedinSeconds) + ", " + String.valueOf(siege.getDuration()));
        for (Player p : Bukkit.getOnlinePlayers()){
            siege.getProgressBar().addPlayer(p);
        }
        if ((siege.getDelayBeforeStart() - timePassed/1000) % 60 != 0 || timePassed < 3000){
             return;
        }
        int timeLeft = siege.getDelayBeforeStart() - (timePassed/1000);
        broadcastSiegePreparation(siege.getPlayerUUID(), siege.getName(), (int) timeLeft);
    }

    private void broadcastSiegePreparation(UUID playerUUID, String siegeName, int timeLeft){
        String playerName = "An offline siege leader";
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null){
            playerName = player.getDisplayName();
        } else {
            playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
        }
        Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes", playerName, siegeName, timeLeft / 60));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
        }
    }
}
