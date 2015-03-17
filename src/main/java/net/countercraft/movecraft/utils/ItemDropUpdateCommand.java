/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.countercraft.movecraft.utils;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Class that stores the data about a item drops to the map in an unspecified world. The world is retrieved contextually from the submitting craft.
 */
public class ItemDropUpdateCommand {
	private final Location location;
	private final ItemStack itemStack;

	public ItemDropUpdateCommand(Location location, ItemStack itemStack ) {
		this.location = location;
		this.itemStack = itemStack;
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public Location getLocation() {
		return location;
	}

}