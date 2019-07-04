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

package net.countercraft.movecraft.listener;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.WorldEditUpdateCommand;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//import com.sk89q.worldedit.blocks.BaseBlock;
//import com.sk89q.worldedit.world.DataException;
@Deprecated
public class WorldEditInteractListener implements Listener {
    private static final Map<Player, Long> timeMap = new HashMap<>();
    private static final Map<Player, Long> repairRightClickTimeMap = new HashMap<>();

    public boolean repairRegion(World w, String regionName) {
        if (w == null || regionName == null)
            return false;
        String repairStateName = Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/RegionRepairStates";
        repairStateName += "/";
        repairStateName += regionName.replaceAll("\\s+", "_");
        repairStateName += ".schematic";
        File file = new File(repairStateName);
        if (!file.exists()) {
            return false;
        }
        SchematicFormat sf = SchematicFormat.getFormat(file);
        CuboidClipboard cc;
        try {
            cc = sf.load(file);
        } catch (DataException | IOException e) {
            e.printStackTrace();
            return false;
        }
        int minx = cc.getOrigin().getBlockX();
        int miny = cc.getOrigin().getBlockY();
        int minz = cc.getOrigin().getBlockZ();
        int maxx = minx + cc.getWidth();
        int maxy = miny + cc.getHeight();
        int maxz = minz + cc.getLength();
        for (int x = minx; x < maxx; x++) {
            for (int y = miny; y < maxy; y++) {
                for (int z = minz; z < maxz; z++) {
                    Vector ccloc = new Vector(x - minx, y - miny, z - minz);
                    BaseBlock bb = cc.getBlock(ccloc);
                    if (!bb.isAir()) { // most blocks will be air, quickly move on to the next. This loop will run 16 million times, needs to be fast
                        if (Settings.AssaultDestroyableBlocks.contains(bb.getId())) {
                            if (!w.getChunkAt(x >> 4, z >> 4).isLoaded())
                                w.loadChunk(x >> 4, z >> 4);
                            if (w.getBlockAt(x, y, z).isEmpty() || w.getBlockAt(x, y, z).isLiquid()) {
                                MovecraftLocation moveloc = new MovecraftLocation(x, y, z);
                                WorldEditUpdateCommand updateCommand = new WorldEditUpdateCommand(bb, w, moveloc, Material.getMaterial(bb.getType()), (byte) bb.getData());
                                MapUpdateManager.getInstance().scheduleUpdate(updateCommand);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

}
