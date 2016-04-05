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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.countercraft.movecraft.utils.TownyWorldHeightLimits;

public class Settings {
	public static boolean IGNORE_RESET = false;
	public static boolean Debug = false;
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
	public static boolean ProtectPilotedCrafts = false;
	public static boolean DisableSpillProtection = false;
	public static boolean RequireCreatePerm = false;
	public static boolean TNTContactExplosives = true;
	public static int FadeWrecksAfter = 0;
	public static int ManOverBoardTimeout = 60;
	public static int FireballLifespan = 6;
	public static int RepairTicksPerBlock = 0;
	public static int BlockQueueChunkSize = 1000;
	public static double RepairMoneyPerBlock = 0.0;
	public static boolean FireballPenetration = true;
	public static boolean AllowCrewSigns = true;
	public static boolean SetHomeToCrewSign = true;
        public static boolean WGCustomFlagsUsePilotFlag = false;
        public static boolean WGCustomFlagsUseMoveFlag = false;
        public static boolean WGCustomFlagsUseRotateFlag = false;
        public static boolean WGCustomFlagsUseSinkFlag = false;
        public static boolean TownyBlockMoveOnSwitchPerm = false;
        public static boolean TownyBlockSinkOnNoPVP = false;
        public static Map<String, TownyWorldHeightLimits> TownProtectionHeightLimits;

    public static Set<String> SiegeName;
    public static Map<String, String> SiegeRegion;
    public static Map<String, ArrayList<String>> SiegeCraftsToWin;
	public static Map<String, Integer> SiegeCost;
	public static Map<String, Boolean> SiegeDoubleCost;
	public static Map<String, Integer> SiegeIncome;
	public static Map<String, Integer> SiegeScheduleStart;
	public static Map<String, Integer> SiegeScheduleEnd;
    public static Map<String, String> SiegeControlRegion;
	public static Map<String, Integer> SiegeDelay;
	public static Map<String, Integer> SiegeDuration;
}
