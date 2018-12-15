package net.countercraft.movecraft.utils;

import org.bukkit.ChatColor;

public class ChatUtils {
    public static final String MOVECRAFT_COMMAND_PREFIX = ChatColor.GOLD + "[" + ChatColor.WHITE + "Movecraft" + ChatColor.GOLD + "] " + ChatColor.RESET;

    public static String timeFromMilliSeconds(long timeInMillisec){
        long hour = (timeInMillisec / 1000) / 3600;
        long minute = (timeInMillisec / 1000) / 60 - hour * 60;
        long second = (timeInMillisec / 1000) - minute * 60;
        String time = String.format("%d:%d:%d", hour, minute, second);
        return time;
    }
}
