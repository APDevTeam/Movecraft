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

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.bukkit.Material;

import java.util.logging.Level;

public class CraftType {
	private String craftName;
	private int maxSize, minSize, minHeightLimit, maxHeightLimit;
	private Integer[] allowedBlocks, forbiddenBlocks;
	private boolean blockedByWater, tryNudge, canCruise, canTeleport, canStaticMove, canHover, canDirectControl, useGravity, canHoverOverWater, moveEntities;
	private boolean allowHorizontalMovement, allowVerticalMovement, cruiseOnPilot;
	private int maxStaticMove;
	private int cruiseSkipBlocks;
	private int staticWaterLevel;
	private double fuelBurnRate;
	private double sinkPercent;
	private double overallSinkPercent;
	private int sinkRateTicks;
	private boolean keepMovingOnSink;
	private int smokeOnSink;
	private float explodeOnCrash;
	private float collisionExplosion;
	private int tickCooldown;
	private HashMap<ArrayList<Integer>, ArrayList<Double>> flyBlocks = new HashMap<ArrayList<Integer>, ArrayList<Double>>();
	private int hoverLimit;
	private List<Material> harvestBlocks;
	
	public CraftType( File f ) {
		try {
			parseCraftDataFromFile( f );
		} catch ( Exception e ) {
			Movecraft.getInstance().getLogger().log( Level.SEVERE, String.format( I18nSupport.getInternationalisedString( "Startup - Error parsing CraftType file" ) , f.getAbsolutePath() ) );
			e.printStackTrace();
		}
	}
	
	private Integer integerFromObject(Object obj) {
		if(obj instanceof Double) {
			return ((Double)obj).intValue();
		}
		return (Integer)obj;
	}

	private Double doubleFromObject(Object obj) {
		if(obj instanceof Integer) {
			return ((Integer)obj).doubleValue();
		}
		return (Double)obj;
	}

	private Integer[] blockIDListFromObject(Object obj) {
		ArrayList<Integer> returnList=new ArrayList<Integer>();
		ArrayList objList=(ArrayList) obj;
		for(Object i : objList) {
			if(i instanceof String) {
				String str=(String)i;
				if(str.contains(":")) {
					String[] parts=str.split(":");
					Integer typeID=Integer.valueOf(parts[0]);
					Integer metaData=Integer.valueOf(parts[1]);
					returnList.add(10000+(typeID<<4)+metaData);  // id greater than 10000 indicates it has a meta data / damage value
				} else {
					Integer typeID=Integer.valueOf(str);
					returnList.add(typeID);
				}
			} else {
				Integer typeID=(Integer)i;
				returnList.add(typeID);
			}
		}
		return returnList.toArray(new Integer[1]);
	}

	private HashMap<ArrayList<Integer>, ArrayList<Double>> blockIDMapListFromObject(Object obj) {
		//flyBlocks = ( HashMap<Integer, ArrayList<Double>> ) data.get( "flyblocks" );
		
		HashMap<ArrayList<Integer>, ArrayList<Double>> returnMap=new HashMap<ArrayList<Integer>, ArrayList<Double>>();
		HashMap<Object, Object> objMap=(HashMap<Object, Object>) obj;
		for(Object i : objMap.keySet()) {
			ArrayList<Integer> rowList=new ArrayList<Integer>();

			// first read in the list of the blocks that type of flyblock. It could be a single string (with or without a ":") or integer, or it could be multiple of them
			if(i instanceof ArrayList<?>) {
				for(Object o : (ArrayList<Object>)i) {
					if(o instanceof String) {
						String str=(String)o;
						if(str.contains(":")) {
							String[] parts=str.split(":");
							Integer typeID=Integer.valueOf(parts[0]);
							Integer metaData=Integer.valueOf(parts[1]);
							rowList.add(10000+(typeID<<4)+metaData);  // id greater than 10000 indicates it has a meta data / damage value
						} else {
							Integer typeID=Integer.valueOf(str);
							rowList.add(typeID);
						}
					} else {
						Integer typeID=(Integer)o;
						rowList.add(typeID);
					}
				}
			} else 
			if(i instanceof String) {
				String str=(String)i;
				if(str.contains(":")) {
					String[] parts=str.split(":");
					Integer typeID=Integer.valueOf(parts[0]);
					Integer metaData=Integer.valueOf(parts[1]);
					rowList.add(10000+(typeID<<4)+metaData);  // id greater than 10000 indicates it has a meta data / damage value
				} else {
					Integer typeID=Integer.valueOf(str);
					rowList.add(typeID);
				}
			} else {
				Integer typeID=(Integer)i;
				rowList.add(typeID);
			}

			ArrayList<Object> objList=(ArrayList<Object>)objMap.get(i);
			ArrayList<Double> limitList=new ArrayList<Double>();
			for(Object limitObj : objList) {
				if(limitObj instanceof Integer) {
					Double ret=((Integer)limitObj).doubleValue();
					limitList.add(ret);
				} else
					limitList.add((Double)limitObj);
			}
			returnMap.put(rowList, limitList);
		}
		return returnMap;
	}

