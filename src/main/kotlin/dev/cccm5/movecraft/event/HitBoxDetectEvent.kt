package dev.cccm5.movecraft.event

import dev.cccm5.movecraft.hitbox.HitBox
import dev.cccm5.movecraft.CraftStateManager
import dev.cccm5.movecraft.craft.Craft
import org.bukkit.World
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Invoked whenever the [CraftStateManager] creates a [HitBox] for a [Craft] that will be later piloted
 */
class HitBoxDetectEvent(var hitBox: HitBox, var world: World): Event(){
    override fun getHandlers(): HandlerList {
        return handlerList
    }



    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }
}