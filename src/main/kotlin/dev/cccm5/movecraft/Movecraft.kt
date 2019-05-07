package dev.cccm5.movecraft

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

class Movecraft : JavaPlugin() {

    companion object{
        val logger : Logger = Bukkit.getLogger();
        lateinit var plugin: Plugin
    }

    override fun onEnable() {
        logger.info{"Block shift has started"}
        plugin = this
        CraftStateManager
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
