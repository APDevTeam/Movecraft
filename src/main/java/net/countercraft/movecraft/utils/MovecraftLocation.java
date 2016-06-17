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

public class MovecraftLocation {
	private int x, y, z;

	public MovecraftLocation( int x, int y, int z ) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getX() {
		return x;
	}

	public void setX( int x ) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY( int y ) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ( int z ) {
		this.z = z;
	}

	/**
	 * Returns a MovecraftLocation that has undergone the given translation.
	 *
	 * This does not change the MovecraftLocation that it is called upon and that should be accounted for in terms of Garbage Collection.
	 *
	 * @param dx - X translation
	 * @param dy - Y translation
	 * @param dz - Z translation
	 * @return New MovecraftLocation shifted by specified amount
	 */
	public MovecraftLocation translate(int dx, int dy, int dz) {
		return new MovecraftLocation( x + dx, y + dy, z + dz );
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof MovecraftLocation){
			MovecraftLocation location = (MovecraftLocation) o;
			if(location.getX() == getX()){
				if(location.getY() == getY()){
					if(location.getZ() == getZ()){
						return true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(x).hashCode() >> 13
				^ Integer.valueOf(y).hashCode() >> 7
				^ Integer.valueOf(z).hashCode();
	}

	public MovecraftLocation add( MovecraftLocation l ) {
		return new MovecraftLocation( getX() + l.getX(), getY() + l.getY(), getZ() + l.getZ() );
	}

	public MovecraftLocation subtract( MovecraftLocation l ) {
		return new MovecraftLocation( getX() - l.getX(), getY() - l.getY(), getZ() - l.getZ() );
	}

}
