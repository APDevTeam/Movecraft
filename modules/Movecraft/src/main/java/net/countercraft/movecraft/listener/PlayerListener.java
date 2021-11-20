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
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.BlockHighlight;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

public class PlayerListener implements Listener {
    private final Map<Craft, Long> timeToReleaseAfter = new WeakHashMap<>();
    private final Map<Craft, Queue<Integer>> highlights = new WeakHashMap<>();

    private Set<Location> checkCraftBorders(Craft craft) {
        Set<Location> mergePoints = new HashSet<>();
        final EnumSet<Material> ALLOWED_BLOCKS = craft.getType().getMaterialSetProperty(CraftType.ALLOWED_BLOCKS);
        final EnumSet<Material> FORBIDDEN_BLOCKS = craft.getType().getMaterialSetProperty(CraftType.FORBIDDEN_BLOCKS);
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
                Block testBlock = test.toBukkit(craft.getWorld()).getBlock();
                Material testMaterial = testBlock.getType();
                //Break the loop if an allowed block is found adjacent to the craft's hitbox
                if (ALLOWED_BLOCKS.contains(testMaterial)){
                    mergePoints.add(testBlock.getLocation());
                }
                //Do the same if a forbidden block is found
                else if (FORBIDDEN_BLOCKS.contains(testMaterial)){
                    mergePoints.add(testBlock.getLocation());
                }
            }
        }
        //Return the string representation of the merging point and alert the pilot
        return mergePoints;
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

        highlights.computeIfAbsent(c, (craft) -> new LinkedList<>());

        if(MathUtils.locationNearHitBox(c.getHitBox(), p.getLocation(), 2)){
            timeToReleaseAfter.remove(c);
            clearHighlights(c, p);
            return;
        }

        if(timeToReleaseAfter.containsKey(c) && timeToReleaseAfter.get(c) < System.currentTimeMillis()){
            CraftManager.getInstance().removeCraft(c, CraftReleaseEvent.Reason.PLAYER);
            timeToReleaseAfter.remove(c);
            clearHighlights(c, p);
            return;
        }

        if (c.isNotProcessing() && c.getType().getBoolProperty(CraftType.MOVE_ENTITIES) && !timeToReleaseAfter.containsKey(c)) {
            if (Settings.ManOverboardTimeout != 0) {
                c.getAudience().sendActionBar(I18nSupport.getInternationalisedComponent("Manoverboard - Player has left craft"));
                CraftManager.getInstance().addOverboard(p);
            } else {
                p.sendMessage(I18nSupport.getInternationalisedString("Release - Player has left craft"));
            }
            var mergePoints = checkCraftBorders(c);
            if(!mergePoints.isEmpty()){
                p.sendMessage(I18nSupport.getInternationalisedString("Manoverboard - Craft May Merge"));
            }
            for(var location : mergePoints){
                highlights.get(c).add(BlockHighlight.highlightBlockAt(location, p));
            }
            timeToReleaseAfter.put(c, System.currentTimeMillis() + c.getType().getIntProperty(CraftType.RELEASE_TIMEOUT) * 1000L);
        }
    }

    @EventHandler
    public void onCraftRelease(CraftReleaseEvent event){
        if(event.getCraft() instanceof PlayerCraft){
            clearHighlights(event.getCraft(), ((PlayerCraft) event.getCraft()).getPlayer());
        }
    }

    private void clearHighlights(Craft craft, Player player){
        highlights.computeIfAbsent(craft, (c) -> new LinkedList<>());
        var queue = highlights.get(craft);
        BlockHighlight.removeHighlights(queue, player);
        queue.clear();
    }
}
