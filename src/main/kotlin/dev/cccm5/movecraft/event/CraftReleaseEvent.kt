package dev.cccm5.movecraft.event

import dev.cccm5.movecraft.CraftStateManager
import dev.cccm5.movecraft.craft.PlayerCraft
import org.bukkit.event.HandlerList
import org.bukkit.block.Block

/**
 * Invoked whenever a [PlayerCraft] is removed from the CraftManager
 */
class CraftReleaseEvent(craft: PlayerCraft, val reason: Reason): CraftRemoveEvent(craft) {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }

    enum class Reason{
        DISCONNECT, SINK, MANUAL
    }
}