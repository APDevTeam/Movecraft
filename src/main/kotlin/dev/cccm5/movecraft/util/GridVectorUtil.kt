package dev.cccm5.movecraft.util

import org.bukkit.World
import org.bukkit.block.Block

fun World.getBlockAt(vector: GridVector): Block = this.getBlockAt(vector.x,vector.y,vector.z)