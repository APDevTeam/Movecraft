package net.countercraft.movecraft.config;

import io.papermc.paper.datapack.Datapack;
import jakarta.inject.Inject;
import net.countercraft.movecraft.lifecycle.HostedService;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class DataPackHostedService implements HostedService {
    private final @NotNull Plugin plugin;
    private final @NotNull Logger logger;
    private boolean isInitialized;

    @Inject
    public DataPackHostedService(@NotNull Plugin plugin, @NotNull Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public void start() {
        isInitialized = isDatapackEnabled() || initializeDatapack();
    }

    public boolean isDatapackInitialized(){
        // TODO: Async
        return isInitialized;
    }

    private boolean initializeDatapack() {
        File datapackDirectory = null;
        Server server = plugin.getServer();

        for(var world : server.getWorlds()) {
            datapackDirectory = new File(world.getWorldFolder(), "datapacks");
            if(datapackDirectory.exists()) {
                break;
            }
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
        } else if(new File(datapackDirectory, "movecraft-data.zip").exists()) {
            logger.warning("Conflicting datapack already exists in " + datapackDirectory.getPath() + ". If you would like to regenerate the datapack, delete the existing one.");

            return false;
        }

        if(!datapackDirectory.canWrite()) {
            logger.warning("Missing permissions to write to world directory.");

            return false;
        }

        try(var stream = new FileOutputStream(new File(datapackDirectory, "movecraft-data.zip"));
            var pack = plugin.getResource("movecraft-data.zip")) {
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
        server.dispatchCommand(server.createCommandSender(response -> {}), "datapack list"); // list datapacks to trigger the server to check
        for (Datapack datapack : server.getDatapackManager().getPacks()) {
            if (!datapack.getName().equals("file/movecraft-data.zip")) {
                continue;
            }

            if (!datapack.isEnabled()) {
                datapack.setEnabled(true);
                logger.info("Datapack enabled.");
            }

            break;
        }

        if (!isDatapackEnabled()) {
            throw new IllegalStateException("Failed to automatically load movecraft datapack. Check if it exists.");
        }

        return true;
    }

    private boolean isDatapackEnabled() {
        Server server = plugin.getServer();
        server.dispatchCommand(server.createCommandSender(response -> {}), "datapack list"); // list datapacks to trigger the server to check
        for (Datapack datapack : server.getDatapackManager().getPacks()) {
            if (!datapack.getName().equals("file/movecraft-data.zip")) {
                continue;
            }

            return datapack.isEnabled();
        }

        return false;
    }

}
