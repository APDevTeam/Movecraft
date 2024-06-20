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

package net.countercraft.movecraft.mapUpdater;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

@Deprecated
public class MapUpdateManager extends BukkitRunnable {

    private final Queue<UpdateCommand> updates = new ConcurrentLinkedQueue<>();
//    private final Queue<UpdateCommand> updates = new LinkedBlockingQueue<>();
    //private PriorityQueue<UpdateCommand> updateQueue = new PriorityQueue<>();

    //@Deprecated
    //public HashMap<Craft, Integer> blockUpdatesPerCraft = new HashMap<>();

    private MapUpdateManager() { }

    public static MapUpdateManager getInstance() {
        return MapUpdateManagerHolder.INSTANCE;
    }

    public void run() {
        if (updates.isEmpty()) return;
        Logger logger = Movecraft.getInstance().getLogger();
        long startTime = System.currentTimeMillis();
        // and set all crafts that were updated to not processing

        //updates.sort((o1, o2) -> o1.getClass().getName().compareToIgnoreCase(o2.getClass().getName()));




        UpdateCommand next = updates.poll();
        while(next != null) {
            next.doUpdate();
            next = updates.poll();
        }

        //TODO: re-add lighting updates
        /*// queue chunks for lighting recalc
        if (!Settings.CompatibilityMode) {
            for (net.minecraft.server.v1_10_R1.Chunk c : chunksToRelight) {
                ChunkUpdater fChunk = FastBlockChanger.getInstance().getChunk(c.world, c.locX, c.locZ, true);
                for (int bx = 0; bx < 16; bx++) {
                    for (int bz = 0; bz < 16; bz++) {
                        for (int by = 0; by < 4; by++) {
                            fChunk.bits[bx][bz][by] = Long.MAX_VALUE;
                        }
                    }
                }
                fChunk.last_modified = System.currentTimeMillis();
                c.e();
            }
        }*/
        if (Settings.Debug) {
            long endTime = System.currentTimeMillis();
            logger.info("Map update took (ms): " + (endTime - startTime));
        }
    }


    public void scheduleUpdate(@NotNull UpdateCommand update){
        updates.add(update);
    }

    public void scheduleUpdates(@NotNull UpdateCommand... updates){
        Collections.addAll(this.updates, updates);
    }

    public void scheduleUpdates(@NotNull Collection<UpdateCommand> updates){
        this.updates.addAll(updates);
    }

    private static class MapUpdateManagerHolder {
        private static final MapUpdateManager INSTANCE = new MapUpdateManager();
    }

}
