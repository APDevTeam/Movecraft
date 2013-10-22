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

/**
 * Class that stores the data about a single blocks changes to the map in an unspecified world. The world is retrieved contextually from the submitting craft.
 */
public class MapUpdateCommand {
	private MovecraftLocation blockLocation;
	private final MovecraftLocation newBlockLocation;
	private final int typeID;
	private final Rotation rotation;

	public MapUpdateCommand( MovecraftLocation blockLocation, MovecraftLocation newBlockLocation, int typeID, Rotation rotation ) {
		this.blockLocation = blockLocation;
		this.newBlockLocation = newBlockLocation;
		this.typeID = typeID;
		this.rotation = rotation;
	}

	public MapUpdateCommand( MovecraftLocation blockLocation, MovecraftLocation newBlockLocation, int typeID ) {
		this.blockLocation = blockLocation;
		this.newBlockLocation = newBlockLocation;
		this.typeID = typeID;
		this.rotation = Rotation.NONE;
	}

	public MapUpdateCommand( MovecraftLocation newBlockLocation, int typeID ) {
		this.newBlockLocation = newBlockLocation;
		this.typeID = typeID;
		this.rotation = Rotation.NONE;
	}

	public int getTypeID() {
		return typeID;
	}

	public MovecraftLocation getOldBlockLocation() {
		return blockLocation;
	}

	public MovecraftLocation getNewBlockLocation() {
		return newBlockLocation;
	}

	public Rotation getRotation() {
		return rotation;
	}
}

