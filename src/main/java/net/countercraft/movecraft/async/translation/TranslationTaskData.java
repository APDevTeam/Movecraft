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

package net.countercraft.movecraft.async.translation;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import net.countercraft.movecraft.utils.EntityUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MovecraftLocation;

public class TranslationTaskData {
	private int dx;
	private int dy;
	private int dz;
	private boolean failed = false;
	private String failMessage;
	private MovecraftLocation[] blockList;
	private MapUpdateCommand[] updates;
	private EntityUpdateCommand[] entityUpdates;
	private int[][][] hitbox;
	private int minX, minZ;
	private int maxHeight, minHeight;
	private boolean collisionExplosion;

	public TranslationTaskData( int dx, int dz, int dy, MovecraftLocation[] blockList, int[][][] hitbox, int minZ, int minX, int maxHeight, int minHeight ) {
		this.dx = dx;
		this.dz = dz;
		this.dy = dy;
		this.blockList = blockList;
		this.hitbox = hitbox;
		this.minZ = minZ;
		this.minX = minX;
		this.maxHeight = maxHeight;
		this.minHeight = minHeight;
	}

	public int getDx() {

		return dx;
	}

	public int getDy() {
		return dy;
	}

	public int getDz() {
		return dz;
	}

	public boolean failed() {
		return failed;
	}

	public void setFailed( boolean failed ) {
		this.failed = failed;
	}

	public boolean collisionExplosion() {
		return collisionExplosion;
	}

	public void setCollisionExplosion( boolean collisionExplosion ) {
		this.collisionExplosion = collisionExplosion;
	}

	public String getFailMessage() {
		return failMessage;
	}

	public void setFailMessage( String failMessage ) {
		this.failMessage = failMessage;
	}

	public MovecraftLocation[] getBlockList() {
		return blockList;
	}

	public void setBlockList( MovecraftLocation[] blockList ) {
		this.blockList = blockList;
	}

	public MapUpdateCommand[] getUpdates() {
		return updates;
	}

	public void setUpdates( MapUpdateCommand[] updates ) {
		this.updates = updates;
	}

	public EntityUpdateCommand[] getEntityUpdates() {
		return entityUpdates;
	}

	public void setEntityUpdates( EntityUpdateCommand[] entityUpdates ) {
		this.entityUpdates = entityUpdates;
	}

	public int[][][] getHitbox() {
		return hitbox;
	}

	public void setHitbox( int[][][] hitbox ) {
		this.hitbox = hitbox;
	}

	public int getMinX() {
		return minX;
	}

	public void setMinX( int minX ) {
		this.minX = minX;
	}

	public int getMinZ() {
		return minZ;
	}

	public void setMinZ( int minZ ) {
		this.minZ = minZ;
	}

	public int getMinHeight() {
		return minHeight;
	}

	public int getMaxHeight() {
		return maxHeight;
	}
}
