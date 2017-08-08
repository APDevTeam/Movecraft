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
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.mapUpdater.update.MapUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapUpdateManager extends BukkitRunnable {

    // only bother to store tile entities for blocks we care about (chests, dispensers, etc)
    // this is more important than it may seem. The more blocks that matter, the more likely
    // a circular trap will occur (a=b, b=c. c=a), potentially damaging tile data
    private final int[] tileEntityBlocksToPreserve = {
            Material.DISPENSER.getId(), Material.NOTE_BLOCK.getId(), Material.CHEST.getId(),
            Material.FURNACE.getId(), Material.BURNING_FURNACE.getId(), Material.SIGN_POST.getId(),
            Material.WALL_SIGN.getId(), Material.COMMAND.getId(), Material.TRAPPED_CHEST.getId(),
            Material.DAYLIGHT_DETECTOR.getId(), Material.HOPPER.getId(), Material.DROPPER.getId(),
            Material.DAYLIGHT_DETECTOR_INVERTED.getId(), Material.COMMAND_REPEATING.getId(), Material.COMMAND_CHAIN.getId()};
    private List<UpdateCommand> updates = new ArrayList<>();
    //private PriorityQueue<UpdateCommand> updateQueue = new PriorityQueue<>();

    public HashMap<Craft, Integer> blockUpdatesPerCraft = new HashMap<>();


    private MapUpdateManager() {
    }

    public static MapUpdateManager getInstance() {
        return MapUpdateManagerHolder.INSTANCE;
    }



    public void run() {
        if (updates.isEmpty()) return;
        long startTime = System.currentTimeMillis();
        //ArrayList<net.minecraft.server.v1_10_R1.Chunk> chunksToRelight = new ArrayList<>();

        // and set all crafts that were updated to not processing

        for (UpdateCommand update : updates) {
            update.doUpdate();
            if (update instanceof MapUpdateCommand) {
                Craft craft = ((MapUpdateCommand) update).getCraft();
                if (!craft.isNotProcessing())
                    craft.setProcessing(false);
            }
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
            Movecraft.getInstance().getServer().broadcastMessage("Map update took (ms): " + (endTime - startTime));
        }
        updates.clear();
    }


    public void scheduleUpdate(UpdateCommand update){
        updates.add(update);
    }

    private static class MapUpdateManagerHolder {
        private static final MapUpdateManager INSTANCE = new MapUpdateManager();
    }

}
