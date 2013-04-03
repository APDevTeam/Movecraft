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
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import org.bukkit.World;

import java.util.concurrent.atomic.AtomicBoolean;

public class Craft {
	int[][][] hitBox;
	private CraftType type;
	private MovecraftLocation[] blockList;
	private World w;
	private AtomicBoolean processing = new AtomicBoolean();
	private int minX, minZ, maxHeightLimit;

	public Craft( CraftType type, World world ) {
		this.type = type;
		this.w = world;
                if (type.getMaxHeightLimit() > w.getMaxHeight() - 1 ){
                    this.maxHeightLimit=w.getMaxHeight() - 1;
                }else{
                    this.maxHeightLimit=type.getMaxHeightLimit();
                }
	}

	public boolean isProcessing() {
		return processing.get();
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
		synchronized ( blockList ) {
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
		AsyncManager.getInstance().submitTask( new DetectionTask( this, startPoint, type.getMinSize(), type.getMaxSize(), type.getAllowedBlocks(), type.getForbiddenBlocks(), playerName, w) , this);
	}

	public void translate( int dx, int dy, int dz ) {
            //AsyncManager.getInstance().submitTask( new TranslationTask( this, dx, dy, dz, hitBox, minX, minZ, w.getMaxHeight() - 1 ), this );
            AsyncManager.getInstance().submitTask( new TranslationTask( this, dx, dy, dz, hitBox, minX, minZ, this.maxHeightLimit, type.getMinHeightLimit() ), this );
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
}
