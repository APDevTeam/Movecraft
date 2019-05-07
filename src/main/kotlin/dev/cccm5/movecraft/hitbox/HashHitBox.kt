package dev.cccm5.movecraft.hitbox

import dev.cccm5.movecraft.util.GridVector
import dev.cccm5.movecraft.util.Rotation
import java.lang.IllegalArgumentException

class HashHitBox internal constructor(private val points: Set<GridVector>, private val origin: GridVector, private val opposite: GridVector) : NonEmptyHitBox() {

    override val minX: Int = origin.x
    override val minY: Int = origin.y
    override val minZ: Int = origin.z
    override val maxX: Int = opposite.x
    override val maxY: Int = opposite.y
    override val maxZ: Int = opposite.z
    override val size: Int = points.size

    init{
        if(points.isEmpty()){
            throw IllegalArgumentException("points must be non-empty in HashHitBox!")
        }
    }

    override fun translate(vector: GridVector, rotation: Rotation): NonEmptyHitBox {
        return HashHitBox(points, origin + vector, opposite + vector)
    }

    override fun minus(other: HitBox): HitBox {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun plus(other: HitBox): HitBox {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun contains(element: GridVector): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsAll(elements: Collection<GridVector>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<GridVector> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}