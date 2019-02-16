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

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.repair.Repair;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class PlayerListener implements Listener {
    private final Map<Craft, Long> timeToReleaseAfter = new WeakHashMap<>();

    @Deprecated
    private String checkCraftBorders(Craft craft) {
        HitBox craftBlocks = craft.getHitBox();
        String ret = null;
        for (MovecraftLocation block : craft.getHitBox()) {
            int x, y, z;
            x = block.getX() + 1;
            y = block.getY();
            z = block.getZ();
            MovecraftLocation test = new MovecraftLocation(x, y, z);
            if (!craft.getHitBox().contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX() - 1;
            y = block.getY();
            z = block.getZ();
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX();
            y = block.getY() + 1;
            z = block.getZ();
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX();
            y = block.getY() - 1;
            z = block.getZ();
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX();
            y = block.getY();
            z = block.getZ() + 1;
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX();
            y = block.getY();
            z = block.getZ() + 1;
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX() + 1;
            y = block.getY() + 1;
            z = block.getZ();
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX() + 1;
            y = block.getY() - 1;
            z = block.getZ();
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX() - 1;
            y = block.getY() + 1;
            z = block.getZ();
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX() - 1;
            y = block.getY() - 1;
            z = block.getZ();
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX();
            y = block.getY() + 1;
            z = block.getZ() + 1;
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX();
            y = block.getY() - 1;
            z = block.getZ() + 1;
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX();
            y = block.getY() + 1;
            z = block.getZ() - 1;
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
            x = block.getX();
            y = block.getY() - 1;
            z = block.getZ() - 1;
            test = new MovecraftLocation(x, y, z);
            if (!craftBlocks.contains(test))
                if (craft.getType().getAllowedBlocks().containsKey(craft.getW().getBlockAt(x, y, z).getType())  || craft.getType().getAllowedBlocks().get(craft.getW().getBlockAt(x, y, z).getType()).contains(craft.getW().getBlockAt(x, y, z).getData())) {
                    ret = "@ " + x + "," + y + "," + z;
                }
        }
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
            CraftManager.getInstance().removeCraft(CraftManager.getInstance().getCraftByPlayer(p));
        }
    }

    public void onPDeath(PlayerDeathEvent event){

    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if (c == null) {
            return;
        }

        if(MathUtils.locationNearHitBox(c.getHitBox(), event.getPlayer().getLocation(), 2)){
            timeToReleaseAfter.remove(c);
            return;
        }

        if(timeToReleaseAfter.containsKey(c) && timeToReleaseAfter.get(c) < System.currentTimeMillis()){
            CraftManager.getInstance().removeCraft(c);
            timeToReleaseAfter.remove(c);
            return;
        }

        if (c.isNotProcessing() && c.getType().getMoveEntities() && !timeToReleaseAfter.containsKey(c)) {
            if (Settings.ManOverBoardTimeout != 0) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("You have left your craft. You may return to your craft by typing /manoverboard any time before the timeout expires"));
            } else {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Release - Player has left craft"));
            }
            if (c.getHitBox().size() > 11000) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Craft is too big to check its borders. Make sure this area is safe to release your craft in."));
            }
            timeToReleaseAfter.put(c, System.currentTimeMillis() + 30000); //30 seconds to release
        }
    }
}
