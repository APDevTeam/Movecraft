package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class CrewBedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("crewBed")) {
            return false;
        }

        if(!commandSender.hasPermission("movecraft.crewbedcommand")) {
            commandSender.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }

        Player player;

        if(args.length > 0 && commandSender.hasPermission("movecraft.crewbedcommand.other")) {

            player = Movecraft.getInstance().getServer().getPlayer(args[0]);

            if(player == null) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Player - Not Found"));
                return true;
            }
        }

        else {
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("CrewBed - Must Be Player"));
                return true;
            }

            player = (Player) commandSender;
        }

        Location bedLocation = player.getBedSpawnLocation();
        if(bedLocation != null) {
            commandSender.sendMessage(I18nSupport.getInternationalisedString("CrewBed - Priority Location") + String.format(": %d, %d, %d", bedLocation.getBlockX(), bedLocation.getBlockY(), bedLocation.getBlockZ()));
        }
        else {
            commandSender.sendMessage(I18nSupport.getInternationalisedString("CrewBed - No Priority Bed"));
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null) {
            return true;
        }
        if(craft.getSinking() || craft.getDisabled() || !craft.getCrewSigns().containsKey(player.getUniqueId())) {
            return true;
        }

        Location respawnLoc = craft.getCrewSigns().get(player.getUniqueId());
        commandSender.sendMessage(I18nSupport.getInternationalisedString("CrewBed - Current Location") + String.format(": %d, %d, %d", respawnLoc.getBlockX(), respawnLoc.getBlockY(), respawnLoc.getBlockZ()));

        return true;
    }
}
