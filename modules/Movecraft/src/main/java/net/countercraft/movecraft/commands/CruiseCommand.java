package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class CruiseCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if(!command.getName().equalsIgnoreCase("cruise")){
            return false;
        }
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Cruise - Must Be Player"));
            return true;
        }
        Player player = (Player) commandSender;

        if(args.length<1){
            final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
            if (craft == null) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft"));
                return true;
            }
            if (!player.hasPermission("movecraft.commands") || !player.hasPermission("movecraft.commands.cruise")) {
                craft.setCruising(false);
                return true;
            }

            if(craft.getCruising()){
                craft.setCruising(false);
                return true;
            }
            // Normalize yaw from [-360, 360] to [0, 360]
            float yaw = (player.getLocation().getYaw() + 360.0f);
            if (yaw >= 360.0f) {
                yaw %= 360.0f;
            }
            if (yaw >= 45 && yaw < 135) { // west
                craft.setCruiseDirection(CruiseDirection.WEST);
            } else if (yaw >= 135 && yaw < 225) { // north
                craft.setCruiseDirection(CruiseDirection.NORTH);
            } else if (yaw >= 225 && yaw <= 315){ // east
                craft.setCruiseDirection(CruiseDirection.EAST);
            } else { // default south
                craft.setCruiseDirection(CruiseDirection.SOUTH);
            }
            craft.setCruising(true);
            return true;
        }
        if (args[0].equalsIgnoreCase("off")) { //This goes before because players can sometimes freeze while cruising
            final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
            if (craft == null) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft"));
                return true;
            }
            craft.setCruising(false);
            return true;
        }
        if (!player.hasPermission("movecraft.commands") || !player.hasPermission("movecraft.commands.cruise")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }

        final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
        if (craft == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return true;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".move")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (!craft.getType().getBoolProperty(CraftType.CAN_CRUISE)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Cruise - Craft Cannot Cruise"));
            return true;
        }


        if (args[0].equalsIgnoreCase("on")) {
            float yaw = (player.getLocation().getYaw() + 360.0f);
            if (yaw >= 360.0f) {
                yaw %= 360.0f;
            }
            if (yaw >= 45 && yaw < 135) { // west
                craft.setCruiseDirection(CruiseDirection.WEST);
            } else if (yaw >= 135 && yaw < 225) { // north
                craft.setCruiseDirection(CruiseDirection.NORTH);
            } else if (yaw >= 225 && yaw <= 315){ // east
                craft.setCruiseDirection(CruiseDirection.EAST);
            } else { // default south
                craft.setCruiseDirection(CruiseDirection.SOUTH);
            }
            craft.setCruising(true);
            return true;
        }
        if (args[0].equalsIgnoreCase("north") || args[0].equalsIgnoreCase("n")) {
            craft.setCruiseDirection(CruiseDirection.NORTH);
            craft.setCruising(true);
            return true;
        }
        if (args[0].equalsIgnoreCase("south") || args[0].equalsIgnoreCase("s")) {
            craft.setCruiseDirection(CruiseDirection.SOUTH);
            craft.setCruising(true);
            return true;
        }
        if (args[0].equalsIgnoreCase("east") || args[0].equalsIgnoreCase("e")) {
            craft.setCruiseDirection(CruiseDirection.EAST);
            craft.setCruising(true);
            return true;
        }
        if (args[0].equalsIgnoreCase("west") || args[0].equalsIgnoreCase("w")) {
            craft.setCruiseDirection(CruiseDirection.WEST);
            craft.setCruising(true);
            return true;
        }
        if (args[0].equalsIgnoreCase("up") || args[0].equalsIgnoreCase("u")) {
            craft.setCruiseDirection(CruiseDirection.UP);
            craft.setCruising(true);
            return true;
        }
        if (args[0].equalsIgnoreCase("down") || args[0].equalsIgnoreCase("d")) {
            craft.setCruiseDirection(CruiseDirection.DOWN);
            craft.setCruising(true);
            return true;
        }
        return false;
    }

    private final String[] completions = {"North", "East", "South", "West", "Up", "Down", "On", "Off"};
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length !=1)
            return Collections.emptyList();
        List<String> returnValues = new ArrayList<>();
        for(String completion : completions)
            if(completion.toLowerCase().startsWith(strings[strings.length-1].toLowerCase()))
                returnValues.add(completion);
        return returnValues;
    }
}
