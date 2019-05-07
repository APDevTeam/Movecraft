package dev.cccm5.movecraft.craft

import dev.cccm5.movecraft.hitbox.NonEmptyHitBox
import dev.cccm5.movecraft.util.Direction
import org.bukkit.World
import org.bukkit.entity.Player

class PlayerCraft(val pilot: Player, override var hitBox: NonEmptyHitBox, override var orientation: Direction, override var world: World) : Craft() {

}