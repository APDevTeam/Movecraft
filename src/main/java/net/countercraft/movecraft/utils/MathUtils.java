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

package net.countercraft.movecraft.utils;

import org.bukkit.Location;

public class MathUtils {

	public static boolean playerIsWithinBoundingPolygon( int[][][] box, int minX, int minZ, MovecraftLocation l ) {

		if( l.getX() >= minX && l.getX() < ( minX + box.length ) ) {
			// PLayer is within correct X boundary
			if ( l.getZ() >= minZ && l.getZ() < ( minZ + box[l.getX() - minX].length ) ) {
				// Player is within valid Z boundary
				int minY = box[l.getX() - minX][l.getZ() - minZ][0];
				int maxY = box[l.getX() - minX][l.getZ() - minZ][1];

				if ( minY != -1 && maxY != -1 ) {
					if ( l.getY() >= minY && l.getY() <= (maxY + 2) ) {
						// Player is on board the vessel
						return true;
					}

				}

			}

		}

		return false;
	}

	public static MovecraftLocation bukkit2MovecraftLoc( Location l ){
		return new MovecraftLocation( l.getBlockX(), l.getBlockY(), l.getBlockZ() );
	}

}
