package dev.cccm5.movecraft.util

import dev.cccm5.movecraft.hitbox.SolidHitBox
import org.bukkit.World
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.pow

data class GridVector(val x: Int,val y: Int,val z: Int) {
    /**
     * Finds the cross product of two vectors in three dimensional euclidean space
     * @param other the second vector of which the cross product is taken
     * @return the cross product of *this* and [other]
     */
    infix fun cross(other : GridVector): GridVector {
        return GridVector(this.y * other.z - this.z * other.y, this.z * other.x - this.x * other.z, this.x * other.y - this.y * other.x)
    }

    /**
     * Finds the dot product of two vectors in three dimensional euclidean space
     * @param other the second vector of which to take the dot product
     * @return the dot product of *this* and [other]
     */
    infix fun dot(other : GridVector): GridVector {
        return GridVector(this.x * other.x, this.y * other.y, this.z * other.z)
    }

    /**
     * Finds the magnitude of the current vector in relation to the zero vector (the origin) in three dimensional euclidean space
     *
     * Note that when comparing distances, [magnitudeSquared] should be used instead
     *
     * @return the distance from the origin
     */
    fun magnitude(): Double{
        return sqrt(this.magnitudeSquared().toDouble())
    }

    /**
     * Finds the magnitude squared of the current vector in relation to the zero vector (the origin) in three deimensional euclidean space
     *
     * @return the distance from the origin squared
     */
    fun magnitudeSquared(): Int{
        return this.x * this.x+ this.y*this.y + this.z*this.z
    }

    /**
     * Finds the magnitude (distance) between two vectors in
     */
    fun magnitude(other: GridVector): Double{
        return sqrt(this.magnitudeSquared(other).toDouble())
    }

    fun magnitudeSquared(other: GridVector): Int{
        val i = this.x - other.x
        val j = this.y - other.y
        val k = this.z - other.z
        return i*i + j*j + k*k
    }

    /**
     * Rotates a vector in respect to [origin] in consideration of the x,z plane
     */
    fun rotate(rotation: Rotation, origin: GridVector): GridVector {
        val corrected = (this - origin)
        TODO("Implement rotation without trig functions")
    }

    operator fun plus(other: GridVector): GridVector {
        return GridVector(this.x + other.x, this.y + other.y, this.z + other.z)
    }

    operator fun minus(other: GridVector): GridVector {
        return GridVector(this.x - other.x, this.y - other.y, this.z - other.z)
    }
    operator fun rangeTo(other: GridVector): SolidHitBox {
        return SolidHitBox(this, other)
    }

    operator fun unaryMinus(): GridVector {
        return GridVector(-this.x, -this.y, -this.z)
    }

    operator fun unaryPlus(): GridVector {
        return GridVector(+this.x, +this.y, +this.z)
    }

    override fun hashCode(): Int {
        // Taken from effective java chapter 3
        var result = x xor x.ushr(32)
        result = 31 * result + (y xor y.ushr(32))
        result = 31 * result + (z xor z.ushr(32))
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as GridVector

        if (x != that.x) return false
        if (y != that.y) return false
        return z == that.z
    }
}
