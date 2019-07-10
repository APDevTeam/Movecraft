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
 
 import net.countercraft.movecraft.Movecraft;
 import net.countercraft.movecraft.ui.ScoreboardUpdateTask;
 import org.bukkit.scoreboard.Scoreboard;
 import org.bukkit.scoreboard.ScoreboardManager;
 import org.bukkit.scoreboard.Objective;
 import org.bukkit.scoreboard.DisplaySlot;
 import org.bukkit.Bukkit;
 import org.bukkit.boss.BossBar;
 import org.bukkit.boss.BarColor;
 import org.bukkit.boss.BarFlag;
 import org.bukkit.boss.BarStyle;
 import org.bukkit.plugin.Plugin;
 import org.bukkit.entity.Player;
 
 import java.util.*;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 
 
 public class MovecraftUIManager {
	 private static MovecraftUIManager ourInstance;
	 private ScoreboardManager scoreboardManager;
	 private ConcurrentMap<Player, ScoreboardUpdateTask> scoreboardTasks = new ConcurrentHashMap();
	 private ConcurrentMap<BossBar, BossBarUpdateTask> bossBarTasks = new ConcurrentHashMap();  
	 private Set<Scoreboard> boards = ConcurrentHashMap.newKeySet();
	 private Set<BossBar> bossBars = ConcurrentHashMap.newKeySet();
	 private Movecraft movecraft;
	 
	 
	 public static void initialize() {
		 ourInstance = new MovecraftUIManager();
	 }
	 
	 public static MovecraftUIManager getInstance() {
		 return ourInstance;
	 }
	 
	 private MovecraftUIManager() {
		 scoreboardManager = Bukkit.getScoreboardManager();
		 movecraft = movecraft.getInstance();
	 }
	 
	 public void newScoreboard(Player p) {
		 if (p == null)  
			 return;
		 if(p.getScoreboard() != null){ 
			 ensureNoDuplicates();
			 return;
		 }
		 Scoreboard newboard = scoreboardManager.getNewScoreboard();
		 p.setScoreboard(newboard);
	 }
	 
	 public void setScoreboardValue(String label, Player p, int value) {
		 if (p == null) {
			 return;
		 }
		 
		 if (p.getScoreboard() == null) {
			 newScoreboard(p);
		 } else {
			 ensureNoDuplicates();
		 }
		 Scoreboard board = p.getScoreboard();
		 Objective boardObjective;
		 
		 if (board.getObjective(p.getName()) == null){
			 boardObjective = board.registerNewObjective(p.getName(), "dummy");
		 } else {
			 boardObjective = board.getObjective(p.getName());
		 }
		 boardObjective.setDisplayName("Movecraft");
		 boardObjective.getScore(label).setScore(value);
		 boardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
		 p.setScoreboard(board);
		 if (!scoreboardTasks.containsKey(p)){
			 ScoreboardUpdateTask task = new ScoreboardUpdateTask(p);
			 scoreboardTasks.put(p, task);
			 task.runTaskTimer((Plugin)movecraft, 0L, 20L);
		 }
		 
	 }
	 
	 public void removeScoreboardValue(String label, Player p) {
		 if (p.getScoreboard() == null) {
			 return;
		 }
		 ensureNoDuplicates();
		 Scoreboard board = p.getScoreboard();
		 if (board.getObjective(p.getName()) == null) {
			 return;
		 }
		 board.resetScores(label); 
	 }
	 
	 public void setupUI(Player p) {
		 Scoreboard board = scoreboardManager.getNewScoreboard();
		 p.setScoreboard(board);
		 for (BossBar bar : bossBars){
			 if(bar.getTitle().contains("Siege") || bar.getTitle().contains("Assault")){
				 bar.addPlayer(p);
			 }
			 if(bar.getTitle().equalsIgnoreCase("Damages")) {
				 bar.addPlayer(p);
			 }
		 }
	 }
	 
	 public BossBar getBarByTitle(String title) {
		 for (BossBar bar : bossBars) {
			 if(bar.getTitle().equalsIgnoreCase(title)){
				 return bar;
			 }
		 }
		 return null;
	 }
	 
	 public BossBar createNewBossBar(String title, long max, long init, boolean createUpdateTask) {
		 BarFlag[] empty = new BarFlag[0];
		 BossBar bar = Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SOLID, empty);
		 if (title.contains("Siege") || title.contains("Assault")) {
			 bar.setColor(BarColor.PURPLE);
		 }
		 if (title.contains("Damages")) {
			 bar.setColor(BarColor.RED);
		 }
		 bossBars.add(bar);
		 if(createUpdateTask) {
			 BossBarUpdateTask task = new BossBarUpdateTask(bar, max, init);
			 bossBarTasks.put(bar, task);
			 task.runTaskTimer((Plugin)movecraft, 0L, 20L);
		 }
		 return bar;
	 }
	 
	 public void removeBossBarTask(BossBar bar) {
		 if (!bossBarTasks.containsKey(bar)) {
			 return;
		 }
		 bossBarTasks.remove(bar);
	 }
	 
	 public void removeScoreboardTask (Player p) {
		 if (!scoreboardTasks.containsKey(p)) {
			 return;
		 }
		 scoreboardTasks.remove(p);
	 }
	 
	 public void removeBossBar (BossBar bar) {
		 if (!bossBars.contains(bar)) {
			 return;
		 }
		 removeBossBarTask(bar);
		 for (Player player : bar.getPlayers()) {
			 bar.removePlayer(player);
		 }
		 bossBars.remove(bar);
	 }
	 
	 private void ensureNoDuplicates() {
		 boards = ConcurrentHashMap.newKeySet();
		 for(Player player : Bukkit.getOnlinePlayers()) {
			 if (player.getScoreboard() == null) {
				 continue;
			 }
			 if (boards.contains(player.getScoreboard())) {
				 Scoreboard newScoreboard = scoreboardManager.getNewScoreboard();
				 boards.add(newScoreboard);
				 player.setScoreboard(newScoreboard);
				 continue;
			 }
			 boards.add(player.getScoreboard());
		 }
	 }
 }