	private void parseCraftDataFromFile( File file ) throws FileNotFoundException {
		InputStream input = new FileInputStream(file);
		Yaml yaml = new Yaml();
		Map data = ( Map ) yaml.load( input );
		craftName = ( String ) data.get( "name" );
		maxSize = integerFromObject(data.get( "maxSize" ));
		minSize = integerFromObject(data.get( "minSize" ));
//		allowedBlocks = ((ArrayList<String> ) data.get( "allowedBlocks" )).toArray( new Integer[1] );
		allowedBlocks = blockIDListFromObject(data.get( "allowedBlocks" ));
		
		forbiddenBlocks = blockIDListFromObject(data.get( "forbiddenBlocks" ));
		if(data.containsKey("canFly")) {
			blockedByWater = ( Boolean ) data.get( "canFly" );
		} else if (data.containsKey("blockedByWater")) {
			blockedByWater = ( Boolean ) data.get( "blockedByWater" );			
		} else {
			blockedByWater = true;
		}
		if(data.containsKey("tryNudge")) {
			tryNudge=(Boolean) data.get("tryNudge");
		} else {
			tryNudge=false;
		}
		tickCooldown = (int) Math.ceil( 20 / ( doubleFromObject(data.get( "speed" )) ) );
//		flyBlocks = ( HashMap<Integer, ArrayList<Double>> ) data.get( "flyblocks" );
		flyBlocks = blockIDMapListFromObject(data.get( "flyblocks" ));
		if(data.containsKey("canCruise")) {
			canCruise=(Boolean) data.get("canCruise");
		} else {
			canCruise=false;
		}
		if(data.containsKey("canTeleport")) {
			canTeleport=(Boolean) data.get("canTeleport");
		} else {
			canTeleport=false;
		}
		if(data.containsKey("cruiseOnPilot")) {
			cruiseOnPilot=(Boolean) data.get("cruiseOnPilot");
		} else {
			cruiseOnPilot=false;
		}
		if(data.containsKey("allowVerticalMovement")) {
			allowVerticalMovement=(Boolean) data.get("allowVerticalMovement");
		} else {
			allowVerticalMovement=true;
		}
		if(data.containsKey("allowHorizontalMovement")) {
			allowHorizontalMovement=(Boolean) data.get("allowHorizontalMovement");
		} else {
			allowHorizontalMovement=true;
		}
		if(data.containsKey("canStaticMove")) {
			canStaticMove=(Boolean) data.get("canStaticMove");
		} else {
			canStaticMove=false;
		}
		if(data.containsKey("maxStaticMove")) {
			maxStaticMove=integerFromObject(data.get("maxStaticMove"));
		} else {
			maxStaticMove=10000;
		}
		if(data.containsKey("cruiseSkipBlocks")) {
			cruiseSkipBlocks=integerFromObject(data.get("cruiseSkipBlocks"));
		} else {
			cruiseSkipBlocks=0;
		}
		if(data.containsKey("staticWaterLevel")) {
			staticWaterLevel=integerFromObject(data.get("staticWaterLevel"));
		} else {
			staticWaterLevel=0;
		}
		if(data.containsKey("fuelBurnRate")) {
			fuelBurnRate=doubleFromObject(data.get("fuelBurnRate"));
		} else {
			fuelBurnRate=0.0;
		}
		if(data.containsKey("sinkPercent")) {
			sinkPercent=doubleFromObject(data.get("sinkPercent"));
		} else {
			sinkPercent=0.0;
		}
		if(data.containsKey("overallSinkPercent")) {
			overallSinkPercent=doubleFromObject(data.get("overallSinkPercent"));
		} else {
			overallSinkPercent=0.0;
		}
		if(data.containsKey("sinkSpeed")) {
			sinkRateTicks=(int) Math.ceil( 20 / ( doubleFromObject(data.get( "sinkSpeed" )) ) );
		} else {
			sinkRateTicks=(int)Settings.SinkRateTicks;
		}
        if(data.containsKey("keepMovingOnSink")) {
        	keepMovingOnSink=(Boolean) data.get("keepMovingOnSink");
        } else {
        	keepMovingOnSink=false;
     	}
        if (data.containsKey("smokeOnSink")){
        	smokeOnSink = integerFromObject(data.get( "smokeOnSink" ));
        }else{
        	smokeOnSink=0; 
        }
		if(data.containsKey("explodeOnCrash")) {
			double temp=doubleFromObject(data.get("explodeOnCrash"));
			explodeOnCrash=(float) temp;
		} else {
			explodeOnCrash=0.0F;
		}
		if(data.containsKey("collisionExplosion")) {
			double temp=doubleFromObject(data.get("collisionExplosion"));
			collisionExplosion=(float) temp;
		} else {
			collisionExplosion=0.0F;
		}
        if (data.containsKey("minHeightLimit")){
            minHeightLimit = integerFromObject(data.get( "minHeightLimit" ));
            if (minHeightLimit<0){minHeightLimit=0;}
        }else{
            minHeightLimit=0;
        }
        if (data.containsKey("maxHeightLimit")){
            maxHeightLimit = integerFromObject(data.get( "maxHeightLimit" ));
            if (maxHeightLimit<=minHeightLimit){maxHeightLimit=255;} 
        }else{
            maxHeightLimit=254; 
        }
        if(data.containsKey("canDirectControl")) {
        	canDirectControl=(Boolean) data.get("canDirectControl");
        } else {
        	canDirectControl=true;
     	}
        if(data.containsKey("canHover")) {
        	canHover=(Boolean) data.get("canHover");
        } else {
        	canHover=false;
     	}
        if (data.containsKey("canHoverOverWater")){
        	canHoverOverWater=(Boolean) data.get("canHoverOverWater");
        } else {
        	canHoverOverWater=true;
        }
        if (data.containsKey("moveEntities")){
        	moveEntities=(Boolean) data.get("moveEntities");
        }else{
        	moveEntities=true;
        }
    	if(data.containsKey("useGravity")) {
    		useGravity=(Boolean) data.get("useGravity");
     	} else {
     		useGravity=false;
    	}
        	         
    	if (data.containsKey("hoverLimit")){
        	hoverLimit = integerFromObject(data.get( "hoverLimit" ));
        	if (hoverLimit<0){
        		hoverLimit=0;
        	}
    	}else{
        	hoverLimit=0;
     	}
    	harvestBlocks = new ArrayList<Material>(); 
    	if (data.containsKey("harvestBlocks")){
/*        	String[] temp = ((ArrayList<String> ) data.get( "harvestBlocks" )).toArray( new String[1] );
        	for (int i = 0; i < temp.length; i++){
        		Material mat = Material.getMaterial(temp[i]);
        		if (mat != null ){
        			harvestBlocks.add(mat);
        		}*/
    		ArrayList objList=(ArrayList) data.get( "harvestBlocks" );
    		for(Object i : objList) {
    			if(i instanceof String) {
    				Material mat = Material.getMaterial((String)i);
	    			harvestBlocks.add(mat);
    			} else {
    				Integer typeID=(Integer)i;
    				Material mat = Material.getMaterial((Integer)i);
	    			harvestBlocks.add(mat);
    			}
    		}

    	}
	}

