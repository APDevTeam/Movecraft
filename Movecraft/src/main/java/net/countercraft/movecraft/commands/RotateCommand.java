package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.entity.Player;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

@CommandAlias("rotate")
@CommandPermission("movecraft.commands")
public class RotateCommand extends BaseCommand {

    @Subcommand("left")
    @CommandPermission("movecraft.commands.rotateleft")
    public static void rotateLeft(Player player) {
        final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
        if(craft==null){
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".rotate")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }

        CraftManager.getInstance().getCraftByPlayerName(player.getName()).rotate(MovecraftRotation.ANTICLOCKWISE, craft.getHitBox().getMidPoint());
    }

    @Subcommand("right")
    @CommandPermission("movecraft.commands.rotateright")
    public static void rotateRight(Player player) {
        final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
        if(craft==null){
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".rotate")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }

        CraftManager.getInstance().getCraftByPlayerName(player.getName()).rotate(MovecraftRotation.CLOCKWISE, craft.getHitBox().getMidPoint());
    }

    @Default
    @Syntax("[left|right]")
    public static void onCommand(Player player, String[] args) {

        if(args.length<1){
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Rotation - Specify Direction"));
            return;
        }

        player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Rotation - Invalid Direction"));
    }
}
