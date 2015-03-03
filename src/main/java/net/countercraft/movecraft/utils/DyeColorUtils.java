/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.countercraft.movecraft.utils;

import org.bukkit.DyeColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.Colorable;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;

/**
 *
 * @author mwkaicz <mwkaicz@gmail.com>
 */
public class DyeColorUtils {
    
    public static DyeColor getWoolColor(Object o){
        if (o instanceof String){
            String s = (String) o;
            return StringToWoolColor(s);
        }else if (o instanceof Integer){
            int i;
            i = (Integer) o;
            return IntToWoolColor(i);
        }else if(o instanceof Colorable){
            Wool w = (Wool) o;
            return w.getColor();
        }
        return null;
    }
    
    public static DyeColor IntToWoolColor(int i){
        switch (i){
            case 0:
                return DyeColor.WHITE;
            case 1:
                return DyeColor.ORANGE;
            case 2:
                return DyeColor.MAGENTA;
            case 3:
                return DyeColor.LIGHT_BLUE;
            case 4:
                return DyeColor.YELLOW;
            case 5:
                return DyeColor.LIME;
            case 6:
                return DyeColor.PINK;
            case 7:
                return DyeColor.GRAY;
            case 8:
                return DyeColor.SILVER;
            case 9:
                return DyeColor.CYAN;
            case 10:
                return DyeColor.PURPLE;
            case 11:
                return DyeColor.BLUE;
            case 12:
                return DyeColor.BROWN;
            case 13:
                return DyeColor.GREEN;
            case 14:
                return DyeColor.RED;
            case 15:
                return DyeColor.BLACK;
            default: return null;
        }
    }
    
    
    public static DyeColor HexToWoolColor(String str){
        return StringToWoolColor(str);
    }
    
    public static DyeColor StringToWoolColor(String str){
        if (str.equals("0") || str.equalsIgnoreCase("white")){
            return DyeColor.WHITE;
        }else if (str.equals("1") || str.equalsIgnoreCase("orange")){
            return DyeColor.ORANGE;
        }else if (str.equals("2") || str.equalsIgnoreCase("magenta")){
            return DyeColor.MAGENTA;
        }else if (str.equals("3") || str.equalsIgnoreCase("light_blue")){
            return DyeColor.LIGHT_BLUE;
        }else if (str.equals("4") || str.equalsIgnoreCase("yellow")){
            return DyeColor.YELLOW;
        }else if (str.equals("5") || str.equalsIgnoreCase("lime")){
            return DyeColor.LIME;
        }else if (str.equals("6") || str.equalsIgnoreCase("pink")){
            return DyeColor.PINK;
        }else if (str.equals("7") || str.equalsIgnoreCase("gray")){
            return DyeColor.GRAY;
        }else if (str.equals("8") || str.equalsIgnoreCase("silver")){
            return DyeColor.SILVER;
        }else if (str.equals("9") || str.equalsIgnoreCase("cyan")){
            return DyeColor.CYAN;
        }else if (str.equalsIgnoreCase("a") || str.equalsIgnoreCase("purple") || str.equalsIgnoreCase("10")){
            return DyeColor.PURPLE;
        }else if (str.equalsIgnoreCase("b") || str.equalsIgnoreCase("blue") || str.equalsIgnoreCase("11")){
            return DyeColor.BLUE;
        }else if (str.equalsIgnoreCase("c") || str.equalsIgnoreCase("brown") || str.equalsIgnoreCase("12")){
            return DyeColor.BROWN;
        }else if (str.equalsIgnoreCase("d") || str.equalsIgnoreCase("green") || str.equalsIgnoreCase("13")){
            return DyeColor.GREEN;
        }else if (str.equalsIgnoreCase("e") || str.equalsIgnoreCase("red") || str.equalsIgnoreCase("14")){
            return DyeColor.RED;
        }else if (str.equalsIgnoreCase("f") || str.equalsIgnoreCase("black") || str.equalsIgnoreCase("15")){
            return DyeColor.BLACK;
        }else{
            return null;
        }
    }
    
