package net.countercraft.movecraft.utils;

import org.bukkit.ChatColor;
import net.countercraft.movecraft.localisation.I18nSupport;

public class ChatUtils {
    public static final String MOVECRAFT_COMMAND_PREFIX = ChatColor.GOLD + "[" + ChatColor.WHITE + "Movecraft" + ChatColor.GOLD + "] " + ChatColor.RESET;
    public static final String ERROR_PREFIX = ChatColor.RED +"[" + I18nSupport.getInternationalisedString("Error") + "]" + ChatColor.RESET;
}
