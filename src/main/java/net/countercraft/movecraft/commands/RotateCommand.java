package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RotateCommand implements TabExecutor{
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if(!command.getName().equalsIgnoreCase("rotate")){
            return false;
        }
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage("you need to be a player to pilot a craft");
            return true;
        }
        Player player = (Player) commandSender;
        if(args.length<1){
            commandSender.sendMessage("you need to supply a direction");
            return true;
        }
        if (args[0].equalsIgnoreCase("left")) {
            if (!player.hasPermission("movecraft.commands") && !player.hasPermission("movecraft.commands.rotateleft")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }
            final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
            if(craft==null){
                player.sendMessage(I18nSupport.getInternationalisedString("You need to be piloting a craft"));
                return true;
            }
            if (!player.hasPermission("movecraft." + craft.getType().getCraftName() + ".rotate")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }
            MovecraftLocation midPoint = getCraftMidPoint(craft);
            CraftManager.getInstance().getCraftByPlayerName(player.getName()).rotate(Rotation.ANTICLOCKWISE, midPoint);
            return true;
        }

        if (args[0].equalsIgnoreCase("right")) {
            if (!player.hasPermission("movecraft.commands") && !player.hasPermission("movecraft.commands.rotateright")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }
            final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
            if(craft==null){
                player.sendMessage(I18nSupport.getInternationalisedString("You need to be piloting a craft"));
                return true;
            }
            if (!player.hasPermission("movecraft." + craft.getType().getCraftName() + ".rotate")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }
            MovecraftLocation midPoint = getCraftMidPoint(craft);
            CraftManager.getInstance().getCraftByPlayerName(player.getName()).rotate(Rotation.CLOCKWISE, midPoint);
            return true;
        }
        player.sendMessage("invalid direction");
        return true;
    }

    private MovecraftLocation getCraftMidPoint(Craft craft) {
        int maxDX = 0;
        int maxDZ = 0;
        int maxY = 0;
        int minY = 32767;
        for (int[][] i1 : craft.getHitBox()) {
            maxDX++;
            if (i1 != null) {
                int indexZ = 0;
                for (int[] i2 : i1) {
                    indexZ++;
                    if (i2 != null) {
                        if (i2[0] < minY) {
                            minY = i2[0];
                        }
                    }
                    if (i2 != null) {
                        if (i2[1] < maxY) {
                            maxY = i2[1];
                        }
                    }
                }
                if (indexZ > maxDZ) {
                    maxDZ = indexZ;
                }

            }
        }
        int midX = craft.getMinX() + (maxDX / 2);
        int midY = (minY + maxY) / 2;
        int midZ = craft.getMinZ() + (maxDZ / 2);
        return new MovecraftLocation(midX, midY, midZ);
    }

    private final String[] completions = {"Right", "Left"};
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
