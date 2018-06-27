package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScuttleCommand implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (!command.getName().equalsIgnoreCase("scuttle")) {
            return false;
        }

        // Scuttle other player
        if (commandSender.hasPermission("movecraft.commands.scuttle.others") && strings.length >= 1) {
            Player player = Bukkit.getPlayer(strings[0]);

            if (player == null) {
                commandSender.sendMessage("Player supplied must be online");
                return true;
            }

            if (CraftManager.getInstance().getCraftByPlayer(player) == null) {
                commandSender.sendMessage("Player supplied must be piloting a craft to perform this command");
                return true;
            }

            CraftManager.getInstance().getCraftByPlayer(player).sink();
        } else if (commandSender.hasPermission("movecraft.commands.scuttle.self") && strings.length == 0) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage("You must be a player to scuttle a craft");
                return true;
            }
            Craft craft = CraftManager.getInstance().getCraftByPlayer(Bukkit.getPlayer(commandSender.getName()));
            if (craft == null) {
                commandSender.sendMessage("You must be piloting a craft to perform this command");
                return true;
            }
            craft.sink();
        }
        commandSender.sendMessage("Scuttle was activated. Abandon ship!");
        return true;

    }
}
