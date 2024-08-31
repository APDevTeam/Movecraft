package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

@CommandAlias("cruise")
@CommandPermission("movecraft.commands,movecraft.commands.cruise")
public class CruiseCommand extends BaseCommand {

    @PreCommand
    public static boolean preCommand(CommandSender sender) {
        if(!(sender instanceof Player player))
            return true;

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

        return false;
    }

    @Default
    @Syntax("[on|off|DIRECTION]")
    @CommandCompletion("@directions")
    @Description("Starts your craft cruising")
    public static void onCommand(Player player, @Optional CruiseDirection direction) {
        final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

        //Resolver returns NONE on resolver fail, null if no argument at all
        if (direction != null && direction != CruiseDirection.NONE) {
            craft.setCruiseDirection(direction);
            craft.setCruising(true);
            return;
        } else if(craft.getCruising()) {
            craft.setCruising(false);
            return;
        }

        yawLocationCruising(player, craft);
    }

    @Subcommand("off")
    public static void offCruising(Player player) {
        final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
        if (craft == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return;
        }

        craft.setCruising(false);
    }

    @Subcommand("on")
    public static void onCruising(Player player) {
        final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
        yawLocationCruising(player, craft);
    }

    private static void yawLocationCruising(Player player, Craft craft) {

        // Normalize yaw from [-360, 360] to [0, 360]
        float yaw = (player.getLocation().getYaw() + 360.0f) % 360.0f;

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
    }
}
