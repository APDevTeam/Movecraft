package dev.cccm5.movecraft.craft

import dev.cccm5.movecraft.hitbox.NonEmptyHitBox
import dev.cccm5.movecraft.util.Direction
import org.bukkit.World

class SubCraft(override var hitBox: NonEmptyHitBox, override var orientation: Direction, override var world: World) : Craft() {

}