	public String getCraftName() {
		return craftName;
	}

	public int getMaxSize() {
		return maxSize;
	}
        
	public int getMinSize() {
		return minSize;
	}
        
	public Integer[] getAllowedBlocks() {
		return allowedBlocks;
	}

	public Integer[] getForbiddenBlocks() {
		return forbiddenBlocks;
	}

	public boolean blockedByWater() {
		return blockedByWater;
	}

	public boolean getCanCruise() {
		return canCruise;
	}
	
	public int getCruiseSkipBlocks() {
		return cruiseSkipBlocks;
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
	
	public boolean getCanStaticMove() {
		return canStaticMove;
	}
	
	public boolean getCruiseOnPilot() {
		return cruiseOnPilot;
	}
	
	public boolean allowVerticalMovement() {
		return allowVerticalMovement;
	}
	
	public boolean allowHorizontalMovement() {
		return allowHorizontalMovement;
	}
	
	public double getFuelBurnRate() {
		return fuelBurnRate;
	}
	
	public double getSinkPercent() {
		return sinkPercent;
	}
	
	public double getOverallSinkPercent() {
		return overallSinkPercent;
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
	
	public int getTickCooldown() {
		return tickCooldown;
	}

	public boolean isTryNudge() {
		return tryNudge;
	}

	public HashMap<ArrayList<Integer>, ArrayList<Double>> getFlyBlocks() {
		return flyBlocks;
	}
	
    public int getMaxHeightLimit(){
        return maxHeightLimit;
    }
    public int getMinHeightLimit(){
            return minHeightLimit;
    }
    public boolean getCanHover(){
    	return canHover;
    	}
    	   
    public boolean getCanDirectControl(){
    	return canDirectControl;
    	}
    	   
  	public int getHoverLimit(){
    	return hoverLimit;
  	}
    	  
	public List<Material> getHarvestBlocks() {
    	return harvestBlocks;
   	}
    	   
    public boolean getCanHoverOverWater(){
    	return canHoverOverWater;
    }
    	     
 	public boolean getMoveEntities(){
    	return moveEntities;
    }
    	     
   	public boolean getUseGravity(){
    	return useGravity;
  	}
}
