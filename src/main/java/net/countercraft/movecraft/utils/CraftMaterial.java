package net.countercraft.movecraft.utils;

import java.util.Objects;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;


//<editor-fold defaultstate="collapsed" desc="//Class for comparing blocks of a craft">
/**************************************
 * 
 * Class for comparing blocks of a craft 
 * 
 **************************************/
//</editor-fold>
public class CraftMaterial{
    private Material material = null;
    private MaterialData data = null;
    private double min = -1;
    private double max = -1;
    private double num = -1;
    
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (Material type, MaterialData data [,[double min, double max] | [double num]])">
    /***************************************************************************
     * 
     *  MaterialData Inits
     * 
     **************************************************************************/
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (Material type, MaterialData data)">
    /**
     * init from Material without limits
     * 
     * @param type - Material
     * @param data - MaterialData
     */
    //</editor-fold>
    public CraftMaterial (Material type, MaterialData data){
        initMaterialData(type, data, -1, -1, -1);
    }
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (Material type, MaterialData data, double min, double max)">
    /**
     * init from Material with limits
     * 
     * @param type - Material
     * @param data - MaterialData
     * @param min - min. limit
     * @param max - max. limit
     */
    //</editor-fold>
    public CraftMaterial (Material type, MaterialData data, double min, double max){
        initMaterialData(type, data, min, max, -1);
    }
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (Material type, MaterialData data, double num)">
    /**
     * init from Material with specific limit
     * 
     * @param type - Material
     * @param data - MaterialData
     * @param num - specific limit
     */
    //</editor-fold>
    public CraftMaterial (Material type, MaterialData data, double num){
        initMaterialData(type, data, -1, -1, num);
    }
    //</editor-fold> 
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (int iType, int iData [,[double min, double max] | [double num]])">
    /***************************************************************************
     * 
     *  Integer Inits
     * 
     **************************************************************************/
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (int iType, int iData)">
    /**
     * init from Integers without limits
     * 
     * @param iType - Block ID
     * @param iData - Block Data
     */
    //</editor-fold>
    public CraftMaterial(int iType, int iData){
        initInt(iType, iData, -1, -1, -1);
    }
    
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (int iType, int iData, double min, double max)">
    /**
     * init from Integers with limits
     * 
     * @param iType - Block ID
     * @param iData - Block Data
     * @param min - min. limit
     * @param max - max. limit
     */
    //</editor-fold>
    public CraftMaterial(int iType, int iData, double min, double max){
        initInt(iType, iData, min, max, -1);
    }
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (int iType, int iData, double num)">
    /**
     * init from Integers with specific limit
     * 
     * @param iType - Block ID
     * @param iData - Block Data
     * @param num - specific limit
     */
    //</editor-fold>
    public CraftMaterial(int iType, int iData, double num){
        initInt(iType, iData, -1, -1, num);
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (String sType, String sData [,[double min, double max] | [double num]])">
    /***************************************************************************
     * 
     *  String Inits
     * 
     **************************************************************************/
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (String sType, String sData)">
    /**
     * init from Strings without limits
     * 
     * @param sType - Block Name/String ID
     * @param sData - Block Data/color (f.e: BLACK)
     */
    //</editor-fold>
    public CraftMaterial(String sType, String sData){
        initString(sType, sData, -1, -1, -1);
    }
    
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (String sType, String sData, double min, double max)">
    /**
     * init from Strings with limits
     * 
     * @param sType - Block Name/String ID
     * @param sData - Block Data/color (f.e: BLACK)
     * @param min - min. limit
     * @param max - max. limit
     */
    //</editor-fold>
    public CraftMaterial(String sType, String sData, double min, double max){
        initString(sType, sData, min, max, -1);
    }
    
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (String sType, String sData, double num)">
    /**
     * 
     * @param sType - Block Name/String ID
     * @param sData - Block Data/color (f.e: BLACK)
     * @param num - specific limit
     */
    //</editor-fold>
    public CraftMaterial(String sType, String sData, double num){
        initString(sType, sData, -1, -1, num);
    }
    
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (Block block [,[double min, double max] | [double num]])">
    /***************************************************************************
     * 
     *  Block Inits
     * 
     **************************************************************************/
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (Block block)">
    /**
     * init from Block without limits
     * 
     * @param block - Block Name/String ID
     */
    //</editor-fold>
    public CraftMaterial(Block block){
        initBlock(block, -1, -1, -1);
    }
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (Block block, double min, double max)">
    /**
     * init from Block with limits
     * 
     * @param block - Block Name/String ID
     * @param min - min. limit
     * @param max - max. limit 
     */
    //</editor-fold>
    public CraftMaterial(Block block, double min, double max){
        initBlock(block, min, max, -1);
    }
    
    //<editor-fold defaultstate="collapsed" desc="CraftMaterial (Block block, double num)">
    /**
     * init from Block with limits
     * 
     * @param block - Block Name/String ID
     * @param num - specific limit
     */
    //</editor-fold>
    public CraftMaterial(Block block, double num){
        initBlock(block, -1, -1, num);
    }
    //</editor-fold>
    
    
    private void initObject(Object oType, Object oData, double min, double max, double num){
        Material mat = null;
        if (oType instanceof String){
            mat = Material.getMaterial((String) oType);
        }else 
        if (oType instanceof Integer){
            mat = Material.getMaterial((Integer) oType);
        }else 
        if (oType instanceof Material){
            mat = (Material) oType;
        }
        
        MaterialData dat = null;
        
        DyeColor c = DyeColorUtils.getWoolColor(oData);
        if (c != null){
            dat = new Wool(c);
        }
        this.material = mat;
        this.data = dat;
        this.min = min;
        this.max = max;
        this.num = num;
    }
    
    private void initBlock(Block block, double min, double max, double num){
        Material mat = block.getType();
        BlockState s = block.getState();
        MaterialData dat = s.getData();
        initMaterialData(mat, dat, min, max, num);
    }
    
    private void initInt(int iType, int iData, double min, double max, double num){
        Material type = Material.getMaterial(iType);
        DyeColor c = DyeColorUtils.IntToWoolColor(iData);
        MaterialData dat = new Wool(c);
        this.material = type;
        this.data = dat;
        this.min = min;
        this.max = max;
        this.num = num;
    }
    
    private void initMaterialData(Material material, MaterialData data, double min, double max, double num){
        this.material = material;
        this.data = data;
        this.min = min;
        this.max = max;
        this.num = num;
    }
    
    private void initString(String sType, String sData, double min, double max, double num){
        Material type = Material.getMaterial(sType);
        MaterialData tmpData = null;
        DyeColor color;
        if (
                type.equals(Material.WOOL)
            || 
                type.equals(Material.STAINED_CLAY)
            || 
                type.equals(Material.STAINED_GLASS)
            || 
                type.equals(Material.STAINED_GLASS_PANE)
            || 
                type.equals(Material.CARPET)
            ){
            color = DyeColor.valueOf(sData);
            tmpData = new Wool(color);
        }
        
                
        this.material = type;
        this.data = tmpData;
        this.min = min;
        this.max = max;
        this.num = num;
    }
    
    public boolean isColorable(){
        return data instanceof Colorable;
    }
    
    public boolean isDirectional(){
        return data instanceof Directional;
    }
    
    public final boolean blockIsColorable(Block block){
        if (block != null){
            BlockState s = block.getState();
            if (s.getData() instanceof Colorable){
                return true;
            }
        }
        return false;
    }
    
    public final boolean blockIsDirectional(Block block){
        if (block != null){
            BlockState s = block.getState();
            if (s.getData() instanceof Directional){
                return true;
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        DyeColor c = ((Colorable)data).getColor();
        return Objects.hashCode(material.toString()+ ":" +c.toString());
    }

    
    /**
     * Compares main data from this class with Block/Material/ItemStack
     * 
     * @param o
     * @return 
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof Block) {
            Block block = (Block) o;
            Material mat = block.getType();
            BlockState s = block.getState();
            MaterialData dat = s.getData();
            return material.equals(mat)
                    && data.equals(dat);
        }
        if (o instanceof Material) {
            Material mat = (Material) o;
            return material.equals(mat);
        }
        if (o instanceof ItemStack) {
            ItemStack stack = (ItemStack) o;
            Material mat = stack.getType();
            MaterialData dat = stack.getData();
            return material.equals(mat)
                    && data.equals(dat);
        }
        return false;
    }
    
    
    public Material getMaterial(){
        return this.material;
    }
    
    public MaterialData getData(){
        return this.data;
    }
    
    public double getMinPercent(){
        return this.min;
    }
    
    public double getMaxPercent(){
        return this.max;
    }
     
    public double getNumericalLimit(){
        return this.num;
    }

    
    
    
    
   
}
