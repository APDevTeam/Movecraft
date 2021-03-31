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

import org.bukkit.Material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Settings {

    public static boolean IGNORE_RESET = false;
    public static boolean Debug = false;
    public static int THREAD_POOL_SIZE = 5;
    public static List<Material> DATA_BLOCKS;
    public static String LOCALE;
    public static Material PilotTool = Material.STICK;
    public static int SilhouetteViewDistance = 200;
    public static int SilhouetteBlockCount = 20;
    public static boolean CompatibilityMode = false;
    public static boolean DelayColorChanges = false;
    public static double SinkRateTicks = 20.0;
    public static double SinkCheckTicks = 100.0;
    public static boolean ProtectPilotedCrafts = false;
    public static boolean DisableSpillProtection = false;
    public static boolean DisableIceForm = true;
    public static boolean RequireCreatePerm = false;
    public static boolean RequireNamePerm = false;
    public static boolean RequireSneakingForDirectControl = false;
    public static HashSet<String> ForbiddenRemoteSigns;
    public static int FadeWrecksAfter = 0;
    public static int FadeTickCooldown = 20;
    public static double FadePercentageOfWreckPerCycle = 10.0;
    public static Map<Material, Integer> ExtraFadeTimePerBlock = new HashMap<>();
    public static int ManOverboardTimeout = 60;
    public static double ManOverboardDistSquared = 1000000;
    public static int CollisionPrimer = 1000;
    public static int MaxRemoteSigns = -1;
    public static boolean CraftsUseNetherPortals = false;
    public static boolean IsPaper = false;
    public static boolean IsPre1_9 = false;
    public static boolean IsLegacy = true; //false if version is 1.13 or higher
    public static boolean is1_14 = false;
    public static boolean UseFAWE = false;
    public static HashSet<Material> DisableShadowBlocks = new HashSet<>();
    public static boolean CheckForUpdates;
    public static boolean GearshiftsWithPilotToolEnabled;
}
