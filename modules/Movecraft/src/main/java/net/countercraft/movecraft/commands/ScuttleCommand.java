package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
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
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Player supplied must be online");
                return true;
            }
            craft = CraftManager.getInstance().getCraftByPlayer(player);
        } else if (commandSender.hasPermission("movecraft.commands.scuttle.self") && strings.length == 0) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "You must be a player to scuttle a craft");
                return true;
            }
            craft = CraftManager.getInstance().getCraftByPlayer(Bukkit.getPlayer(commandSender.getName()));
        }
        if (craft == null) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "no craft found to sink!");
            return true;
        }
        if(craft.getSinking()){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "the craft is already sinking!");
            return true;
        }
        craft.setCruising(false);
        craft.sink();
        CraftManager.getInstance().removePlayerFromCraft(craft);
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Scuttle was activated. Abandon ship!");
        return true;

    }
}
