package net.countercraft.movecraft.commands;

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

public class MovecraftCommandManager extends PaperCommandManager {
    public MovecraftCommandManager(Plugin plugin) {
        super(plugin);
    }

    private static final Pattern COMMA = Pattern.compile(",");
    private static final Pattern PIPE = Pattern.compile("\\|");

    @Override
    public boolean hasPermission(CommandIssuer issuer, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        //handle AND like normal using comma ","
        String[] perms = COMMA.split(permission);
        if (perms.length > 1) {
            return super.hasPermission(issuer, new HashSet<>(Arrays.asList(perms)));
        }

        //handle OR using pipe "|"
        CommandSender sender = issuer.getIssuer();
        for (String perm : PIPE.split(permission)) {
            perm = perm.trim();
            if (!perm.isEmpty() && senderHasPermission(sender, perm)) {
                return true;
            }
        }

        return false;
    }

    public static boolean senderHasPermission(@NotNull CommandSender sender, @NotNull String perm) {
        if (!(sender instanceof Player p)) {
            return sender.isOp();
        }

        if (p.isOp())
            return true;

        if (p.hasPermission(perm) || p.hasPermission("movecraft.all")) {
            return true;
        }

        return false;
    }

}
