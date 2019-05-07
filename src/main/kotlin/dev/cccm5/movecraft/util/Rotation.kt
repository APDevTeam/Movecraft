package dev.cccm5.movecraft.util

enum class Rotation(val degree: Int) {
    CLOCKWISE(90), COUNTERCLOCKWISE(270), NONE(0), REVERSE(180);
    operator fun plus(other: Rotation): Rotation {
        return when((this.degree + other.degree)%360){
            0 -> NONE
            90 -> CLOCKWISE
            180 -> REVERSE
            270 -> COUNTERCLOCKWISE
            else -> throw IllegalArgumentException("Invalid rotation")
        }
    }
}