package net.countercraft.movecraft.warfare.siege;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.World;

import java.util.Calendar;
import java.util.logging.Level;

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
                siege.setLastPayout(System.currentTimeMillis());
                //for (String tSiegeName : Settings.SiegeName) {
                for (World tW : Movecraft.getInstance().getServer().getWorlds()) {
                    ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(tW).getRegion(siege.getCaptureRegion());
                    if (tRegion != null) {
                        for (String tPlayerName : tRegion.getOwners().getPlayers()) {
                            int share = siege.getDailyIncome() / tRegion.getOwners().getPlayers().size();
                            Movecraft.getInstance().getEconomy().depositPlayer(tPlayerName, share);
                            Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Siege - Ownership Payout Console"), tPlayerName, share, siege.getName()));
                        }
                    }
                    else {
                        Movecraft.getInstance().getLogger().log(Level.INFO, "[SIEGE DEBUG] Siege region null in world");
                        Movecraft.getInstance().getLogger().log(Level.INFO, "[SIEGE DEBUG] World: " + tW.toString());

                    }
                }
            }
            else {
                Movecraft.getInstance().getLogger().log(Level.INFO, "[SIEGE DEBUG] Not 1:01 AM");
                Movecraft.getInstance().getLogger().log(Level.INFO, "[SIEGE DEBUG] Time: " + Integer.toString(hour) + ":" + Integer.toString(minute));
            }
        }
        else {
            Movecraft.getInstance().getLogger().log(Level.INFO,"[SIEGE DEBUG] Siege payment within 23 hours");
            Movecraft.getInstance().getLogger().log(Level.INFO,"[SIEGE DEBUG] Seconds Elapsed: " + Long.toString(secsElapsed));
        }
    }
}
