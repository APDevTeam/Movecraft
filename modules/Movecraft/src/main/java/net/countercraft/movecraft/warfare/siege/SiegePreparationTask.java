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
        /*String playerDisplayName = Bukkit.getPlayer(siege.getPlayerUUID()).getDisplayName();
        String siegeName = siege.getName();
        int delayInMinutes = (siege.getDelayBeforeStart() / 60) / 4 * 3;
        Bukkit.getServer().broadcastMessage(playerDisplayName + " is preparing to siege " + siegeName + "! All players wishing to participate in the defense should head there immediately! Siege will begin in " + delayInMinutes + " minutes");*/
        int timePassed = (int)(System.currentTimeMillis()/1000) - siege.getStartTime(); //time passed in seconds
        if ((timePassed == siege.getDelayBeforeStart() / 4 * 3)||
                (timePassed == siege.getDelayBeforeStart() / 4 * 2)||
                (timePassed == siege.getDelayBeforeStart() / 4 )||
                (timePassed == siege.getDelayBeforeStart())){
            int timeLeft = siege.getDelayBeforeStart() - timePassed;
            broadcastSiegePreparation(Bukkit.getPlayer(siege.getPlayerUUID()).getDisplayName(), siege.getName(), timeLeft);
            if (timeLeft == 0){
                siege.setStage(SiegeStage.IN_PROGRESS);
            }
        }
        /*Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                , Bukkit.getPlayer(siege.getPlayerUUID()).getDisplayName()
                , siege.getName()
                , (siege.getDelayBeforeStart() / 60) / 4 * 3));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
        }*/
        //for debug only
        /*Bukkit.getServer().broadcastMessage("Time left: "+String.valueOf(timePassed) +
                " Start time: "+ String.valueOf(siege.getStartTime() +
                "Duration: " + String.valueOf(siege.getDuration() +
                "Siege stage: " + siege.getStage().toString())));*/

    }

    private void broadcastSiegePreparation(String playerName, String siegeName, int timeLeft){
        Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                , playerName
                , siegeName
                , timeLeft / 60));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
        }
    }
}
