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

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public class StickListener implements Listener {
	private static Map<Player, Long> timeMap = new HashMap<Player, Long>();

	@EventHandler
	public void onPlayerInteract ( PlayerInteractEvent event ) {
		if ( event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
			if ( event.getItem() != null && event.getItem().getType().equals( Material.STICK ) ) {
				Craft craft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
				if ( craft != null ) {
					Long time = timeMap.get( event.getPlayer() );
					if ( time != null ) {
						long ticksElapsed = (System.currentTimeMillis() - time) / 50;
						if ( ticksElapsed < craft.getType().getTickCooldown() ) {
							return;
						}
					}

					if ( MathUtils.playerIsWithinBoundingPolygon( craft.getHitBox(), craft.getMinX(), craft.getMinZ(), MathUtils.bukkit2MovecraftLoc( event.getPlayer().getLocation() ) ) ) {

						// Player is onboard craft and right clicking
						float rotation = (float) Math.PI * event.getPlayer().getLocation().getYaw() / 180f;

						float nx = -(float) Math.sin(rotation);
						float nz = (float) Math.cos(rotation);

						int dx = (Math.abs(nx) >= 0.5 ? 1 : 0) * (int) Math.signum(nx);
						int dz = (Math.abs(nz) > 0.5 ? 1 : 0) * (int) Math.signum(nz);
						int dy = 0;

						float p = event.getPlayer().getLocation().getPitch();

						dy = -(Math.abs(p) >= 25 ? 1 : 0)
								* (int) Math.signum(p);

						if (Math.abs(event.getPlayer().getLocation().getPitch()) >= 75) {
							dx = 0;
							dz = 0;
						}

						craft.translate( dx, dy, dz );
						timeMap.put( event.getPlayer(), System.currentTimeMillis() );
					}
				}
			}
		}
	}

	public void onStickRightClick () {

	}

}
