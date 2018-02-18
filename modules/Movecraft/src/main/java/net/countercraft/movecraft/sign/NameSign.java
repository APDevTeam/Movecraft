package net.countercraft.movecraft.sign;

import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class NameSign implements Listener{
	private static final String HEADER = "Name:";
	public static void onSignChange(SignChangeEvent event) {
		if (event.getLine(0).equalsIgnoreCase(HEADER)) {
			
			
		}
	}

}
