package net.countercraft.movecraft.towny;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.*;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.TownyUtils;
import net.countercraft.movecraft.utils.TownyWorldHeightLimits;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

public class TownyCompatManager implements Listener {

    @EventHandler
    public void onCraftTranslate(CraftTranslateEvent event){
        final Set<TownBlock> townBlocks = new HashSet<>();
        final Craft craft = event.getCraft();
        final TownyWorld townyWorld = TownyUtils.getTownyWorld(craft.getWorld());
        if (!Settings.TownyBlockMoveOnSwitchPerm)
            return;
        for (MovecraftLocation ml : event.getNewHitBox()) {

            TownBlock townBlock = TownyUtils.getTownBlock(ml.toBukkit(event.getCraft().getWorld()));
            if (townBlock == null || townBlocks.contains(townBlock)) {
                continue;
            }
            if (TownyUtils.validateCraftMoveEvent(craft.getNotificationPlayer(), ml.toBukkit(craft.getWorld()), townyWorld)) {
                townBlocks.add(townBlock);
                continue;
            }
            Town town = TownyUtils.getTown(townBlock);
            if (town == null)
                continue;
            final TownyWorldHeightLimits whLim = TownyUtils.getWorldLimits(event.getCraft().getWorld());
            final Location spawnLoc = TownyUtils.getTownSpawn(townBlock);
            if (whLim.validate(ml.getY(), spawnLoc.getBlockY())) {
                continue;
            }
            event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Towny - Translation Failed") + " %s @ %d,%d,%d", town.getName(), ml.getX(), ml.getY(), ml.getZ()));
            event.setCancelled(true);
            break;
        }

    }

    @EventHandler
    public void onCraftRotate(CraftRotateEvent event) {
        final Set<TownBlock> townBlocks = new HashSet<>();
        final Craft craft = event.getCraft();
        final TownyWorld townyWorld = TownyUtils.getTownyWorld(craft.getWorld());
        if (!Settings.TownyBlockMoveOnSwitchPerm)
            return;
        for (MovecraftLocation ml : event.getNewHitBox()) {

            TownBlock townBlock = TownyUtils.getTownBlock(ml.toBukkit(event.getCraft().getWorld()));
            if (townBlock == null || townBlocks.contains(townBlock)) {
                continue;
            }
            if (TownyUtils.validateCraftMoveEvent(craft.getNotificationPlayer(), ml.toBukkit(craft.getWorld()), townyWorld)) {
                townBlocks.add(townBlock);
                continue;
            }
            Town town = TownyUtils.getTown(townBlock);
            if (town == null)
                continue;
            final TownyWorldHeightLimits whLim = TownyUtils.getWorldLimits(event.getCraft().getWorld());
            final Location spawnLoc = TownyUtils.getTownSpawn(townBlock);
            if (whLim.validate(ml.getY(), spawnLoc.getBlockY())) {
                continue;
            }
            event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Towny - Rotation Failed") + " %s @ %d,%d,%d", town.getName(), ml.getX(), ml.getY(), ml.getZ()));
            event.setCancelled(true);
            break;
        }
    }

    @EventHandler
    public void onHitboxDetect(CraftDetectEvent event) {
        final Set<TownBlock> townBlocks = new HashSet<>();
        final Craft craft = event.getCraft();
        final TownyWorld townyWorld = TownyUtils.getTownyWorld(craft.getWorld());
        for (MovecraftLocation ml : event.getCraft().getHitBox()) {

            TownBlock townBlock = TownyUtils.getTownBlock(ml.toBukkit(event.getCraft().getWorld()));
            if (townBlock == null || townBlocks.contains(townBlock)) {
                continue;
            }
            if (TownyUtils.validateCraftMoveEvent(craft.getNotificationPlayer(), ml.toBukkit(craft.getWorld()), townyWorld)) {
                townBlocks.add(townBlock);
                continue;
            }
            Town town = TownyUtils.getTown(townBlock);
            if (town == null)
                continue;
            final TownyWorldHeightLimits whLim = TownyUtils.getWorldLimits(event.getCraft().getWorld());
            final Location spawnLoc = TownyUtils.getTownSpawn(townBlock);
            if (whLim.validate(ml.getY(), spawnLoc.getBlockY())) {
                continue;
            }
            event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Towny - Detection Failed") + " %s @ %d,%d,%d", town.getName(), ml.getX(), ml.getY(), ml.getZ()));
            event.setCancelled(true);
            break;
        }
    }

    @EventHandler
    public void onCraftSink(CraftSinkEvent event) {
        final Set<TownBlock> townBlocks = new HashSet<>();
        final Craft craft = event.getCraft();
        if (!Settings.TownyBlockSinkOnNoPVP)
            return;
        final HitBox hitBox = craft.getHitBox();
        if (hitBox.isEmpty())
            return;
        for (MovecraftLocation ml : hitBox) {

            TownBlock townBlock = TownyUtils.getTownBlock(ml.toBukkit(event.getCraft().getWorld()));
            if (townBlock == null || townBlocks.contains(townBlock)) {
                continue;
            }
            if (TownyUtils.validatePVP(townBlock)) {
                townBlocks.add(townBlock);
                continue;
            }
            final Player notifyP = craft.getNotificationPlayer();
            if (notifyP != null) {
                notifyP.sendMessage(String.format(I18nSupport.getInternationalisedString("Towny - Sinking a craft is not allowed in this town plot") + " @ %d,%d,%d", ml.getX(), ml.getY(), ml.getZ()));
            }
            event.setCancelled(true);
            break;
        }
    }
}
