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
import net.countercraft.movecraft.util.BukkitTeleport;
import net.countercraft.movecraft.util.Tags;
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
import java.util.Set;
import java.util.logging.Logger;

public class Movecraft extends JavaPlugin {
    private static Movecraft instance;
    private static BukkitAudiences adventure = null;

    private Logger logger;
    private boolean shuttingDown;
    private WorldHandler worldHandler;
    private SmoothTeleport smoothTeleport;
    private AsyncManager asyncManager;

    public static synchronized Movecraft getInstance() {
        return instance;
    }

    @NotNull
    public static BukkitAudiences getAdventure() {
        if (adventure == null)
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");

        return adventure;
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
    }

    @Override
    public void onEnable() {
        // Read in config
        Settings.LOCALE = getConfig().getString("Locale");
        Settings.Debug = getConfig().getBoolean("Debug", false);
        Settings.DisableNMSCompatibilityCheck = getConfig().getBoolean("IReallyKnowWhatIAmDoing", false);
        Settings.DisableSpillProtection = getConfig().getBoolean("DisableSpillProtection", false);
        Settings.DisableIceForm = getConfig().getBoolean("DisableIceForm", true);

        String[] localisations = {"en", "cz", "nl", "fr"};
        for (String s : localisations) {
            if (!new File(getDataFolder()
                    + "/localisation/movecraftlang_" + s + ".properties").exists()) {
                saveResource("localisation/movecraftlang_" + s + ".properties", false);
            }
        }
        I18nSupport.init();


        // if the PilotTool is specified in the config.yml file, use it
        String pilotTool = getConfig().getString("PilotTool");
        if (pilotTool != null) {
            Material material = Material.getMaterial(pilotTool);
            if (material != null) {
                logger.info("Recognized PilotTool setting of: " + pilotTool);
                Settings.PilotTool = material;
            }
            else {
                logger.info("No PilotTool setting, using default of stick");
            }
        }
        else {
            logger.info("No PilotTool setting, using default of stick");
        }

        String minecraftVersion = getServer().getMinecraftVersion();
        getLogger().info("Loading support for " + minecraftVersion);
        try {
            final Class<?> worldHandlerClazz = Class.forName("net.countercraft.movecraft.compat." + WorldHandler.getPackageName(minecraftVersion) + ".IWorldHandler");
            // Check if we have a NMSHandler class at that location.
            if (WorldHandler.class.isAssignableFrom(worldHandlerClazz)) { // Make sure it actually implements NMS
                worldHandler = (WorldHandler) worldHandlerClazz.getConstructor().newInstance(); // Set our handler

                // Try to setup the smooth teleport handler
                try {
                    final Class<?> smoothTeleportClazz = Class.forName("net.countercraft.movecraft.support." + WorldHandler.getPackageName(minecraftVersion) + ".ISmoothTeleport");
                    if (SmoothTeleport.class.isAssignableFrom(smoothTeleportClazz)) {
                        smoothTeleport = (SmoothTeleport) smoothTeleportClazz.getConstructor().newInstance();
                    }
                    else {
                        smoothTeleport = new BukkitTeleport(); // Fall back to bukkit teleportation
                        getLogger().warning("Did not find smooth teleport, falling back to bukkit teleportation provider.");
                    }
                }
                catch (final ReflectiveOperationException e) {
                    if (Settings.Debug) {
                        e.printStackTrace();
                    }
                    smoothTeleport = new BukkitTeleport(); // Fall back to bukkit teleportation
                    getLogger().warning("Falling back to bukkit teleportation provider.");
                }
            }
        }
        catch (final Exception e) {
            e.printStackTrace();
            getLogger().severe("Could not find support for this version.");
            if (!Settings.DisableNMSCompatibilityCheck) {
                // Disable ourselves and exit
                setEnabled(false);
                return;
            }
            else {
                // Server owner claims to know what they are doing, warn them of the possible consequences
                getLogger().severe("WARNING!\n\t"
                        + "Running Movecraft on an incompatible version can corrupt your world and break EVERYTHING!\n\t"
                        + "We provide no support for any issues.");
            }
        }


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
        if (getConfig().contains("ExtraFadeTimePerBlock")) {
            Map<String, Object> temp = getConfig().getConfigurationSection("ExtraFadeTimePerBlock").getValues(false);
            for (String str : temp.keySet()) {
                Set<Material> materials = Tags.parseMaterials(str);
                for (Material m : materials) {
                    Settings.ExtraFadeTimePerBlock.put(m, (Integer) temp.get(str));
                }
            }
        }

        Settings.ForbiddenRemoteSigns = new HashSet<>();
        for(String s : getConfig().getStringList("ForbiddenRemoteSigns")) {
            Settings.ForbiddenRemoteSigns.add(s.toLowerCase());
        }

        adventure = BukkitAudiences.create(this);

        if(shuttingDown && Settings.IGNORE_RESET) {
            logger.severe("Movecraft is incompatible with the reload command. Movecraft has shut down and will restart when the server is restarted.");
            logger.severe("If you wish to use the reload command and Movecraft, you may disable this check inside the config.yml by setting 'safeReload: false'");
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

        logger.info("[V " + getDescription().getVersion() + "] has been enabled.");
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
            logger.severe("Failed to initialize Movecraft data pack due to first time world initialization.");
            return false;
        }
        if(!datapackDirectory.exists()) {
            logger.info("Creating a datapack directory at " + datapackDirectory.getPath());
            if(!datapackDirectory.mkdir()) {
                logger.severe("Failed to create datapack directory!");
                return false;
            }
        }
        else if(new File(datapackDirectory, "movecraft-data.zip").exists()) {
            logger.warning("Conflicting datapack already exists in " + datapackDirectory.getPath()
                    + ". If you would like to regenerate the datapack, delete the existing one and set the GeneratedDatapack config option to false.");
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
        logger.info("Saved default Movecraft datapack.");
        getConfig().set("GeneratedDatapack", true);
        saveConfig();

        logger.info("It is expected that your crafts are not loaded during startup on the first boot.  They will be loaded after startup.");

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            logger.info("Enabling datapack and reloading craft types.");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "datapack list"); // required for some reason
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "datapack enable \"file/movecraft-data.zip\""))
                logger.severe("Failed to automatically load movecraft datapack. Check if it exists.");

            CraftManager.getInstance().reloadCraftTypes();
        }, 600); // Wait 30 seconds before reloading.  Needed to prevent Paper from running this during startup.
        return false;
    }


    public WorldHandler getWorldHandler(){
        return worldHandler;
    }

    public SmoothTeleport getSmoothTeleport() {
        return smoothTeleport;
    }

    public AsyncManager getAsyncManager(){return asyncManager;}
}
