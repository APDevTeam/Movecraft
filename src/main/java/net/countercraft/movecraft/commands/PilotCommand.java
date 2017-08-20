package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PilotCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("pilot"))
            return false;
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage("you need to be a player to pilot a craft");
            return true;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("movecraft.commands") || !player.hasPermission("movecraft.commands.pilot")) {
            player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage("You need to supply a craft type");
            return true;
        }
        if (!player.hasPermission("movecraft." + args[0] + ".pilot")) {
            player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        CraftType craftType = getCraftTypeFromString(args[0]);
        if (craftType != null) {
            Craft oldCraft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
            if (oldCraft != null) {
                CraftManager.getInstance().removeCraft(oldCraft);
            }
            Craft newCraft = new Craft(craftType, player.getWorld());
            MovecraftLocation startPoint = MathUtils.bukkit2MovecraftLoc(player.getLocation());
            newCraft.detect(player, player, startPoint);
        } else {
            player.sendMessage(I18nSupport.getInternationalisedString("Unknown craft type"));
        }
        return true;
    }

    private CraftType getCraftTypeFromString(String s) {
        for (CraftType t : CraftManager.getInstance().getCraftTypes()) {
            if (s.equalsIgnoreCase(t.getCraftName())) {
                return t;
            }
        }

        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length !=1 || !commandSender.hasPermission("movecraft.commands") || !commandSender.hasPermission("movecraft.commands.pilot"))
            return Collections.emptyList();
        List<String> completions = new ArrayList<>();
        for(CraftType type : CraftManager.getInstance().getCraftTypes())
            if(commandSender.hasPermission("movecraft." + type.getCraftName() + ".pilot"))
                completions.add(type.getCraftName());
        List<String> returnValues = new ArrayList<>();
        for(String completion : completions)
            if(completion.toLowerCase().startsWith(strings[strings.length-1].toLowerCase()))
                returnValues.add(completion);
        return returnValues;
    }
}
