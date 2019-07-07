package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class ScuttleCommand implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (!command.getName().equalsIgnoreCase("scuttle")) {
            return false;
        }

        Craft craft = null;
        // Scuttle other player
        if (commandSender.hasPermission("movecraft.commands.scuttle.others") && strings.length >= 1) {
            Player player = Bukkit.getPlayer(strings[0]);
            if (player == null) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Scuttle - Must Be Online"));
                return true;
            }
            craft = CraftManager.getInstance().getCraftByPlayer(player);
        } else if (commandSender.hasPermission("movecraft.commands.scuttle.self") && strings.length == 0) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Scuttle - Must Be Player"));
                return true;
            }
            craft = CraftManager.getInstance().getCraftByPlayer(Bukkit.getPlayer(commandSender.getName()));
        }
        if (craft == null) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX +  I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return true;
        }
        if(craft.getSinking()){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Scuttle - Craft Already Sinking");
            return true;
        }
        craft.setCruising(false);
        craft.sink();
        CraftManager.getInstance().removePlayerFromCraft(craft);
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Scuttle - Scuttle Activated");
        return true;

    }
}

