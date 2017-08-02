package net.countercraft.movecraft.warfare.siege;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicReference;

public class SiegeProgressTask extends SiegeTask {

    public SiegeProgressTask(Siege siege) {
        super(siege);
    }

    //every 180 seconds = 3600 ticks
    public void run() {
        if ((siege.getDuration() - (System.currentTimeMillis() - siege.getStartTime()) / 1000) % 60 != 0) {
            return;
        }
        Player siegeLeader = Movecraft.getInstance().getServer().getPlayer(siege.getPlayerUUID());
        Craft siegeCraft = CraftManager.getInstance().getCraftByPlayer(siegeLeader);
        boolean siegeLeaderShipInRegion = false, siegeLeaderPilotingShip = siege.getCraftsToWin().contains(siegeCraft.getType().getCraftName());
        int midX = 0;
        int midY = 0;
        int midZ = 0;
        if (siegeLeaderPilotingShip) {
            midX = (siegeCraft.getMaxX() + siegeCraft.getMinX()) / 2;
            midY = (siegeCraft.getMaxY() + siegeCraft.getMinY()) / 2;
            midZ = (siegeCraft.getMaxZ() + siegeCraft.getMinZ()) / 2;
            siegeLeaderShipInRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(siegeLeader.getWorld()).getRegion(siege.getAttackRegion()).contains(midX, midY, midZ);

        }
        int timeLeft = (int) (siege.getDuration() - (System.currentTimeMillis() - siege.getStartTime()) / 1000);
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
            siege.setStage(new AtomicReference<SiegeStage>(SiegeStage.INACTIVE));
        }
    }
}
