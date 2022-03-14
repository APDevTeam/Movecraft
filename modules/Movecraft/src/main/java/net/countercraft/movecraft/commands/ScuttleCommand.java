package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftScuttleEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

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
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString("Scuttle - Must Be Online"));
                return true;
            }
            craft = CraftManager.getInstance().getCraftByPlayer(player);
        }
        else if (commandSender.hasPermission("movecraft.commands.scuttle.self") && strings.length == 0) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString("Scuttle - Must Be Player"));
                return true;
            }
            craft = CraftManager.getInstance().getCraftByPlayer(Bukkit.getPlayer(commandSender.getName()));
        }
        if (craft == null) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return true;
        }
        if (craft instanceof SinkingCraft) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Scuttle - Craft Already Sinking"));
            return true;
        }
        if (!commandSender.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME)
                + ".scuttle")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }

        CraftScuttleEvent e = new CraftScuttleEvent(craft, (Player) commandSender);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (e.isCancelled())
            return true;

        craft.setCruising(false);
        CraftManager.getInstance().sink(craft);
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Scuttle - Scuttle Activated"));
        return true;

    }
}

