package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.events.ManOverboardEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

@CommandAlias("manoverboard")
public class ManOverboardCommand extends BaseCommand {

    @Default
    @Description("If enabled, returns you to a craft you have fallen out of")
    public static void onCommand(Player player) {

        Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
        if (craft == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("ManOverboard - No Craft Found"));
            return;
        }

        Location telPoint = getCraftTeleportPoint(craft);
        if (craft.getWorld() != player.getWorld()) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("ManOverboard - Other World"));
            return;
        }

        if ((System.currentTimeMillis() -
                CraftManager.getInstance().getTimeFromOverboard(player)) / 1_000 > Settings.ManOverboardTimeout
                && !MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(player.getLocation()))) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("ManOverboard - Timed Out"));
            return;
        }

        if (telPoint.distanceSquared(player.getLocation()) > Settings.ManOverboardDistSquared) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("ManOverboard - Distance Too Far"));
            return;
        }

        if (craft.getDisabled() || craft instanceof SinkingCraft) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("ManOverboard - Disabled"));
            return;
        }

        ManOverboardEvent event = new ManOverboardEvent(craft, telPoint);
        Bukkit.getServer().getPluginManager().callEvent(event);

        telPoint.setYaw(player.getLocation().getYaw());
        telPoint.setPitch(player.getLocation().getPitch());
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0);
        Movecraft.getInstance().getSmoothTeleport().teleport(player, telPoint);
    }

    private static @NotNull Location getCraftTeleportPoint(@NotNull Craft craft) {
        double telX = ((craft.getHitBox().getMinX() + craft.getHitBox().getMaxX()) / 2D) + 0.5D;
        double telZ = ((craft.getHitBox().getMinZ() + craft.getHitBox().getMaxZ()) / 2D) + 0.5D;
        double telY = craft.getHitBox().getMaxY() + 1;
        return new Location(craft.getWorld(), telX, telY, telZ);
    }
}
