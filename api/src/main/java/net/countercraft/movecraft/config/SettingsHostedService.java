package net.countercraft.movecraft.config;

import jakarta.inject.Inject;
import net.countercraft.movecraft.lifecycle.HostedService;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.Plugin;
import org.int4.dirk.annotations.Opt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class SettingsHostedService implements HostedService {
    private final @NotNull Configuration configuration;
    private final @NotNull Logger logger;

    @Inject
    public SettingsHostedService(@NotNull Plugin plugin, @NotNull Logger logger) {
        this.configuration = plugin.getConfig();
        this.logger = logger;
    }

    @Override
    public void start() {
        Settings.LOCALE = configuration.getString("Locale");
        Settings.Debug = configuration.getBoolean("Debug", false);
        Settings.DisableNMSCompatibilityCheck = configuration.getBoolean("IReallyKnowWhatIAmDoing", false);
        Settings.DisableSpillProtection = configuration.getBoolean("DisableSpillProtection", false);
        Settings.DisableIceForm = configuration.getBoolean("DisableIceForm", true);
        Settings.ReleaseOnDeath = configuration.getBoolean("ReleaseOnDeath", false);
        Settings.SinkCheckTicks = configuration.getDouble("SinkCheckTicks", 100.0);
        Settings.ManOverboardTimeout = configuration.getInt("ManOverboardTimeout", 30);
        Settings.ManOverboardDistSquared = Math.pow(configuration.getDouble("ManOverboardDistance", 1000), 2);
        Settings.SilhouetteViewDistance = configuration.getInt("SilhouetteViewDistance", 200);
        Settings.SilhouetteBlockCount = configuration.getInt("SilhouetteBlockCount", 20);
        Settings.ProtectPilotedCrafts = configuration.getBoolean("ProtectPilotedCrafts", false);
        Settings.MaxRemoteSigns = configuration.getInt("MaxRemoteSigns", -1);
        Settings.CraftsUseNetherPortals = configuration.getBoolean("CraftsUseNetherPortals", false);
        Settings.RequireCreatePerm = configuration.getBoolean("RequireCreatePerm", false);
        Settings.RequireNamePerm = configuration.getBoolean("RequireNamePerm", true);
        Settings.FadeWrecksAfter = configuration.getInt("FadeWrecksAfter", 0);
        Settings.FadeTickCooldown = configuration.getInt("FadeTickCooldown", 20);
        Settings.FadePercentageOfWreckPerCycle = configuration.getDouble("FadePercentageOfWreckPerCycle", 10.0);

        // if the PilotTool is specified in the config.yml file, use it
        String pilotTool = configuration.getString("PilotTool");
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

        if (configuration.contains("ExtraFadeTimePerBlock")) {
            Map<String, Object> temp = configuration.getConfigurationSection("ExtraFadeTimePerBlock").getValues(false);
            for (String str : temp.keySet()) {
                Set<Material> materials = Tags.parseMaterials(str);
                for (Material m : materials) {
                    Settings.ExtraFadeTimePerBlock.put(m, (Integer) temp.get(str));
                }
            }
        }

        Settings.ForbiddenRemoteSigns = new HashSet<>(configuration.getStringList("ForbiddenRemoteSigns"));
    }

}
