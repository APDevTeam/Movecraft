package net.countercraft.movecraft.utils;

import com.google.gson.Gson;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class UpdateManager extends BukkitRunnable implements Listener {
    private static UpdateManager instance;

    private UpdateManager() {
        runTaskTimerAsynchronously(Movecraft.getInstance(), 40, 36000);
        Bukkit.getPluginManager().registerEvents(this, Movecraft.getInstance());
    }

    @NotNull
    public static UpdateManager getInstance() {
        if (instance == null) {
            instance = new UpdateManager();
        }
        return instance;
    }

    @Override
    public void run() {
        if (!Settings.CheckForUpdates)
            return;
        final Logger log = Movecraft.getInstance().getLogger();
        log.info(I18nSupport.getInternationalisedString("Update - Checking for updates"));
        new BukkitRunnable() {
            @Override
            public void run() {
                String newVersion = getNewVersion();
                if (newVersion == null) {
                    log.info(I18nSupport.getInternationalisedString("Update - Up to date"));
                    return;
                }
                Bukkit.broadcast(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Update - Update available"), "movecraft.update");
                Bukkit.broadcast("https://github.com/eirikh1996/Movecraft/releases/tag/" + newVersion, "movecraft.update");
                log.warning(I18nSupport.getInternationalisedString("Update - Update available"));
                log.warning("https://github.com/eirikh1996/Movecraft/releases/tag/" + newVersion);
            }
        }.runTaskLaterAsynchronously(Movecraft.getInstance(), 100);

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player p = event.getPlayer();
        if (!p.hasPermission("movecraft.update")) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                String newVersion = getNewVersion();
                if (newVersion == null)  {
                    return;
                }
                p.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Update - Update available"));
                p.sendMessage("https://github.com/eirikh1996/Movecraft/releases/tag/" + newVersion);
            }
        }.runTaskLaterAsynchronously(Movecraft.getInstance(), 100);
    }

    @Nullable
    private String getNewVersion() {
        try {
            final URL url = new URL("https://api.github.com/repos/eirikh1996/Movecraft/releases");
            final URLConnection conn = url.openConnection();
            conn.setConnectTimeout(6000);
            conn.addRequestProperty("User-Agent", "Movecraft update checker");
            conn.setDoOutput(true);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            final String response = reader.readLine();
            final Gson gson = new Gson();
            List objList = gson.fromJson(response, List.class);
            if (objList.size() == 0) {
                Movecraft.getInstance().getLogger().severe(I18nSupport.getInternationalisedString("Update - No update or bad URL"));
                return null;
            }
            Map<String, Object> data = (Map<String, Object>) objList.get(0);
            //Get the new version
            String newVersion = (String) data.get("tag_name");
            int betaNo = 0;
            if (newVersion.contains("beta")) {
                String beta = newVersion.substring(newVersion.lastIndexOf("-") + 1);
                betaNo = Integer.parseInt(beta);
            }
            String baseVersion = newVersion.split("-")[0];
            baseVersion = baseVersion.replace("v", "").replace(".", "");
            int nv = Integer.parseInt(baseVersion) * betaNo > 0 ? 10000 : 1;
            nv += betaNo;
            //Then get the current version
            String currentVersion = Movecraft.getInstance().getDescription().getVersion();
            int currBetaNo = 0;
            if (currentVersion.contains("beta")) {
                String beta = currentVersion.substring(currentVersion.lastIndexOf("_") + 1);
                currBetaNo = Integer.parseInt(beta);
            }
            String currBaseVersion = currentVersion.split("_")[0].replace(".", "").replace("v", "");
            int cv = Integer.parseInt(currBaseVersion) * 100;
            cv += currBetaNo;
            if (nv > cv) {
                return newVersion;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
