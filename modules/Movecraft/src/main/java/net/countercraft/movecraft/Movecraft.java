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
import net.countercraft.movecraft.commands.CraftReportCommand;
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
import net.countercraft.movecraft.sign.SpeedSign;
import net.countercraft.movecraft.sign.StatusSign;
import net.countercraft.movecraft.sign.SubcraftRotateSign;
import net.countercraft.movecraft.sign.TeleportSign;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
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
    public static BukkitAudiences getAdventure(){
        if(adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
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
        this.saveDefaultConfig();
        try {
            Class.forName("com.destroystokyo.paper.Title");
            Settings.IsPaper = true;
        }catch (Exception e){
            Settings.IsPaper=false;
        }


        Settings.LOCALE = getConfig().getString("Locale");
        Settings.Debug = getConfig().getBoolean("Debug", false);
        Settings.DisableSpillProtection = getConfig().getBoolean("DisableSpillProtection", false);
        Settings.DisableIceForm = getConfig().getBoolean("DisableIceForm", true);
        
        // moved localisation initialisation to the start so that startup messages can be localised
        String[] localisations = {"en", "cz", "nl"};
        for (String s : localisations) {
            if (!new File(getDataFolder()
                    + "/localisation/movecraftlang_" + s + ".properties").exists()) {
                this.saveResource("localisation/movecraftlang_" + s + ".properties", false);
            }
        }

        I18nSupport.init();
        
        
        // if the PilotTool is specified in the config.yml file, use it
        if (getConfig().getInt("PilotTool") != 0) {
            logger.log(Level.INFO, I18nSupport.getInternationalisedString("Startup - Recognized Pilot Tool")
                    + getConfig().getInt("PilotTool"));
            Settings.PilotTool = Material.valueOf(getConfig().getString("PilotTool"));
        } else {
            logger.log(Level.INFO, I18nSupport.getInternationalisedString("Startup - No Pilot Tool"));
        }
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
            this.getLogger().severe(I18nSupport.getInternationalisedString("Startup - Version Not Supported"));
            this.setEnabled(false);
            return;
        }
        this.getLogger().info(I18nSupport.getInternationalisedString("Startup - Loading Support") + " " + version);


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
                Material type;
                try {
                    type = Material.getMaterial(str);
                } catch (NumberFormatException e) {
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

        if (!Settings.CompatibilityMode) {
            for (Material typ : Settings.DisableShadowBlocks) {
                worldHandler.disableShadow(typ);
            }
        }
        adventure = BukkitAudiences.create(this);
        
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

            CraftManager.initialize();

            getServer().getPluginManager().registerEvents(new InteractListener(), this);

            this.getCommand("movecraft").setExecutor(new MovecraftCommand());
            this.getCommand("release").setExecutor(new ReleaseCommand());
            this.getCommand("pilot").setExecutor(new PilotCommand());
            this.getCommand("rotate").setExecutor(new RotateCommand());
            this.getCommand("cruise").setExecutor(new CruiseCommand());
            this.getCommand("craftreport").setExecutor(new CraftReportCommand());
            this.getCommand("manoverboard").setExecutor(new ManOverboardCommand());
            this.getCommand("contacts").setExecutor(new ContactsCommand());
            this.getCommand("scuttle").setExecutor(new ScuttleCommand());

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


    public WorldHandler getWorldHandler(){
        return worldHandler;
    }

    public AsyncManager getAsyncManager(){return asyncManager;}
}

