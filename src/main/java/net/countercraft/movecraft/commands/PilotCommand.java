package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PilotCommand implements CommandExecutor {
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
        if (args.length < 0) {
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
}
