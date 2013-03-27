package net.countercraft.movecraft.async;

public class AsyncManager {
	private static AsyncManager ourInstance = new AsyncManager();

	public static AsyncManager getInstance() {
		return ourInstance;
	}

	private AsyncManager() {
	}
}
