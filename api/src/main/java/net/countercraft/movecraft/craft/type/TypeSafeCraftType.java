package net.countercraft.movecraft.craft.type;

import com.google.errorprone.annotations.concurrent.LazyInit;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.transform.TypeSafeTransform;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.LazyLoadField;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.SerializationUtil;
import net.countercraft.movecraft.util.registration.SimpleRegistry;
import net.countercraft.movecraft.util.registration.TypedContainer;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TypeSafeCraftType extends TypedContainer<PropertyKey<?>> {

    // Initialization
    static {
        PropertyKeys.registerAll();
        Transformers.registerAll();
        Validators.registerAll();
    }

    static final SimpleRegistry<NamespacedKey, PropertyKey> PROPERTY_REGISTRY = new SimpleRegistry();
    static final Set<TypeSafeTransform> TRANSFORM_REGISTRY = new HashSet<>();
    static final Set<Pair<Predicate<TypeSafeCraftType>, String>> VALIDATOR_REGISTRY = new HashSet<>();

    protected final Function<String, TypeSafeCraftType> typeRetriever;
    private final String name;

    protected String parentName = null;
    protected final LazyLoadField<BiFunction<PropertyKey<?>, TypeSafeCraftType, Object>> parentRetrievalFunction = new LazyLoadField<>(this::computeParentFunction);

    private BiFunction<PropertyKey<?>, TypeSafeCraftType, Object> computeParentFunction() {
        final BiFunction<PropertyKey<?>, TypeSafeCraftType, Object> defaultValue = (key, type) -> key.getDefault(type);
        if (this.parentName == null || this.parentName.isEmpty()) {
            return defaultValue;
        } else {
            final TypeSafeCraftType parentType = this.typeRetriever.apply(this.parentName);
            if (parentType != null) {
                return parentType::get;
            } else {
                return defaultValue;
            }
        }
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

    private static TypeSafeCraftType buildType(String name, Function<String, TypeSafeCraftType> typeRetriever, Map<String, Object> yamlMapping) {
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

        Map<String, Map<String, Object>> namespaces = new HashMap<>();

        Set<Map.Entry<NamespacedKey, PropertyKey>> entries = PROPERTY_REGISTRY.entries();
        // TODO: Sort the entries by registration index

        // Step 1: Set all default values
        for (Map.Entry<NamespacedKey, PropertyKey> entry : entries) {
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

        // Step 4: Apply transforms
        for (TypeSafeTransform<?> transform : TRANSFORM_REGISTRY) {
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
    static <T> void runTransformer(TypeSafeTransform<T> transform, final TypeSafeCraftType typeSafeCraftType) {
        Map<PropertyKey<T>, T> output = new HashMap<>();
        if (transform.transform(typeSafeCraftType::getWithoutParent, output)) {
            output.entrySet().forEach(entry -> {
                typeSafeCraftType.set(entry.getKey(), entry.getValue());
            });
        }
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
        final TypeSafeCraftType self = this;
        T result = super.get(key, (k) -> (T)this.parentRetrievalFunction.get().apply(k, self));
        return result;
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
}
