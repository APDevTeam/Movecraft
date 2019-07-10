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

import net.countercraft.movecraft.utils.TownyWorldHeightLimits;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Settings {
    public static boolean RestrictSiBsToRegions = false;
    public static boolean IGNORE_RESET = false;
    public static boolean Debug = false;
    public static int THREAD_POOL_SIZE = 5;
    public static List<Integer> DATA_BLOCKS;
    public static String LOCALE;
    public static int PilotTool = 280;
    public static int SilhouetteViewDistance = 200;
    public static int SilhouetteBlockCount = 20;
    public static boolean CompatibilityMode = false;
    public static boolean DelayColorChanges = false;
    public static double SinkRateTicks = 20.0;
    public static double SinkCheckTicks = 100.0;
    public static double TracerRateTicks = 5.0;
    public static long TracerMinDistanceSqrd = 360;
    public static boolean WorldGuardBlockMoveOnBuildPerm = false;
    public static boolean WorldGuardBlockSinkOnPVPPerm = false;
    public static boolean ProtectPilotedCrafts = false;
    public static boolean DisableSpillProtection = false;
    public static boolean RequireCreatePerm = false;
    public static boolean RequireNamePerm = false;
    public static boolean TNTContactExplosives = true;
    public static int FadeWrecksAfter = 0;
    public static int ManOverboardTimeout = 60;
    public static double ManOverboardDistSquared = 1000000;
    public static int FireballLifespan = 6;
    public static int RepairTicksPerBlock = 0;
    public static double RepairMaxPercent = 50;
    public static HashMap<Material, List<Integer>> RepairRequireSpecificMaterials = new HashMap<>();
    public static int BlockQueueChunkSize = 1000;
    public static int SiegeTaskSeconds = 600;
    public static double RepairMoneyPerBlock = 0.0;
    public static boolean FireballPenetration = true;
    public static boolean AllowCrewSigns = true;
    public static boolean SetHomeToCrewSign = true;
    public static int MaxRemoteSigns = -1;
    public static boolean WGCustomFlagsUsePilotFlag = false;
    public static boolean WGCustomFlagsUseMoveFlag = false;
    public static boolean WGCustomFlagsUseRotateFlag = false;
    public static boolean WGCustomFlagsUseSinkFlag = false;
    public static boolean TownyBlockMoveOnSwitchPerm = false;
    public static boolean TownyBlockSinkOnNoPVP = false;
    public static Map<String, TownyWorldHeightLimits> TownProtectionHeightLimits;
    public static Map<Integer, Integer> DurabilityOverride;
    public static boolean IsPaper = false;

    public static boolean AssaultEnable;
    public static double AssaultDamagesCapPercent;
    public static int AssaultCooldownHours;
    public static int AssaultDelay;
    public static int AssaultDuration;
    public static int AssaultRequiredDefendersOnline;
    public static int AssaultRequiredOwnersOnline;
    public static double AssaultCostPercent;
    public static double AssaultMaxBalance;
    public static double AssaultOwnerWeightPercent;
    public static double AssaultMemberWeightPercent;
    public static HashSet<Integer> AssaultDestroyableBlocks;
    public static int AssaultDamagesPerBlock;
    public static HashSet<Integer> DisableShadowBlocks;
    public static HashSet<String> ForbiddenRemoteSigns;

    public static boolean SiegeEnable;
}
