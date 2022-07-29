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

import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.commands.ContactsCommand;
import net.countercraft.movecraft.commands.CraftInfoCommand;
import net.countercraft.movecraft.commands.CraftReportCommand;
import net.countercraft.movecraft.commands.CraftTypeCommand;
import net.countercraft.movecraft.commands.CruiseCommand;
import net.countercraft.movecraft.commands.ManOverboardCommand;
import net.countercraft.movecraft.commands.MovecraftCommand;
import net.countercraft.movecraft.commands.PilotCommand;
import net.countercraft.movecraft.commands.ReleaseCommand;
import net.countercraft.movecraft.commands.RotateCommand;
import net.countercraft.movecraft.commands.ScuttleCommand;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.ChunkManager;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.listener.BlockListener;
import net.countercraft.movecraft.listener.InteractListener;
import net.countercraft.movecraft.listener.PlayerListener;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.sign.AscendSign;
import net.countercraft.movecraft.sign.ContactsSign;
import net.countercraft.movecraft.sign.CraftSign;
import net.countercraft.movecraft.sign.CruiseSign;
import net.countercraft.movecraft.sign.DescendSign;
import net.countercraft.movecraft.sign.HelmSign;
import net.countercraft.movecraft.sign.MoveSign;
import net.countercraft.movecraft.sign.NameSign;
import net.countercraft.movecraft.sign.PilotSign;
import net.countercraft.movecraft.sign.RelativeMoveSign;
import net.countercraft.movecraft.sign.ReleaseSign;
import net.countercraft.movecraft.sign.RemoteSign;
import net.countercraft.movecraft.sign.ScuttleSign;
import net.countercraft.movecraft.sign.SpeedSign;
import net.countercraft.movecraft.sign.StatusSign;
import net.countercraft.movecraft.sign.SubcraftRotateSign;
import net.countercraft.movecraft.sign.TeleportSign;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

public class Movecraft extends JavaPlugin {
    private static Movecraft instance;
    private static BukkitAudiences adventure = null;

    private Logger logger;
    private boolean shuttingDown;
    private WorldHandler worldHandler;
    private AsyncManager asyncManager;

    public static synchronized Movecraft getInstance() {
        return instance;
    }

    @NotNull
    public static BukkitAudiences getAdventure() {
        if(adventure == null)
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");

        return adventure;
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        if(adventure != null) {
            adventure.close();
            adventure = null;
        }
    }

