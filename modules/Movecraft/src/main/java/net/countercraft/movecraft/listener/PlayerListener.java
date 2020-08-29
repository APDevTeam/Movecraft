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

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

public class PlayerListener implements Listener {
    private final Map<Craft, Long> timeToReleaseAfter = new WeakHashMap<>();

    private String checkCraftBorders(Craft craft) {
        String ret = "";
        final int[] ALLOWED_BLOCKS = craft.getType().getAllowedBlocks();
        final int[] FORBIDDEN_BLOCKS = craft.getType().getForbiddenBlocks();
        final MovecraftLocation[] SHIFTS = {
                //x
                new MovecraftLocation(-1, 0, 0),
                new MovecraftLocation(-1, -1, 0),
                new MovecraftLocation(-1,1,0),
                new MovecraftLocation(1, -1, 0),
                new MovecraftLocation(1, 1, 0),
                new MovecraftLocation(1, 0, 0),
                //z
                new MovecraftLocation(0, 1, 1),
                new MovecraftLocation(0, 0, 1),
                new MovecraftLocation(0, -1, 1),
                new MovecraftLocation(0, 1, -1),
                new MovecraftLocation(0, 0, -1),
                new MovecraftLocation(0, -1, -1),
                //y
                new MovecraftLocation(0, 1, 0),
                new MovecraftLocation(0, -1, 0)};
        //Check each location in the hitbox
        for (MovecraftLocation ml : craft.getHitBox()){
            //Check the surroundings of each location
            for (MovecraftLocation shift : SHIFTS){
                MovecraftLocation test = ml.add(shift);
                //Ignore locations contained in the craft's hitbox
                if (craft.getHitBox().contains(test)){
                    continue;
                }
                Block testBlock = test.toBukkit(craft.getW()).getBlock();
                int typeID = testBlock.getTypeId();
                int metaData = testBlock.getData();
                int shiftedID = 10000 + (typeID << 4) + metaData;
                //Break the loop if an allowed block is found adjacent to the craft's hitbox
                if (Arrays.binarySearch(ALLOWED_BLOCKS, typeID) >= 0 || Arrays.binarySearch(ALLOWED_BLOCKS, shiftedID) >= 0){
                    ret = "@ " + test.toString() + " " + Material.getMaterial(typeID).name();
                    break;
                }
                //Do the same if a forbidden block is found
                else if (Arrays.binarySearch(FORBIDDEN_BLOCKS, typeID) >= 0 || Arrays.binarySearch(FORBIDDEN_BLOCKS, shiftedID) >= 0){
                    ret = "@ " + test.toString() + " " + Material.getMaterial(typeID).name();
                    break;
                }
            }
            //When a block that can merge is found, break this loop
            if (ret.length() > 0){
                break;
            }
        }
        //Return the string representation of the merging point and alert the pilot
        return ret;
    }

    @EventHandler
    public void onPLayerLogout(PlayerQuitEvent e) {
        CraftManager.getInstance().removeCraftByPlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(EntityDamageByEntityEvent e) {  // changed to death so when you shoot up an airship and hit the pilot, it still sinks
        if (e instanceof Player) {
            Player p = (Player) e;
            CraftManager.getInstance().removeCraft(CraftManager.getInstance().getCraftByPlayer(p), CraftReleaseEvent.Reason.DEATH);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        final Craft c = CraftManager.getInstance().getCraftByPlayer(p);

        if (c == null) {
            return;
        }

        if(MathUtils.locationNearHitBox(c.getHitBox(), p.getLocation(), 2)){
            timeToReleaseAfter.remove(c);
            return;
        }

        if(timeToReleaseAfter.containsKey(c) && timeToReleaseAfter.get(c) < System.currentTimeMillis()){
            CraftManager.getInstance().removeCraft(c, CraftReleaseEvent.Reason.PLAYER);
            timeToReleaseAfter.remove(c);
            return;
        }

        if (c.isNotProcessing() && c.getType().getMoveEntities() && !timeToReleaseAfter.containsKey(c)) {
            if (Settings.ManOverboardTimeout != 0) {
                p.sendMessage(I18nSupport.getInternationalisedString("Manoverboard - Player has left craft"));
                String mergePoint = checkCraftBorders(c);
                if (mergePoint.length() > 0){
                    p.sendMessage(I18nSupport.getInternationalisedString("Manoverboard - Craft May Merge") + " " + mergePoint);
                }
                CraftManager.getInstance().addOverboard(p);
            } else {
                p.sendMessage(I18nSupport.getInternationalisedString("Release - Player has left craft"));
            }
            timeToReleaseAfter.put(c, System.currentTimeMillis() + 30000); //30 seconds to release TODO: config
        }
    }
}
