package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BlockContainer implements Iterable<MovecraftBlock>{
    private final Set<MovecraftBlock> blocks = new HashSet<>();
    private final Map<Material, MovecraftBlock> byMaterial = new HashMap<>();

    /**
     * Constructs an instance of an empty Block container
     */
    public BlockContainer(){

    }

    public BlockContainer(Object obj){
        if (obj instanceof ArrayList){
            ArrayList objList = (ArrayList) obj;
            for (Object o : objList){
                Material type;

                if (o instanceof String){
                    String str = (String) o;
                    str = str.toUpperCase();
                    if (str.contains(":")){
                        String[] parts = str.split(":");
                        try{
                            int id = Integer.parseInt(parts[0]);
                            type = Material.getMaterial(id);
                        } catch (NumberFormatException e){
                            type = Material.getMaterial(parts[0]);
                        } catch (Exception e){
                            throw new IllegalArgumentException("Numerical IDs are not supported by this version: " + Bukkit.getVersion(), e);
                        }
                        Byte data = Byte.parseByte(parts[1]);
                        blocks.add(new MovecraftBlock(type, data));
                        continue;
                    } else if (str.startsWith("ALL_")){
                        str = str.replace("ALL_", "_");
                        for (Material m : Material.values()){
                            if (!m.name().endsWith(str)){
                                continue;
                            }
                            blocks.add(new MovecraftBlock(m));
                        }
                        continue;
                    }
                    type = Material.getMaterial(str);
                    if (type == null){
                        throw new IllegalArgumentException(str + "is not a valid Material ID");
                    }
                    blocks.add(new MovecraftBlock(type));
                } else if (o instanceof Integer){
                    int id = (int) o;
                    try {
                        type = Material.getMaterial(id);
                        blocks.add(new MovecraftBlock(type));
                    } catch (Throwable t){
                        throw new IllegalArgumentException("Numerical material IDs are not supportedby this version: " + Bukkit.getVersion());
                    }
                } else {
                    type = (Material) o;
                    blocks.add(new MovecraftBlock(type));
                }
            }
        } else if (obj instanceof Integer){
            int id = (int) obj;
            try {
                Material type = Material.getMaterial(id);
                blocks.add(new MovecraftBlock(type));
            } catch (Throwable t){
                throw new IllegalArgumentException("Numerical material IDs are not supportedby this version: " + Bukkit.getVersion());
            }
        } else if (obj instanceof String) {
            Material type;
            String str = (String) obj;
            if (str.contains(":")){
                String[] parts = str.split(":");
                try{
                    int id = Integer.parseInt(parts[0]);
                    type = Material.getMaterial(id);
                } catch (NumberFormatException e){
                    type = Material.getMaterial(parts[0]);
                } catch (Exception e){
                    throw new IllegalArgumentException("Numerical IDs are not supported by this version: " + Bukkit.getVersion(), e);
                }
                Byte data = Byte.parseByte(parts[1]);
                blocks.add(new MovecraftBlock(type, data));
                return;
            }
            type = Material.getMaterial(str);
            if (type == null){
                throw new IllegalArgumentException(str + "is not a valid Material ID");
            }
            blocks.add(new MovecraftBlock(type));
        } else {
            blocks.add(new MovecraftBlock((Material) obj));
        }
        for (MovecraftBlock mb : blocks){
            byMaterial.put(mb.getType(), mb);
        }
    }

    public boolean contains(MovecraftBlock movecraftBlock){
        return blocks.contains(movecraftBlock);
    }

    /**
     * Check if this block container contains given material
     * @param type the Material this container should contain
     * @return true if it contains a block with the same type and no meta data. False otherwise
     */
    public boolean contains(Material type){
        return contains(new MovecraftBlock(type));
    }

    /**
     * Check if this block container contains given material and metadata value
     * @param type the Material this container should contain
     * @param data the corresponding metadata value this container should contain
     * @return true if the block container contains both the type and data values. False otherwhise
     */
    public boolean contains(Material type, Byte data){
        return contains(new MovecraftBlock(type, data));
    }

    public boolean isEmpty(){
        return blocks.isEmpty();
    }

    public int size(){
        return blocks.size();
    }

    public MovecraftBlock get(Material type){
        return byMaterial.get(type);
    }

    @NotNull
    @Override
    public Iterator<MovecraftBlock> iterator() {
        return Collections.unmodifiableSet(blocks).iterator();
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks);
    }
}
