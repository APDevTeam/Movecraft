/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft;

import at.pavlov.cannons.Cannons;
import com.earth2me.essentials.Essentials;
import com.mewin.WGCustomFlags.WGCustomFlagsPlugin;
import com.palmergames.bukkit.towny.Towny;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.commands.*;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.listener.BlockListener;
import net.countercraft.movecraft.listener.InteractListener;
import net.countercraft.movecraft.listener.PlayerListener;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.repair.RepairManager;
import net.countercraft.movecraft.sign.*;
import net.countercraft.movecraft.utils.TownyUtils;
import net.countercraft.movecraft.utils.WGCustomFlagsUtils;
import net.countercraft.movecraft.warfare.assault.AssaultManager;
import net.countercraft.movecraft.warfare.siege.Siege;
import net.countercraft.movecraft.warfare.siege.SiegeManager;
import net.countercraft.movecraft.worldguard.WorldGuardCompatManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Movecraft extends JavaPlugin {
    public static StateFlag FLAG_PILOT = null; //new StateFlag("movecraft-pilot", true);
    public static StateFlag FLAG_MOVE = null; //new StateFlag("movecraft-move", true);
    public static StateFlag FLAG_ROTATE = null; //new StateFlag("movecraft-rotate", true);
    public static StateFlag FLAG_SINK = null; //new StateFlag("movecraft-sink", true);
    private static Movecraft instance;
    private static WorldGuardPlugin worldGuardPlugin;
    private static WorldEditPlugin worldEditPlugin;
    private static WGCustomFlagsPlugin wgCustomFlagsPlugin = null;
    private static Economy economy;
    private static Cannons cannonsPlugin = null;
    private static Towny townyPlugin = null;
    private static Essentials essentialsPlugin = null;
    /*public HashMap<MovecraftLocation, Long> blockFadeTimeMap = new HashMap<>();
    public HashMap<MovecraftLocation, Integer> blockFadeTypeMap = new HashMap<>();
    public HashMap<MovecraftLocation, Boolean> blockFadeWaterMap = new HashMap<>();
    public HashMap<MovecraftLocation, World> blockFadeWorldMap = new HashMap<>();*/
    private Logger logger;
    private boolean shuttingDown;
    private WorldHandler worldHandler;


    private AsyncManager asyncManager;
    private AssaultManager assaultManager;
    private SiegeManager siegeManager;
    private RepairManager repairManager;

    public static synchronized Movecraft getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
    }

    @Override
    public void onEnable() {
        // Read in config
        this.saveDefaultConfig();
        try {
            Class.forName("com.destroystokyo.paper.Title");
            Settings.IsPaper = true;
        }catch (Exception e){
            Settings.IsPaper=false;
        }


        Settings.LOCALE = getConfig().getString("Locale");
        Settings.RestrictSiBsToRegions = getConfig().getBoolean("RestrictSiBsToRegions", false);
        Settings.Debug = getConfig().getBoolean("Debug", false);
        Settings.DisableSpillProtection = getConfig().getBoolean("DisableSpillProtection", false);
        // if the PilotTool is specified in the config.yml file, use it
        if (getConfig().getInt("PilotTool") != 0) {
            logger.log(Level.INFO, "Recognized PilotTool setting of: "
                    + getConfig().getInt("PilotTool"));
            Settings.PilotTool = getConfig().getInt("PilotTool");
        } else {
            logger.log(Level.INFO, "No PilotTool setting, using default of 280");
        }
        // if the CompatibilityMode is specified in the config.yml file, use it.
        // Otherwise set to false.
        /*Settings.CompatibilityMode = getConfig().getBoolean("CompatibilityMode", false);
        if (!Settings.CompatibilityMode) {
            try {
                Class.forName("net.minecraft.server.v1_10_R1.Chunk");
            } catch (ClassNotFoundException e) {
                Settings.CompatibilityMode = true;
                logger.log(Level.INFO, "WARNING: CompatibilityMode was set to false, but required build-specific classes were not found. FORCING COMPATIBILITY MODE");
            }
        }
        logger.log(Level.INFO, "CompatiblityMode is set to {0}", Settings.CompatibilityMode);*/
        //Switch to interfaces
        String packageName = this.getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        try {
            final Class<?> clazz = Class.forName("net.countercraft.movecraft.compat." + version + ".IWorldHandler");
            // Check if we have a NMSHandler class at that location.
            if (WorldHandler.class.isAssignableFrom(clazz)) { // Make sure it actually implements NMS
                this.worldHandler = (WorldHandler) clazz.getConstructor().newInstance(); // Set our handler
            }
        } catch (final Exception e) {
            e.printStackTrace();
            this.getLogger().severe("Could not find support for this version.");
            this.setEnabled(false);
            return;
        }
        this.getLogger().info("Loading support for " + version);


        Settings.SinkCheckTicks = getConfig().getDouble("SinkCheckTicks", 100.0);
        Settings.TracerRateTicks = getConfig().getDouble("TracerRateTicks", 5.0);
        Settings.TracerMinDistanceSqrd = getConfig().getLong("TracerMinDistance", 60);
        Settings.TracerMinDistanceSqrd *= Settings.TracerMinDistanceSqrd;
        Settings.ManOverBoardTimeout = getConfig().getInt("ManOverBoardTimeout", 30);
        Settings.SilhouetteViewDistance = getConfig().getInt("SilhouetteViewDistance", 200);
        Settings.SilhouetteBlockCount = getConfig().getInt("SilhouetteBlockCount", 20);
        Settings.FireballLifespan = getConfig().getInt("FireballLifespan", 6);
        Settings.FireballPenetration = getConfig().getBoolean("FireballPenetration", true);
        Settings.ProtectPilotedCrafts = getConfig().getBoolean("ProtectPilotedCrafts", false);
        Settings.AllowCrewSigns = getConfig().getBoolean("AllowCrewSigns", true);
        Settings.SetHomeToCrewSign = getConfig().getBoolean("SetHomeToCrewSign", true);
        Settings.RequireCreatePerm = getConfig().getBoolean("RequireCreatePerm", false);
        Settings.RequireNamePerm = getConfig().getBoolean("RequireNamePerm", true);
        Settings.TNTContactExplosives = getConfig().getBoolean("TNTContactExplosives", true);
        Settings.FadeWrecksAfter = getConfig().getInt("FadeWrecksAfter", 0);
        if (getConfig().contains("DurabilityOverride")) {
            Map<String, Object> temp = getConfig().getConfigurationSection("DurabilityOverride").getValues(false);
            Settings.DurabilityOverride = new HashMap<>();
            for (String str : temp.keySet()) {
                Settings.DurabilityOverride.put(Integer.parseInt(str), (Integer) temp.get(str));
            }
        }

        Settings.AssaultEnable = getConfig().getBoolean("AssaultEnable", false);
        Settings.AssaultDamagesCapPercent = getConfig().getDouble("AssaultDamagesCapPercent", 1.0);
        Settings.AssaultCooldownHours = getConfig().getInt("AssaultCooldownHours", 24);
        Settings.AssaultDelay = getConfig().getInt("AssaultDelay", 1800);
        Settings.AssaultDuration = getConfig().getInt("AssaultDuration", 1800);
        Settings.AssaultCostPercent = getConfig().getDouble("AssaultCostPercent", 0.25);
        Settings.AssaultDamagesPerBlock = getConfig().getInt("AssaultDamagesPerBlock", 15);
        Settings.AssaultRequiredDefendersOnline = getConfig().getInt("AssaultRequiredDefendersOnline", 2);
        Settings.AssaultRequiredOwnersOnline = getConfig().getInt("AssaultRequiredOwnersOnline", 1);
        Settings.AssaultMaxBalance = getConfig().getDouble("AssaultMaxBalance", 5000000);
        Settings.AssaultOwnerWeightPercent = getConfig().getDouble("AssaultOwnerWeightPercent", 1.0);
        Settings.AssaultMemberWeightPercent = getConfig().getDouble("AssaultMemberWeightPercent", 1.0);
        Settings.AssaultDestroyableBlocks = new HashSet<>(getConfig().getIntegerList("AssaultDestroyableBlocks"));
        Settings.DisableShadowBlocks = new HashSet<>(getConfig().getIntegerList("DisableShadowBlocks"));  //REMOVE FOR PUBLIC VERSION
        Settings.ForbiddenRemoteSigns = new HashSet<>();

        for(String s : getConfig().getStringList("ForbiddenRemoteSigns")) {
            Settings.ForbiddenRemoteSigns.add(s.toLowerCase());
        }

        Settings.SiegeEnable = getConfig().getBoolean("SiegeEnable", false);




        if (!Settings.CompatibilityMode) {
            for (int typ : Settings.DisableShadowBlocks) {
                worldHandler.disableShadow(Material.getMaterial(typ));
            }
        }
        //load up WorldGuard if it's present
        Plugin wGPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (wGPlugin == null || !(wGPlugin instanceof WorldGuardPlugin)) {
            logger.log(Level.INFO, "Movecraft did not find a compatible version of WorldGuard. Disabling WorldGuard integration");
            Settings.SiegeEnable = false;
            Settings.AssaultEnable = false;
            Settings.RestrictSiBsToRegions = false;
        } else {
            logger.log(Level.INFO, "Found a compatible version of WorldGuard. Enabling WorldGuard integration");
            Settings.WorldGuardBlockMoveOnBuildPerm = getConfig().getBoolean("WorldGuardBlockMoveOnBuildPerm", false);
            Settings.WorldGuardBlockSinkOnPVPPerm = getConfig().getBoolean("WorldGuardBlockSinkOnPVPPerm", false);
            logger.log(Level.INFO, "Settings: WorldGuardBlockMoveOnBuildPerm - {0}, WorldGuardBlockSinkOnPVPPerm - {1}", new Object[]{Settings.WorldGuardBlockMoveOnBuildPerm, Settings.WorldGuardBlockSinkOnPVPPerm});
            getServer().getPluginManager().registerEvents(new WorldGuardCompatManager(), this);
        }
        worldGuardPlugin = (WorldGuardPlugin) wGPlugin;

        //load up WorldEdit if it's present
        Plugin wEPlugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (wEPlugin == null || !(wEPlugin instanceof WorldEditPlugin)) {
            logger.log(Level.INFO, "Movecraft did not find a compatible version of WorldEdit. Disabling WorldEdit integration");
            Settings.AssaultEnable = false;
        } else {
            logger.log(Level.INFO, "Found a compatible version of WorldEdit. Enabling WorldEdit integration");
            Settings.RepairTicksPerBlock = getConfig().getInt("RepairTicksPerBlock", 0);
            Settings.RepairMaxPercent = getConfig().getDouble("RepairMaxPercent",50);
        }
        worldEditPlugin = (WorldEditPlugin) wEPlugin;

        // next is Cannons
        Plugin plug = getServer().getPluginManager().getPlugin("Cannons");
        if (plug != null && plug instanceof Cannons) {
            cannonsPlugin = (Cannons) plug;
            logger.log(Level.INFO, "Found a compatible version of Cannons. Enabling Cannons integration");
        }
        if (worldGuardPlugin != null && worldGuardPlugin instanceof WorldGuardPlugin) {
            if (worldGuardPlugin.isEnabled()) {
                Plugin tempWGCustomFlagsPlugin = getServer().getPluginManager().getPlugin("WGCustomFlags");
                if (tempWGCustomFlagsPlugin != null && tempWGCustomFlagsPlugin instanceof WGCustomFlagsPlugin) {
                    logger.log(Level.INFO, "Found a compatible version of WGCustomFlags. Enabling WGCustomFlags integration.");
                    wgCustomFlagsPlugin = (WGCustomFlagsPlugin) tempWGCustomFlagsPlugin;
                    WGCustomFlagsUtils WGCFU = new WGCustomFlagsUtils();
                    FLAG_PILOT = WGCFU.getNewStateFlag("movecraft-pilot", true);
                    FLAG_MOVE = WGCFU.getNewStateFlag("movecraft-move", true);
                    FLAG_ROTATE = WGCFU.getNewStateFlag("movecraft-rotate", true);
                    FLAG_SINK = WGCFU.getNewStateFlag("movecraft-sink", true);
                    WGCFU.init();
                    Settings.WGCustomFlagsUsePilotFlag = getConfig().getBoolean("WGCustomFlagsUsePilotFlag", false);
                    Settings.WGCustomFlagsUseMoveFlag = getConfig().getBoolean("WGCustomFlagsUseMoveFlag", false);
                    Settings.WGCustomFlagsUseRotateFlag = getConfig().getBoolean("WGCustomFlagsUseRotateFlag", false);
                    Settings.WGCustomFlagsUseSinkFlag = getConfig().getBoolean("WGCustomFlagsUseSinkFlag", false);
                    logger.log(Level.INFO, "Settings: WGCustomFlagsUsePilotFlag - {0}", Settings.WGCustomFlagsUsePilotFlag);
                    logger.log(Level.INFO, "Settings: WGCustomFlagsUseMoveFlag - {0}", Settings.WGCustomFlagsUseMoveFlag);
                    logger.log(Level.INFO, "Settings: WGCustomFlagsUseRotateFlag - {0}", Settings.WGCustomFlagsUseRotateFlag);
                    logger.log(Level.INFO, "Settings: WGCustomFlagsUseSinkFlag - {0}", Settings.WGCustomFlagsUseSinkFlag);
                } else {
                    logger.log(Level.INFO, "Movecraft did not find a compatible version of WGCustomFlags. Disabling WGCustomFlags integration.");
                }
            }
        }
        Plugin tempTownyPlugin = getServer().getPluginManager().getPlugin("Towny");
        if (tempTownyPlugin != null && tempTownyPlugin instanceof Towny) {
            logger.log(Level.INFO, "Found a compatible version of Towny. Enabling Towny integration.");
            townyPlugin = (Towny) tempTownyPlugin;
            TownyUtils.initTownyConfig();
            Settings.TownyBlockMoveOnSwitchPerm = getConfig().getBoolean("TownyBlockMoveOnSwitchPerm", false);
            Settings.TownyBlockSinkOnNoPVP = getConfig().getBoolean("TownyBlockSinkOnNoPVP", false);
            logger.log(Level.INFO, "Settings: TownyBlockMoveOnSwitchPerm - {0}", Settings.TownyBlockMoveOnSwitchPerm);
            logger.log(Level.INFO, "Settings: TownyBlockSinkOnNoPVP - {0}", Settings.TownyBlockSinkOnNoPVP);

        } else {
            logger.log(Level.INFO, "Movecraft did not find a compatible version of Towny. Disabling Towny integration.");
        }

        Plugin tempEssentialsPlugin = getServer().getPluginManager().getPlugin("Essentials");
        if (tempEssentialsPlugin != null) {
            if (tempEssentialsPlugin.getDescription().getName().equalsIgnoreCase("essentials")) {
                if (tempEssentialsPlugin.getClass().getName().equals("com.earth2me.essentials.Essentials")) {
                    if (tempEssentialsPlugin instanceof Essentials) {
                        essentialsPlugin = (Essentials) tempEssentialsPlugin;
                        logger.log(Level.INFO, "Found a compatible version of Essentials. Enabling Essentials integration.");
                    }
                }
            }
        }
        if (essentialsPlugin == null) {
            logger.log(Level.INFO, "Movecraft did not find a compatible version of Essentials. Disabling Essentials integration.");
        }

        // and now Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                Settings.RepairMoneyPerBlock = getConfig().getDouble("RepairMoneyPerBlock", 0.0);
                logger.log(Level.INFO, "Found a compatible Vault plugin.");
            } else {
                logger.log(Level.INFO, "Could not find compatible Vault plugin. Disabling Vault integration.");
                economy = null;
                Settings.SiegeEnable = false;
                Settings.AssaultEnable = false;
            }
        } else {
            logger.log(Level.INFO, "Could not find compatible Vault plugin. Disabling Vault integration.");
            economy = null;
            Settings.SiegeEnable = false;
        }
        String[] localisations = {"en", "cz", "nl"};
        for (String s : localisations) {
            if (!new File(getDataFolder()
                    + "/localisation/movecraftlang_" + s + ".properties").exists()) {
                this.saveResource("localisation/movecraftlang_" + s + ".properties", false);
            }
        }

        I18nSupport.init();
        if (shuttingDown && Settings.IGNORE_RESET) {
            logger.log(
                    Level.SEVERE,
                    I18nSupport
                            .getInternationalisedString("Startup - Error - Reload error"));
            logger.log(
                    Level.INFO,
                    I18nSupport
                            .getInternationalisedString("Startup - Error - Disable warning for reload"));
            getPluginLoader().disablePlugin(this);
        } else {

            // Startup procedure
            asyncManager = new AsyncManager();
            asyncManager.runTaskTimer(this, 0, 1);
            MapUpdateManager.getInstance().runTaskTimer(this, 0, 1);
            if(Settings.AssaultEnable) {
                assaultManager = new AssaultManager(this);
                assaultManager.runTaskTimerAsynchronously(this, 0, 20);
            }

            if(Settings.SiegeEnable) {

                siegeManager = new SiegeManager(this);
                logger.info("Enabling siege");
                //load the sieges.yml file
                File siegesFile = new File(Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/sieges.yml");
                InputStream input;
                try {
                    input = new FileInputStream(siegesFile);
                } catch (FileNotFoundException e) {
                    input = null;
                }
                if (input != null) {
                    Map data = new Yaml().loadAs(input, Map.class);
                    Map<String, Map<String, ?>> siegesMap = (Map<String, Map<String, ?>>) data.get("sieges");
                    List<Siege> sieges = siegeManager.getSieges();
                    for (Map.Entry<String, Map<String, ?>> entry : siegesMap.entrySet()) {
                        Map<String,Object> siegeMap = (Map<String, Object>) entry.getValue();
                        sieges.add(new Siege(
                                entry.getKey(),
                                (String) siegeMap.get("RegionToControl"),
                                (String) siegeMap.get("SiegeRegion"),
                                (Integer) siegeMap.get("ScheduleStart"),
                                (Integer) siegeMap.get("ScheduleEnd"),
                                (Integer) siegeMap.getOrDefault("DelayBeforeStart", 0),
                                (Integer) siegeMap.get("SiegeDuration"),
                                (Integer) siegeMap.getOrDefault("DayOfTheWeek", 1),
                                (Integer) siegeMap.getOrDefault("DailyIncome", 0),
                                (Integer) siegeMap.getOrDefault("CostToSiege", 0),
                                (Boolean) siegeMap.getOrDefault("DoubleCostPerOwnedSiegeRegion", true),
                                (List<String>) siegeMap.getOrDefault("CraftsToWin", Collections.emptyList()),
                                (List<String>) siegeMap.getOrDefault("SiegeCommandsOnStart", Collections.emptyList()),
                                (List<String>) siegeMap.getOrDefault("SiegeCommandsOnWin", Collections.emptyList()),
                                (List<String>) siegeMap.getOrDefault("SiegeCommandsOnLose", Collections.emptyList())));
                    }
                    logger.log(Level.INFO, "Siege configuration loaded.");

                }
                siegeManager.runTaskTimerAsynchronously(this, 0, 20);
            }
            CraftManager.initialize();

            getServer().getPluginManager().registerEvents(new InteractListener(), this);
            if (worldEditPlugin != null) {
                final Class clazz;
                MovecraftRepair.initialize(this);
                if (MovecraftRepair.getInstance() != null){
                    repairManager = new RepairManager();
                    repairManager.runTaskTimerAsynchronously(this,0,1);
                }
            }
            this.getCommand("movecraft").setExecutor(new MovecraftCommand());
            this.getCommand("release").setExecutor(new ReleaseCommand());
            this.getCommand("pilot").setExecutor(new PilotCommand());
            this.getCommand("rotate").setExecutor(new RotateCommand());
            this.getCommand("cruise").setExecutor(new CruiseCommand());
            this.getCommand("craftreport").setExecutor(new CraftReportCommand());
            this.getCommand("manoverboard").setExecutor(new ManOverboardCommand());
            this.getCommand("contacts").setExecutor(new ContactsCommand());
            this.getCommand("scuttle").setExecutor(new ScuttleCommand());

            if(Settings.SiegeEnable)
                this.getCommand("siege").setExecutor(new SiegeCommand());
            if(Settings.AssaultEnable) {
                this.getCommand("assaultinfo").setExecutor(new AssaultInfoCommand());
                this.getCommand("assault").setExecutor(new AssaultCommand());
            }
            getServer().getPluginManager().registerEvents(new BlockListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(), this);
            getServer().getPluginManager().registerEvents(new AntiAircraftDirectorSign(), this);
            getServer().getPluginManager().registerEvents(new AscendSign(), this);
            getServer().getPluginManager().registerEvents(new CannonDirectorSign(), this);
            getServer().getPluginManager().registerEvents(new ContactsSign(), this);
            getServer().getPluginManager().registerEvents(new CraftSign(), this);
            getServer().getPluginManager().registerEvents(new CrewSign(), this);
            getServer().getPluginManager().registerEvents(new CruiseSign(), this);
            getServer().getPluginManager().registerEvents(new DescendSign(), this);
            getServer().getPluginManager().registerEvents(new HelmSign(), this);
            getServer().getPluginManager().registerEvents(new MoveSign(), this);
            getServer().getPluginManager().registerEvents(new NameSign(), this);
            getServer().getPluginManager().registerEvents(new PilotSign(), this);
            getServer().getPluginManager().registerEvents(new RelativeMoveSign(), this);
            getServer().getPluginManager().registerEvents(new ReleaseSign(), this);
            getServer().getPluginManager().registerEvents(new RemoteSign(), this);
            getServer().getPluginManager().registerEvents(new RepairSign(), this);
            getServer().getPluginManager().registerEvents(new SpeedSign(), this);
            getServer().getPluginManager().registerEvents(new StatusSign(), this);
            getServer().getPluginManager().registerEvents(new SubcraftRotateSign(), this);
            getServer().getPluginManager().registerEvents(new TeleportSign(), this);

            logger.log(Level.INFO, String.format(
                    I18nSupport.getInternationalisedString("Startup - Enabled message"),
                    getDescription().getVersion()));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        instance = this;
        logger = getLogger();
    }



    public WorldGuardPlugin getWorldGuardPlugin() {
        return worldGuardPlugin;
    }

    public WorldEditPlugin getWorldEditPlugin() {
        return worldEditPlugin;
    }

    public Economy getEconomy() {
        return economy;
    }

    public Cannons getCannonsPlugin() {
        return cannonsPlugin;
    }

    public WGCustomFlagsPlugin getWGCustomFlagsPlugin() {
        return wgCustomFlagsPlugin;
    }

    public Towny getTownyPlugin() {
        return townyPlugin;
    }

    public Essentials getEssentialsPlugin() {
        return essentialsPlugin;
    }

    public AssaultManager getAssaultManager() {
        return assaultManager;
    }

    public SiegeManager getSiegeManager(){return siegeManager;}

    public WorldHandler getWorldHandler(){
        return worldHandler;
    }

    public AsyncManager getAsyncManager(){return asyncManager;}

    public RepairManager getRepairManager() {
        return repairManager;
    }
}

