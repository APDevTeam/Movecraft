package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class SavedStatesCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("savedstates")) {
            return false;
        }

        if(!commandSender.hasPermission("movecraft.savedstates")) {
            commandSender.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }

        Player player;

        if(args.length > 0 && commandSender.hasPermission("movecraft.savedstates.other")) {

            player = Movecraft.getInstance().getServer().getPlayer(args[0]);

            if(player == null) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Player - Not Found"));
                return true;
            }
        }

        else {
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("SavedStates - Must Be Player"));
                return true;
            }

            player = (Player) commandSender;
        }

        File dataDirectory = new File(Movecraft.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(dataDirectory, player.getUniqueId().toString());
        if (!playerDirectory.exists() || playerDirectory.listFiles().length == 0) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("SavedStates - No Saved States"));
            return true;
        }

        commandSender.sendMessage(I18nSupport.getInternationalisedString(ChatColor.GOLD + I18nSupport.getInternationalisedString("SavedStates - Saved States")+":"));
        for(File file : playerDirectory.listFiles()) {
            if(file.isFile())
            {
                commandSender.sendMessage(file.getName().replace(".schematic", ""));
            }
        }

        return true;
    }
}
