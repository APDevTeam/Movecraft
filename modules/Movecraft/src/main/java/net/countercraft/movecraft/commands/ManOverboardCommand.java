package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ManOverboardCommand implements CommandExecutor{

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!command.getName().equalsIgnoreCase("manOverBoard")) {
            return false;
        }
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage("you need to be a player to get man-overboard");
            return true;
        }
        Player player = (Player) commandSender;

        if (CraftManager.getInstance().getCraftByPlayerName(player.getName()) != null) { //player is in craft
            Location telPoint = getCraftTeleportPoint(CraftManager.getInstance().getCraftByPlayerName(player.getName()));
            if (!CraftManager.getInstance().getCraftByPlayerName(player.getName()).getDisabled())
                player.teleport(telPoint);
            return true;
        }

        for(Craft playerCraft : CraftManager.getInstance()){
            if (playerCraft.getMovedPlayers().containsKey(player)) {
                if (playerCraft.getW() != player.getWorld()) {
                    player.sendMessage(I18nSupport.getInternationalisedString("Distance to craft is too far"));
                    return true;
                }
                if ((System.currentTimeMillis() - playerCraft.getMovedPlayers().get(player)) / 1_000 < Settings.ManOverBoardTimeout) {
                    player.sendMessage(I18nSupport.getInternationalisedString("You waited to long"));
                    return true;

                }
                Location telPoint = getCraftTeleportPoint(playerCraft);
                if (telPoint.distanceSquared(player.getLocation()) > 1_000_000) {
                    player.sendMessage(I18nSupport.getInternationalisedString("Distance to craft is too far"));
                    return true;
                }
                if (!CraftManager.getInstance().getCraftByPlayerName(player.getName()).getDisabled())
                    player.teleport(telPoint);
                return true;
            }
        }
        return true;
    }

    private Location getCraftTeleportPoint(Craft craft) {
        double telX = (craft.getHitBox().getMinX() + craft.getHitBox().getMaxX())/2D;
        double telZ = (craft.getHitBox().getMinZ() + craft.getHitBox().getMaxZ())/2D;
        double telY = craft.getHitBox().getMaxY();
        return new Location(craft.getW(), telX, telY, telZ);
    }
}
