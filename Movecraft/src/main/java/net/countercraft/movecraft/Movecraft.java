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

import io.papermc.paper.datapack.Datapack;
import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.commands.*;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.ChunkManager;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.features.contacts.ContactsCommand;
import net.countercraft.movecraft.features.contacts.ContactsManager;
import net.countercraft.movecraft.features.contacts.ContactsSign;
import net.countercraft.movecraft.features.fading.WreckManager;
import net.countercraft.movecraft.features.status.StatusManager;
import net.countercraft.movecraft.features.status.StatusSign;
import net.countercraft.movecraft.listener.*;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.sign.*;
import net.countercraft.movecraft.util.BukkitTeleport;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class Movecraft extends JavaPlugin {
    private static Movecraft instance;

    private Logger logger;
    private boolean shuttingDown;
    private WorldHandler worldHandler;
    private SmoothTeleport smoothTeleport;
    private AsyncManager asyncManager;
    private WreckManager wreckManager;

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
        Settings.LOCALE = getConfig().getString("Locale");
        Settings.Debug = getConfig().getBoolean("Debug", false);
        Settings.DisableNMSCompatibilityCheck = getConfig().getBoolean("IReallyKnowWhatIAmDoing", false);
        Settings.DisableSpillProtection = getConfig().getBoolean("DisableSpillProtection", false);
        Settings.DisableIceForm = getConfig().getBoolean("DisableIceForm", true);
        Settings.ReleaseOnDeath = getConfig().getBoolean("ReleaseOnDeath", false);
        Settings.ManOverboardCooldown = getConfig().getInt("ManoverboardCooldown", 30);

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
        Settings.ReleaseCraftOnLogout = getConfig().getBoolean("ReleaseCraftOnLogout", true);
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

        if(shuttingDown && Settings.IGNORE_RESET) {
            logger.severe("Movecraft is incompatible with the reload command. Movecraft has shut down and will restart when the server is restarted.");
            logger.severe("If you wish to use the reload command and Movecraft, you may disable this check inside the config.yml by setting 'safeReload: false'");
            getPluginLoader().disablePlugin(this);
            return;
        }

        // Startup procedure
        boolean datapackInitialized = isDatapackEnabled() || initializeDatapack();
        asyncManager = new AsyncManager();
        asyncManager.runTaskTimer(this, 0, 1);
        MapUpdateManager.getInstance().runTaskTimer(this, 0, 1);


        CraftManager.initialize(datapackInitialized);
        Bukkit.getScheduler().runTaskTimer(this, WorldManager.INSTANCE::run, 0,1);
        wreckManager = new WreckManager(WorldManager.INSTANCE);

        getServer().getPluginManager().registerEvents(new InteractListener(), this);

        getCommand("movecraft").setExecutor(new MovecraftCommand());
        getCommand("release").setExecutor(new ReleaseCommand());
        getCommand("pilot").setExecutor(new PilotCommand());
        getCommand("rotate").setExecutor(new RotateCommand());
        getCommand("cruise").setExecutor(new CruiseCommand());
        getCommand("craftreport").setExecutor(new CraftReportCommand());
        getCommand("manoverboard").setExecutor(new ManOverboardCommand());
        getCommand("scuttle").setExecutor(new ScuttleCommand());
        getCommand("crafttype").setExecutor(new CraftTypeCommand());
        getCommand("craftinfo").setExecutor(new CraftInfoCommand());

        // Naming scheme: If it has parameters, append a double colon except if it is a subcraft
        // Parameters follow on the following lines
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkManager(), this);
        //getServer().getPluginManager().registerEvents(new AscendSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Ascend:", new AscendSign("Ascend:"));
        //getServer().getPluginManager().registerEvents(new CruiseSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Cruise:", new CruiseSign("Cruise:"));
        //getServer().getPluginManager().registerEvents(new DescendSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Descend:", new DescendSign("Descend:"));
        //getServer().getPluginManager().registerEvents(new HelmSign(), this);
        MovecraftSignRegistry.INSTANCE.register("[Helm]", new HelmSign());
        MovecraftSignRegistry.INSTANCE.register(HelmSign.PRETTY_HEADER, new HelmSign());
        //getServer().getPluginManager().registerEvents(new MoveSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Move:", new MoveSign());
        //getServer().getPluginManager().registerEvents(new NameSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Name:", new NameSign());
        //getServer().getPluginManager().registerEvents(new PilotSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Pilot:", new PilotSign());
        //getServer().getPluginManager().registerEvents(new RelativeMoveSign(), this);
        MovecraftSignRegistry.INSTANCE.register("RMove:", new RelativeMoveSign());
        //getServer().getPluginManager().registerEvents(new ReleaseSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Release", new ReleaseSign());
        //getServer().getPluginManager().registerEvents(new RemoteSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Remote Sign", new RemoteSign());
        //getServer().getPluginManager().registerEvents(new SpeedSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Speed:", new SpeedSign());
        MovecraftSignRegistry.INSTANCE.register("Status:", new StatusSign());
        MovecraftSignRegistry.INSTANCE.register("Contacts:", new ContactsSign());
        //getServer().getPluginManager().registerEvents(new SubcraftRotateSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Subcraft Rotate", new SubcraftRotateSign(CraftManager.getInstance()::getCraftTypeFromString, Movecraft::getInstance));
        //getServer().getPluginManager().registerEvents(new TeleportSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Teleport:", new TeleportSign());
        //getServer().getPluginManager().registerEvents(new ScuttleSign(), this);
        MovecraftSignRegistry.INSTANCE.register("Scuttle", new ScuttleSign());
        getServer().getPluginManager().registerEvents(new CraftPilotListener(), this);
        getServer().getPluginManager().registerEvents(new CraftReleaseListener(), this);
        getServer().getPluginManager().registerEvents(new SignListener(), this);
        // Moved to compat section!
        //getServer().getPluginManager().registerEvents(new SignListener(), this);

        MovecraftSignRegistry.INSTANCE.registerCraftPilotSigns(CraftManager.getInstance().getCraftTypes(), CraftPilotSign::new);

        var contactsManager = new ContactsManager();
        contactsManager.runTaskTimerAsynchronously(this, 0, 20);
        getServer().getPluginManager().registerEvents(contactsManager, this);
        //getServer().getPluginManager().registerEvents(new ContactsSign(), this);
        getServer().getPluginManager().registerEvents(new CraftTypeListener(), this);
        getCommand("contacts").setExecutor(new ContactsCommand());

        var statusManager = new StatusManager();
        statusManager.runTaskTimerAsynchronously(this, 0, 1);
        getServer().getPluginManager().registerEvents(statusManager, this);
        //getServer().getPluginManager().registerEvents(new StatusSign(), this);

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
            logger.warning("Conflicting datapack already exists in " + datapackDirectory.getPath() + ". If you would like to regenerate the datapack, delete the existing one.");
            return false;
        }
        if(!datapackDirectory.canWrite()) {
            logger.warning("Missing permissions to write to world directory.");
            return false;
        }

        try(var stream = new FileOutputStream(new File(datapackDirectory, "movecraft-data.zip"));
                var pack = getResource("movecraft-data.zip")) {
            if(pack == null) {
                logger.severe("No internal datapack found, report this.");
                return false;
            }
            pack.transferTo(stream);
        }
        catch(IOException e) {
            e.printStackTrace();
            return false;
        }
        logger.info("Saved default Movecraft datapack.");

        getServer().dispatchCommand(getServer().createCommandSender(response -> {}), "datapack list"); // list datapacks to trigger the server to check
        for (Datapack datapack : getServer().getDatapackManager().getPacks()) {
            if (!datapack.getName().equals("file/movecraft-data.zip"))
                continue;

            if (!datapack.isEnabled()) {
                datapack.setEnabled(true);
                logger.info("Datapack enabled.");
            }
            break;
        }

        if (!isDatapackEnabled()) {
            logger.severe("Failed to automatically load movecraft datapack. Check if it exists.");
            setEnabled(false);
            return false;
        }
        return true;
    }

    private boolean isDatapackEnabled() {
        getServer().dispatchCommand(getServer().createCommandSender(response -> {}), "datapack list"); // list datapacks to trigger the server to check
        for (Datapack datapack : getServer().getDatapackManager().getPacks()) {
            if (!datapack.getName().equals("file/movecraft-data.zip"))
                continue;

            return datapack.isEnabled();
        }
        return false;
    }


    public WorldHandler getWorldHandler(){
        return worldHandler;
    }

    public SmoothTeleport getSmoothTeleport() {
        return smoothTeleport;
    }

    public AsyncManager getAsyncManager() {
        return asyncManager;
    }

    public @NotNull WreckManager getWreckManager(){
        return wreckManager;
    }
}
