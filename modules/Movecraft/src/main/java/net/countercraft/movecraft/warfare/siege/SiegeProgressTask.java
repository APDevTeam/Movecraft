package net.countercraft.movecraft.warfare.siege;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

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
        int timeLeft = (siege.getDuration() - (int) ((System.currentTimeMillis() - siege.getStartTime())/1000));

        if (timeLeft > 10) {
            if (!leaderPilotingShip(siegeCraft)) {
                return;
            }

            if (leaderShipInRegion(siegeCraft, siegeLeader)) {
                MovecraftLocation mid = siegeCraft.getHitBox().getMidPoint();
                Bukkit.getServer().broadcastMessage(String.format(
                        I18nSupport.getInternationalisedString("Siege - Flagship In Box"),
                        siege.getName(),
                        siegeCraft.getType().getCraftName(),
                        siegeCraft.getOrigBlockCount(),
                        siegeLeader.getDisplayName(), mid.getX(), mid.getY(), mid.getZ())
                        + formatMinutes(timeLeft));
            } else {
                Bukkit.getServer().broadcastMessage(String.format(
                        I18nSupport.getInternationalisedString("Siege - Flagship Not In Box"),
                        siege.getName(), siegeLeader.getDisplayName())
                        + formatMinutes(timeLeft));
            }
        } else {
            endSiege(siegeCraft, siegeLeader);
        }



        for (Player p : Bukkit.getOnlinePlayers()){
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1,0);
        }
    }

    private void endSiege(Craft siegeCraft, Player siegeLeader) {
        if (leaderPilotingShip(siegeCraft)) {
            if (leaderShipInRegion(siegeCraft, siegeLeader)) {
                Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Siege - Siege Success"),
                        siege.getName(), siegeLeader.getDisplayName()));
                winSiege(siegeLeader);
            }
            else {
                failSiege(siegeLeader);
            }
        }
        else {
            failSiege(siegeLeader);
        }
        siege.setStage(SiegeStage.INACTIVE);
    }

    private void winSiege(Player siegeLeader) {
        ProtectedRegion controlRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(siegeLeader.getWorld()).getRegion(siege.getCaptureRegion());
        DefaultDomain newOwner = new DefaultDomain();
        newOwner.addPlayer(siege.getPlayerUUID());
        controlRegion.setOwners(newOwner);
        DefaultDomain newMember = new DefaultDomain();
        newOwner.addPlayer(siege.getPlayerUUID()); //Is this supposed to be newMember?
        controlRegion.setMembers(newMember);
        processCommands(siegeLeader, true);
    }

    private void failSiege(Player siegeLeader) {
        Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Siege - Siege Failure"),
                siege.getName(), siegeLeader.getDisplayName()));

        processCommands(siegeLeader, false);
    }

    private void processCommands(Player siegeLeader, boolean win) {
        if(win && siege.getCommandsOnWin() == null) {
            return;
        }
        else if(siege.getCommandsOnLose() == null) {
            return;
        }

        List<String> commands = win ? siege.getCommandsOnWin() : siege.getCommandsOnLose();
        for (String command : commands) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command
                    .replaceAll("%r", siege.getCaptureRegion())
                    .replaceAll("%c", "" + siege.getCost())
                    .replaceAll("%l", siegeLeader.toString()));
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
        ProtectedRegion r = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(siegeLeader.getWorld()).getRegion(siege.getAttackRegion());
        return r.contains(mid.getX(), mid.getY(), mid.getZ());
    }
}
