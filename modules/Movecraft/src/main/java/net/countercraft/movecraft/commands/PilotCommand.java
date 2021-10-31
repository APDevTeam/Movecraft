package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class PilotCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("pilot"))
            return false;
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Pilot - Must Be Player"));
            return true;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("movecraft.commands") || !player.hasPermission("movecraft.commands.pilot")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Pilot - No Craft Type"));
            return true;
        }
        if (!player.hasPermission("movecraft." + args[0] + ".pilot")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        CraftType craftType = CraftManager.getInstance().getCraftTypeFromString(args[0]);
        if (craftType != null) {
            Craft oldCraft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
            if (oldCraft != null) {
                CraftManager.getInstance().removeCraft(oldCraft, CraftReleaseEvent.Reason.PLAYER);
            }
            PilotedCraft newCraft = new PilotedCraft(craftType, player.getWorld(), player);
            MovecraftLocation startPoint = MathUtils.bukkit2MovecraftLoc(player.getLocation());
            newCraft.detect(player, player, startPoint);
            Bukkit.getServer().getPluginManager().callEvent(new CraftPilotEvent(newCraft, CraftPilotEvent.Reason.PLAYER));
        } else {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Pilot - Invalid Craft Type"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length !=1 || !commandSender.hasPermission("movecraft.commands") || !commandSender.hasPermission("movecraft.commands.pilot"))
            return Collections.emptyList();
        List<String> completions = new ArrayList<>();
        for(CraftType type : CraftManager.getInstance().getCraftTypes())
            if(commandSender.hasPermission("movecraft." + type.getStringProperty(CraftType.NAME) + ".pilot"))
                completions.add(type.getStringProperty(CraftType.NAME));
        List<String> returnValues = new ArrayList<>();
        for(String completion : completions)
            if(completion.toLowerCase().startsWith(strings[strings.length-1].toLowerCase()))
                returnValues.add(completion);
        return returnValues;
    }
}
