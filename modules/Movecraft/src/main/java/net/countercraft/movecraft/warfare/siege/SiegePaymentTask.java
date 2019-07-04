package net.countercraft.movecraft.warfare.siege;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.logging.Level;
import java.util.*;

public class SiegePaymentTask extends SiegeTask {

    public SiegePaymentTask(Siege siege) {
        super(siege);
    }

    @Override
    public void run() {
        // and now process payments every morning at 1:01 AM, as long as it has
        // been 23 hours after the last payout
        long secsElapsed = (System.currentTimeMillis() - siege.getLastPayout()) / 1000;

        if (secsElapsed > 23 * 60 * 60) {
            Calendar rightNow = Calendar.getInstance();
            int hour = rightNow.get(Calendar.HOUR_OF_DAY);
            int minute = rightNow.get(Calendar.MINUTE);
            if ((hour == 1) && (minute == 1)) {
                for (World tW : Movecraft.getInstance().getServer().getWorlds()) {
                    ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(tW).getRegion(siege.getCaptureRegion());

                    if (tRegion != null) {
                        payRegion(tRegion);
                        return;
                    }
                }
            }
        }
    }

    private void payRegion(ProtectedRegion region) {
        ArrayList<OfflinePlayer> owners = new ArrayList<>();

        for(String name : region.getOwners().getPlayers()) {
            owners.add(Movecraft.getInstance().getServer().getOfflinePlayer(name));
        }
        for(UUID uuid : region.getOwners().getUniqueIds()) {
            owners.add(Movecraft.getInstance().getServer().getOfflinePlayer(uuid));
        }

        int share = siege.getDailyIncome() / owners.size();

        for (OfflinePlayer player : owners) {
            Movecraft.getInstance().getEconomy().depositPlayer(player, share);
            Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Siege - Ownership Payout Console"), player.getName(), share, siege.getName()));
        }
        siege.setLastPayout(System.currentTimeMillis());
    }
}
