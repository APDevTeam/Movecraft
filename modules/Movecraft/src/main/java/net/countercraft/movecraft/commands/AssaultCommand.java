package net.countercraft.movecraft.commands;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftRepair;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.warfare.assault.Assault;
import net.countercraft.movecraft.warfare.assault.AssaultUtils;
import net.countercraft.movecraft.warfare.siege.Siege;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;
import static net.countercraft.movecraft.warfare.assault.AssaultUtils.getCostToAssault;

public class AssaultCommand implements CommandExecutor{
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("assault")) {
            return false;
        }
        if (!Settings.AssaultEnable) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - Disabled"));
            return true;
        }
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("AssaultInfo - Must Be Player"));
            return true;
        }
        Player player = (Player) commandSender;

        if (!player.hasPermission("movecraft.assault")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - No Region Specified"));
            return true;
        }
        ProtectedRegion aRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(args[0]);
        if (aRegion == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - Region Not Found"));
            return true;
        }
        LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(player);
        Map<String, ProtectedRegion> allRegions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegions();
        boolean foundOwnedRegion = false;
        for (ProtectedRegion iRegion : allRegions.values()) {
            if (iRegion.isOwner(lp) && iRegion.getFlag(DefaultFlag.TNT) == StateFlag.State.DENY) {
                foundOwnedRegion = true;
                break;
            }
        }
        if (!foundOwnedRegion) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - No Region Owned"));
            return true;
        }
        boolean canBeAssaulted = true;
        for (Siege siege : Movecraft.getInstance().getSiegeManager().getSieges()) {
            // siegable regions can not be assaulted
            if (aRegion.getId().equalsIgnoreCase(siege.getAttackRegion()) || aRegion.getId().equalsIgnoreCase(siege.getCaptureRegion())) {
                canBeAssaulted = false;
                break;
            }
        }
        // a region can only be assaulted if it disables TNT, this is to prevent child regions or sub regions from being assaulted
        // regions with no owners can not be assaulted
        if (aRegion.getFlag(DefaultFlag.TNT) != StateFlag.State.DENY || aRegion.getOwners().size() == 0)
            canBeAssaulted = false;

        {
            Assault assault = null;
            for (Assault tempAssault : Movecraft.getInstance().getAssaultManager().getAssaults()) {
                if (tempAssault.getRegionName().equals(aRegion.getId())) {
                    assault = tempAssault;
                    break;
                }
            }
            if (assault != null) {
                long startTime = assault.getStartTime();
                long curtime = System.currentTimeMillis();
                if (curtime - startTime < Settings.AssaultCooldownHours * (60 * 60 * 1000)) {
                    canBeAssaulted = false;
                }
            }
        }
        if (!AssaultUtils.areDefendersOnline(aRegion)) {
            canBeAssaulted = false;
        }
        if (aRegion.isMember(lp)) {
            canBeAssaulted = false;
        }
        for(Assault assault : Movecraft.getInstance().getAssaultManager().getAssaults()){
            if(assault.getRegionName().equals(args[0])){
                canBeAssaulted = false;
                break;
            }
        }
        if (!canBeAssaulted) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - Cannot Assault"));
            return true;
        }
        OfflinePlayer offP = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (Movecraft.getInstance().getEconomy().getBalance(offP) < getCostToAssault(aRegion)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - Insufficient Funds"));
            return true;
        }
//			if(aRegion.getType() instanceof ProtectedCuboidRegion) { // Originally I wasn't going to do non-cubes, but we'll try it and see how it goes. In theory it may repair more than it should but... meh...
        if (!MovecraftRepair.getInstance().saveRegionRepairState(player.getWorld(), aRegion)){
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Repair - Could not save file"));
            return true;
        }
        Vector min = new Vector(aRegion.getMinimumPoint().getBlockX(), aRegion.getMinimumPoint().getBlockY(), aRegion.getMinimumPoint().getBlockZ());
        Vector max = new Vector(aRegion.getMaximumPoint().getBlockX(), aRegion.getMaximumPoint().getBlockY(), aRegion.getMaximumPoint().getBlockZ());

        if (max.subtract(min).getBlockX() > 256) {
            if (min.getBlockX() < player.getLocation().getBlockX() - 128) {
                min = min.setX(player.getLocation().getBlockX() - 128);
            }
            if (max.getBlockX() > player.getLocation().getBlockX() + 128) {
                max = max.setX(player.getLocation().getBlockX() + 128);
            }
        }
        if (max.subtract(min).getBlockZ() > 256) {
            if (min.getBlockZ() < player.getLocation().getBlockZ() - 128) {
                min = min.setZ(player.getLocation().getBlockZ() - 128);
            }
            if (max.getBlockZ() > player.getLocation().getBlockZ() + 128) {
                max = max.setZ(player.getLocation().getBlockZ() + 128);
            }
        }
//			} else {
//				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "This region is not a cuboid - see an admin" ) ) );
//				return true;
//			}
        Movecraft.getInstance().getEconomy().withdrawPlayer(offP, getCostToAssault(aRegion));
        Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Assault - Starting Soon")
                , player.getDisplayName(), args[0], Settings.AssaultDelay / 60));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, (float) 0.25);
        }
        final String taskAssaultName = args[0];
        final Player taskPlayer = player;
        final World taskWorld = player.getWorld();
        final Long taskMaxDamages = (long) AssaultUtils.getMaxDamages(aRegion);
        final Vector taskMin = min;
        final Vector taskMax = max;
        //TODO: Make async
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Assault - Assault Begun")
                        , taskAssaultName, taskPlayer.getDisplayName()));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0.25F);
                }
                Movecraft.getInstance().getAssaultManager().getAssaults().add(new Assault(taskAssaultName, taskPlayer, taskWorld, System.currentTimeMillis(), taskMaxDamages, taskMin, taskMax));
                ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(taskWorld).getRegion(taskAssaultName);
                tRegion.setFlag(DefaultFlag.TNT, StateFlag.State.ALLOW);
            }
        }.runTaskLater(Movecraft.getInstance(), (20 * Settings.AssaultDelay));
        return true;


    }
}
