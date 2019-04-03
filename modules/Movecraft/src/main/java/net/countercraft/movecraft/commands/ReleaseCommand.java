package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class ReleaseCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("release")) {
            return false;
        }

        if (!commandSender.hasPermission("movecraft.commands") && !commandSender.hasPermission("movecraft.commands.release")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (args.length == 0) {
            if(!(commandSender instanceof Player)){
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Player- Error - You do not have a craft to release!"));
                return true;
            }
            Player player = (Player) commandSender;
            final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

            if (pCraft == null) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Player- Error - You do not have a craft to release!"));
                return true;
            }
            CraftManager.getInstance().removeCraft(pCraft);
            return true;
        }
        if (!commandSender.hasPermission("movecraft.commands.release.others")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "You do not have permission to make others release");
            return true;
        }
        if (args[0].equalsIgnoreCase("-p")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(name);
                if (pCraft != null) {
                    CraftManager.getInstance().removeCraft(pCraft);

                }
            }
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "You forcibly released all player controlled crafts");
            return true;
        }

        if (args[0].equalsIgnoreCase("-a")) {
            final List<Craft> craftsToRelease = new ArrayList<>(CraftManager.getInstance().getCraftList());
            for (Craft craft : craftsToRelease) {
                CraftManager.getInstance().removeCraft(craft);
            }
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "You forcibly released all crafts");
            return true;
        }

        if (args[0].equalsIgnoreCase("-n")) {
            final List<Craft> craftsToRelease = new ArrayList<>(CraftManager.getInstance().getCraftList());
            for (Craft craft : craftsToRelease) {
                if(craft.getNotificationPlayer()==null) {
                    CraftManager.getInstance().removeCraft(craft);
                }
            }
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "You forcibly released all null piloted crafts");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "That player could not be found");
            return true;
        }
        final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(args[0]);
        if (pCraft != null) {
            CraftManager.getInstance().removeCraft(pCraft);
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "You have successfully force released a ship");
            return true;
        }
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "That player is not piloting a craft");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length !=1 || !commandSender.hasPermission("movecraft.commands.release.others"))
            return Collections.emptyList();
        List<String> completions = new ArrayList<>();
        completions.add("-a");
        completions.add("-p");
        for(Player player : Bukkit.getOnlinePlayers())
            completions.add(player.getName());

        List<String> returnValues = new ArrayList<>();
        for(String completion : completions)
            if(completion.toLowerCase().startsWith(strings[strings.length-1].toLowerCase()))
                returnValues.add(completion);
        return returnValues;
    }
}

