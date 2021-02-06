package net.countercraft.movecraft.worldguard;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WorldGuardCompatManager implements Listener {
    @EventHandler
    public void onCraftTranslateEvent(CraftTranslateEvent event){
        if(!Settings.WorldGuardBlockMoveOnBuildPerm)
            return;
        if(event.getCraft().getNotificationPlayer() == null)
            return;
        for(MovecraftLocation location : event.getNewHitBox()){
            if(!Movecraft.getInstance().getWorldGuardPlugin().canBuild(event.getCraft().getNotificationPlayer(),location.toBukkit(event.getCraft().getW()))){
                event.setCancelled(true);
                event.setFailMessage(String.format( I18nSupport.getInternationalisedString( "Translation - WorldGuard - Not Permitted To Build" )+" @ %d,%d,%d", location.getX(), location.getY(), location.getZ() ) );
                return;
            }
        }
    }

    @EventHandler
    public void onCraftRotateEvent(CraftRotateEvent event){
        if(!Settings.WorldGuardBlockMoveOnBuildPerm)
            return;
        if(event.getCraft().getNotificationPlayer() == null)
            return;
        for(MovecraftLocation location : event.getNewHitBox()){
            if(!Movecraft.getInstance().getWorldGuardPlugin().canBuild(event.getCraft().getNotificationPlayer(), location.toBukkit(event.getCraft().getW()))){
                event.setCancelled(true);
                event.setFailMessage(String.format( I18nSupport.getInternationalisedString("Rotation - WorldGuard - Not Permitted To Build" )+" @ %d,%d,%d", location.getX(), location.getY(), location.getZ()));
                return;
            }
        }
    }

}
