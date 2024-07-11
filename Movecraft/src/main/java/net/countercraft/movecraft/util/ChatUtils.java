package net.countercraft.movecraft.util;

import net.countercraft.movecraft.localisation.I18nSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public class ChatUtils {
    @Deprecated(forRemoval = true)
    public static final String MOVECRAFT_COMMAND_PREFIX = ChatColor.GOLD + "[" + ChatColor.WHITE + "Movecraft" + ChatColor.GOLD + "] " + ChatColor.RESET;
    @Deprecated(forRemoval = true)
    public static final String ERROR_PREFIX = ChatColor.RED +"[" + I18nSupport.getInternationalisedString("Error") + "]" + ChatColor.RESET;

    public static @NotNull Component commandPrefix() {
        return Component.text("[", NamedTextColor.GOLD)
                .append(Component.text("Movecraft", (NamedTextColor.WHITE)))
                .append(Component.text("] ", NamedTextColor.GOLD));
    }

    public static @NotNull Component errorPrefix() {
        return Component.text("[")
                .append(I18nSupport.getInternationalisedComponent("Error"))
                .append(Component.text("] "))
                .color(NamedTextColor.RED);
    }
}
