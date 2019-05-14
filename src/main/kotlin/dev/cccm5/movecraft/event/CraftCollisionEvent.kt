package dev.cccm5.movecraft.event

import dev.cccm5.movecraft.CraftStateManager
import dev.cccm5.movecraft.craft.Craft
import dev.cccm5.movecraft.util.GridVector
import org.bukkit.event.HandlerList
import org.bukkit.block.Block

/**
 * Invoked whenever a movement of a [Craft] by the [CraftStateManager] causes the craft to collide with a non-phasable
 * [Block]
 */
class CraftCollisionEvent(craft: Craft, val collision: Collection<GridVector>): CraftEvent(craft) {

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }
}