    @Override
    public void onEnable() {
        // Read in config
        Settings.LOCALE = getConfig().getString("Locale");
        Settings.Debug = getConfig().getBoolean("Debug", false);
        Settings.DisableSpillProtection = getConfig().getBoolean("DisableSpillProtection", false);
        Settings.DisableIceForm = getConfig().getBoolean("DisableIceForm", true);
        
        String[] localisations = {"en", "cz", "nl"};
        for(String s : localisations) {
            if(!new File(getDataFolder()
                    + "/localisation/movecraftlang_" + s + ".properties").exists()) {
                saveResource("localisation/movecraftlang_" + s + ".properties", false);
            }
        }
        I18nSupport.init();
        
        
        // if the PilotTool is specified in the config.yml file, use it
        String pilotTool = getConfig().getString("PilotTool");
        if(pilotTool != null) {
            Material material = Material.getMaterial(pilotTool);
            if(material != null) {
                logger.info(I18nSupport.getInternationalisedString("Startup - Recognized Pilot Tool")
                        + pilotTool);
                Settings.PilotTool = material;
            }
            else
                logger.info(I18nSupport.getInternationalisedString("Startup - No Pilot Tool"));
        }
        else
            logger.info(I18nSupport.getInternationalisedString("Startup - No Pilot Tool"));

        String packageName = getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        try {
            final Class<?> clazz = Class.forName("net.countercraft.movecraft.compat." + version + ".IWorldHandler");
            // Check if we have a NMSHandler class at that location.
            if (WorldHandler.class.isAssignableFrom(clazz)) // Make sure it actually implements NMS
                worldHandler = (WorldHandler) clazz.getConstructor().newInstance(); // Set our handler
        }
        catch(final Exception e) {
            e.printStackTrace();
            getLogger().severe(I18nSupport.getInternationalisedString("Startup - Version Not Supported"));
            setEnabled(false);
            return;
        }
        getLogger().info(I18nSupport.getInternationalisedString("Startup - Loading Support") + " " + version);


        Settings.SinkCheckTicks = getConfig().getDouble("SinkCheckTicks", 100.0);
        Settings.ManOverboardTimeout = getConfig().getInt("ManOverboardTimeout", 30);
        Settings.ManOverboardDistSquared = Math.pow(getConfig().getDouble("ManOverboardDistance", 1000), 2);
        Settings.SilhouetteViewDistance = getConfig().getInt("SilhouetteViewDistance", 200);
        Settings.SilhouetteBlockCount = getConfig().getInt("SilhouetteBlockCount", 20);
        Settings.ProtectPilotedCrafts = getConfig().getBoolean("ProtectPilotedCrafts", false);
        Settings.MaxRemoteSigns = getConfig().getInt("MaxRemoteSigns", -1);
        Settings.CraftsUseNetherPortals = getConfig().getBoolean("CraftsUseNetherPortals", false);
        Settings.RequireCreatePerm = getConfig().getBoolean("RequireCreatePerm", false);
        Settings.RequireNamePerm = getConfig().getBoolean("RequireNamePerm", true);
        Settings.FadeWrecksAfter = getConfig().getInt("FadeWrecksAfter", 0);
        Settings.FadeTickCooldown = getConfig().getInt("FadeTickCooldown", 20);
        Settings.FadePercentageOfWreckPerCycle = getConfig().getDouble("FadePercentageOfWreckPerCycle", 10.0);
        if(getConfig().contains("ExtraFadeTimePerBlock")) {
            Map<String, Object> temp = getConfig().getConfigurationSection("ExtraFadeTimePerBlock").getValues(false);
            for(String str : temp.keySet()) {
                Material type;
                try {
                    type = Material.getMaterial(str);
                }
                catch(NumberFormatException e) {
                    type = Material.getMaterial(str);
                }
                Settings.ExtraFadeTimePerBlock.put(type, (Integer) temp.get(str));
            }
        }

        Settings.CollisionPrimer = getConfig().getInt("CollisionPrimer", 1000);
        Settings.DisableShadowBlocks = EnumSet.noneOf(Material.class);  //REMOVE FOR PUBLIC VERSION
//        for(String s : getConfig().getStringList("DisableShadowBlocks")){
//            Settings.DisableShadowBlocks.add(Material.valueOf(s));
//        }

        Settings.ForbiddenRemoteSigns = new HashSet<>();

        for(String s : getConfig().getStringList("ForbiddenRemoteSigns")) {
            Settings.ForbiddenRemoteSigns.add(s.toLowerCase());
        }

        if(!Settings.CompatibilityMode) {
            for(Material typ : Settings.DisableShadowBlocks) {
                worldHandler.disableShadow(typ);
            }
        }
        adventure = BukkitAudiences.create(this);

        if(shuttingDown && Settings.IGNORE_RESET) {
            logger.severe(I18nSupport.getInternationalisedString("Startup - Error - Reload error"));
            logger.severe(I18nSupport.getInternationalisedString("Startup - Error - Disable warning for reload"));
            getPluginLoader().disablePlugin(this);
            return;
        }


        // Startup procedure
        boolean datapackInitialized = initializeDatapack();
        asyncManager = new AsyncManager();
        asyncManager.runTaskTimer(this, 0, 1);
        MapUpdateManager.getInstance().runTaskTimer(this, 0, 1);

        CraftManager.initialize(datapackInitialized);
        Bukkit.getScheduler().runTaskTimer(this, WorldManager.INSTANCE::run, 0,1);


        getServer().getPluginManager().registerEvents(new InteractListener(), this);

        getCommand("movecraft").setExecutor(new MovecraftCommand());
        getCommand("release").setExecutor(new ReleaseCommand());
        getCommand("pilot").setExecutor(new PilotCommand());
        getCommand("rotate").setExecutor(new RotateCommand());
        getCommand("cruise").setExecutor(new CruiseCommand());
        getCommand("craftreport").setExecutor(new CraftReportCommand());
        getCommand("manoverboard").setExecutor(new ManOverboardCommand());
        getCommand("contacts").setExecutor(new ContactsCommand());
        getCommand("scuttle").setExecutor(new ScuttleCommand());
        getCommand("crafttype").setExecutor(new CraftTypeCommand());
        getCommand("craftinfo").setExecutor(new CraftInfoCommand());

        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkManager(), this);
        getServer().getPluginManager().registerEvents(new AscendSign(), this);
        getServer().getPluginManager().registerEvents(new ContactsSign(), this);
        getServer().getPluginManager().registerEvents(new CraftSign(), this);
        getServer().getPluginManager().registerEvents(new CruiseSign(), this);
        getServer().getPluginManager().registerEvents(new DescendSign(), this);
        getServer().getPluginManager().registerEvents(new HelmSign(), this);
        getServer().getPluginManager().registerEvents(new MoveSign(), this);
        getServer().getPluginManager().registerEvents(new NameSign(), this);
        getServer().getPluginManager().registerEvents(new PilotSign(), this);
        getServer().getPluginManager().registerEvents(new RelativeMoveSign(), this);
        getServer().getPluginManager().registerEvents(new ReleaseSign(), this);
        getServer().getPluginManager().registerEvents(new RemoteSign(), this);
        getServer().getPluginManager().registerEvents(new SpeedSign(), this);
        getServer().getPluginManager().registerEvents(new StatusSign(), this);
        getServer().getPluginManager().registerEvents(new SubcraftRotateSign(), this);
        getServer().getPluginManager().registerEvents(new TeleportSign(), this);
        getServer().getPluginManager().registerEvents(new ScuttleSign(), this);

