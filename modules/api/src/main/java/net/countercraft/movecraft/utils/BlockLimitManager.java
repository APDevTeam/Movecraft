package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.*;

public class BlockLimitManager {
    private final Set<Entry> entries = new HashSet<>();
    public BlockLimitManager(){}

    public BlockLimitManager(Object obj){
        HashMap objMap = (HashMap) obj;
        for (Object i : objMap.keySet()) {
            Set<MovecraftBlock> blocks = new HashSet<>();
            if (i instanceof ArrayList<?>) {
                for (Object o : (ArrayList) i) {
                    Material type;
                    if (o instanceof Integer) {
                        try {
                            Integer typeID = (Integer) o;
                            type = Material.getMaterial(typeID);
                            blocks.add(new MovecraftBlock(type));
                        } catch (Throwable t){
                            throw new IllegalArgumentException("Numerical block IDs are not supported by this version: " + Bukkit.getVersion(), t);
                        }

                    } else if (o instanceof String) {
                        String string = (String) o;
                        string = string.toUpperCase();
                        if (string.contains(":")) {
                            String[] parts = string.split(":");
                            Byte data = Byte.valueOf(parts[1]);
                            try {
                                int id = Integer.parseInt(parts[0]);
                                type = Material.getMaterial(id);
                            } catch (Throwable t){
                                if (!(t instanceof NumberFormatException))
                                    throw new IllegalArgumentException("Numerical block IDs are not supported by this version: " + Bukkit.getVersion(), t);
                                type = Material.getMaterial(parts[0]);
                            }
                            blocks.add(new MovecraftBlock(type, data));
                        } else if (string.toUpperCase().startsWith("ALL_")){
                            string = string.replace("ALL_", "");
                            for (Material m : Material.values()){
                                if (!m.name().endsWith(string)){
                                    continue;
                                }
                                blocks.add(new MovecraftBlock(m));
                            }
                        } else {
                            try {
                                int id = Integer.parseInt(string);
                                type = Material.getMaterial(id);
                            } catch (Throwable t){
                                if (!(t instanceof NumberFormatException)){
                                    throw new IllegalArgumentException("Numerical block IDs are not supported by this version: " + Bukkit.getVersion(), t);
                                }
                                type = Material.getMaterial(string);
                            }
                            blocks.add(new MovecraftBlock(type));
                        }
                    } else {
                        type = (Material) o;
                        blocks.add(new MovecraftBlock(type));
                    }
                }
            } else if (i instanceof Map) {
                Material type;
                HashMap<Object, Object> objMapKey = (HashMap<Object, Object>) i;
                for (Object o : objMapKey.keySet()) {
                    List<Byte> dataList = (List<Byte>) objMapKey.get(o);
                    if (o instanceof Integer) {

                        try {
                            Integer typeID = (Integer) o;
                            type = Material.getMaterial(typeID);
                        } catch (Throwable t){
                            throw new IllegalArgumentException("Numerical block IDs are not supported by this version: " + Bukkit.getVersion(), t);
                        }
                    } else if (o instanceof String) {
                        String string = (String) o;
                        string = string.toUpperCase();
                        if (string.contains(":")) {
                            String[] parts = string.split(":");
                            Byte data = Byte.valueOf(parts[1]);
                            try {
                                int id = Integer.parseInt(parts[0]);
                                type = Material.getMaterial(id);
                            } catch (Throwable t){
                                if (!(t instanceof NumberFormatException))
                                    throw new IllegalArgumentException("Numerical block IDs are not supported by this version: " + Bukkit.getVersion(), t);
                                type = Material.getMaterial(parts[0]);
                            }
                            if (dataList == null)
                                dataList = new ArrayList<>();
                            dataList.add(data);
                        } else if (string.toUpperCase().startsWith("ALL_")){
                            string = string.replace("ALL_", "");
                            for (Material m : Material.values()){
                                if (!m.name().endsWith(string)){
                                    continue;
                                }
                                blocks.add(new MovecraftBlock(m));
                            }
                            continue;
                        } else {
                            try {
                                int id = Integer.parseInt(string);
                                type = Material.getMaterial(id);
                            } catch (Throwable t){
                                if (!(t instanceof NumberFormatException)){
                                    throw new IllegalArgumentException("Numerical block IDs are not supported by this version: " + Bukkit.getVersion(), t);
                                }
                                type = Material.getMaterial(string);
                            }
                        }

                    } else {
                        type = (Material) o;
                    }
                    if (dataList == null || dataList.isEmpty()){
                        blocks.add(new MovecraftBlock(type));
                    } else {
                        for (int data : dataList){
                            blocks.add(new MovecraftBlock(type, (byte) data));
                        }
                    }
                }
            } else if (i instanceof Integer) {
                try {
                    Integer typeID = (Integer) i;
                    Material type = Material.getMaterial(typeID);
                    blocks.add(new MovecraftBlock(type));
                } catch (Throwable t){
                    throw new IllegalArgumentException("Numerical block IDs are not supported by this version: " + Bukkit.getVersion(), t);
                }
            } else if (i instanceof String) {
                String string = (String) i;
                Material type;
                string = string.toUpperCase();
                if (string.contains(":")) {
                    String[] parts = string.split(":");
                    Byte data = Byte.valueOf(parts[1]);
                    try {
                        int id = Integer.parseInt(parts[0]);
                        type = Material.getMaterial(id);
                    } catch (Throwable t){
                        if (!(t instanceof NumberFormatException))
                            throw new IllegalArgumentException("Numerical block IDs are not supported by this version: " + Bukkit.getVersion(), t);
                        type = Material.getMaterial(parts[0]);
                    }
                    blocks.add(new MovecraftBlock(type, data));
                } else if (string.toUpperCase().startsWith("ALL_")){
                    string = string.replace("ALL_", "");
                    for (Material m : Material.values()){
                        if (!m.name().endsWith(string)){
                            continue;
                        }
                        blocks.add(new MovecraftBlock(m));
                    }
                } else {
                    try {
                        int id = Integer.parseInt(string);
                        type = Material.getMaterial(id);
                    } catch (Throwable t){
                        if (!(t instanceof NumberFormatException)){
                            throw new IllegalArgumentException("Numerical block IDs are not supported by this version: " + Bukkit.getVersion(), t);
                        }
                        type = Material.getMaterial(string);
                    }
                    blocks.add(new MovecraftBlock(type));
                }
            } else {
                Material type = (Material) i;
                blocks.add(new MovecraftBlock(type));
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
                } else {
                    limitList.add((Double) limitObj);
                }
            }
            entries.add(new Entry(blocks, limitList.get(0), limitList.get(1)));
        }
    }

    public Set<Entry> getEntries() {
        return entries;
    }

    public boolean contains(Material type){
        MovecraftBlock block = new MovecraftBlock(type);
        for (Entry e : getEntries()){
            if (!e.getBlocks().contains(block)){
                continue;
            }
            return true;
        }
        return false;
    }

    public boolean contains(Material type, byte data){
        MovecraftBlock block = new MovecraftBlock(type, data);
        for (Entry e : getEntries()){
            if (!e.getBlocks().contains(block)){
                continue;
            }
            return true;
        }
        return false;
    }

    public boolean hasMetaData(Material type){
        for (Entry e : getEntries()){
            for (MovecraftBlock block : e.getBlocks()){
                if (block.getType() != type){
                    continue;
                }
                return block.getData() != null;
            }
        }
        return false;
    }

    public double getLowerLimit(Material type){
        Entry entry = get(type);
        return entry != null && entry.getBlocks().contains(new MovecraftBlock(type)) ? entry.getLowerLimit() : 0.0;
    }

    public double getLowerLimit(Material type, byte data) {
        Entry entry = get(type);
        return entry != null && entry.getBlocks().contains(new MovecraftBlock(type, data)) ? entry.getLowerLimit() : 0.0;
    }

    public double getUpperLimit(Material type){
        Entry entry = get(type);
        return entry != null && entry.getBlocks().contains(new MovecraftBlock(type)) ? entry.getUpperLimit() : 0.0;
    }

    public double getUpperLimit(Material type, byte data){
        Entry entry = get(type);
        return entry != null && entry.getBlocks().contains(new MovecraftBlock(type, data)) ? entry.getUpperLimit() : 0.0;
    }
    public Entry get(Material type, byte data){
        Entry entry = get(type);
        for (MovecraftBlock b : entry.getBlocks()){
            if (b.getData() != data)
                continue;
            return entry;
        }
        return null;
    }

    public Entry get(Material type){
        for (Entry e : entries){
            for (MovecraftBlock block : e.getBlocks()){
                if (block.getType() != type){
                    continue;
                }
                return e;
            }
        }
        return null;
    }
    public static class Entry {
        private final Set<MovecraftBlock> blocks;
        private final double lowerLimit;
        private final double upperLimit;

        Entry(Set<MovecraftBlock> blocks, double lowerLimit, double upperLimit) {
            this.lowerLimit = lowerLimit;
            this.upperLimit = upperLimit;
            this.blocks = blocks;
        }

        public double getLowerLimit() {
            return lowerLimit;
        }

        public double getUpperLimit() {
            return upperLimit;
        }

        public Set<MovecraftBlock> getBlocks() {
            return blocks;
        }
    }
}
