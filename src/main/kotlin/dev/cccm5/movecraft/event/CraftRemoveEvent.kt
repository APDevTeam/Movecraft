package dev.cccm5.movecraft.event

import dev.cccm5.movecraft.CraftStateManager
import dev.cccm5.movecraft.craft.Craft
import org.bukkit.event.HandlerList


/**
 * Called whenever a [Craft] removed from the [CraftStateManager]
 */
open class CraftRemoveEvent(craft: Craft): CraftEvent(craft) {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }
}