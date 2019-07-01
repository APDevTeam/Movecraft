package net.countercraft.movecraft.warfare.siege;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicReference;

public class SiegeProgressTask extends SiegeTask {

    public SiegeProgressTask(Siege siege) {
        super(siege);
    }

    //every 180 seconds = 3600 ticks
    public void run() {
        if ((siege.getDuration() - ((System.currentTimeMillis() - siege.getStartTime()) / 1000)) % 60 != 0) {
            return;
        }
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
        int timeLeft = (siege.getDuration() - (((int)System.currentTimeMillis() - siege.getStartTime())/1000));

        if (timeLeft > 10) {
            if (siegeLeaderShipInRegion) {
                Bukkit.getServer().broadcastMessage(String.format(
                        I18nSupport.getInternationalisedString("Siege - Flagship In Box"),
                        siege.getName(),
                        siegeCraft.getType().getCraftName(),
                        siegeCraft.getOrigBlockCount(),
                        siegeLeader.getDisplayName(), midX, midY, midZ, timeLeft / 60));
            } else {
                Bukkit.getServer().broadcastMessage(String.format(
                		I18nSupport.getInternationalisedString("Siege - Flagship Not In Box"),
                        siege.getName(), siegeLeader.getDisplayName(), timeLeft / 60));
            }
        } else {
            if (siegeLeaderShipInRegion) {
                Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Siege - Siege Success"),
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
                Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Siege - Siege Failure"),
                        siege.getName(), siegeLeader.getDisplayName()));
                if (siege.getCommandsOnLose() != null)
                    for (String command : siege.getCommandsOnLose()) {
                        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command
                                .replaceAll("%r", siege.getCaptureRegion())
                                .replaceAll("%c", "" + siege.getCost())
                                .replaceAll("%l", siegeLeader.toString()));
                    }
            }
            siege.setStage(SiegeStage.INACTIVE);
        }
        for (Player p : Bukkit.getOnlinePlayers()){
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1,0);
        }
    }
}
