package net.countercraft.movecraft.commands;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.warfare.siege.Siege;
import net.countercraft.movecraft.warfare.siege.SiegeManager;
import net.countercraft.movecraft.warfare.siege.SiegeStage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class SiegeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("siege")) {
            return false;
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("you need to be a player to get assault info");
            return true;
        }
        Player player = (Player) commandSender;

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
        search:
        for (ProtectedRegion tRegion : regions.getRegions()) {
            for (Siege tempSiege : siegeManager.getSieges()) {
                if (tRegion.getId().equalsIgnoreCase(tempSiege.getAttackRegion())) {
                    siege = tempSiege;
                    break search;
                }
            }
        }
        if (siege == null) {
            player.sendMessage(I18nSupport.getInternationalisedString("Could not find a siege configuration for the region you are in"));
            return true;
        }
        long cost = siege.getCost();
        for (Siege tempSiege : siegeManager.getSieges()) {
            ProtectedRegion tregion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(tempSiege.getCaptureRegion());
            assert tregion!=null;
            if (tempSiege.isDoubleCostPerOwnedSiegeRegion() && tregion.getOwners().contains(player.getUniqueId()))
                cost *= 2;
        }

        if (!Movecraft.getInstance().getEconomy().has(player, cost)) {
            player.sendMessage(String.format("You do not have enough money. You need %d", cost));
            return true;
        }
        Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int minute = rightNow.get(Calendar.MINUTE);
        int currMilitaryTime = hour * 100 + minute;
        int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
        if (currMilitaryTime <= siege.getScheduleStart() || currMilitaryTime >= siege.getScheduleEnd() || dayOfWeek != siege.getDayOfWeek()) {
            player.sendMessage(I18nSupport.getInternationalisedString("The time is not during the Siege schedule"));
            return true;
        }
        for (String startCommand : siege.getCommandsOnStart()) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), startCommand.replaceAll("%r", siege.getAttackRegion()).replaceAll("%c", "" + siege.getCost()));
        }
        Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                , player.getDisplayName(), siege.getName(), siege.getDelayBeforeStart() / 60));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0.25F);
        }
        Movecraft.getInstance().getLogger().log(Level.INFO, String.format("Siege: %s commenced by %s for a cost of %d", siege.getName(), player.getName(), cost));
        Movecraft.getInstance().getEconomy().withdrawPlayer(player, cost);
        siege.setStage(new AtomicReference<>(SiegeStage.PREPERATION));
        return true;
    }
}