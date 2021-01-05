package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class ManOverboardCommand implements CommandExecutor{

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!command.getName().equalsIgnoreCase("manOverBoard")) {
            return false;
        }
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("ManOverboard - Must Be Player"));
            return true;
        }
        Player player = (Player) commandSender;
        Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

        if(craft == null){
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("ManOverboard - No Craft Found"));
            return true;
        }

        Location telPoint = craft.getCrewSigns().containsKey(player.getUniqueId()) ? craft.getCrewSigns().get(player.getUniqueId()) : getCraftTeleportPoint(craft);
        if (craft.getW() != player.getWorld()) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("ManOverboard - Other World"));
            return true;
        }

        if ((System.currentTimeMillis() - CraftManager.getInstance().getTimeFromOverboard(player)) / 1_000 > Settings.ManOverboardTimeout
                && !MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(player.getLocation()))) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("ManOverboard - Timed Out"));
            return true;
        }

        if (telPoint.distanceSquared(player.getLocation()) > Settings.ManOverboardDistSquared) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("ManOverboard - Distance Too Far"));
            return true;
        }

        if (craft.getDisabled() || craft.getSinking() || CraftManager.getInstance().getPlayerFromCraft(craft) == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("ManOverboard - Disabled"));
            return true;
        }

        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0);
        player.teleport(telPoint);
        return true;
    }

    private Location getCraftTeleportPoint(Craft craft) {
        double telX = (craft.getHitBox().getMinX() + craft.getHitBox().getMaxX())/2D + 0.5D;
        double telZ = (craft.getHitBox().getMinZ() + craft.getHitBox().getMaxZ())/2D + 0.5D;
        double telY = craft.getHitBox().getMaxY() + 1;
        return new Location(craft.getW(), telX, telY, telZ);
    }
}
