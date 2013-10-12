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

public class BoundingBoxUtils {

	public static int[][][] formBoundingBox( MovecraftLocation[] blockList, Integer minX, Integer maxX, Integer minZ, Integer maxZ ) {
		int sizeX = ( maxX - minX ) + 1;
		int sizeZ = ( maxZ - minZ ) + 1;

		int[][][] polygonalBox = new int[sizeX][][];

		for ( MovecraftLocation l : blockList ) {
			if ( polygonalBox[l.getX() - minX] == null ) {
				polygonalBox[l.getX() - minX] = new int[sizeZ][];
			}

			int minY, maxY;

			if ( polygonalBox[l.getX() - minX][l.getZ() - minZ] == null ) {

				polygonalBox[l.getX() - minX][l.getZ() - minZ] = new int[2];
				polygonalBox[l.getX() - minX][l.getZ() - minZ][0] = l.getY();
				polygonalBox[l.getX() - minX][l.getZ() - minZ][1] = l.getY();

			} else {
				minY = polygonalBox[l.getX() - minX][l.getZ() - minZ][0];
				maxY = polygonalBox[l.getX() - minX][l.getZ() - minZ][1];

				if ( l.getY() < minY ) {
					polygonalBox[l.getX() - minX][l.getZ() - minZ][0] = l.getY();
				}
				if ( l.getY() > maxY ) {
					polygonalBox[l.getX() - minX][l.getZ() - minZ][1] = l.getY();
				}

			}


		}

		return polygonalBox;
	}

	public static int[][][] translateBoundingBoxVertically( int[][][] hitbox, int dy ) {
		int[][][] newHitbox = new int[hitbox.length][][];

		for ( int x = 0; x < hitbox.length; x++ ) {
			newHitbox[x] = new int[hitbox[x].length][];

			for ( int z = 0; z < hitbox[x].length; z++ ) {

				if(hitbox[x][z]!=null) {
					newHitbox[x][z] = new int[2];
					newHitbox[x][z][0] = hitbox[x][z][0] + dy;
					newHitbox[x][z][1] = hitbox[x][z][1] + dy;
				}

			}

		}


		return newHitbox;
	}

}