        logger.info(String.format(
                I18nSupport.getInternationalisedString("Startup - Enabled message"),
                getDescription().getVersion()));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        instance = this;
        logger = getLogger();
        saveDefaultConfig();

    }

    private boolean initializeDatapack() {
        if(getConfig().getBoolean("GeneratedDatapack"))
            return true;

        File datapackDirectory = null;
        for(var world : getServer().getWorlds()) {
            datapackDirectory = new File(world.getWorldFolder(), "datapacks");
            if(datapackDirectory.exists())
                break;
        }
        if(datapackDirectory == null) {
            logger.severe(I18nSupport.getInternationalisedString("Startup - Datapack World Error"));
            return false;
        }
        if(!datapackDirectory.exists()) {
            logger.info(I18nSupport.getInternationalisedString("Startup - Datapack Directory") + datapackDirectory.getPath());
            if(!datapackDirectory.mkdir()) {
                logger.severe(I18nSupport.getInternationalisedString("Startup - Datapack Directory Error"));
                return false;
            }
        }
        else if(new File(datapackDirectory, "movecraft-data.zip").exists()) {
            logger.warning(String.format(
                    I18nSupport.getInternationalisedString("Startup - Datapack Conflict"),
                    datapackDirectory.getPath())
            );
            getConfig().set("GeneratedDatapack", true);
            saveConfig();
            return false;
        }
        if(!datapackDirectory.canWrite()) {
            logger.warning("Missing permissions to write to world directory.");
            return false;
        }

        try(var stream = new FileOutputStream(new File(datapackDirectory, "movecraft-data.zip"));
                var pack = getResource("movecraft-data.zip")) {
            if(pack == null) {
                logger.warning("No internal datapack found, report this.");
                return false;
            }
            pack.transferTo(stream);
        }
        catch(IOException e) {
            e.printStackTrace();
            return false;
        }
        logger.info(I18nSupport.getInternationalisedString("Startup - Datapack Saved"));
        getConfig().set("GeneratedDatapack", true);
        saveConfig();

        logger.info(I18nSupport.getInternationalisedString("Startup - Datapack First Boot"));

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            logger.info(I18nSupport.getInternationalisedString("Startup - Datapack Enabling"));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "datapack list"); // required for some reason
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "datapack enable \"file/movecraft-data.zip\""))
                logger.severe(I18nSupport.getInternationalisedString("Startup - Datapack Enable Error"));

            CraftManager.getInstance().reloadCraftTypes();
        }, 200); // Wait 10 seconds before reloading.  Needed to prevent Paper from running this during startup.
        return false;
    }


    public WorldHandler getWorldHandler(){
        return worldHandler;
    }

    public AsyncManager getAsyncManager(){return asyncManager;}
}

