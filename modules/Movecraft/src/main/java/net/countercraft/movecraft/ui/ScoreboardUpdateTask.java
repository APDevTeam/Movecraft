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
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.ui.MovecraftUIManager;


public class ScoreboardUpdateTask extends BukkitRunnable {
	private MovecraftUIManager manager;
	private long lastUpdate;
	private Player player;
	
	public ScoreboardUpdateTask(Player p) {
		manager = MovecraftUIManager.getInstance();
		player = p;
		lastUpdate = System.currentTimeMillis();
	}
	
	public void run() {
        if(System.currentTimeMillis() < lastUpdate + 1000) {
        	return;
        }
		
        long dt = System.currentTimeMillis() - lastUpdate;
        lastUpdate = System.currentTimeMillis();
        
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) {
        	this.cancel();
        	return;
       	}
       	boolean cancelthis = true;
        for (Objective objective : scoreboard.getObjectives()) {
       		if (!objective.getName().equalsIgnoreCase(player.getName())){
       			continue;
       		}
       		
       		int convertedDT = (int)Math.round(dt/1000f);
       		if(objective.getScore("Release Timer").getScore() < convertedDT){
       			scoreboard.resetScores("Release Timer");
       		} else if (objective.getScore("Release Timer") != null) {
       			objective.getScore("Release Timer").setScore(objective.getScore("Release Timer").getScore() - convertedDT);
       			cancelthis = false;
       		}
       		player.setScoreboard(scoreboard);
       	}
        if(cancelthis) {
        	manager.removeScoreboardTask(player);
        	this.cancel();
        }
    }
	
	
	
	
	
}