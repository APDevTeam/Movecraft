/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.ui;

import java.util.List;
import org.bukkit.boss.BossBar;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.ui.MovecraftUIManager;

public class BossBarUpdateTask extends BukkitRunnable {
	private BossBar bossBar;
	private long lastUpdate;
	private MovecraftUIManager manager;
	private long maxValue;
	private long currentValue;
	
	public BossBarUpdateTask (BossBar bar, long maxValue, long initValue) {
		manager = MovecraftUIManager.getInstance();
		bossBar = bar;
		lastUpdate = System.currentTimeMillis();
		this.maxValue = maxValue;
		this.currentValue = initValue;
	}
	
	public void run() {
		if (System.currentTimeMillis() < lastUpdate+1000) {
			return;
		}
		long dt = System.currentTimeMillis() - lastUpdate;
		lastUpdate = System.currentTimeMillis();
		if (bossBar.getTitle().contains("Siege") || bossBar.getTitle().contains("Assault")) {
			if(currentValue <= dt) {
				removeThis();
				return;
			}
			currentValue -= dt;
		}
		
		float actualValue = (float)currentValue / maxValue;
		double barProgress = (double)actualValue;
		bossBar.setProgress(barProgress);
	}
	private void removeThis() {
		manager.removeBossBar(bossBar);
		this.cancel();
		return;
	}
	
}