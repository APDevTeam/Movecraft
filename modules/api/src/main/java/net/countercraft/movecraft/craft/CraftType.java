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

package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.utils.BlockContainer;
import net.countercraft.movecraft.utils.BlockLimitManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.lang.Math.max;

final public class CraftType {
    private final boolean blockedByWater;
    private final boolean requireWaterContact;
    private final boolean tryNudge;
    private final boolean canBeNamed;
    private final boolean canCruise;
    private final boolean canTeleport;
    private final boolean canSwitchWorld;
    private final boolean canStaticMove;
    private final boolean canHover;
    private final boolean canDirectControl;
    private final boolean useGravity;
    private final boolean canHoverOverWater;
    private final boolean moveEntities;
    private final boolean onlyMovePlayers;
    private final boolean allowHorizontalMovement;
    private final boolean allowVerticalMovement;
    private final boolean allowRemoteSign;
    private final boolean cruiseOnPilot;
    private final boolean allowVerticalTakeoffAndLanding;
    private final boolean rotateAtMidpoint;
    private final boolean halfSpeedUnderwater;
    private final boolean focusedExplosion;
    private final boolean mustBeSubcraft;
    private final boolean keepMovingOnSink;
    private final int maxSize;
    private final int minSize;
    private final int minHeightLimit;
    @NotNull private final Map<String, Integer> perWorldMinHeightLimit;
    private final int maxHeightLimit;
    @NotNull private final Map<String, Integer> perWorldMaxHeightLimit;
    private final int maxHeightAboveGround;
    @NotNull private final Map<String, Integer> perWorldMaxHeightAboveGround;
    private final int cruiseOnPilotVertMove;
    private final int maxStaticMove;
    private final int cruiseSkipBlocks;
    @NotNull private final Map<String, Integer> perWorldCruiseSkipBlocks;
    private final int vertCruiseSkipBlocks;
    @NotNull private final Map<String, Integer> perWorldVertCruiseSkipBlocks;
    private final int cruiseTickCooldown;
    @NotNull private final Map<String, Integer> perWorldCruiseTickCooldown; // cruise speed setting
    private final int vertCruiseTickCooldown;
    @NotNull private final Map<String, Integer> perWorldVertCruiseTickCooldown; // cruise speed setting
    private final int staticWaterLevel;
    private final int sinkRateTicks;
    private final int smokeOnSink;
    private final int tickCooldown;
    @NotNull private final Map<String, Integer> perWorldTickCooldown; // speed setting
    private final int hoverLimit;
    private final Set<Material> dynamicFlyBlocks;
    private final double fuelBurnRate;
    @NotNull private final Map<String, Double> perWorldFuelBurnRate;
    private final double sinkPercent;
    private final double overallSinkPercent;
    private final double detectionMultiplier;
    @NotNull private final Map<String, Double> perWorldDetectionMultiplier;
    private final double underwaterDetectionMultiplier;
    private final double staticDetectionRange, underwaterStaticDetectionRange;
    @NotNull private final Map<String, Double> perWorldStaticDetectionRange, perWorldUnderwaterStaticDetectionRange;
    @NotNull private final Map<String, Double> perWorldUnderwaterDetectionMultiplier;
    private final double dynamicLagSpeedFactor;
    private final double dynamicLagPowerFactor;
    private final double dynamicLagMinSpeed;
    private final double dynamicFlyBlockSpeedFactor;
    private final double chestPenalty;
    private final float explodeOnCrash;
    private final float collisionExplosion;
    @NotNull private final String craftName;
    @NotNull private final BlockContainer allowedBlocks;
    @NotNull private final BlockContainer forbiddenBlocks;
    @NotNull private final String[] forbiddenSignStrings;
    @NotNull private final BlockLimitManager flyBlocks;
    @NotNull private final BlockLimitManager moveBlocks;
    @NotNull private final List<Material> harvestBlocks;
    @NotNull private final List<Material> harvesterBladeBlocks;
    @NotNull private final Set<Material> passthroughBlocks;
    @NotNull private final Set<Material> forbiddenHoverOverBlocks;
    @NotNull private final Map<Material, Double> fuelTypes;
    @NotNull private final Set<String> disableTeleportToWorlds;
    private final int teleportationCooldown;
    private final int gravityDropDistance;
    private final int gravityInclineDistance;
    private final int gearShifts;
    private final Sound collisionSound;
    @NotNull private final int effectRange;
    @NotNull private final Map<PotionEffect,Integer> potionEffectsToApply;
    @NotNull private final Map<List<String>, Double> maxSignsWithString;
    private final boolean gearShiftsAffectTickCooldown;
    private final boolean gearShiftsAffectDirectMovement;
    private final boolean gearShiftsAffectCruiseSkipBlocks;
    private final boolean lockPilotAtDirectControl;
    @SuppressWarnings("unchecked")
    public CraftType(File f) {
        final Map data;
        try {
            InputStream input = new FileInputStream(f);
            Yaml yaml = new Yaml();
            data = (Map) yaml.load(input);
            input.close();
        }
        catch (IOException e) {
            throw new TypeNotFoundException("No file found at path " + f.getAbsolutePath());
        }

        //Required craft flags
        craftName = (String) data.get("name");
        maxSize = integerFromObject(data.get("maxSize"));
        minSize = integerFromObject(data.get("minSize"));
        allowedBlocks = new BlockContainer(data.get("allowedBlocks"));
        forbiddenBlocks = new BlockContainer(data.get("forbiddenBlocks"));
        forbiddenSignStrings = stringListFromObject(data.get("forbiddenSignStrings"));
        tickCooldown = (int) Math.ceil(20 / (doubleFromObject(data.get("speed"))));
        perWorldTickCooldown = new HashMap<>();
        Map<String, Double> tickCooldownMap = stringToDoubleMapFromObject(data.getOrDefault("perWorldSpeed", new HashMap<>()));
        tickCooldownMap.forEach((world, speed) -> perWorldTickCooldown.put(world, (int) Math.ceil(20 / speed)));
        flyBlocks = new BlockLimitManager(data.get("flyblocks"));
        //Optional craft flags
        blockedByWater = (boolean) (data.containsKey("canFly") ? data.get("canFly") : data.getOrDefault("blockedByWater", true));
        requireWaterContact = (boolean) data.getOrDefault("requireWaterContact", false);
        tryNudge = (boolean) data.getOrDefault("tryNudge", false);
        moveBlocks = new BlockLimitManager(data.getOrDefault("moveblocks", new HashMap<>()));
        canCruise = (boolean) data.getOrDefault("canCruise", false);
        canTeleport = (boolean) data.getOrDefault("canTeleport", false);
        canSwitchWorld = (boolean) data.getOrDefault("canSwitchWorld", false);
        canBeNamed = (boolean) data.getOrDefault("canBeNamed", true);
        cruiseOnPilot = (boolean) data.getOrDefault("cruiseOnPilot", false);
        cruiseOnPilotVertMove = integerFromObject(data.getOrDefault("cruiseOnPilotVertMove", 0));
        allowVerticalMovement = (boolean) data.getOrDefault("allowVerticalMovement", true);
        rotateAtMidpoint = (boolean) data.getOrDefault("rotateAtMidpoint", false);
        allowHorizontalMovement = (boolean) data.getOrDefault("allowHorizontalMovement", true);
        allowRemoteSign = (boolean) data.getOrDefault("allowRemoteSign", true);
        canStaticMove = (boolean) data.getOrDefault("canStaticMove", false);
        maxStaticMove = integerFromObject(data.getOrDefault("maxStaticMove", 10000));
        cruiseSkipBlocks = integerFromObject(data.getOrDefault("cruiseSkipBlocks", 0));
        perWorldCruiseSkipBlocks = stringToIntMapFromObject(data.getOrDefault("perWorldCruiseSkipBlocks", new HashMap<>()));
        vertCruiseSkipBlocks = integerFromObject(data.getOrDefault("vertCruiseSkipBlocks", cruiseSkipBlocks));
        perWorldVertCruiseSkipBlocks = stringToIntMapFromObject(data.getOrDefault("perWorldVertCruiseSkipBlocks", new HashMap<>()));
        halfSpeedUnderwater = (boolean) data.getOrDefault("halfSpeedUnderwater", false);
        focusedExplosion = (boolean) data.getOrDefault("focusedExplosion", false);
        mustBeSubcraft = (boolean) data.getOrDefault("mustBeSubcraft", false);
        staticWaterLevel = integerFromObject(data.getOrDefault("staticWaterLevel", 0));
        fuelBurnRate = doubleFromObject(data.getOrDefault("fuelBurnRate", 0d));
        perWorldFuelBurnRate = stringToDoubleMapFromObject(data.getOrDefault("perWorldFuelBurnRate", new HashMap<>()));
        sinkPercent = doubleFromObject(data.getOrDefault("sinkPercent", 0d));
        overallSinkPercent = doubleFromObject(data.getOrDefault("overallSinkPercent", 0d));
        detectionMultiplier = doubleFromObject(data.getOrDefault("detectionMultiplier", 0d));
        perWorldDetectionMultiplier = stringToDoubleMapFromObject(data.getOrDefault("perWorldDetectionMultiplier", new HashMap<>()));
        underwaterDetectionMultiplier = doubleFromObject(data.getOrDefault("underwaterDetectionMultiplier", detectionMultiplier));
        perWorldUnderwaterDetectionMultiplier = stringToDoubleMapFromObject(data.getOrDefault("perWorldUnderwaterDetectionMultiplier", new HashMap<>()));
        sinkRateTicks = data.containsKey("sinkSpeed") ? (int) Math.ceil(20 / (doubleFromObject(data.get("sinkSpeed")))) : integerFromObject(data.getOrDefault("sinkTickRate", 0));
        keepMovingOnSink = (Boolean) data.getOrDefault("keepMovingOnSink", false);
        smokeOnSink = integerFromObject(data.getOrDefault("smokeOnSink", 0));
        explodeOnCrash = floatFromObject(data.getOrDefault("explodeOnCrash", 0F));
        collisionExplosion = floatFromObject(data.getOrDefault("collisionExplosion", 0F));
        minHeightLimit = max(0, integerFromObject(data.getOrDefault("minHeightLimit", 0)));
        perWorldMinHeightLimit = new HashMap<>();
        Map<String, Integer> minHeightMap = stringToIntMapFromObject(data.getOrDefault("perWorldMinHeightLimit", new HashMap<>()));
        minHeightMap.forEach((world, height) -> perWorldMinHeightLimit.put(world, max(0, height)));

        double cruiseSpeed = doubleFromObject(data.getOrDefault("cruiseSpeed", 20.0 / tickCooldown));
        cruiseTickCooldown = (int) Math.round((1.0 + cruiseSkipBlocks) * 20.0 / cruiseSpeed);
        perWorldCruiseTickCooldown = new HashMap<>();
        Map<String, Double> cruiseTickCooldownMap = stringToDoubleMapFromObject(data.getOrDefault("perWorldCruiseSpeed", new HashMap<>()));
        cruiseTickCooldownMap.forEach((world, speed) -> {
            double worldCruiseSkipBlocks = perWorldCruiseSkipBlocks.getOrDefault(world, cruiseSkipBlocks);
            perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldCruiseSkipBlocks) * 20.0 / cruiseSpeed));
        });
        tickCooldownMap.forEach((world, speed) -> {
            if (!perWorldCruiseTickCooldown.containsKey(world)) {
                perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + cruiseSkipBlocks) * 20.0 / speed));
            }
        });

        double vertCruiseSpeed = doubleFromObject(data.getOrDefault("vertCruiseSpeed", cruiseSpeed));
        vertCruiseTickCooldown = (int) Math.round((1.0 + vertCruiseSkipBlocks) * 20.0 / vertCruiseSpeed);
        perWorldVertCruiseTickCooldown = new HashMap<>();
        Map<String, Double> vertCruiseTickCooldownMap = stringToDoubleMapFromObject(data.getOrDefault("perWorldVertCruiseSpeed", new HashMap<>()));
        vertCruiseTickCooldownMap.forEach((world, speed) -> {
            double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, vertCruiseSkipBlocks);
            perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / speed));
        });
        cruiseTickCooldownMap.forEach((world, speed) -> {
            if (!perWorldVertCruiseTickCooldown.containsKey(world)) {
                double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, vertCruiseSkipBlocks);
                perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / vertCruiseSpeed));
            }
        });

        int value = Math.min(integerFromObject(data.getOrDefault("maxHeightLimit", 254)), 255);
        if (value <= minHeightLimit) {
            value = 255;
        }

        maxHeightLimit = value;
        perWorldMaxHeightLimit = new HashMap<>();
        Map<String, Integer> maxHeightMap = stringToIntMapFromObject(data.getOrDefault("perWorldMaxHeightLimit", new HashMap<>()));
        maxHeightMap.forEach((world, height) -> {
            int worldValue = Math.min(height, 255);
            int worldMinHeight = perWorldMinHeightLimit.getOrDefault(world, minHeightLimit);
            if (worldValue <= worldMinHeight) worldValue = 255;
            perWorldMaxHeightLimit.put(world, worldValue);
        });
        
        maxHeightAboveGround = integerFromObject(data.getOrDefault("maxHeightAboveGround", -1));
        perWorldMaxHeightAboveGround = stringToIntMapFromObject(data.getOrDefault("perWorldMaxHeightAboveGround", new HashMap<>()));
        canDirectControl = (boolean) data.getOrDefault("canDirectControl", true);
        canHover = (boolean) data.getOrDefault("canHover", false);
        canHoverOverWater = (boolean) data.getOrDefault("canHoverOverWater", true);
        moveEntities = (boolean) data.getOrDefault("moveEntities", true);
        onlyMovePlayers = (boolean) data.getOrDefault("onlyMovePlayers", true);
        useGravity = (boolean) data.getOrDefault("useGravity", false);
        hoverLimit = max(0, integerFromObject(data.getOrDefault("hoverLimit", 0)));
        harvestBlocks = new ArrayList<>();
        harvesterBladeBlocks = new ArrayList<>();
        if (data.containsKey("harvestBlocks")) {
            ArrayList objList = (ArrayList) data.get("harvestBlocks");
            for (Object i : objList) {
                if (i instanceof String) {
                    Material mat = Material.getMaterial(((String) i).toUpperCase());
                    harvestBlocks.add(mat);
                } else {
                    Material mat = Material.getMaterial((Integer) i);
                    harvestBlocks.add(mat);
                }
            }

        }
        if (data.containsKey("harvesterBladeBlocks")) {
            ArrayList objList = (ArrayList) data.get("harvesterBladeBlocks");
            for (Object i : objList) {
                if (i instanceof String) {
                    Material mat = Material.getMaterial(((String) i).toUpperCase());
                    harvesterBladeBlocks.add(mat);
                } else {
                    Integer typeID = (Integer) i;
                    Material mat = Material.getMaterial((Integer) i);
                    harvesterBladeBlocks.add(mat);
                }
            }
        }
        passthroughBlocks = new HashSet<>();
        if (data.containsKey("passthroughBlocks")) {
            ArrayList objList = (ArrayList) data.get("passthroughBlocks");
            for (Object i : objList) {
                if (i instanceof String) {
                    Material mat = Material.getMaterial(((String) i).toUpperCase());
                    passthroughBlocks.add(mat);
                } else {
                    Material mat = Material.getMaterial((Integer) i);
                    passthroughBlocks.add(mat);
                }
            }
        }
        if(!blockedByWater){
            passthroughBlocks.add(Material.WATER);
            if (Settings.IsLegacy)
                passthroughBlocks.add(Material.STATIONARY_WATER);
            else
                passthroughBlocks.add(Material.getMaterial("BUBBLE_COLUMN"));
        }
        forbiddenHoverOverBlocks = new HashSet<>();
        if (data.containsKey("forbiddenHoverOverBlocks")){
            final ArrayList objList = (ArrayList) data.get("forbiddenHoverOverBlocks");
            for (Object i : objList){
                if (i instanceof Integer){
                    forbiddenHoverOverBlocks.add(Material.getMaterial((int) i));
                } else if (i instanceof String){
                    forbiddenHoverOverBlocks.add(Material.getMaterial(((String) i).toUpperCase()));
                }
            }
        }
        if (!canHoverOverWater){
            forbiddenHoverOverBlocks.add(Material.WATER);
            if (Settings.IsLegacy) {
                forbiddenHoverOverBlocks.add(Material.STATIONARY_WATER);
            } else {
                forbiddenHoverOverBlocks.add(Material.getMaterial("BUBBLE_COLUMN"));
            }
        }
        allowVerticalTakeoffAndLanding = (boolean) data.getOrDefault("allowVerticalTakeoffAndLanding", true);
        dynamicLagSpeedFactor = doubleFromObject(data.getOrDefault("dynamicLagSpeedFactor", 0d));
        dynamicLagPowerFactor = doubleFromObject(data.getOrDefault("dynamicLagPowerFactor", 0d));
        dynamicLagMinSpeed = doubleFromObject((data.getOrDefault("dynamicLagMinSpeed", 0d)));
        dynamicFlyBlockSpeedFactor = doubleFromObject(data.getOrDefault("dynamicFlyBlockSpeedFactor", 0d));
        dynamicFlyBlocks = new HashSet<>();
        if (data.containsKey("dynamicFlyBlock")) {
            Object d = data.get("dynamicFlyBlock");
            Material type;
            if (d instanceof Integer) {
                type = Material.getMaterial((int) d);
            } else {
                type = Material.getMaterial(((String) d).toUpperCase());
            }
            dynamicFlyBlocks.add(type);
        }
        if (data.containsKey("dynamicFlyBlocks")) {
            Object d = data.get("dynamicFlyBlocks");
            if (d instanceof Integer) {
                dynamicFlyBlocks.add(Material.getMaterial((int) d));
            } else if (d instanceof String){
                dynamicFlyBlocks.add(Material.getMaterial(((String) d).toUpperCase()));
            } else if (d instanceof List) {
                List l = (List) d;
                for (Object i : l) {
                    Material type;
                    if (i instanceof Integer) {
                        type = Material.getMaterial((int) d);
                    } else {
                        type = Material.getMaterial(((String) d).toUpperCase());
                    }
                    dynamicFlyBlocks.add(type);
                }
            }
        }
        chestPenalty = doubleFromObject(data.getOrDefault("chestPenalty", 0));
        gravityInclineDistance = integerFromObject(data.getOrDefault("gravityInclineDistance", -1));
        int dropdist = integerFromObject(data.getOrDefault("gravityDropDistance", -8));
        gravityDropDistance = dropdist > 0 ? -dropdist : dropdist;
        collisionSound = Sound.valueOf((String) data.getOrDefault("collisionSound", "BLOCK_ANVIL_LAND"));
        effectRange = data.containsKey("effectRange") ? integerFromObject(data.get("effectRange")) : 0;
        potionEffectsToApply = data.containsKey("potionEffectsToApply") ? effectListFromObject(data.get("potionEffectsToApply")) : Collections.emptyMap();
        maxSignsWithString = stringDoubleMapFromObject(data.getOrDefault("maxSignsWithString", new HashMap<>()));
        staticDetectionRange = doubleFromObject(data.getOrDefault("staticDetectionRange", 0.0));
        underwaterStaticDetectionRange = doubleFromObject(data.getOrDefault("underwaterStaticDetectionRange", 0.0));
        perWorldUnderwaterStaticDetectionRange = stringToDoubleMapFromObject(data.getOrDefault("perWorldUnderwaterStaticDetectionRange", new HashMap<>()));
        perWorldStaticDetectionRange = stringToDoubleMapFromObject(data.getOrDefault("perWorldStaticDetectionRange", new HashMap<>()));
        fuelTypes = new HashMap<>();
        Map<Object, Object> fTypes = (Map<Object, Object>) data.getOrDefault("fuelTypes", new HashMap<>());
        if (!fTypes.isEmpty()) {
            for (Object k : fTypes.keySet()) {
                Material type;
                if (k instanceof Integer) {
                    type = Material.getMaterial((int) k);
                } else {
                    type = Material.getMaterial(((String) k).toUpperCase());
                }
                Object v = fTypes.get(k);
                double burnRate;
                if (v instanceof String) {
                    burnRate = Double.parseDouble((String) v);
                } else if (v instanceof Integer) {
                    burnRate = ((Integer) v).doubleValue();
                } else {
                    burnRate = (double) v;
                }
                fuelTypes.put(type, burnRate);
            }
        } else {
            fuelTypes.put(Material.COAL_BLOCK, 79.0);
            fuelTypes.put(Material.COAL, 7.0);
            if (!Settings.IsLegacy) {
                fuelTypes.put(Material.getMaterial("CHARCOAL"), 7.0);
            }
        }
        disableTeleportToWorlds = new HashSet<>();
        List<String> disabledWorlds = (List<String>) data.getOrDefault("disableTeleportToWorlds", new ArrayList<>());
        disableTeleportToWorlds.addAll(disabledWorlds);
        teleportationCooldown = integerFromObject(data.getOrDefault("teleportationCooldown", 0));
        gearShifts = Math.max(integerFromObject(data.getOrDefault("gearShifts", 1)), 1);
        gearShiftsAffectTickCooldown = (boolean) data.getOrDefault("gearShiftsAffectTickCooldown", true);
        gearShiftsAffectDirectMovement = (boolean) data.getOrDefault("gearShiftsAffectDirectMovement", false);
        gearShiftsAffectCruiseSkipBlocks = (boolean) data.getOrDefault("gearShiftsAffectCruiseSkipBlocks", false);
        lockPilotAtDirectControl = (boolean) data.getOrDefault("lockPilotAtDirectControl", false);
    }

    private int integerFromObject(Object obj) {
        if (obj instanceof Double) {
            return ((Double) obj).intValue();
        }
        return (Integer) obj;
    }


    private double doubleFromObject(Object obj) {
        if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        }
        return (Double) obj;
    }

    private float floatFromObject(Object obj) {
        if (obj instanceof Double) {
            return ((Double) obj).floatValue();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).floatValue();
        }
        return (float) obj;
    }
    
    private Map<String, Integer> stringToIntMapFromObject(Object obj) {
        HashMap<String, Integer> returnMap = new HashMap<>();
        HashMap<Object, Object> objMap = (HashMap<Object, Object>) obj;
        for (Object key : objMap.keySet()) {
            String str = (String) key;
            Integer i = (Integer) objMap.get(key);
            returnMap.put(str, i);
        }
        return returnMap;
    }
    
    private Map<String, Double> stringToDoubleMapFromObject(Object obj) {
        HashMap<String, Double> returnMap = new HashMap<>();
        HashMap<Object, Object> objMap = (HashMap<Object, Object>) obj;
        for (Object key : objMap.keySet()) {
            String str = (String) key;
            Double d = (Double) objMap.get(key);
            returnMap.put(str, d);
        }
        return returnMap;
    }

    private int[] blockIDListFromObject(Object obj) {
        ArrayList<Integer> returnList = new ArrayList<>();
        ArrayList objList = (ArrayList) obj;
        for (Object i : objList) {
            if (i instanceof String) {
                String str = (String) i;
                if (str.contains(":")) {
                    String[] parts = str.split(":");
                    Integer typeID = Integer.valueOf(parts[0]);
                    Integer metaData = Integer.valueOf(parts[1]);
                    returnList.add(10000 + (typeID << 4) + metaData);  // id greater than 10000 indicates it has a meta data / damage value
                } else {
                    Integer typeID = Integer.valueOf(str);
                    returnList.add(typeID);
                }
            } else {
                Integer typeID = (Integer) i;
                returnList.add(typeID);
            }
        }
        int[] output = new int[returnList.size()];
        for (int i = 0; i < output.length; i++)
            output[i] = returnList.get(i);
        return output;
    }

    private Map<List<String>, Double> stringDoubleMapFromObject(Object obj) {
        Map objMap = (Map) obj;
        Map<List<String>, Double> retMap = new HashMap<>();
        for (Object k : objMap.keySet()) {
            final List<String> key = new ArrayList<>();
            if (k instanceof List) {
                List strings = (List) k;
                for (Object i : strings) {
                    if (i instanceof String) {
                        key.add(((String) i).toLowerCase());
                    } else {
                        key.add(String.valueOf(i).toLowerCase());
                    }
                }
            } else if (k instanceof String) {
                key.add(((String) k).toLowerCase());
            } else {
                key.add(String.valueOf(k).toLowerCase());
            }
            final Object v = objMap.get(k);
            final double value;
            if (v instanceof String) {
                final String str = (String) v;
                if (str.startsWith("N")) {
                    value = 10000.0 + Double.parseDouble(str.substring(1));
                } else {
                    value = Double.parseDouble(str);
                }
            } else {
                value = (double) v;
            }
            retMap.put(key, value);
        }
        return retMap;
    }

    private HashMap<PotionEffect, Integer> effectListFromObject(Object obj){
        Map<Object,Object> objMap = (Map<Object, Object>) obj;
        HashMap<PotionEffect,Integer> ret = new HashMap<>();
        for (Object o : objMap.keySet()){
            PotionEffectType effect = null;
            int duration = 0;
            int amplifier = 1;
            int delay = 0;
            if (o instanceof String){
                String string = (String) o;
                effect = PotionEffectType.getByName(string);
            }
            Map<Object, Object> subObjMap = (Map<Object, Object>) objMap.get(o);
            for (Object i : subObjMap.keySet()){
                String str = (String) i;
                int integer = (int) subObjMap.get(i);
                if (str.equals("duration")){
                    duration = 20 * integer;
                }
                if (str.equals("amplifier")){
                    amplifier = integer;
                }
                if (str.equals("delay")){
                    delay = integer;
                }
            }
            if (effect == null){
                continue;
            }
            PotionEffect potEffect = new PotionEffect(effect,duration,amplifier,true,true);
            ret.put(potEffect,delay);
        }
        return ret;
    }

    private String[] stringListFromObject(Object obj) {
        ArrayList<String> returnList = new ArrayList<>();
        if (obj == null) {
            return returnList.toArray(new String[1]);
        }
        ArrayList objList = (ArrayList) obj;
        for (Object i : objList) {
            if (i instanceof String) {
                String str = (String) i;
                returnList.add(str);
            }
        }
        return returnList.toArray(new String[1]);
    }

    private Map<List<Integer>, List<Double>> blockIDMapListFromObject(Object obj) {
        HashMap<List<Integer>, List<Double>> returnMap = new HashMap<>();
        HashMap<Object, Object> objMap = (HashMap<Object, Object>) obj;
        for (Object i : objMap.keySet()) {
            ArrayList<Integer> rowList = new ArrayList<>();

            // first read in the list of the blocks that type of flyblock. It could be a single string (with or without a ":") or integer, or it could be multiple of them
            if (i instanceof ArrayList<?>) {
                for (Object o : (ArrayList<Object>) i) {
                    if (o instanceof String) {
                        String str = (String) o;
                        if (str.contains(":")) {
                            String[] parts = str.split(":");
                            Integer typeID = Integer.valueOf(parts[0]);
                            Integer metaData = Integer.valueOf(parts[1]);
                            rowList.add(10000 + (typeID << 4) + metaData);  // id greater than 10000 indicates it has a meta data / damage value
                        } else {
                            Integer typeID = Integer.valueOf(str);
                            rowList.add(typeID);
                        }
                    } else {
                        Integer typeID = (Integer) o;
                        rowList.add(typeID);
                    }
                }
            } else if (i instanceof String) {
                String str = (String) i;
                if (str.contains(":")) {
                    String[] parts = str.split(":");
                    Integer typeID = Integer.valueOf(parts[0]);
                    Integer metaData = Integer.valueOf(parts[1]);
                    rowList.add(10000 + (typeID << 4) + metaData);  // id greater than 10000 indicates it has a meta data / damage value
                } else {
                    Integer typeID = Integer.valueOf(str);
                    rowList.add(typeID);
                }
            } else {
                Integer typeID = (Integer) i;
                rowList.add(typeID);
            }

            // then read in the limitation values, low and high
            ArrayList<Object> objList = (ArrayList<Object>) objMap.get(i);
            ArrayList<Double> limitList = new ArrayList<>();
            for (Object limitObj : objList) {
                if (limitObj instanceof String) {
                    String str = (String) limitObj;
                    if (str.contains("N")) { // a # indicates a specific quantity, IE: #2 for exactly 2 of the block
                        String[] parts = str.split("N");
                        Double val = Double.valueOf(parts[1]);
                        limitList.add(10000d + val);  // limit greater than 10000 indicates an specific quantity (not a ratio)
                    } else {
                        Double val = Double.valueOf(str);
                        limitList.add(val);
                    }
                } else if (limitObj instanceof Integer) {
                    Double ret = ((Integer) limitObj).doubleValue();
                    limitList.add(ret);
                } else
                    limitList.add((Double) limitObj);
            }
            returnMap.put(rowList, limitList);
        }
        return returnMap;
    }

    @NotNull
    public String getCraftName() {
        return craftName;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMinSize() {
        return minSize;
    }

    public BlockContainer getAllowedBlocks() {
        return allowedBlocks;
    }

    public BlockContainer getForbiddenBlocks() {
        return forbiddenBlocks;
    }

    public String[] getForbiddenSignStrings() {
        return forbiddenSignStrings;
    }

    public boolean blockedByWater() {
        return blockedByWater;
    }

    public boolean getRequireWaterContact() {
        return requireWaterContact;
    }

    public boolean getCanCruise() {
        return canCruise;
    }

    @Deprecated
    public int getCruiseSkipBlocks() {
        return cruiseSkipBlocks;
    }
    public int getCruiseSkipBlocks(@NotNull World world) {
        return perWorldCruiseSkipBlocks.getOrDefault(world.getName(), cruiseSkipBlocks);
    }

    @Deprecated
    public int getVertCruiseSkipBlocks() {
        return vertCruiseSkipBlocks;
    }

    public int getVertCruiseSkipBlocks(@NotNull World world) {
        return perWorldVertCruiseSkipBlocks.getOrDefault(world.getName(), vertCruiseSkipBlocks);
    }

    public boolean getCanBeNamed(){
        return canBeNamed;
    }

    public int maxStaticMove() {
        return maxStaticMove;
    }

    public int getStaticWaterLevel() {
        return staticWaterLevel;
    }

    public boolean getCanTeleport() {
        return canTeleport;
    }
    
    public boolean getCanSwitchWorld() {
        return canSwitchWorld;
    }

    public boolean getCanStaticMove() {
        return canStaticMove;
    }

    public boolean getCruiseOnPilot() {
        return cruiseOnPilot;
    }

    public int getCruiseOnPilotVertMove() {
        return cruiseOnPilotVertMove;
    }

    public boolean allowVerticalMovement() {
        return allowVerticalMovement;
    }

    public boolean rotateAtMidpoint() {
        return rotateAtMidpoint;
    }

    public boolean allowHorizontalMovement() {
        return allowHorizontalMovement;
    }

    public boolean allowRemoteSign() {
        return allowRemoteSign;
    }

    @Deprecated
    public double getFuelBurnRate() {
        return fuelBurnRate;
    }
    public double getFuelBurnRate(@NotNull World world) {
        return perWorldFuelBurnRate.getOrDefault(world.getName(), fuelBurnRate);
    }

    public double getSinkPercent() {
        return sinkPercent;
    }

    public double getOverallSinkPercent() {
        return overallSinkPercent;
    }

    @Deprecated
    public double getDetectionMultiplier() {
        return detectionMultiplier;
    }
    public double getDetectionMultiplier(@NotNull World world) {
        return perWorldDetectionMultiplier.getOrDefault(world.getName(), detectionMultiplier);
    }

    @Deprecated
    public double getUnderwaterDetectionMultiplier() {
        return underwaterDetectionMultiplier;
    }
    public double getUnderwaterDetectionMultiplier(@NotNull World world) {
        return perWorldUnderwaterDetectionMultiplier.getOrDefault(world.getName(), underwaterDetectionMultiplier);
    }

    public int getSinkRateTicks() {
        return sinkRateTicks;
    }

    public boolean getKeepMovingOnSink() {
        return keepMovingOnSink;
    }

    public float getExplodeOnCrash() {
        return explodeOnCrash;
    }

    public int getSmokeOnSink() {
        return smokeOnSink;
    }

    public float getCollisionExplosion() {
        return collisionExplosion;
    }

    @Deprecated
    public int getTickCooldown() {
        return tickCooldown;
    }
    public int getTickCooldown(@NotNull World world) {
        return perWorldTickCooldown.getOrDefault(world.getName(), tickCooldown);
    }

    @Deprecated
    public int getCruiseTickCooldown() {
        return cruiseTickCooldown;
    }

    public int getCruiseTickCooldown(@NotNull World world) {
        return perWorldCruiseTickCooldown.getOrDefault(world.getName(), cruiseTickCooldown);
    }

    @Deprecated
    public int getVertCruiseTickCooldown() {
        return vertCruiseTickCooldown;
    }

    public int getVertCruiseTickCooldown(@NotNull World world) {
        return perWorldVertCruiseTickCooldown.getOrDefault(world.getName(), vertCruiseTickCooldown);
    }

    public boolean getHalfSpeedUnderwater() {
        return halfSpeedUnderwater;
    }

    public boolean getFocusedExplosion() {
        return focusedExplosion;
    }

    public boolean getMustBeSubcraft() {
        return mustBeSubcraft;
    }

    public boolean isTryNudge() {
        return tryNudge;
    }

    @NotNull
    public BlockLimitManager getFlyBlocks() {
        return flyBlocks;
    }

    @NotNull
    public BlockLimitManager getMoveBlocks() {
        return moveBlocks;
    }

    @Deprecated
    public int getMaxHeightLimit() {
        return maxHeightLimit;
    }
    public int getMaxHeightLimit(@NotNull World world) {
        return perWorldMaxHeightLimit.getOrDefault(world.getName(), maxHeightLimit);
    }

    @Deprecated
    public int getMinHeightLimit() {
        return minHeightLimit;
    }
    public int getMinHeightLimit(@NotNull World world) {
        return perWorldMinHeightLimit.getOrDefault(world.getName(), minHeightLimit);
    }

    @Deprecated
    public int getMaxHeightAboveGround() {
        return maxHeightAboveGround;
    }
    public int getMaxHeightAboveGround(@NotNull World world) {
        return perWorldMaxHeightAboveGround.getOrDefault(world.getName(), maxHeightAboveGround);
    }

    public boolean getCanHover() {
        return canHover;
    }

    public boolean getCanDirectControl() {
        return canDirectControl;
    }

    public int getHoverLimit() {
        return hoverLimit;
    }

    @NotNull
    public List<Material> getHarvestBlocks() {
        return harvestBlocks;
    }

    @NotNull
    public List<Material> getHarvesterBladeBlocks() {
        return harvesterBladeBlocks;
    }

    public boolean getCanHoverOverWater() {
        return canHoverOverWater;
    }

    public boolean getMoveEntities() {
        return moveEntities;
    }

    public boolean getUseGravity() {
        return useGravity;
    }

    public boolean allowVerticalTakeoffAndLanding() {
        return allowVerticalTakeoffAndLanding;
    }

    public double getDynamicLagSpeedFactor() {
        return dynamicLagSpeedFactor;
    }

    public double getDynamicLagPowerFactor() {
        return dynamicLagPowerFactor;
    }

    public double getDynamicLagMinSpeed() {
        return dynamicLagMinSpeed;
    }

    public double getDynamicFlyBlockSpeedFactor() {
        return dynamicFlyBlockSpeedFactor;
    }

    public Set<Material> getDynamicFlyBlocks() {
        return dynamicFlyBlocks;
    }

    public double getChestPenalty() {
        return chestPenalty;
    }

    @NotNull
    public Set<Material> getPassthroughBlocks() {
        return passthroughBlocks;
    }

    public boolean getOnlyMovePlayers() {
        return onlyMovePlayers;
    }

    @NotNull
    public Set<Material> getForbiddenHoverOverBlocks() {
        return forbiddenHoverOverBlocks;
    }

    public int getGravityDropDistance() {
        return gravityDropDistance;
    }

    public int getGravityInclineDistance() {
        return gravityInclineDistance;
    }

    @NotNull
    public Sound getCollisionSound() {
        return collisionSound;
    }

    public int getEffectRange() {
        return effectRange;
    }

    @NotNull
    public Map<PotionEffect, Integer> getPotionEffectsToApply() {
        return potionEffectsToApply;
    }

    @NotNull
    public Map<List<String>, Double> getMaxSignsWithString() {
        return maxSignsWithString;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public double getStaticDetectionRange() {
        return staticDetectionRange;
    }

    public double getStaticDetectionRange(World world) {
        return perWorldStaticDetectionRange.getOrDefault(world.getName(), staticDetectionRange);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public double getUnderwaterStaticDetectionRange() {
        return underwaterStaticDetectionRange;
    }

    public double getUnderwaterStaticDetectionRange(World world) {
        return perWorldUnderwaterStaticDetectionRange.getOrDefault(world.getName(), underwaterStaticDetectionRange);
    }

    @NotNull
    public Map<Material, Double> getFuelTypes() {
        return fuelTypes;
    }

    @NotNull
    public Set<String> getDisableTeleportToWorlds() {
        return disableTeleportToWorlds;
    }

    public int getTeleportationCooldown() {
        return teleportationCooldown;
    }

    public int getGearShifts() {
        return gearShifts;
    }

    public boolean getGearShiftsAffectTickCooldown() {
        return gearShiftsAffectTickCooldown;
    }

    public boolean getGearShiftsAffectDirectMovement() {
        return gearShiftsAffectDirectMovement;
    }

    public boolean getGearShiftsAffectCruiseSkipBlocks() {
        return gearShiftsAffectCruiseSkipBlocks;
    }

    public boolean getLockPilotAtDirectControl() {
        return lockPilotAtDirectControl;
    }

    public class TypeNotFoundException extends RuntimeException {
        public TypeNotFoundException(String s) {
            super(s);
        }
    }
}