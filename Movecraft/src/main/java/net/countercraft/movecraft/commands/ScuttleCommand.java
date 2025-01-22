package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftScuttleEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

@CommandAlias("scuttle")
public class ScuttleCommand extends BaseCommand {

    @Default
    @Syntax("<player>")
    @Description("Sinks piloted craft")
    @CommandCompletion("@players")
    public static void onCommand(CommandSender commandSender, String[] strings) {

        Craft craft = null;
        // Scuttle other player
        if (commandSender.hasPermission("movecraft.commands.scuttle.others") && strings.length >= 1) {
            Player player = Bukkit.getPlayer(strings[0]);
            if (player == null) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString("Scuttle - Must Be Online"));
                return;
            }
            craft = CraftManager.getInstance().getCraftByPlayer(player);
        }
        else if (commandSender.hasPermission("movecraft.commands.scuttle.self") && strings.length == 0) {
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString("Scuttle - Must Be Player"));
                return;
            }
            craft = CraftManager.getInstance().getCraftByPlayer(player);
        }
        if (craft == null) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return;
        }
        if (craft instanceof SinkingCraft) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Scuttle - Craft Already Sinking"));
            return;
        }
        if (!commandSender.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME)
                + ".scuttle")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }

        CraftScuttleEvent e = new CraftScuttleEvent(craft, (Player) commandSender);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (e.isCancelled())
            return;

        craft.setCruising(false);
        CraftManager.getInstance().sink(craft);
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Scuttle - Scuttle Activated"));
    }
}

