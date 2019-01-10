package net.countercraft.movecraft.warfare.assault;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashSet;
import java.util.UUID;

public class AssaultUtils {

    public static boolean areDefendersOnline(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getOwners().getUniqueIds());
        int numOnline = 0;

        if(Settings.AssaultRequiredOwnersOnline > 0) {
            for (UUID playerName : players) {
                if (Bukkit.getPlayer(playerName) != null) {
                    numOnline++;
                }
            }

            if (numOnline < Settings.AssaultRequiredOwnersOnline) {
                return false;
            }
            numOnline = 0;
        }

        players.addAll(tRegion.getMembers().getUniqueIds());
        for (UUID playerName : players) {
            if (Bukkit.getPlayer(playerName) != null) {
                numOnline++;
            }
        }
        return numOnline >= Settings.AssaultRequiredDefendersOnline;

    }

    public static double getCostToAssault(ProtectedRegion tRegion) {
        return getAssaultBalance(tRegion) * Settings.AssaultCostPercent / 100.0;
    }

    public static double getMaxDamages(ProtectedRegion tRegion) {

        return getAssaultBalance(tRegion) * Settings.AssaultDamagesCapPercent / 100.0;
    }

    private static double getAssaultBalance(ProtectedRegion tRegion) {
        return getOwnerBalance(tRegion) + getMemberBalance(tRegion);
    }

    private static double getOwnerBalance(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getOwners().getUniqueIds());
        double total = 0.0;
        for (UUID playerName : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerName);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > Settings.AssaultMaxBalance)
                    total += Settings.AssaultMaxBalance;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * (Settings.AssaultOwnerWeightPercent / 100.0);
    }

    private static double getMemberBalance(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getMembers().getUniqueIds());
        double total = 0.0;
        for (UUID playerName : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerName);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > Settings.AssaultMaxBalance)
                    total += Settings.AssaultMaxBalance;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * (Settings.AssaultMemberWeightPercent / 100.0);
    }
}
