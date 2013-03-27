package net.countercraft.movecraft.craft;

public class CraftManager {
	private static CraftManager ourInstance = new CraftManager();

	public static CraftManager getInstance() {
		return ourInstance;
	}

	private CraftManager() {
	}
}
