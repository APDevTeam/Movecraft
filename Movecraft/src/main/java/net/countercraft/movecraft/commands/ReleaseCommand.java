package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class ReleaseCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("release"))
            return false;

        if (!commandSender.hasPermission("movecraft.commands")
                || !commandSender.hasPermission("movecraft.commands.release")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (args.length == 0) {
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString(
                                "Player - Error - You do not have a craft to release!"));
                return true;
            }
            Player player = (Player) commandSender;
            final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

            if (pCraft == null) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString(
                                "Player - Error - You do not have a craft to release!"));
                return true;
            }
            CraftManager.getInstance().release(pCraft, CraftReleaseEvent.Reason.PLAYER, false);
            return true;
        }
        if (!commandSender.hasPermission("movecraft.commands.release.others")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - No Force Release"));
            return true;
        }
        if (args[0].equalsIgnoreCase("-p")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(name);
                if (pCraft != null)
                    CraftManager.getInstance().release(pCraft, CraftReleaseEvent.Reason.FORCE, false);
            }
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - Released All Player Crafts"));
            return true;
        }

        if (args[0].equalsIgnoreCase("-a")) {
            final List<Craft> craftsToRelease = new ArrayList<>(CraftManager.getInstance().getCrafts());
            for (Craft craft : craftsToRelease) {
                CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.FORCE, false);
            }
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - Released All Crafts"));
            return true;
        }

        if (args[0].equalsIgnoreCase("-n")) {
            final List<Craft> craftsToRelease = new ArrayList<>(CraftManager.getInstance().getCrafts());
            for (Craft craft : craftsToRelease) {
                if (!(craft instanceof PilotedCraft))
                    CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.FORCE, false);
            }
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - Released All Null Crafts"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Player - Not Found"));
            return true;
        }
        final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(args[0]);
        if (pCraft != null) {
            CraftManager.getInstance().release(pCraft, CraftReleaseEvent.Reason.FORCE, false);
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - Successful Force Release"));
            return true;
        }
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Player - Not Piloting"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length !=1 || !commandSender.hasPermission("movecraft.commands.release.others"))
            return Collections.emptyList();
        List<String> completions = new ArrayList<>();
        completions.add("-a");
        completions.add("-p");
        completions.add("-n");
        for(Player player : Bukkit.getOnlinePlayers()) {
            completions.add(player.getName());
        }

        List<String> returnValues = new ArrayList<>();
        for(String completion : completions) {
            if (completion.toLowerCase().startsWith(strings[strings.length - 1].toLowerCase()))
                returnValues.add(completion);
        }
        return returnValues;
    }
}

