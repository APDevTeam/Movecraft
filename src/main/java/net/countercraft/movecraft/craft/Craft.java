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

package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.async.translation.TranslationTaskData;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import org.bukkit.World;

import java.util.concurrent.atomic.AtomicBoolean;

public class Craft {
	private int[][][] hitBox;
	private final CraftType type;
	private MovecraftLocation[] blockList;
	private World w;
	private AtomicBoolean processing = new AtomicBoolean();
	private int minX, minZ, maxHeightLimit;
	private boolean cruising;
	private byte cruiseDirection;
	private long lastCruiseUpdate;
	private long lastBlockCheck;
	private long lastRightClick;
	private int lastDX, lastDY, lastDZ;
	private double burningFuel;

	public Craft( CraftType type, World world ) {
		this.type = type;
		this.w = world;
		this.blockList = new MovecraftLocation[1];
		if ( type.getMaxHeightLimit() > w.getMaxHeight() - 1 ) {
			this.maxHeightLimit = w.getMaxHeight() - 1;
		} else {
			this.maxHeightLimit = type.getMaxHeightLimit();
		}
	}

	public boolean isNotProcessing() {
		return !processing.get();
	}

	public void setProcessing( boolean processing ) {
		this.processing.set( processing );
	}

	public MovecraftLocation[] getBlockList() {
		synchronized ( blockList ) {
			return blockList.clone();
		}
	}

	public void setBlockList( MovecraftLocation[] blockList ) {
		synchronized ( this.blockList ) {
			this.blockList = blockList;
		}
	}

	public CraftType getType() {
		return type;
	}

	public World getW() {
		return w;
	}

	public int[][][] getHitBox() {
		return hitBox;
	}

	public void setHitBox( int[][][] hitBox ) {
		this.hitBox = hitBox;
	}

	public void detect( String playerName, MovecraftLocation startPoint ) {
		AsyncManager.getInstance().submitTask( new DetectionTask( this, startPoint, type.getMinSize(), type.getMaxSize(), type.getAllowedBlocks(), type.getForbiddenBlocks(), playerName, w ), this );
	}

	public void translate( int dx, int dy, int dz ) {
		AsyncManager.getInstance().submitTask( new TranslationTask( this, new TranslationTaskData( dx, dz, dy, getBlockList(), getHitBox(), minZ, minX, type.getMaxHeightLimit(), type.getMinHeightLimit() ) ), this );
	}

	public void rotate( Rotation rotation, MovecraftLocation originPoint ) {
		AsyncManager.getInstance().submitTask( new RotationTask( this, originPoint, this.getBlockList(), rotation, this.getW() ), this );
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
	
	public boolean getCruising() {
		return cruising;
	}
	
	public void setCruiseDirection(byte cruiseDirection) {
		this.cruiseDirection=cruiseDirection;
	}

	public byte getCruiseDirection() {
		return cruiseDirection;
	}
	
	public void setCruising(boolean cruising) {
		this.cruising=cruising;
	}

	public void setLastCruisUpdate(long update) {
		this.lastCruiseUpdate=update;
	}
	
	public long getLastCruiseUpdate() {
		return lastCruiseUpdate;
	}
	
	public void setLastBlockCheck(long update) {
		this.lastBlockCheck=update;
	}
	
	public long getLastBlockCheck() {
		return lastBlockCheck;
	}
	
	public void setLastRightClick(long update) {
		this.lastRightClick=update;
	}
	
	public long getLastRightClick() {
		return lastRightClick;
	}
	
	public int getLastDX() {
		return lastDX;
	}

	public void setLastDX( int dX ) {
		this.lastDX = dX;
	}
	
	public int getLastDY() {
		return lastDY;
	}

	public void setLastDY( int dY ) {
		this.lastDY = dY;
	}
	
	public int getLastDZ() {
		return lastDZ;
	}

	public void setLastDZ( int dZ ) {
		this.lastDZ = dZ;
	}
	
	public void setBurningFuel(double burningFuel) {
		this.burningFuel=burningFuel;
	}
	
	public double getBurningFuel() {
		return burningFuel;
	}
	
}
