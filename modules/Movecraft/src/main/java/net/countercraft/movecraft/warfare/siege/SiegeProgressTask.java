package net.countercraft.movecraft.warfare.siege;

import com.avaje.ebean.validation.NotNull;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicReference;

public class SiegeProgressTask extends SiegeTask {

    public SiegeProgressTask(Siege siege) {
        super(siege);
    }

    //every 20 ticks = 1 second
    public void run() {
        if ((siege.getDuration() - ((System.currentTimeMillis() - siege.getStartTime()) / 1000)) % Settings.SiegeTaskSeconds != 0) {
            return;
        }

        Player siegeLeader = Movecraft.getInstance().getServer().getPlayer(siege.getPlayerUUID());
        Craft siegeCraft = CraftManager.getInstance().getCraftByPlayer(siegeLeader);
        int timeLeft = (siege.getDuration() - (((int)System.currentTimeMillis() - siege.getStartTime())/1000));

        if (timeLeft > 10) {
            if (leaderPilotingShip(siegeCraft)) {
                if (leaderShipInRegion(siegeCraft, siegeLeader)) {
                    MovecraftLocation mid = siegeCraft.getHitBox().getMidPoint();
                    Bukkit.getServer().broadcastMessage(String.format(
                            "The Siege of %s is under way. The Siege Flagship is a %s of size %d under the command of %s at [x:%d, y:%d, z:%d]. Siege will end ",
                            siege.getName(),
                            siegeCraft.getType().getCraftName(),
                            siegeCraft.getOrigBlockCount(),
                            siegeLeader.getDisplayName(), mid.getX(), mid.getY(), mid.getZ())
                            + formatMinutes(timeLeft));
                } else {
                    Bukkit.getServer().broadcastMessage(String.format(
                            "The Siege of %s is under way. The Siege Leader, %s, is not in command of a Flagship within the Siege Region! If they are still not when the duration expires, the siege will fail! Siege will end ",
                            siege.getName(), siegeLeader.getDisplayName())
                            + formatMinutes(timeLeft));
                }
            }
            else {
                return;
            }
        } else {
            if (leaderPilotingShip(siegeCraft)) {
                if (leaderShipInRegion(siegeCraft, siegeLeader)) {
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
                }
                else {
                    failSiege(siegeLeader);
                }
            }
            else {
                failSiege(siegeLeader);
            }
        }

        siege.setStage(SiegeStage.INACTIVE);

        for (Player p : Bukkit.getOnlinePlayers()){
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1,0);
        }
    }

    private void failSiege(Player siegeLeader) {
        Bukkit.getServer().broadcastMessage(String.format("The Siege of %s has failed! The forces of %s have been crushed!",
                siege.getName(), siegeLeader.getDisplayName()));
        if (siege.getCommandsOnLose() != null) {
            for (String command : siege.getCommandsOnLose()) {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command
                        .replaceAll("%r", siege.getCaptureRegion())
                        .replaceAll("%c", "" + siege.getCost())
                        .replaceAll("%l", siegeLeader.toString()));
            }
        }
    }

    private boolean leaderPilotingShip(Craft siegeCraft) {
        if (siegeCraft == null) {
            return false;
        } else if (siege.getCraftsToWin().contains(siegeCraft.getType().getCraftName())){
            return true;
        } else {
            return false;
        }
    }

    private boolean leaderShipInRegion(Craft siegeCraft, Player siegeLeader) {
        MovecraftLocation mid = siegeCraft.getHitBox().getMidPoint();
        return Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(siegeLeader.getWorld()).getRegion(siege.getAttackRegion()).contains(mid.getX(), mid.getY(), mid.getZ());
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
