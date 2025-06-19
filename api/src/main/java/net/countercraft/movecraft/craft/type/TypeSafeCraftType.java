package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.registration.SimpleRegistry;
import net.countercraft.movecraft.util.registration.TypedContainer;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TypeSafeCraftType extends TypedContainer<PropertyKey<?>> {

    static final SimpleRegistry<NamespacedKey, PropertyKey> PROPERTY_REGISTRY = new SimpleRegistry();

    @NotNull
    public static TypeSafeCraftType load(@NotNull File file) {
        final InputStream input;
        try {
            input = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return new TypeSafeCraftType();
        }
        try(input) {
            Yaml yaml = new Yaml();
            return buildType(yaml.load(input));
        }
        catch (IOException e) {
            e.printStackTrace();
            return new TypeSafeCraftType();
        }
    }

    private TypeSafeCraftType() {
        super();
    }

    private static TypeSafeCraftType buildType(Map<String, Object> yamlMapping) {
        TypeSafeCraftType result = new TypeSafeCraftType();

        // Structure: Simple map per namespace inside the file
        // Helps to differentiate things

        Map<String, Map<String, Object>> namespaces = new HashMap<>();
        // Step 1: Set all default values
        for (Map.Entry<NamespacedKey, PropertyKey> entry : PROPERTY_REGISTRY.entries()) {
            namespaces.computeIfAbsent(entry.getKey().getNamespace(), k -> new HashMap<>()).putIfAbsent(entry.getKey().getKey(), entry.getValue().getDefault(result));
        }
        // Step 2: Load the values from the file
        for (Map.Entry<String, Object> entry : yamlMapping.entrySet()) {
            String namespace = "movecraft";
            if (entry.getValue() instanceof Map) {
                namespace = entry.getKey();
                try {
                    namespaces.put(namespace, (Map<String, Object>) entry.getValue());
                } catch(ClassCastException cce) {
                    // TODO: log error
                }
            } else {
                namespaces.putIfAbsent(namespace, yamlMapping);
            }
        }
        // Step 3: Read from the parsed namespaces and apply it
        for (Map.Entry<String, Map<String, Object>> entry : namespaces.entrySet()) {
            readNamespace(entry.getKey(), entry.getValue(), result);
        }

        return result;
    }

    private static <T> void readNamespace(final String namespace, final Map<String, Object> data, final TypeSafeCraftType type) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            NamespacedKey key = NamespacedKey.fromString(entry.getKey());
            PropertyKey<T> propertyKey = PROPERTY_REGISTRY.get(key);
            if (propertyKey == null) {
                // TODO: Log => unknown property!
                continue;
            }

            try {
                T value = propertyKey.read(entry.getValue(), type);
                if (value == null) {
                    continue;
                }
                type.set(propertyKey, value);
            } catch(ClassCastException cce) {
                // TODO: Log error
            }
        }
    }

    @Override
    protected <T> void set(@NotNull TypedKey<T> tagKey, @NotNull T value) {
        super.set(tagKey, value);
    }

    public CraftProperties createCraftProperties() {
        // TODO: Implement
        return null;
    }

}
