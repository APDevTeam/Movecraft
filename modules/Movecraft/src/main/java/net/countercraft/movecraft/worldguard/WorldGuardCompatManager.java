package net.countercraft.movecraft.worldguard;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.LegacyUtils;
import net.countercraft.movecraft.utils.WorldguardUtils;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;

public class WorldGuardCompatManager implements Listener {
    public static StateFlag FLAG_PILOT = new StateFlag("movecraft-pilot", true);
    public static StateFlag FLAG_MOVE = new StateFlag("movecraft-move", true);
    public static StateFlag FLAG_ROTATE = new StateFlag("movecraft-rotate", true);
    public static StateFlag FLAG_SINK = new StateFlag("movecraft-sink", true);

    public static void registerFlags() {
        FlagRegistry flags;
        if (Settings.IsLegacy) {
            try {
                final Method getFlagRegistry = WorldGuardPlugin.class.getDeclaredMethod("getFlagRegistry");
                flags = (FlagRegistry) getFlagRegistry.invoke(Movecraft.getInstance().getWorldGuardPlugin());
            } catch (Exception e) {
                return;
            }

        } else {
            flags = WorldGuard.getInstance().getFlagRegistry();
        }
        Movecraft.getInstance().getLogger().info("Registered custom flags");
        flags.register(FLAG_MOVE);
        flags.register(FLAG_PILOT);
        flags.register(FLAG_ROTATE);
        flags.register(FLAG_SINK);
    }

    @EventHandler
    public void onCraftTranslateEvent(CraftTranslateEvent event){
        if(!Settings.WorldGuardBlockMoveOnBuildPerm)
            return;
        if(event.getCraft().getNotificationPlayer() == null)
            return;
        for(MovecraftLocation location : event.getNewHitBox()){
            final ApplicableRegionSet regions = WorldguardUtils.getRegionsAt(location.toBukkit(event.getCraft().getWorld()));
            final LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(event.getCraft().getNotificationPlayer());
            final StateFlag.State state = regions.queryValue(lp, FLAG_MOVE);
            if (state == StateFlag.State.ALLOW) {
                continue;
            } else if (state == StateFlag.State.DENY) {
                event.setFailMessage(I18nSupport.getInternationalisedString("WGCustomFlags - Translation Failed"));
                event.setCancelled(true);
                break;
            }
            else if (!pilotHasAccessToRegion(event.getCraft().getNotificationPlayer(), location, event.getCraft().getWorld())){
                event.setCancelled(true);
                event.setFailMessage(String.format( I18nSupport.getInternationalisedString( "Translation - WorldGuard - Not Permitted To Build" )+" @ %d,%d,%d", location.getX(), location.getY(), location.getZ() ) );
                break;
            }

        }
    }

    @EventHandler
    public void onCraftRotateEvent(CraftRotateEvent event){

        if(event.getCraft().getNotificationPlayer() == null)
            return;
        for(MovecraftLocation location : event.getNewHitBox()){
            final ApplicableRegionSet regions = WorldguardUtils.getRegionsAt(location.toBukkit(event.getCraft().getWorld()));
            final LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(event.getCraft().getNotificationPlayer());
            if (regions.queryValue(lp, FLAG_ROTATE) == StateFlag.State.ALLOW) {
                continue;
            } else if (regions.queryValue(lp, FLAG_ROTATE) == StateFlag.State.DENY) {
                event.setFailMessage(I18nSupport.getInternationalisedString("WGCustomFlags - Rotation Failed"));
                event.setCancelled(true);
                break;
            } else if(!pilotHasAccessToRegion(event.getCraft().getNotificationPlayer(), location, event.getCraft().getWorld())){
                event.setCancelled(true);
                event.setFailMessage(String.format( I18nSupport.getInternationalisedString("Rotation - WorldGuard - Not Permitted To Build" )+" @ %d,%d,%d", location.getX(), location.getY(), location.getZ()));
                break;
            }
        }
    }
    private boolean pilotHasAccessToRegion(Player player, MovecraftLocation location, World world){
        if(!Settings.WorldGuardBlockMoveOnBuildPerm)
            return true;
        ApplicableRegionSet regions = WorldguardUtils.getRegionsAt(location.toBukkit(world));
        if (Settings.IsLegacy){
            return LegacyUtils.canBuild(Movecraft.getInstance().getWorldGuardPlugin(), world, location, player);
        } else {

            final RegionAssociable associable = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(player);
            return regions.queryValue(associable, Flags.BUILD) == StateFlag.State.ALLOW;
        }
    }


    @EventHandler
    public void onCraftSink(CraftSinkEvent event){
        Craft pcraft = event.getCraft();
        Player notifyP = event.getCraft().getNotificationPlayer();
        for (MovecraftLocation location : pcraft.getHitBox()) {
            ApplicableRegionSet regions = WorldguardUtils.getRegionsAt(location.toBukkit(pcraft.getWorld()));
            final LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(event.getCraft().getNotificationPlayer());
            final StateFlag.State state = regions.queryValue(lp, FLAG_SINK);
            if (state == StateFlag.State.ALLOW) {
                continue;
            } else if (state == StateFlag.State.DENY) {
                notifyP.sendMessage(I18nSupport.getInternationalisedString("WGCustomFlags - Sinking a craft is not allowed in this WorldGuard region"));
                event.setCancelled(true);
                break;
            } else if (Settings.WorldGuardBlockSinkOnPVPPerm && !WorldguardUtils.pvpAllowed(regions)) {
                notifyP.sendMessage(I18nSupport.getInternationalisedString("Player- Craft should sink but PVP is not allowed in this WorldGuard region"));
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event) {
        Craft pcraft = event.getCraft();
        Player notifyP = pcraft.getNotificationPlayer();
        for (MovecraftLocation location : pcraft.getHitBox()) {
            ApplicableRegionSet regions = WorldguardUtils.getRegionsAt(location.toBukkit(pcraft.getWorld()));
            final LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(event.getCraft().getNotificationPlayer());
            final StateFlag.State state = regions.queryValue(lp, FLAG_PILOT);
            if (state == StateFlag.State.DENY) {
                notifyP.sendMessage(I18nSupport.getInternationalisedString("WGCustomFlags - Detection Failed"));
                event.setCancelled(true);
                break;
            }
        }

    }
}
