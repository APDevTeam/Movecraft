package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

@CommandAlias("movecraft")
@CommandPermission("movecraft.commands.movecraft")
public class MovecraftCommand extends BaseCommand {

    @Default
    @Syntax("<reloadtypes>")
    public static void displayAuthors(CommandSender commandSender) {
        PluginDescriptionFile descriptionFile = Movecraft.getInstance().getDescription();
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Movecraft " + descriptionFile.getVersion() + " by " + descriptionFile.getAuthors());
    }

    @CommandPermission("movecraft.commands.movecraft.reloadtypes")
    @Subcommand("reloadtypes")
    public static void reloadTypes(CommandSender commandSender) {
        CraftManager.getInstance().reloadCraftTypes();
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Movecraft - Reloaded Types"));
    }
}
