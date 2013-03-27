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

public class BlockUtils {
	private static final int[] dataBlocks = new int[] { 17, 51, 18, 84, 6, 81, 83, 8, 3, 59, 115, 86, 25, 50, 66, 53, 69, 64, 77, 63, 65, 23, 86, 70, 43, 78, 92, 26, 93, 55, 2, 96, 29, 34, 98, 99, 106, 107, 117, 118, 120, 139, 140, 144, 145, 5, 155, 9, 60, 141, 75, 27, 67, 71, 143, 68, 72, 44, 94, 33, 100, 10, 142, 76, 28, 108, 61, 147, 11, 109, 62, 148, 114, 128, 134, 135, 136, 156};

	public static boolean blockHasData ( int id ) {
		for ( int i : dataBlocks ) {
			if ( id == i ) {
				return true;
			}
		}
		return false;
	}

	public static boolean arrayContainsOverlap( Object[] array1, Object[] array2 ) {
		for( Object o : array1 ) {

			for ( Object o1 : array2 ) {
				if ( o.equals( o1 ) ){
					return  true;
				}
			}

		}

		return false;
	}

}
