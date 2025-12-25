package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.transform.TypeSafeTransform;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.LazyLoadField;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.registration.SimpleRegistry;
import net.countercraft.movecraft.util.registration.TypedContainer;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class TypeSafeCraftType extends TypedContainer<PropertyKey<?>> {

    // Initialization
    static {
        // TODO: Replace with event
        PropertyKeys.registerAll();
        Transformers.registerAll();
        Validators.registerAll();
    }

    public static final SimpleRegistry<NamespacedKey, PropertyKey> PROPERTY_REGISTRY = new SimpleRegistry();
    public static final Set<TypeSafeTransform> TRANSFORM_REGISTRY = new HashSet<>();
    public static final Set<Pair<Predicate<TypeSafeCraftType>, String>> VALIDATOR_REGISTRY = new HashSet<>();

    protected final Function<String, TypeSafeCraftType> typeRetriever;
    private final String name;

    protected String parentName = null;
    protected final LazyLoadField<TypeSafeCraftType> parentInstance = new LazyLoadField<>(this::computeParent);

    protected final TypeSafeCraftType computeParent() {
        if (this.parentName == null || this.parentName.isEmpty()) {
            return null;
        }
        return this.typeRetriever.apply(this.parentName);
    }

    @NotNull
    public static TypeSafeCraftType load(@NotNull File file, String name, Function<String, TypeSafeCraftType> typeRetriever) {
        final InputStream input;
        try {
            input = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return new TypeSafeCraftType(name, typeRetriever);
        }
        try(input) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            return buildType(name, typeRetriever, yaml.getValues(false));
        }
        catch (IOException e) {
            e.printStackTrace();
            return new TypeSafeCraftType(name, typeRetriever);
        }
    }

    public String getName() {
        return this.name;
    }

    protected TypeSafeCraftType(final String name, Function<String, TypeSafeCraftType> typeRetriever) {
        super();
        this.name = name;
        this.typeRetriever = typeRetriever;
    }

    private static TypeSafeCraftType buildType(String name, Function<String, TypeSafeCraftType> typeRetriever, final Map<String, Object> yamlMapping) {
        TypeSafeCraftType result = new TypeSafeCraftType(name, typeRetriever);
        Object parentObj = yamlMapping.getOrDefault("parent", null);
        if (parentObj != null) {
            String parentStr = String.valueOf(parentObj);
            if (parentStr != "null" && !parentStr.isEmpty()) {
                result.parentName = parentStr;
            }
        }

        // Structure: Simple map per namespace inside the file
        // Helps to differentiate things

        // No longer necessary. Through the parentloading it will end up at the property level if not set and retrieve the default from there
//        Set<Map.Entry<NamespacedKey, PropertyKey>> entries = PROPERTY_REGISTRY.entries();
//        Map<String, Map<String, Object>> namespaces = new HashMap<>();
//        // Step 1: Set all default values
//        for (Map.Entry<NamespacedKey, PropertyKey> entry : entries) {
//            namespaces.computeIfAbsent(entry.getKey().getNamespace(), k -> new HashMap<>()).putIfAbsent(entry.getKey().getKey(), entry.getValue().getDefault(result));
//        }
//        // Step 2: Load the values from the file
//        for (Map.Entry<String, Object> entry : yamlMapping.entrySet()) {
//            String namespace = "movecraft";
//            if (entry.getValue() instanceof Map) {
//                namespace = entry.getKey();
//                try {
//                    namespaces.put(namespace, (Map<String, Object>) entry.getValue());
//                } catch(ClassCastException cce) {
//                    // TODO: log error
//                }
//            } else {
//                namespaces.putIfAbsent(namespace, yamlMapping);
//            }
//        }
//
//        // Try to read every key we have instead
//        // Step 3: Read from the parsed namespaces and apply it
//        for (Map.Entry<String, Map<String, Object>> entry : namespaces.entrySet()) {
//            readNamespace(entry.getKey(), entry.getValue(), result);
//        }
        // DONE: Add support for sorting => dashes in the ID separate to own sections!
        // Simplified loading strategy => Simply attempt to load all properties that have been registered
        PROPERTY_REGISTRY.getAllValues().forEach(
                prop -> {
                    if (!readProperty(prop, yamlMapping, result)) {
                        // TODO: Log warning
                    }
                }
        );

        // Step 4: Apply transforms
        for (TypeSafeTransform transform : TRANSFORM_REGISTRY) {
            runTransformer(transform, result);
        }
        // Step 5: Validate!
        for (Pair<Predicate<TypeSafeCraftType>, String> validator : VALIDATOR_REGISTRY) {
            if (!validator.getLeft().test(result)) {
                throw new IllegalArgumentException(validator.getRight());
            }
        }

        return result;
    }

    // Run transformer, if anything was changed, merge
    static void runTransformer(TypeSafeTransform transform, final TypeSafeCraftType typeSafeCraftType) {
        Map<PropertyKey, Object> output = new HashMap<>();
        Set<PropertyKey> toDelete = new HashSet<>();
        if (transform.transform(typeSafeCraftType, output::put, toDelete)) {
            output.entrySet().forEach(entry -> {
                typeSafeCraftType.set(entry.getKey(), entry.getValue());
            });
            toDelete.forEach(typeSafeCraftType::delete);
        }
    }

    // Returns false if nothing was read or if it failed
    // Returns true if something was parsed successfully
    private static <T> boolean readProperty(final PropertyKey<T> key, final Map<String, Object> yamlData, final TypeSafeCraftType type) {
        NamespacedKey namespacedKey = key.key();
        Object namespaceValues = yamlData.getOrDefault(namespacedKey.getNamespace(), null);
        if (namespaceValues != null && (namespaceValues instanceof Map namespaceMappingRaw)) {
            Map<String, Object> namespaceMapping;
            try {
                namespaceMapping = (Map<String, Object>) namespaceMappingRaw;
            } catch(ClassCastException cce) {
                // TODO: Print message
                return false;
            }
            String[] valueArr = namespacedKey.value().split("/");
            T value = null;
            for (int i = 0; i < valueArr.length; i++) {
                String workingPath = valueArr[i];
                Object objTmp = namespaceMapping.getOrDefault(workingPath, null);
                if (objTmp == null) {
                    break;
                } else {
                    if (i == valueArr.length - 1) {
                        try {
                            value = key.read(objTmp, type);
                        } catch (Exception exception) {
                            // TODO: Log warning
                            return false;
                        }
                    } else {
                        try {
                            namespaceMapping = (Map<String, Object>) namespaceMappingRaw;
                        } catch(ClassCastException cce) {
                            // TODO: Print message
                            break;
                        }
                    }
                }
            }
            if (value != null) {
                type.set(key, value);
                return true;
            }
        }
        return false;
    }

    private static <T> void readNamespace(final String namespace, final Map<String, Object> data, final TypeSafeCraftType type) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            NamespacedKey key = new NamespacedKey(namespace, entry.getKey());
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
        if (!(tagKey instanceof PropertyKey<T>)) {
            throw new IllegalStateException("Trying to set value for key that is not a property key!");
        }
        super.set(tagKey, value);
    }

    protected <T> T getWithoutParent(@NotNull PropertyKey<T> key) {
        if (this.has(key)) {
            return this.get(key);
        }
        return null;
    }

    // TODO: This implementation is not ideal for perworlddata! It does not fully merge with parent types
    public <T> T get(@NotNull PropertyKey<PerWorldData<T>> key, MovecraftWorld world) {
        return this.get(key, world.getName());
    }

    public <T> T get(@NotNull PropertyKey<PerWorldData<T>> key, World world) {
        return this.get(key, world.getName());
    }

    public <T> T get(@NotNull PropertyKey<PerWorldData<T>> key, String world) {
        final PerWorldData<T> data = this.get(key);
        return data.get(world);
    }

    public <T> T get(@NotNull PropertyKey<T> key) {
        final TypeSafeCraftType self = this;
        return this.get(key, self);
    }

    // Internal retrieval function, do not override unless you know what you are doing
    protected <T> T get(@NotNull PropertyKey<T> key, TypeSafeCraftType type) {
        T result = this.getOrDefault(key, null);
        if (result != null) {
            return result;
        } else if (this.parentInstance.get() != null) {
            result = this.parentInstance.get().get(key, type);
            if (result != null) {
                return result;
            }
        }
        // If nothing was found, return the default value!
        return key.getDefault(type);
    }

    public CraftProperties createCraftProperties(final Craft craft) {
        return new CraftProperties(this, craft);
    }

    Set<Map.Entry<PropertyKey<?>, Object>> entrySet() {
        return this.entries();
    }

    public <T> boolean hasInSelfOrAnyParent(PropertyKey<T> key) {
        if (!this.has(key)) {
            return this.get(key, this) != null;
        }
        return true;
    }

    @Nullable
    public TypeSafeCraftType getParent() {
        return this.parentInstance.get();
    }

}
