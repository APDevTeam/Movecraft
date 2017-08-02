package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReleaseCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("release")) {
            return false;
        }
        if(!(commandSender instanceof Player)) {
            return true;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("movecraft.commands") && !player.hasPermission("movecraft.commands.release")) {
            player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (args.length == 0) {
            final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

            if (pCraft == null) {
                player.sendMessage(I18nSupport.getInternationalisedString("Player- Error - You do not have a craft to release!"));
                return true;
            }
            CraftManager.getInstance().removeCraft(pCraft);
            return true;
        }
        if (!player.hasPermission("movecraft.commands.release.others")) {
            player.sendMessage("You do not have permission to make others release");
            return true;
        }
        if (args[0].equalsIgnoreCase("-a")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(name);
                if (pCraft != null) {
                    CraftManager.getInstance().removeCraft(pCraft);

                }
            }
            player.sendMessage("You forced release every player's ship");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            commandSender.sendMessage("That player could not be found");
            return true;
        }
        final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(args[0]);
        if (pCraft != null) {
            CraftManager.getInstance().removeCraft(pCraft);
            player.sendMessage("You have successfully force released a ship");
            return true;
        }
        player.sendMessage("That player is not piloting a craft");
        return true;
    }

}

