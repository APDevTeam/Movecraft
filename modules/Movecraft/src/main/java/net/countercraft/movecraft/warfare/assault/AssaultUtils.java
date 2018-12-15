package net.countercraft.movecraft.warfare.assault;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BossBar;

import java.util.HashSet;
import java.util.UUID;

public class AssaultUtils {

    public static boolean areDefendersOnline(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getMembers().getUniqueIds());
        players.addAll(tRegion.getOwners().getUniqueIds());
        int numOnline = 0;
        for (UUID playerName : players) {
            if (Bukkit.getPlayer(playerName) != null) {
                numOnline++;
            }
        }
        return numOnline >= Settings.AssaultRequiredDefendersOnline;
    }


    public static double getCostToAssault(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getMembers().getUniqueIds());
        players.addAll(tRegion.getOwners().getUniqueIds());
        double total = 0.0;
        for (UUID playerName : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerName);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > 5000000)
                    total += 5000000;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * Settings.AssaultCostPercent / 100.0;
    }

    public static double getMaxDamages(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getMembers().getUniqueIds());
        players.addAll(tRegion.getOwners().getUniqueIds());
        double total = 0.0;
        for (UUID playerName : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerName);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > 5000000)
                    total += 5000000;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * (Settings.AssaultDamagesCapPercent / 100.0);
    }
}
