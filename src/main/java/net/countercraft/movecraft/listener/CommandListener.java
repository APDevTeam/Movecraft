/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.listener;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.warfare.assault.Assault;
import net.countercraft.movecraft.warfare.siege.Siege;
import net.countercraft.movecraft.warfare.siege.SiegeManager;
import net.countercraft.movecraft.warfare.siege.SiegeStage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

//public class CommandListener implements Listener {
public class CommandListener implements CommandExecutor {

    public CommandListener(){
        Bukkit.getLogger().info("init");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//	public void onCommand( PlayerCommandPreprocessEvent e ) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("siege")) {
            if (!player.hasPermission("movecraft.siege")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }
            SiegeManager siegeManager = Movecraft.getInstance().getSiegeManager();
            if (siegeManager.getSieges().size() == 0) {
                player.sendMessage(I18nSupport.getInternationalisedString("Siege is not configured on this server"));
                return true;
            }
            for (Siege siege : siegeManager.getSieges()) {
                if (siege.getStage().get() != SiegeStage.INACTIVE) {
                    player.sendMessage(I18nSupport.getInternationalisedString("A Siege is already taking place"));
                    return true;
                }
            }
            Siege siege = null;
            ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation());
            if (regions.size() != 0) {
                for (ProtectedRegion tRegion : regions.getRegions()) {
                    for (Siege tempSiege : siegeManager.getSieges()) {
                        if (tRegion.getId().equalsIgnoreCase(tempSiege.getAttackRegion()))
                            siege = tempSiege;
                    }
                }
            }
            if (siege != null) {
                long cost = siege.getCost();
                for (Siege tempSiege : siegeManager.getSieges()) {
                    ProtectedRegion tregion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(tempSiege.getCaptureRegion());
                    if (tempSiege.isDoubleCostPerOwnedSiegeRegion() && tregion.getOwners().contains(player.getUniqueId())) {
                        cost *= 2;
                    }
                }

                if (Movecraft.getInstance().getEconomy().has(player, cost)) {
                    Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    int hour = rightNow.get(Calendar.HOUR_OF_DAY);
                    int minute = rightNow.get(Calendar.MINUTE);
                    int currMilitaryTime = hour * 100 + minute;
                    int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
                    if (currMilitaryTime > siege.getScheduleStart() && currMilitaryTime < siege.getScheduleEnd() && dayOfWeek == siege.getDayOfWeek()) {

                        for (String command : siege.getCommandsOnStart()) {
                            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replaceAll("%r", siege.getAttackRegion()).replaceAll("%c", "" + siege.getCost()));
                        }
                        Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                                , player.getDisplayName(), siege.getName(), siege.getDelayBeforeStart() / 60));
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1,0.25F);
                        }
                        Movecraft.getInstance().getLogger().log(Level.INFO, String.format("Siege: %s commenced by %s for a cost of %d", siege.getName(), player.getName(), cost));
                        Movecraft.getInstance().getEconomy().withdrawPlayer(player, cost);
                        siege.setStage(new AtomicReference<SiegeStage>(SiegeStage.PREPERATION));
                        return true;
                    } else {
                        player.sendMessage(I18nSupport.getInternationalisedString("The time is not during the Siege schedule"));
                        return true;
                    }
                } else {
                    player.sendMessage(String.format("You do not have enough money. You need %d", cost));
                    return true;
                }
            } else {
                player.sendMessage(I18nSupport.getInternationalisedString("Could not find a siege configuration for the region you are in"));
                return true;
            }
        }

        return false;
    }

}
