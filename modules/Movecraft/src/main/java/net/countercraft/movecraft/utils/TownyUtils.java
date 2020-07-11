package net.countercraft.movecraft.utils;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.war.flagwar.TownyWarConfig;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author mwkaicz <mwkaicz@gmail.com>
 */
public class TownyUtils {
    public static final String TOWN_MIN = "worldMin";
    public static final String TOWN_MAX = "worldMax";
    public static final String TOWN_ABOVE = "aboveTownSpawn";
    public static final String TOWN_UNDER = "underTownSpawn";
    public static final String TOWN_HEIGHT_LIMITS = "TownyWorldHeightLimits";

    private static Method GET_CACHE_PERMISSION_NON_LEGACY;

    static {
        try {
            GET_CACHE_PERMISSION_NON_LEGACY = PlayerCacheUtil.class.getDeclaredMethod("getCachePermission", Player.class, Location.class, Material.class, TownyPermission.ActionType.class);
        } catch (NoSuchMethodException e) {
            GET_CACHE_PERMISSION_NON_LEGACY = null;
        }
    }

    public static TownyWorld getTownyWorld(World w) {
        TownyWorld tw;
        try {
            tw = TownyUniverse.getDataSource().getWorld(w.getName());
            if (!tw.isUsingTowny())
                return null;
        } catch (NotRegisteredException e) {
            return null;
        }
        return tw;
    }

    public static TownBlock getTownBlock(Location loc) {
        Coord coo = Coord.parseCoord(loc);
        TownyWorld tw = getTownyWorld(loc.getWorld());
        TownBlock tb = null;
        try {
            if (tw != null) {
                tb = tw.getTownBlock(coo);
            }
        } catch (NotRegisteredException ex) {
            //Logger.getLogger(TownyUtils.class.getName()).log(Level.SEVERE, null, ex);
            //free land
        }
        return tb;
    }

    public static Town getTown(TownBlock townBlock) {
        try {
            Town town;
            town = townBlock.getTown();
            return town;
        } catch (TownyException e) {
            //Logger.getLogger(TownyUtils.class.getName()).log(Level.SEVERE, null, ex);
            //none
        }
        return null;
    }

    public static Location getTownSpawn(TownBlock townBlock) {
        if (townBlock == null) return null;
        Town t = getTown(townBlock);
        if (t != null) {
            try {
                return t.getSpawn();
            } catch (TownyException ex) {
                Movecraft.getInstance().getLogger().log(Level.SEVERE, String.format(I18nSupport.getInternationalisedString("Towny - Spawn Not Found"),t.getName()), ex);
                //town hasn't spawn
            }
        }
        return null;
    }


    public static boolean validateResident(Player player) {
        Resident resident;
        try {
            resident = TownyUniverse.getDataSource().getResident(player.getName());
            return true;
        } catch (TownyException e) {
            //System.out.print("Failed to fetch resident: " + player.getName());
            //return TownBlockStatus.NOT_REGISTERED;
        }
        return false;
    }

    public static boolean validateCraftMoveEvent(Player player, Location loc, TownyWorld world) {
        // Get switch permissions (updates if none exist)
        if (player != null && !validateResident(player)) {
            return true; //probably NPC or CBWrapper Dummy player
        }
        Material type = Material.STONE_BUTTON;
        byte data = 0;

        boolean bSwitch;
        if (GET_CACHE_PERMISSION_NON_LEGACY != null) {
            try {
                bSwitch = (boolean) GET_CACHE_PERMISSION_NON_LEGACY.invoke(player, loc, type, TownyPermission.ActionType.SWITCH);
            } catch (IllegalAccessException | InvocationTargetException e) {
                bSwitch = true;
            }
        } else {
            bSwitch = PlayerCacheUtil.getCachePermission(player, loc, type.getId(), data, TownyPermission.ActionType.SWITCH);
        }


        // Allow move if we are permitted to switch
        if (bSwitch)
            return true;

        PlayerCache cache = Movecraft.getInstance().getTownyPlugin().getCache(player);
        TownBlockStatus status = cache.getStatus();

        return !cache.hasBlockErrMsg() && status == TownBlockStatus.WARZONE && TownyWarConfig.isAllowingSwitchesInWarZone();

    }

    public static boolean validatePVP(TownBlock tb) {
        Town t = getTown(tb);
        if (t != null) {
            return t.getPermissions().pvp || tb.getPermissions().pvp;
        } else {
            return tb.getPermissions().pvp;
        }
    }

    public static boolean validateExplosion(TownBlock tb) {
        Town t = getTown(tb);
        if (t != null) {
            return t.getPermissions().explosion || tb.getPermissions().explosion;
        } else {
            return tb.getPermissions().explosion;
        }
    }

