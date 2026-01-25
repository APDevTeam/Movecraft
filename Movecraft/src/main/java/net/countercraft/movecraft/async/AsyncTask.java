/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.async;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public abstract class AsyncTask extends BukkitRunnable {
    protected final Craft craft;

    protected AsyncTask(Craft c) {
        craft = c;
    }

    public void run() {
        try {
            execute();
            Movecraft.getInstance().getAsyncManager().submitCompletedTask(this);
        } catch (Exception e) {
            Movecraft.getInstance().getLogger().log(Level.SEVERE, I18nSupport.getInternationalisedString("Internal - Error - Processor thread encountered an error"));
            e.printStackTrace();
        }
    }

    protected abstract void execute() throws InterruptedException, ExecutionException;

    protected Craft getCraft() {
        return craft;
    }

    // DONE: Move into it's own async task
    // DONE: Check against craft datatag
    protected boolean checkFuel() {
        if (!FuelBurnRunnable.doesBurnFuel(this.getCraft())) {
            return true;
        }
        return this.getCraft().getDataTag(FuelBurnRunnable.IS_FUELED);
    }
}