    public static Byte WoolColorToByte(DyeColor color){
        if (color.equals(DyeColor.WHITE)){
            return 0x0;
        }else if (color.equals(DyeColor.ORANGE)){
            return 0x1;
        }else if (color.equals(DyeColor.MAGENTA)){
            return 0x2;
        }else if (color.equals(DyeColor.LIGHT_BLUE)){
            return 0x3;
        }else if (color.equals(DyeColor.YELLOW)){
            return 0x4;
        }else if (color.equals(DyeColor.LIME)){
            return 0x5;
        }else if (color.equals(DyeColor.PINK)){
            return 0x6;
        }else if (color.equals(DyeColor.GRAY)){
            return 0x7;
        }else if (color.equals(DyeColor.SILVER)){
            return 0x8;
        }else if (color.equals(DyeColor.CYAN)){
            return 0x9;
        }else if (color.equals(DyeColor.PURPLE)){
            return 0xA;
        }else if (color.equals(DyeColor.BLUE)){
            return 0xB;
        }else if (color.equals(DyeColor.BROWN)){
            return 0xC;
        }else if (color.equals(DyeColor.GREEN)){
            return 0xD;
        }else if (color.equals(DyeColor.RED)){
            return 0xE;
        }else if (color.equals(DyeColor.BLACK)){
            return 0xF;
        }
        return null;
    }
    
    
    public static int WoolColorToDec(DyeColor color){
        if (color.equals(DyeColor.WHITE)){
            return 0;
        }else if (color.equals(DyeColor.ORANGE)){
            return 1;
        }else if (color.equals(DyeColor.MAGENTA)){
            return 2;
        }else if (color.equals(DyeColor.LIGHT_BLUE)){
            return 3;
        }else if (color.equals(DyeColor.YELLOW)){
            return 4;
        }else if (color.equals(DyeColor.LIME)){
            return 5;
        }else if (color.equals(DyeColor.PINK)){
            return 6;
        }else if (color.equals(DyeColor.GRAY)){
            return 7;
        }else if (color.equals(DyeColor.SILVER)){
            return 8;
        }else if (color.equals(DyeColor.CYAN)){
            return 9;
        }else if (color.equals(DyeColor.PURPLE)){
            return 10;
        }else if (color.equals(DyeColor.BLUE)){
            return 11;
        }else if (color.equals(DyeColor.BROWN)){
            return 12;
        }else if (color.equals(DyeColor.GREEN)){
            return 13;
        }else if (color.equals(DyeColor.RED)){
            return 14;
        }else if (color.equals(DyeColor.BLACK)){
            return 15;
        }
        return -1;
    }
    
    public static String WoolColorToHex(DyeColor color){
        if (color.equals(DyeColor.WHITE)){
            return "0";
        }else if (color.equals(DyeColor.ORANGE)){
            return "1";
        }else if (color.equals(DyeColor.MAGENTA)){
            return "2";
        }else if (color.equals(DyeColor.LIGHT_BLUE)){
            return "3";
        }else if (color.equals(DyeColor.YELLOW)){
            return "4";
        }else if (color.equals(DyeColor.LIME)){
            return "5";
        }else if (color.equals(DyeColor.PINK)){
            return "6";
        }else if (color.equals(DyeColor.GRAY)){
            return "7";
        }else if (color.equals(DyeColor.SILVER)){
            return "8";
        }else if (color.equals(DyeColor.CYAN)){
            return "9";
        }else if (color.equals(DyeColor.PURPLE)){
            return "A";
        }else if (color.equals(DyeColor.BLUE)){
            return "B";
        }else if (color.equals(DyeColor.BROWN)){
            return "C";
        }else if (color.equals(DyeColor.GREEN)){
            return "D";
        }else if (color.equals(DyeColor.RED)){
            return "E";
        }else if (color.equals(DyeColor.BLACK)){
            return "F";
        }
        return null;
    }
    
    public static Byte DyeColorToByte(DyeColor color){
        if (color.equals(DyeColor.WHITE)){
            return 0xF;
        }else if (color.equals(DyeColor.ORANGE)){
            return 0xE;
        }else if (color.equals(DyeColor.MAGENTA)){
            return 0xD;
        }else if (color.equals(DyeColor.LIGHT_BLUE)){
            return 0xC;
        }else if (color.equals(DyeColor.YELLOW)){
            return 0xB;
        }else if (color.equals(DyeColor.LIME)){
            return 0xA;
        }else if (color.equals(DyeColor.PINK)){
            return 0x9;
        }else if (color.equals(DyeColor.GRAY)){
            return 0x8;
        }else if (color.equals(DyeColor.SILVER)){
            return 0x7;
        }else if (color.equals(DyeColor.CYAN)){
            return 0x6;
        }else if (color.equals(DyeColor.PURPLE)){
            return 0x5;
        }else if (color.equals(DyeColor.BLUE)){
            return 0x4;
        }else if (color.equals(DyeColor.BROWN)){
            return 0x3;
        }else if (color.equals(DyeColor.GREEN)){
            return 0x2;
        }else if (color.equals(DyeColor.RED)){
            return 0x1;
        }else if (color.equals(DyeColor.BLACK)){
            return 0x0;
        }
        return null;
    }
    
    
    
    public final DyeColor getColorFromBlock(Block block){
        if (block != null){
            BlockState s = block.getState();
            if (s.getData() instanceof Colorable){
                DyeColor c = ((Colorable)s.getData()).getColor();   
                return c;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }
}