    public static void initTownyConfig() {
        Settings.TownProtectionHeightLimits = getTownyConfigFromUniverse();
        loadTownyConfig();
    }

    private static Map<String, TownyWorldHeightLimits> getTownyConfigFromUniverse() {
        Map<String, TownyWorldHeightLimits> worldsMap = new HashMap<>();
        List<World> worlds = Movecraft.getInstance().getServer().getWorlds();
        for (World w : worlds) {
            TownyWorld tw = getTownyWorld(w);
            if (tw != null) {
                if (tw.isUsingTowny()) {
                    worldsMap.put(w.getName(), new TownyWorldHeightLimits());
                }
            }
        }
        return worldsMap;
    }

    public static TownyWorldHeightLimits getWorldLimits(World w) {
        boolean oNew = false;
        String wName = w.getName();
        Map<String, TownyWorldHeightLimits> worldsMap = Settings.TownProtectionHeightLimits;
        if (!worldsMap.containsKey(wName)) {
            TownyWorld tw = getTownyWorld(w);
            if (tw != null) {
                if (tw.isUsingTowny()) {
                    worldsMap.put(wName, new TownyWorldHeightLimits());
                    Settings.TownProtectionHeightLimits = worldsMap;
                    Movecraft.getInstance().getLogger().log(Level.INFO, I18nSupport.getInternationalisedString("Towny - Added Config Defaults"), w.getName());
                    oNew = true;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        if (oNew) {
            saveWorldLimits();
        }
        return worldsMap.get(wName);
    }

    private static void saveWorldLimits() {
        Map<String, TownyWorldHeightLimits> worldsMap = Settings.TownProtectionHeightLimits;
        Map<String, Object> townyWorldsMap = new HashMap<>();
        Set<String> worlds = worldsMap.keySet();

        for (String world : worlds) {
            World w = Movecraft.getInstance().getServer().getWorld(world);
            if (w != null) {
                TownyWorld tw = getTownyWorld(w);
                if (tw != null) {
                    if (tw.isUsingTowny()) {
                        Map<String, Integer> townyTWorldMap = new HashMap<>();
                        TownyWorldHeightLimits twhl = worldsMap.get(world);
                        townyTWorldMap.put(TOWN_MIN, twhl.world_min);
                        townyTWorldMap.put(TOWN_MAX, twhl.world_max);
                        townyTWorldMap.put(TOWN_ABOVE, twhl.above_town);
                        townyTWorldMap.put(TOWN_UNDER, twhl.under_town);
                        townyWorldsMap.put(world, townyTWorldMap);
                        Movecraft.getInstance().getConfig().set(TOWN_HEIGHT_LIMITS + "." + world, townyTWorldMap);
                    }
                }
            }
        }
        Movecraft.getInstance().saveConfig();
        Movecraft.getInstance().getLogger().log(Level.INFO, I18nSupport.getInternationalisedString("Towny - Saved Settings"));
    }

    private static void loadTownyConfig() {
        FileConfiguration fc = Movecraft.getInstance().getConfig();
        ConfigurationSection csObj = fc.getConfigurationSection(TOWN_HEIGHT_LIMITS);
        Map<String, TownyWorldHeightLimits> townyWorldHeightLimits = new HashMap<>();

        if (csObj != null) {
            Set<String> worlds = csObj.getKeys(false);
            for (String worldName : worlds) {
                TownyWorldHeightLimits twhl = new TownyWorldHeightLimits();
                twhl.world_min = fc.getInt(TOWN_HEIGHT_LIMITS + "." + worldName + "." + TOWN_MIN, TownyWorldHeightLimits.DEFAULT_WORLD_MIN);
                twhl.world_max = fc.getInt(TOWN_HEIGHT_LIMITS + "." + worldName + "." + TOWN_MAX, TownyWorldHeightLimits.DEFAULT_WORLD_MAX);
                twhl.above_town = fc.getInt(TOWN_HEIGHT_LIMITS + "." + worldName + "." + TOWN_ABOVE, TownyWorldHeightLimits.DEFAULT_TOWN_ABOVE);
                twhl.under_town = fc.getInt(TOWN_HEIGHT_LIMITS + "." + worldName + "." + TOWN_UNDER, TownyWorldHeightLimits.DEFAULT_TOWN_UNDER);
                townyWorldHeightLimits.put(worldName, twhl);
                Movecraft.getInstance().getLogger().log(Level.INFO, I18nSupport.getInternationalisedString("Towny - Loaded Settings"), worldName);
            }
        }
        if (!townyWorldHeightLimits.equals(Settings.TownProtectionHeightLimits)) {
            Settings.TownProtectionHeightLimits.putAll(townyWorldHeightLimits);
            saveWorldLimits();
        }
    }
}
