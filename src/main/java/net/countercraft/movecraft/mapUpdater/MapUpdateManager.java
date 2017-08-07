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
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.mapUpdater.update.MapUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.FastBlockChanger;
import net.countercraft.movecraft.utils.FastBlockChanger.ChunkUpdater;
import net.countercraft.movecraft.utils.Rotation;
import net.minecraft.server.v1_10_R1.EnumBlockRotation;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapUpdateManager extends BukkitRunnable {
    private static EnumBlockRotation ROTATION[];

    static {
        ROTATION = new EnumBlockRotation[3];
        ROTATION[Rotation.NONE.ordinal()] = EnumBlockRotation.NONE;
        ROTATION[Rotation.CLOCKWISE.ordinal()] = EnumBlockRotation.CLOCKWISE_90;
        ROTATION[Rotation.ANTICLOCKWISE.ordinal()] = EnumBlockRotation.COUNTERCLOCKWISE_90;
    }

    // only bother to store tile entities for blocks we care about (chests, dispensers, etc)
    // this is more important than it may seem. The more blocks that matter, the more likely
    // a circular trap will occur (a=b, b=c. c=a), potentially damaging tile data
    private final int[] tileEntityBlocksToPreserve = {
            Material.DISPENSER.getId(), Material.NOTE_BLOCK.getId(), Material.CHEST.getId(),
            Material.FURNACE.getId(), Material.BURNING_FURNACE.getId(), Material.SIGN_POST.getId(),
            Material.WALL_SIGN.getId(), Material.COMMAND.getId(), Material.TRAPPED_CHEST.getId(),
            Material.DAYLIGHT_DETECTOR.getId(), Material.HOPPER.getId(), Material.DROPPER.getId(),
            Material.DAYLIGHT_DETECTOR_INVERTED.getId(), Material.COMMAND_REPEATING.getId(), Material.COMMAND_CHAIN.getId()};
    //    private final HashMap<World, ArrayList<MapUpdateCommand>> updates = new HashMap<>();
//    private final HashMap<World, ArrayList<EntityUpdateCommand>> entityUpdates = new HashMap<>();
//    private final HashMap<World, ArrayList<ItemDropUpdateCommand>> itemDropUpdates = new HashMap<>();
//    private final HashMap<World, ArrayList<ExplosionUpdateCommand>> explosionUpdates = new HashMap<>();
    private List<UpdateCommand> updates = new ArrayList<>();
    public HashMap<Craft, Integer> blockUpdatesPerCraft = new HashMap<>();


    private MapUpdateManager() {
    }

    public static MapUpdateManager getInstance() {
        return MapUpdateManagerHolder.INSTANCE;
    }

    private void addBlockUpdateTracking(Craft craft, int qty) {
        if (craft == null)
            return;
        if (blockUpdatesPerCraft.containsKey(craft)) {
            blockUpdatesPerCraft.put(craft, blockUpdatesPerCraft.get(craft) + qty);
        } else {
            blockUpdatesPerCraft.put(craft, qty);
        }
    }

    private void addBlockUpdateTracking(Craft craft) {
        if (craft == null)
            return;
        if (blockUpdatesPerCraft.containsKey(craft)) {
            blockUpdatesPerCraft.put(craft, blockUpdatesPerCraft.get(craft) + 1);
        } else {
            blockUpdatesPerCraft.put(craft, 1);
        }
    }

    public void run() {
        if (updates.isEmpty()) return;
        long startTime = System.currentTimeMillis();
        ArrayList<net.minecraft.server.v1_10_R1.Chunk> chunksToRelight = new ArrayList<>();



        // and set all crafts that were updated to not processing

        for (UpdateCommand update : updates) {
            if (update instanceof MapUpdateCommand) {
                Craft craft = ((MapUpdateCommand) update).getCraft();
                if (!craft.isNotProcessing()) {
                    craft.setProcessing(false);
                }

            }
        }


        // queue chunks for lighting recalc
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
        }

        long endTime = System.currentTimeMillis();
        if (Settings.Debug) {
            Movecraft.getInstance().getServer().broadcastMessage("Map update took (ms): " + (endTime - startTime));
        }


        updates.clear();

    }



    // NOTE: The below is slow and should NOT be run synchronously if it can be avoided!
    public void sortUpdates(MapUpdateCommand[] mapUpdates) {
        // the point of this is to sort the block updates so that an update never overwrites the source of a later update

/*		boolean sorted=false;
		HashMap<MovecraftLocation,Integer> newBlockLocationIndexes=new HashMap<MovecraftLocation,Integer>();
		Integer index=0;
		for(MapUpdateCommand i : mapUpdates) {
			if(i.getOldBlockLocation()!=null) {
//				if((Arrays.binarySearch(tileEntityBlocksToPreserve,i.getType())>=0) || (Arrays.binarySearch(tileEntityBlocksToPreserve,i.getCurrentType())>=0)) {
					newBlockLocationIndexes.put(i.getNewBlockLocation(), index);
	//			}
		//	} else {
			//	if(Arrays.binarySearch(tileEntityBlocksToPreserve,i.getType())>=0)
					newBlockLocationIndexes.put(i.getNewBlockLocation(), index);
			}
			index++;
		}

		int iterations=0;
		while(!sorted && iterations<25) {
			iterations++;
			sorted=true;
			for(index=0; index<mapUpdates.length; index++) {
				MapUpdateCommand i=mapUpdates[index];
				if(i.getOldBlockLocation()!=null) {
					boolean needsSort=false;
					if(Arrays.binarySearch(tileEntityBlocksToPreserve,i.getType())>=0) {
						needsSort=true;
					} else {
//						int sourceIndex=newBlockLocationIndexes.get(i.getOldBlockLocation());
						if(Arrays.binarySearch(tileEntityBlocksToPreserve,i.getCurrentType())>=0) {
							needsSort=true;
						}
					}
					if(needsSort) {
						Integer sourceIndex=newBlockLocationIndexes.get(i.getOldBlockLocation());
						if((sourceIndex!=null) && (sourceIndex<index)) {
							sorted=false;
							MapUpdateCommand temp=i;
							mapUpdates[index]=mapUpdates[sourceIndex];
							mapUpdates[sourceIndex]=temp;
							newBlockLocationIndexes.put(i.getOldBlockLocation(), index);
							newBlockLocationIndexes.put(i.getNewBlockLocation(), sourceIndex);
						}
					}
				}
			}
		}
		iterations++; // just to give a convenient breakpoint*/
    }

    public void scheduleUpdate(UpdateCommand update){
        updates.add(update);
    }

    /*@Deprecated
    public boolean addWorldUpdate(World w, MapUpdateCommand[] mapUpdates, EntityUpdateCommand[] eUpdates, ItemDropUpdateCommand[] iUpdates, ExplosionUpdateCommand[] exUpdates) {

        if (mapUpdates != null) {
            ArrayList<MapUpdateCommand> get = updates.get(w);
            if (get != null) {
                updates.remove(w);
                ArrayList<MapUpdateCommand> tempUpdates = new ArrayList<>();
                tempUpdates.addAll(Arrays.asList(mapUpdates));
                get.addAll(tempUpdates);
            } else {
                get = new ArrayList<>(Arrays.asList(mapUpdates));
            }
            updates.put(w, get);
        }

        //now do entity updates
        if (eUpdates != null) {
            ArrayList<EntityUpdateCommand> eGet = entityUpdates.get(w);
            if (eGet != null) {
                entityUpdates.remove(w);
                ArrayList<EntityUpdateCommand> tempEUpdates = new ArrayList<>();
                tempEUpdates.addAll(Arrays.asList(eUpdates));
                eGet.addAll(tempEUpdates);
            } else {
                eGet = new ArrayList<>(Arrays.asList(eUpdates));
            }
            entityUpdates.put(w, eGet);
        }

        //now do item drop updates
        if (iUpdates != null) {
            ArrayList<ItemDropUpdateCommand> iGet = itemDropUpdates.get(w);
            if (iGet != null) {
                itemDropUpdates.remove(w);
                ArrayList<ItemDropUpdateCommand> tempIDUpdates = new ArrayList<>();
                tempIDUpdates.addAll(Arrays.asList(iUpdates));
                iGet.addAll(tempIDUpdates);
            } else {
                iGet = new ArrayList<>(Arrays.asList(iUpdates));
            }
            itemDropUpdates.put(w, iGet);
        }

        if (exUpdates != null) {
            ArrayList<ExplosionUpdateCommand> exGet = explosionUpdates.get(w);
            if (exUpdates != null) {
                explosionUpdates.remove(w);
                ArrayList<ExplosionUpdateCommand> tempEXUpdates = new ArrayList<>();
                tempEXUpdates.addAll(Arrays.asList(exUpdates));
                exGet.addAll(tempEXUpdates);
            } else {
                exGet = new ArrayList<>(Arrays.asList(exUpdates));
            }
            explosionUpdates.put(w, exGet);
        }

        return false;
    }
*/
    private boolean setContainsConflict(ArrayList<MapUpdateCommand> set, MapUpdateCommand c) {
        for (MapUpdateCommand command : set) {
            if (command != null && c != null)
                if (command.getNewBlockLocation().equals(c.getNewBlockLocation())) {
                    return true;
                }
        }

        return false;
    }

    private boolean arrayContains(int[] oA, int o) {
        for (int testO : oA) {
            if (testO == o) {
                return true;
            }
        }

        return false;
    }

    private static class MapUpdateManagerHolder {
        private static final MapUpdateManager INSTANCE = new MapUpdateManager();
    }

}
