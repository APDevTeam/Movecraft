package dev.cccm5.movecraft.hitbox

import dev.cccm5.movecraft.util.GridVector
import dev.cccm5.movecraft.util.Rotation

class SolidHitBox(override val minX: Int, override val minY: Int, override val minZ: Int, override val maxX: Int, override val maxY: Int, override val maxZ: Int) : NonEmptyHitBox() {
    override fun translate(vector: GridVector, rotation: Rotation): NonEmptyHitBox {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    constructor(start: GridVector, end: GridVector): this(start.x, start.y,start.z, end.x, end.y, end.z)

    override fun minus(other: HitBox): HitBox {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun plus(other: HitBox): HitBox {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val size by lazy { this.xLength * this.yLength * this.zLength }

    override operator fun contains(element: GridVector): Boolean {
        return element.x in minX..maxX &&
                element.y in minY..maxY &&
                element.z in minZ..maxZ
    }

    override fun containsAll(elements: Collection<GridVector>): Boolean {
        for (location in elements) {
            if (!this.contains(location)) {
                return false
            }
        }
        return true
    }

    override fun iterator(): Iterator<GridVector> {
        return object : Iterator<GridVector> {
            private var lastX = minX
            private var lastY = minY
            private var lastZ = minZ
            override fun hasNext(): Boolean {
                return lastZ <= maxZ
            }

            override fun next(): GridVector {
                val output = GridVector(lastX, lastY, lastZ)
                lastX++
                if (lastX > maxX) {
                    lastX = minX
                    lastY++
                }
                if (lastY > maxY) {
                    lastY = minY
                    lastZ++
                }
                return output
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if(this === other){
            return true
        }
        if(other is SolidHitBox){
            return this.minX == other.minX && this.minY == other.minY && this.minZ == other.minZ && this.maxX == other.maxX && this.minY == other.minY && this.minZ == other.minZ
        }
        return false
    }

    override fun hashCode(): Int {
        var result = minX
        result = 31 * result + minY
        result = 31 * result + minZ
        result = 31 * result + maxX
        result = 31 * result + maxY
        result = 31 * result + maxZ
        return result
    }
}