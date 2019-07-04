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
            //if ((hour == 1) && (minute == 1)) {
            if( rightNow.get(Calendar.SECOND) == 0) {
                output("Running siege: " + siege);
                siege.setLastPayout(System.currentTimeMillis());
                for (World tW : Movecraft.getInstance().getServer().getWorlds()) {
                    output("Processing world: " + tW);
                    ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(tW).getRegion(siege.getCaptureRegion());
                    if (tRegion != null) {
                        output("Region " + siege.getCaptureRegion() + " found in world: " + tW);
                        for (String tPlayerName : tRegion.getOwners().getPlayers()) {
                            output("Processing owner: " + tPlayerName);
                            int share = siege.getDailyIncome() / tRegion.getOwners().getPlayers().size();
                            Movecraft.getInstance().getEconomy().depositPlayer(tPlayerName, share);
                            Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Siege - Ownership Payout Console"), tPlayerName, share, siege.getName()));
                        }
                    }
                    else {
                        output("Region " + siege.getCaptureRegion() + " null in world: " + tW);
                    }
                }
            }
        }
        else {
            output("Siege payment within 23 hours, " + Long.toString(secsElapsed));
        }
    }

    //TODO: temp function, remove after usage
    private void output(String s) {
        Movecraft.getInstance().getLogger().log(Level.INFO,"[SIEGE DEBUG] " + s);
    }
}
