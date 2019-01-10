package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.CraftManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class MovecraftCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("movecraft")) {
            return false;
        }
        if(!commandSender.hasPermission("movecraft.commands.movecraft")){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Error, invalid permissions");
            return true;
        }

        if(args.length == 0){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Movecraft 7.0 Pre Release 7 SNAPSHOT-4 by cccm5");
            return true;
        }

        if(args.length==1 && args[0].equalsIgnoreCase("reloadtypes") && commandSender.hasPermission("movecraft.commands.movecraft.reloadtypes")){
            CraftManager.getInstance().initCraftTypes();
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Reloaded types");
            return true;
        }
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Error, invalid syntax");
        return true;

    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if(args.length !=1 || !commandSender.hasPermission("movecraft.commands") || !commandSender.hasPermission("movecraft.commands.movecraft"))
            return Collections.emptyList();
        List<String> completions = new ArrayList<>();
        completions.add("reloadtypes");
        List<String> returnValues = new ArrayList<>();
        for(String completion : completions)
            if(completion.toLowerCase().startsWith(args[args.length-1].toLowerCase()))
                returnValues.add(completion);
        return returnValues;
    }
}
