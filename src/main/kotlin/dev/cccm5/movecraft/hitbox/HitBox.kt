package dev.cccm5.movecraft.hitbox
import dev.cccm5.movecraft.util.GridVector
import dev.cccm5.movecraft.util.Rotation
import java.util.Collections.emptyIterator


fun hitBoxOf(first: GridVector, vararg elements: GridVector): NonEmptyHitBox {
    val locations = elements.toSet() + first
    val origin = GridVector(locations.minBy { it.x }!!.x, locations.minBy { it.y }!!.y, locations.minBy { it.z }!!.z)
    val opposite = GridVector(locations.maxBy { it.x }!!.x, locations.maxBy { it.y }!!.y, locations.maxBy { it.z }!!.z)
    return HashHitBox(locations.map { it - origin }.toSet(), origin, opposite)
}

fun hitBoxOf() = EmptyHitBox



fun Set<GridVector>.toHitBox(): HitBox {
    return if(this.isEmpty()){
        EmptyHitBox
    } else {
        val origin = GridVector(this.minBy { it.x }!!.x, this.minBy { it.y }!!.y, this.minBy { it.z }!!.z)
        val opposite = GridVector(this.maxBy { it.x }!!.x, this.maxBy { it.y }!!.y, this.maxBy { it.z }!!.z)
        HashHitBox(this, origin, opposite)
    }
}

sealed class HitBox: Iterable<GridVector>{
    abstract val xLength: Int
    abstract val yLength: Int
    abstract val zLength: Int
    abstract fun inBounds(x: Int, y: Int, z: Int): Boolean
    abstract fun inBounds(vector: GridVector): Boolean
    abstract fun boundingHitBox(): HitBox
    abstract operator fun minus(other: HitBox): HitBox
    abstract operator fun plus(other: HitBox): HitBox
}

object EmptyHitBox: HitBox() {
    override fun iterator(): Iterator<GridVector> = emptyIterator()

    override val xLength = 0
    override val yLength = 0
    override val zLength = 0
    override fun inBounds(x: Int, y: Int, z: Int) = false
    override fun inBounds(vector: GridVector) = false
    override fun boundingHitBox() = this
    override fun minus(other: HitBox) = this
    override fun plus(other: HitBox) = other
}


abstract class NonEmptyHitBox : Set<GridVector>, HitBox() {
    abstract val minX: Int
    abstract val minY: Int
    abstract val minZ: Int
    abstract val maxX: Int
    abstract val maxY: Int
    abstract val maxZ: Int

    fun translate(vector: GridVector) = translate(vector, Rotation.NONE)
    fun translate(rotation: Rotation) = translate(GridVector(0, 0, 0), rotation)
    abstract fun translate(vector: GridVector, rotation: Rotation): NonEmptyHitBox
    final override fun isEmpty() = false

    override val xLength by lazy{ this.maxX - this.minX + 1 }
    override val yLength by lazy{ this.maxY - this.minY + 1 }
    override val zLength by lazy{ this.maxZ - this.minZ + 1 }
    open val midpoint: GridVector by lazy{ GridVector((this.minX + this.maxX) / 2, (this.minY + this.maxY) / 2, (this.minZ + this.maxZ) / 2) }

    fun contains(x: Int, y: Int, z: Int) = this.contains(GridVector(x, y, z))

    override fun inBounds(x: Int,y: Int,z: Int) =
            x >= this.minX &&
            x <= this.maxX &&
            y >= this.minY &&
            y <= this.maxY &&
            z >= this.minZ &&
            z <= this.maxZ

    override fun inBounds(vector: GridVector): Boolean = this.inBounds(vector.x, vector.y, vector.z)

    override fun boundingHitBox() = SolidHitBox(GridVector(this.minX, this.minY, this.minZ), GridVector(this.maxX, this.maxY, this.maxZ))
}

