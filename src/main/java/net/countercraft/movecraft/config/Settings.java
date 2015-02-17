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

package net.countercraft.movecraft.config;

import java.util.List;

public class Settings {
	public static boolean IGNORE_RESET = false;
	public static int THREAD_POOL_SIZE = 5;
	public static List<Integer> DATA_BLOCKS;
	public static String LOCALE;
	public static int PilotTool = 280;
	public static boolean CompatibilityMode = false;
	public static double SinkRateTicks = 20.0;
	public static double SinkCheckTicks = 100.0;
	public static double TracerRateTicks = 5.0;
	public static boolean WorldGuardBlockMoveOnBuildPerm = false;
	public static boolean WorldGuardBlockSinkOnPVPPerm = false;
	public static boolean DisableCrates = false;
	public static boolean DisableSpillProtection = false;
	public static boolean RequireCreatePerm = false;
	public static boolean TNTContactExplosives = true;
	public static int FadeWrecksAfter = 0;
	public static int ManOverBoardTimeout = 60;
	public static int FireballLifespan = 6;
	public static boolean FireballPenetration = true;
}
