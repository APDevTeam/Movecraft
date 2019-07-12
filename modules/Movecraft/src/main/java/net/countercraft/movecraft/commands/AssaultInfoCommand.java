package net.countercraft.movecraft.commands;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.WorldguardUtils;
import net.countercraft.movecraft.warfare.assault.Assault;
import net.countercraft.movecraft.warfare.assault.AssaultUtils;
import net.countercraft.movecraft.warfare.siege.Siege;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class AssaultInfoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("assaultinfo")) {
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

        if (!player.hasPermission("movecraft.assaultinfo")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation());
        if (regions.size() == 0) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("AssaultInfo - No Region Found"));
            return true;
        }
        LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(player);
        Map<String, ProtectedRegion> allRegions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegions();
        boolean foundOwnedRegion = false;
        for (ProtectedRegion iRegion : allRegions.values()) {
            if (iRegion.isOwner(lp) && iRegion.getFlag(DefaultFlag.TNT) == StateFlag.State.DENY) {
                foundOwnedRegion = true;
            }
        }
        if (!foundOwnedRegion) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - No Regions Owned"));
            return true;
        }

        ProtectedRegion assaultRegion = null;
        Search:
        for (ProtectedRegion tRegion : regions.getRegions()) {
            // a region can only be assaulted if it disables TNT, this is to prevent child regions or sub regions from being assaulted
            // regions with no owners can not be assaulted
            if (tRegion.getFlag(DefaultFlag.TNT) != StateFlag.State.DENY || tRegion.getOwners().size() == 0)
                continue ;
            for (Siege siege : Movecraft.getInstance().getSiegeManager().getSieges()) {
                // siegable regions can not be assaulted
                if (tRegion.getId().equalsIgnoreCase(siege.getAttackRegion()) || tRegion.getId().equalsIgnoreCase(siege.getCaptureRegion()))
                    continue Search;
            }
            assaultRegion = tRegion;
        }
        if (assaultRegion == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("AssaultInfo - No Region Found"));
            return true;
        }

        boolean canBeAssaulted = true;
        String output = I18nSupport.getInternationalisedString("Region Name") + ": ";
        output += assaultRegion.getId();
        output += ", "+I18nSupport.getInternationalisedString("Region Owner")+": " ;
        output += WorldguardUtils.getRegionOwnerList(assaultRegion);
        output += ", "+I18nSupport.getInternationalisedString("Max Damages")+": ";
        double maxDamage = AssaultUtils.getMaxDamages(assaultRegion);
        output += String.format("%.2f", maxDamage);
        output += ", "+I18nSupport.getInternationalisedString("Assault Cost")+": ";
        double cost = AssaultUtils.getCostToAssault(assaultRegion);
        output += String.format("%.2f", cost);
        for (Assault assault : Movecraft.getInstance().getAssaultManager().getAssaults()) {
            if (assault.getRegionName().equals(assaultRegion.getId()) && System.currentTimeMillis() - assault.getStartTime() < Settings.AssaultCooldownHours * (60 * 60 * 1000)) {
                canBeAssaulted = false;
                output += ", "+I18nSupport.getInternationalisedString("AssaultInfo - Not Assaultable Damaged");
                break;
            }
        }
        if (!AssaultUtils.areDefendersOnline(assaultRegion)) {
            canBeAssaulted = false;
            output += ", "+I18nSupport.getInternationalisedString("AssaultInfo - Not Assaultable Defenders");
        }
        if (assaultRegion.isMember(lp)) {
            output += ", "+I18nSupport.getInternationalisedString("AssaultInfo - Not Assaultable Member");
            canBeAssaulted = false;
        }
        if (canBeAssaulted) {
            output += ", "+I18nSupport.getInternationalisedString("AssaultInfo - Assaultable");
        }
        player.sendMessage(output);
        return true;
    }
}
