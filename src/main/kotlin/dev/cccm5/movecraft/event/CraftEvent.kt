package dev.cccm5.movecraft.event

import dev.cccm5.movecraft.craft.Craft
import org.bukkit.event.Event


/**
 * A base event for all craft-related event
 * @see Craft
 */
abstract class CraftEvent(val craft: Craft) : Event()