package net.countercraft.movecraft.compatmanager;

import br.net.fabiozumbi12.RedProtect.Bukkit.API.RedProtectAPI;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.HashHitBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RedProtectCompatManager implements Listener {
    @EventHandler
    public void onCraftTranslate(CraftTranslateEvent event){
        Craft craft = event.getCraft();
        Player p = craft.getNotificationPlayer();
        RedProtectAPI redProtectAPI = new RedProtectAPI();

        Region toEnter = null;
        Region toExit = null;
        for (MovecraftLocation loc : event.getOldHitBox()){
            toExit = redProtectAPI.getRegion(loc.toBukkit(craft.getW()));
            if (toExit != null){
                break;
            }
        }
        for (MovecraftLocation loc : event.getNewHitBox()){
            toEnter = redProtectAPI.getRegion(loc.toBukkit(craft.getW()));
            if (toEnter != null){
                break;
            }
        }
        if (toEnter != null) {
            if (!toEnter.canBuild(p)&& Settings.RedProtectBlockMoveOnNoBuild) {
                event.setFailMessage("RedProtect - Translation Failed Player cannot build");
                event.setCancelled(true);
            }
            if (!toEnter.canEnter(p) && Settings.RedProtectBlockMoveOnNoEntry){
                event.setFailMessage("RedProtect - Translation Failed Player cannot enter");
                event.setCancelled(true);
            }
        }
        if (toExit != null){
            if (!toExit.canExit(p) && Settings.RedProtectBlockMoveOnNoExit){
                for (MovecraftLocation loc : event.getNewHitBox()) {
                    Region foundRegion = redProtectAPI.getRegion(loc.toBukkit(craft.getW()));
                    if (foundRegion == null || !foundRegion.equals(toExit)) {
                        event.setFailMessage("RedProtect - Translation Failed Player cannot exit");
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        }

    }
    @EventHandler
    public void onCraftRotate (CraftRotateEvent event){
        Craft craft = event.getCraft();
        Player p = craft.getNotificationPlayer();
        RedProtectAPI redProtectAPI = new RedProtectAPI();

        Region toEnter = null;
        Region toExit = null;
        for (MovecraftLocation loc : event.getOldHitBox()){
            toExit = redProtectAPI.getRegion(loc.toBukkit(craft.getW()));
            if (toExit != null){
                break;
            }
        }
        for (MovecraftLocation loc : event.getNewHitBox()){
            toEnter = redProtectAPI.getRegion(loc.toBukkit(craft.getW()));
            if (toEnter != null){
                break;
            }
        }
        if (toEnter != null) {
            if (!toEnter.canBuild(p)&& Settings.RedProtectBlockMoveOnNoBuild) {
                event.setFailMessage("RedProtect - Translation Failed Player cannot build");
                event.setCancelled(true);
            }
            if (!toEnter.canEnter(p) && Settings.RedProtectBlockMoveOnNoEntry){
                event.setFailMessage("RedProtect - Translation Failed Player cannot enter");
                event.setCancelled(true);
            }
        }
        if (toExit != null){
            if (!toExit.canExit(p) && Settings.RedProtectBlockMoveOnNoExit){
                for (MovecraftLocation loc : event.getNewHitBox()) {
                    Region foundRegion = redProtectAPI.getRegion(loc.toBukkit(craft.getW()));
                    if (foundRegion == null || !foundRegion.equals(toExit)) {
                        event.setFailMessage("RedProtect - Translation Failed Player cannot exit");
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        }

    }
    @EventHandler
    public void onCraftSink(CraftSinkEvent event){
        HashHitBox hitBox = event.getCraft().getHitBox();
        Region foundRegion = null;
        RedProtectAPI redProtectAPI = new RedProtectAPI();
        for (MovecraftLocation loc : hitBox){
            foundRegion = redProtectAPI.getRegion(loc.toBukkit(event.getCraft().getW()));
            if (foundRegion != null){
                break;
            }
        }
        if (foundRegion == null){
            return;
        }
        if (!foundRegion.canPVP(event.getCraft().getNotificationPlayer(),null) && Settings.RedProtectBlockSinkOnNoPvP){
            event.setFailMessage(I18nSupport.getInternationalisedString("RedProtect - Craft should sink but PvP is disabled"));
        }
    }
}
