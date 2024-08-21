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
@CommandPermission("movecraft.commands.cruise")
public class CruiseCommand extends BaseCommand {

    @PreCommand
    public static boolean preCommand(CommandSender sender, String[] args) {
        if(!(sender instanceof Player player))
            return false;

        final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
        if (craft == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return false;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".move")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return false;
        }
        if (!craft.getType().getBoolProperty(CraftType.CAN_CRUISE)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Cruise - Craft Cannot Cruise"));
            return false;
        }

        return true;
    }

    @Default
    @Syntax("[on|off|DIRECTION]")
    @CommandCompletion("@directions")
    @Description("Starts your craft cruising")
    public static void onCommand(Player player, CruiseDirection direction) {
        final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());


        /* Might use this over aikar's permission annotation after confirm it doesn't support multiple permission checks
        if (!player.hasPermission("movecraft.commands") || !player.hasPermission("movecraft.commands.cruise")) {
            craft.setCruising(false);
            return;
        }*/

        //Resolver returns NONE on fail
        if (direction != CruiseDirection.NONE) {
            craft.setCruiseDirection(direction);
            craft.setCruising(true);
            return;
        }

        if(craft.getCruising()){
            craft.setCruising(false);
            return;
        }
        // Normalize yaw from [-360, 360] to [0, 360]
        yawLocationCruising(player);
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
        yawLocationCruising(player);
    }

    private static void yawLocationCruising(Player player) {
        final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

        float yaw = (player.getLocation().getYaw() + 360.0f);
        if (yaw >= 360.0f) {
            yaw %= 360.0f;
        }
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
