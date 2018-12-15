package net.countercraft.movecraft.warfare.siege;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.craft.CraftManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;

public class SiegeProgressTask extends SiegeTask {

    public SiegeProgressTask(Siege siege) {
        super(siege);
    }

    //every 180 seconds = 3600 ticks
    public void run() {


        Player siegeLeader = Movecraft.getInstance().getServer().getPlayer(siege.getPlayerUUID());
        Craft siegeCraft = CraftManager.getInstance().getCraftByPlayer(siegeLeader);
        boolean siegeLeaderShipInRegion = false, siegeLeaderPilotingShip;
        //Allows the siege leader to not pilot a craft without having an NPE generated
        if (siegeCraft == null){
            siegeLeaderPilotingShip = false;
        } else if (siege.getCraftsToWin().contains(siegeCraft.getType().getCraftName())){
            siegeLeaderPilotingShip = true;
        } else {
            siegeLeaderPilotingShip = false;
        }
        int midX = 0;
        int midY = 0;
        int midZ = 0;
        if (siegeLeaderPilotingShip) {
            HashHitBox hitBox = siegeCraft.getHitBox();
            midX = (hitBox.getMaxX() + hitBox.getMinX()) / 2;
            midY = (hitBox.getMaxY() + hitBox.getMinY()) / 2;
            midZ = (hitBox.getMaxZ() + hitBox.getMinZ()) / 2;
            siegeLeaderShipInRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(siegeLeader.getWorld()).getRegion(siege.getAttackRegion()).contains(midX, midY, midZ);

        }



        double progress = (double) ((((int)System.currentTimeMillis() - siege.getStartTime())/1000) - siege.getDelayBeforeStart()) / (double) (siege.getDuration() - siege.getDelayBeforeStart());
        if (progress <= 1.0 && progress >= 0.0) { //Make sure progress is between 0.0 and 1.0 when it is registered to the progressbar
            siege.getProgressBar().setProgress(progress);
        }
        if (siegeLeaderShipInRegion){
            siege.getProgressBar().setColor(BarColor.GREEN);
        } else {
            siege.getProgressBar().setColor(BarColor.RED);
        }
        if ((siege.getDuration() - ((System.currentTimeMillis() - siege.getStartTime()) / 1000)) % 60 != 0) {
            return;
        }
        int timeLeft = (siege.getDuration() - (((int)System.currentTimeMillis() - siege.getStartTime())/1000));
        Bukkit.getPlayer(siege.getPlayerUUID()).sendMessage(String.valueOf(timeLeft));
        if (timeLeft > 10) {
            if (siegeLeaderShipInRegion) {
                Bukkit.getServer().broadcastMessage(String.format(
                        "The Siege of %s is under way. The Siege Flagship is a %s of size %d under the command of %s at %d, %d, %d. Siege will end in %d minutes",
                        siege.getName(),
                        siegeCraft.getType().getCraftName(),
                        siegeCraft.getOrigBlockCount(),
                        siegeLeader.getDisplayName(), midX, midY, midZ, timeLeft / 60));
            } else {
                Bukkit.getServer().broadcastMessage(String.format(
                        "The Siege of %s is under way. The Siege Leader, %s, is not in command of a Flagship within the Siege Region! If they are still not when the duration expires, the siege will fail! Siege will end in %d minutes",
                        siege.getName(), siegeLeader.getDisplayName(), timeLeft / 60));
            }
        } else {
            if (siegeLeaderShipInRegion) {
                Bukkit.getServer().broadcastMessage(String.format("The Siege of %s has succeeded! The forces of %s have been victorious!",
                        siege.getName(), siegeLeader.getDisplayName()));
                ProtectedRegion controlRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(siegeLeader.getWorld()).getRegion(siege.getCaptureRegion());
                DefaultDomain newOwner = new DefaultDomain();
                newOwner.addPlayer(siege.getPlayerUUID());
                controlRegion.setOwners(newOwner);
                DefaultDomain newMember = new DefaultDomain();
                newOwner.addPlayer(siege.getPlayerUUID()); //Is this supposed to be newMember?
                controlRegion.setMembers(newMember);
                if (siege.getCommandsOnWin() != null)
                    for (String command : siege.getCommandsOnWin()) {
                        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command
                                .replaceAll("%r", siege.getCaptureRegion())
                                .replaceAll("%c", "" + siege.getCost())
                                .replaceAll("%w", siegeLeader.toString()));
                    }
            } else {
                Bukkit.getServer().broadcastMessage(String.format("The Siege of %s has failed! The forces of %s have been crushed!",
                        siege.getName(), siegeLeader.getDisplayName()));
                if (siege.getCommandsOnLose() != null)
                    for (String command : siege.getCommandsOnLose()) {
                        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command
                                .replaceAll("%r", siege.getCaptureRegion())
                                .replaceAll("%c", "" + siege.getCost())
                                .replaceAll("%l", siegeLeader.toString()));
                    }
            }
            siege.getProgressBar().setVisible(false);
            siege.setStage(SiegeStage.INACTIVE);
        }
        for (Player p : Bukkit.getOnlinePlayers()){
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1,0);
        }
    }
}
