package dev.cccm5.movecraft.event

import dev.cccm5.movecraft.craft.Craft
import dev.cccm5.movecraft.CraftStateManager
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList

/**
 * Called whenever a [Craft] is piloted
 * The term piloted is used to indicate that a craft is being added to the [CraftStateManager]
 */
class CraftPilotEvent(craft: Craft): CraftEvent(craft), Cancellable {
    private var cancelled = false
    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    override fun isCancelled(): Boolean {
        return this.cancelled
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }
}