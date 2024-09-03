package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

@CommandAlias("release")
@CommandPermission("movecraft.commands,movecraft.commands.release")
public class ReleaseCommand extends BaseCommand {

    @Subcommand("-p")
    @Conditions("pilot_others")
    public static void releaseAllPlayers(CommandSender commandSender) {
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
    @Conditions("pilot_others")
    public static void releaseAllCrafts(CommandSender commandSender) {
        final List<Craft> craftsToRelease = new ArrayList<>(CraftManager.getInstance().getCrafts());
        for (Craft craft : craftsToRelease) {
            CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.FORCE, false);
        }

        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Release - Released All Crafts"));
    }

    @Subcommand("-n")
    @Conditions("pilot_others")
    public static void releaseAllNullCrafts(CommandSender commandSender) {
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
    @CommandCompletion("@players")
    public static void onCommand(CommandSender commandSender, String[] args) {

        if (args.length == 0) {
            if(!(commandSender instanceof Player player)) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString(
                                "Player - Error - You do not have a craft to release!"));
                return;
            }

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
}

