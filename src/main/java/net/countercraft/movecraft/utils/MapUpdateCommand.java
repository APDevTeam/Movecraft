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

import net.countercraft.movecraft.craft.Craft;

/**
 * Class that stores the data about a single blocks changes to the map in an unspecified world. The world is retrieved contextually from the submitting craft.
 */
public class MapUpdateCommand {
	private MovecraftLocation blockLocation;
	private final MovecraftLocation newBlockLocation;
	private final int typeID;
	private final byte dataID;
	private final Integer currentTypeID;
	private final Byte currentDataID;
	private final Object worldEditBaseBlock;
	private final Rotation rotation;
	private Craft craft;
	private int smoke;

	public MapUpdateCommand( MovecraftLocation blockLocation, Integer currentTypeID, Byte currentDataID, MovecraftLocation newBlockLocation, int typeID, byte dataID, Rotation rotation, Craft craft ) {
		this.blockLocation = blockLocation;
		this.newBlockLocation = newBlockLocation;
		this.typeID = typeID;
		this.dataID = dataID;
		this.worldEditBaseBlock = null;
		this.rotation = rotation;
		this.craft = craft;
		this.smoke = 0;
		this.currentTypeID = currentTypeID;
		this.currentDataID = currentDataID;
	}

	public MapUpdateCommand( MovecraftLocation blockLocation, Integer currentTypeID, Byte currentDataID, MovecraftLocation newBlockLocation, int typeID, byte dataID, Craft craft ) {
		this.blockLocation = blockLocation;
		this.newBlockLocation = newBlockLocation;
		this.typeID = typeID;
		this.dataID = dataID;
		this.worldEditBaseBlock = null;
		this.rotation = Rotation.NONE;
		this.craft = craft;
		this.smoke = 0;
		this.currentTypeID = currentTypeID;
		this.currentDataID = currentDataID;

	}

	public MapUpdateCommand( MovecraftLocation newBlockLocation, int typeID, byte dataID, Craft craft ) {
		this.newBlockLocation = newBlockLocation;
		this.typeID = typeID;
		this.dataID = dataID;
		this.worldEditBaseBlock = null;
		this.rotation = Rotation.NONE;
		this.craft = craft;
		this.smoke = 0;
		this.currentTypeID = null;
		this.currentDataID = null;

	}

	public MapUpdateCommand( MovecraftLocation newBlockLocation, int typeID, byte dataID, Object worldEditBaseBlock, Craft craft ) {
		this.newBlockLocation = newBlockLocation;
		this.typeID = typeID;
		this.dataID = dataID;
		this.worldEditBaseBlock = worldEditBaseBlock;
		this.rotation = Rotation.NONE;
		this.craft = craft;
		this.smoke = 0;
		this.currentTypeID = null;
		this.currentDataID = null;

	}

	public MapUpdateCommand( MovecraftLocation newBlockLocation, int typeID, byte dataID, Craft craft, int smoke ) {
		this.newBlockLocation = newBlockLocation;
		this.typeID = typeID;
		this.dataID = dataID;
		this.worldEditBaseBlock = null;
		this.rotation = Rotation.NONE;
		this.craft = craft;
		this.smoke = smoke;
		this.currentTypeID = null;
		this.currentDataID = null;

	}

	public int getTypeID() {
		return typeID;
	}

	public byte getDataID() {
		return dataID;
	}

	public Integer getCurrentTypeID() {
		return currentTypeID;
	}

	public Byte getCurrentDataID() {
		return currentDataID;
	}

	public Object getWorldEditBaseBlock() {
		return worldEditBaseBlock;
	}

	public int getSmoke() {
		return smoke;
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

	public Craft getCraft() {
		return craft;
	}
}

