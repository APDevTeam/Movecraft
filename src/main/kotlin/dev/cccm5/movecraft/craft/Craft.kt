package dev.cccm5.movecraft.craft

import dev.cccm5.movecraft.hitbox.NonEmptyHitBox
import dev.cccm5.movecraft.util.Direction
import org.bukkit.World

abstract class Craft {
    abstract var hitBox: NonEmptyHitBox
    val subCrafts: MutableCollection<SubCraft> = mutableListOf()
    abstract var orientation: Direction
    abstract var world: World
}