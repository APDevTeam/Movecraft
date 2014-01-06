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
	private boolean canFly, tryNudge, canCruise, canTeleport, canStaticMove, canHover, useGravity, canHoverOverWater, moveEntities;;
	private int cruiseSkipBlocks;
	private int staticWaterLevel;
	private double fuelBurnRate;
	private double sinkPercent;
	private float collisionExplosion;
	private int tickCooldown;
	private HashMap<Integer, ArrayList<Double>> flyBlocks = new HashMap<Integer, ArrayList<Double>>();
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

	private void parseCraftDataFromFile( File file ) throws FileNotFoundException {
		InputStream input = new FileInputStream(file);
		Yaml yaml = new Yaml();
		Map data = ( Map ) yaml.load( input );
		craftName = ( String ) data.get( "name" );
		maxSize = ( Integer ) data.get( "maxSize" );
		minSize = ( Integer ) data.get( "minSize" );
		allowedBlocks = ((ArrayList<Integer> ) data.get( "allowedBlocks" )).toArray( new Integer[1] );
		forbiddenBlocks = ((ArrayList<Integer> ) data.get( "forbiddenBlocks" )).toArray( new Integer[1] );
		canFly = ( Boolean ) data.get( "canFly" );
		tryNudge = ( Boolean ) data.get( "tryNudge" );
		tickCooldown = (int) Math.ceil( 20 / ( ( Double ) data.get( "speed" ) ) );
		flyBlocks = ( HashMap<Integer, ArrayList<Double>> ) data.get( "flyblocks" );
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
		if(data.containsKey("canStaticMove")) {
			canStaticMove=(Boolean) data.get("canStaticMove");
		} else {
			canStaticMove=false;
		}
		if(data.containsKey("cruiseSkipBlocks")) {
			cruiseSkipBlocks=(Integer) data.get("cruiseSkipBlocks");
		} else {
			cruiseSkipBlocks=0;
		}
		if(data.containsKey("staticWaterLevel")) {
			staticWaterLevel=(Integer) data.get("staticWaterLevel");
		} else {
			staticWaterLevel=0;
		}
		if(data.containsKey("fuelBurnRate")) {
			fuelBurnRate=(Double) data.get("fuelBurnRate");
		} else {
			fuelBurnRate=0.0;
		}
		if(data.containsKey("sinkPercent")) {
			sinkPercent=(Double) data.get("sinkPercent");
		} else {
			sinkPercent=0.0;
		}
		if(data.containsKey("collisionExplosion")) {
			double temp=(Double) data.get("collisionExplosion");
			collisionExplosion=(float) temp;
		} else {
			collisionExplosion=0.0F;
		}
        if (data.containsKey("minHeightLimit")){
            minHeightLimit = ( Integer ) data.get( "minHeightLimit" );
            if (minHeightLimit<0){minHeightLimit=0;}
        }else{
            minHeightLimit=0;
        }
        if (data.containsKey("maxHeightLimit")){
            maxHeightLimit = ( Integer ) data.get( "maxHeightLimit" );
            if (maxHeightLimit<=minHeightLimit){maxHeightLimit=255;} 
        }else{
            maxHeightLimit=254; 
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
        	hoverLimit = ( Integer ) data.get( "hoverLimit" );
        	if (hoverLimit<0){
        		hoverLimit=0;
        	}
    	}else{
        	hoverLimit=0;
     	}
    	harvestBlocks = new ArrayList<Material>(); 
    	if (data.containsKey("harvestBlocks")){
        	String[] temp = ((ArrayList<String> ) data.get( "harvestBlocks" )).toArray( new String[1] );
        	for (int i = 0; i < temp.length; i++){
        		Material mat = Material.getMaterial(temp[i]);
        		if (mat != null ){
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

	public boolean canFly() {
		return canFly;
	}

	public boolean getCanCruise() {
		return canCruise;
	}
	
	public int getCruiseSkipBlocks() {
		return cruiseSkipBlocks;
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
	
	public double getFuelBurnRate() {
		return fuelBurnRate;
	}
	
	public double getSinkPercent() {
		return sinkPercent;
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

	public HashMap<Integer, ArrayList<Double>> getFlyBlocks() {
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
