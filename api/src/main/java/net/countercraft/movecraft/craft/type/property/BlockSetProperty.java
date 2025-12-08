package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.util.Tags;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.function.Supplier;

public class BlockSetProperty extends HashSet<NamespacedKey> implements Supplier<EnumSet<Material>> {

    public BlockSetProperty(EnumSet<Material> materialEnumSet) {
        this();
        this.addEnumSet(materialEnumSet);
    }

    public BlockSetProperty() {
        super();
    }

    public BlockSetProperty(Collection<NamespacedKey> values) {
        super(values);
    }

    public BlockSetProperty(BlockSetProperty copyFrom) {
        super(copyFrom);
    }

    @Override
    public EnumSet<Material> get() {
        EnumSet result = EnumSet.noneOf(Material.class);
        for (NamespacedKey key : this) {
            String materialName = key.asString();
            EnumSet<Material> materials = Tags.parseMaterials(materialName);
            result.addAll(materials);
        }
        return result;
    }

    public void addEnumSet(EnumSet<Material> materialEnumSet) {
        for (Material material : materialEnumSet) {
            this.add(material.getKey());
        }
    }

    public void addTag(Tag<Material> tag) {
        for (Material material : tag.getValues()) {
            this.add(material.getKey());
        }
    }
}
