package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

@CommandAlias("release")
@CommandPermission("movecraft.commands,movecraft.commands.release")
public class ReleaseCommand extends BaseCommand {

    @Subcommand("-p")
    public static void releaseAllPlayers(CommandSender commandSender) {
        if (!commandSender.hasPermission("movecraft.commands.release.others")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - No Force Release"));
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            String name = p.getName();
            final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(name);
            if (pCraft != null)
                CraftManager.getInstance().release(pCraft, CraftReleaseEvent.Reason.FORCE, false);
        }
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Release - Released All Player Crafts"));
    }

    @Subcommand("-a")
    public static void releaseAllCrafts(CommandSender commandSender) {
        if (!commandSender.hasPermission("movecraft.commands.release.others")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - No Force Release"));
            return;
        }

        final List<Craft> craftsToRelease = new ArrayList<>(CraftManager.getInstance().getCrafts());
        for (Craft craft : craftsToRelease) {
            CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.FORCE, false);
        }
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Release - Released All Crafts"));
    }

    @Subcommand("-n")
    public static void releaseAllNullCrafts(CommandSender commandSender) {
        if (!commandSender.hasPermission("movecraft.commands.release.others")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - No Force Release"));
            return;
        }

        final List<Craft> craftsToRelease = new ArrayList<>(CraftManager.getInstance().getCrafts());
        for (Craft craft : craftsToRelease) {
            if (!(craft instanceof PilotedCraft))
                CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.FORCE, false);
        }
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Release - Released All Null Crafts"));
    }

    @Default
    @Syntax("<-p|-a|-n|player>")
    public static void onCommand(CommandSender commandSender, String[] args) {

        if (args.length == 0) {
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString(
                                "Player - Error - You do not have a craft to release!"));
                return;
            }
            Player player = (Player) commandSender;
            final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

            if (pCraft == null) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString(
                                "Player - Error - You do not have a craft to release!"));
                return;
            }
            CraftManager.getInstance().release(pCraft, CraftReleaseEvent.Reason.PLAYER, false);
            return;
        }

        if (!commandSender.hasPermission("movecraft.commands.release.others")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - No Force Release"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Player - Not Found"));
            return;
        }
        final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(args[0]);
        if (pCraft != null) {
            CraftManager.getInstance().release(pCraft, CraftReleaseEvent.Reason.FORCE, false);
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Release - Successful Force Release"));
            return;
        }
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Player - Not Piloting"));
    }

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

