package net.countercraft.movecraft.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
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
            if (canMoveInRegion(location, event.getCraft().getWorld())) {
                event.setFailMessage(I18nSupport.getInternationalisedString("WGCustomFlags - Translation Failed"));
                event.setCancelled(true);
                return;
            }

            if(!pilotHasAccessToRegion(event.getCraft().getNotificationPlayer(), location, event.getCraft().getWorld())){
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
            if (!canRotateInRegion(location, event.getCraft().getWorld())) {
                event.setFailMessage(I18nSupport.getInternationalisedString("WGCustomFlags - Rotation Failed"));
                event.setCancelled(true);
                return;
            }
            if(!pilotHasAccessToRegion(event.getCraft().getNotificationPlayer(), location, event.getCraft().getWorld())){
                event.setCancelled(true);
                event.setFailMessage(String.format( I18nSupport.getInternationalisedString("Rotation - WorldGuard - Not Permitted To Build" )+" @ %d,%d,%d", location.getX(), location.getY(), location.getZ()));
                return;
            }
        }
    }
    private boolean pilotHasAccessToRegion(Player player, MovecraftLocation location, World world){
        if (Settings.IsLegacy){
            return LegacyUtils.canBuild(Movecraft.getInstance().getWorldGuardPlugin(), world, location, player);
        } else {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            LocalPlayer lPlayer = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(player);
            com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
            Location wgLoc = new Location(weWorld, location.getX(), location.getY(), location.getZ());
            ApplicableRegionSet regions = query.getApplicableRegions(wgLoc);
            return regions.isOwnerOfAll(lPlayer) || regions.isMemberOfAll(lPlayer) ||
                    player.hasPermission("worldguard.build.*");
        }
    }

    private boolean canRotateInRegion(MovecraftLocation location, World world) {
        RegionManager manager;
        ApplicableRegionSet regions;
        if (Settings.IsLegacy) {
            manager = LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), world);
            regions = LegacyUtils.getApplicableRegions(manager, location.toBukkit(world));
        } else {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            manager = container.get(BukkitAdapter.adapt(world));
            regions = manager.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
        }
        for (ProtectedRegion region : regions) {
            if (region.getFlag(FLAG_ROTATE) == StateFlag.State.DENY)
                return false;
        }
        return true;
    }

    private boolean canMoveInRegion(MovecraftLocation location, World world) {
        RegionManager manager;
        if (Settings.IsLegacy) {
            manager = LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), world);
        } else {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            manager = container.get(BukkitAdapter.adapt(world));
        }
        if (manager == null) {
            return true;
        }
        ApplicableRegionSet regions = Settings.IsLegacy ? LegacyUtils.getApplicableRegions(manager, location.toBukkit(world)) : manager.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
        for (ProtectedRegion region : regions) {
            if (region.getFlag(FLAG_MOVE) == StateFlag.State.DENY)
                return false;
        }
        return true;
    }

    @EventHandler
    public void onCraftSink(CraftSinkEvent event){
        Craft pcraft = event.getCraft();
        Player notifyP = event.getCraft().getNotificationPlayer();
        if (Movecraft.getInstance().getWorldGuardPlugin() != null){
            WorldGuardPlugin wgPlugin = Movecraft.getInstance().getWorldGuardPlugin();
            ProtectedRegion region = null;
            RegionManager regionManager;
            if (Settings.IsLegacy){
                regionManager = LegacyUtils.getRegionManager(wgPlugin, pcraft.getWorld());
            } else {
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                regionManager = container.get(BukkitAdapter.adapt(pcraft.getWorld()));
            }
            for (MovecraftLocation location : pcraft.getHitBox()){
                ApplicableRegionSet regions;
                if (Settings.IsLegacy)
                    regions = LegacyUtils.getApplicableRegions(regionManager, location.toBukkit(pcraft.getWorld()));
                else {
                    regions = regionManager.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
                }
                for (ProtectedRegion pr : regions.getRegions()){
                    if (WorldguardUtils.pvpAllowed(pr) || pr.getFlag(FLAG_SINK) == StateFlag.State.DENY){
                        region = pr;
                        break;
                    }
                }
                if (region != null){
                    break;
                }
            }
            if (region == null) {
                return;
            }
            if (notifyP == null) {
                return;
            }
            if (Settings.WorldGuardBlockSinkOnPVPPerm && !WorldguardUtils.pvpAllowed(region)){
                notifyP.sendMessage(I18nSupport.getInternationalisedString("Player- Craft should sink but PVP is not allowed in this WorldGuard region"));
                event.setCancelled(true);
            }
            if (region.getFlag(FLAG_SINK) == StateFlag.State.DENY) {
                notifyP.sendMessage(I18nSupport.getInternationalisedString("WGCustomFlags - Sinking a craft is not allowed in this WorldGuard region"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event) {
        Craft pcraft = event.getCraft();
        Player notifyP = pcraft.getNotificationPlayer();
        WorldGuardPlugin wgPlugin = Movecraft.getInstance().getWorldGuardPlugin();
        ProtectedRegion region = null;
        RegionManager regionManager;
        if (Settings.IsLegacy){
            regionManager = LegacyUtils.getRegionManager(wgPlugin, pcraft.getWorld());
        } else {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            regionManager = container.get(BukkitAdapter.adapt(pcraft.getWorld()));
        }
        for (MovecraftLocation location : pcraft.getHitBox()){
            ApplicableRegionSet regions;
            if (Settings.IsLegacy)
                regions = LegacyUtils.getApplicableRegions(regionManager, location.toBukkit(pcraft.getWorld()));
            else {
                regions = regionManager.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
            }
            for (ProtectedRegion pr : regions.getRegions()){
                if (pr.getFlag(FLAG_PILOT) == StateFlag.State.DENY){
                    region = pr;
                    break;
                }
            }
            if (region != null){
                break;
            }
        }
        if (region == null) {
            return;
        }
        if (notifyP == null) {
            return;
        }
        if (region.getFlag(FLAG_PILOT) == StateFlag.State.DENY) {
            notifyP.sendMessage(I18nSupport.getInternationalisedString("WGCustomFlags - Detection Failed"));
            event.setCancelled(true);
        }
    }
}
