package dev.cccm5.movecraft.event

import org.bukkit.event.HandlerList
import dev.cccm5.movecraft.hitbox.HashHitBox
import dev.cccm5.movecraft.craft.Craft
import dev.cccm5.movecraft.CraftStateManager
import org.bukkit.event.Cancellable


/**
 * Called whenever a [Craft] is translated by the [CraftStateManager]
 * This event is called before the craft is physically moved, but after collision is checked.
 */

class CraftTranslateEvent(craft: Craft,val oldHitBox: HashHitBox, val newHitBox: HashHitBox) : CraftEvent(craft), Cancellable {
    private var isCancelled = false

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }
}