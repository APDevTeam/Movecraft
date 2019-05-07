package dev.cccm5.movecraft.hitbox

import dev.cccm5.movecraft.util.GridVector
import dev.cccm5.movecraft.util.Rotation
import kotlin.math.min
import kotlin.math.max

/**
 * Stores a view of multiple HitBoxs combined as a singular NonEmptyHitBox
 */
class MultiHitBox internal constructor(private val hitBoxes: Iterable<NonEmptyHitBox>): NonEmptyHitBox() {
    override fun translate(vector: GridVector, rotation: Rotation): NonEmptyHitBox {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun minus(other: HitBox): HitBox {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun plus(other: HitBox): HitBox {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val minX: Int
    override val minY: Int
    override val minZ: Int
    override val maxX: Int
    override val maxY: Int
    override val maxZ: Int
    override val size: Int

    init{
        var minX = 0
        var minY = 0
        var minZ = 0
        var maxX = 0
        var maxY = 0
        var maxZ = 0
        var size = 0
        for(hitBox in hitBoxes){
            minX = min(hitBox.minX, minX)
            minY = min(hitBox.minY, minY)
            minZ = min(hitBox.minZ, minZ)
            maxX = max(hitBox.maxX, maxX)
            maxY = max(hitBox.maxY, maxY)
            maxZ = max(hitBox.maxZ, maxZ)
            size += hitBox.size
        }
        this.minX = minX
        this.minY = minY
        this.minZ = minZ
        this.maxX = maxX
        this.maxY = maxY
        this.maxZ = maxZ
//        this.size = hitBoxes.sumBy { it.size }
        this.size = size
    }

    override fun contains(element: GridVector): Boolean {
        for(hitBox in hitBoxes){
            if(element in hitBox){
                return true
            }
        }
        return false
    }

    override fun containsAll(elements: Collection<GridVector>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<GridVector> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}