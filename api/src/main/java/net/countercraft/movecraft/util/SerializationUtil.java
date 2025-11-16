package net.countercraft.movecraft.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;

import java.util.*;

public class SerializationUtil {

    public static Object serializeEnumSet(EnumSet<Material> materials) {
        List<String> result = new ArrayList<>(materials.size());
        materials.forEach(m -> result.add(m.name()));
        return result;
    }

    static void parseInternal(Set<NamespacedKey> collector, Object object) {
        String workingString = null;
        if (object instanceof Keyed keyed) {
            if (keyed.key() instanceof NamespacedKey namespacedKey) {
                collector.add(namespacedKey);
            } else {
                workingString = keyed.key().toString();
            }
        } else if (object instanceof String string) {
            workingString = string;
        }
        if (workingString != null) {
            collector.add(NamespacedKey.fromString(workingString));
        }
    }

//    // Use TagKey and similar according to https://copilot.microsoft.com/shares/V2YcA9LmddP53ZsBdnAZG
//    // Primarily, parse tag key with "#" marker, remove marker, create TagKey object and use that
//    // Careful: Registry.getTag() throws an exception if the tag does not exist!
//    private static <T extends org.bukkit.Keyed> void parseTagInternal(Set<NamespacedKey> result, TagKey namespacedKey, Registry<T> registry) {
//        Queue<TagKey> tagQueue = new LinkedList<>();
//        final Set<TagKey> visited = new HashSet<>();
//        tagQueue.add(namespacedKey);
//        visited.add(namespacedKey);
//        while (!tagQueue.isEmpty()) {
//            final TagKey polledKey = tagQueue.poll();
//            if (!registry.hasTag(polledKey)) {
//                // TODO: Log warnign that the tag does not exist for this registry!
//                continue;
//            }
//            Tag<T> tag = registry.getTag(polledKey);
//            for (T key : tag.resolve(RegistryAccess.registryAccess().getRegistry(tag.registryKey()))) {
//                if (registry.get(key.getKey()) == null) {
//                    // TODO: Is this necessary?
//                    TagKey tagKeyTmp = TagKey.create(tag.registryKey(), key.key());
//                    if (visited.add(tagKeyTmp)) {
//                        tagQueue.add(tagKeyTmp);
//                    }
//                } else {
//                    result.add(key.getKey());
//                }
//            }
//        }
//    }
//
//    public static <T extends org.bukkit.Keyed> Set<NamespacedKey> deserializeNamespacedKeySet(Object rawDataObject, Set<NamespacedKey> defaultValue, RegistryKey<T>... registryKeys) {
//        Set<NamespacedKey> resultTmp = new HashSet<>();
//        if (rawDataObject != null) {
//            if (rawDataObject instanceof List list) {
//                for (Object obj : list) {
//                    parseInternal(resultTmp, obj);
//                }
//            } else {
//                parseInternal(resultTmp, rawDataObject);
//            }
//        }
//
//        Set<NamespacedKey> result = new HashSet<>();
//        for (RegistryKey<T> registryKey : registryKeys) {
//            final Registry<T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);
//            for (NamespacedKey namespacedKey : resultTmp) {
//                if (namespacedKey.namespace().startsWith("#")) {
//                    parseTagInternal(result, TagKey.create(registryKey, namespacedKey.toString().substring(1)), registry);
//                } else {
//                    T value = registry.get(namespacedKey);
//                    if (value != null) {
//                        result.add(namespacedKey);
//                    } else {
//                       throw new IllegalArgumentException("Unable to lookup value for key <" + namespacedKey.toString() + "> in registry <" + registryKey.toString() + ">!");
//                    }
//                }
//            }
//        }
//
//        if (result.isEmpty()) {
//            return defaultValue;
//        }
//        return result;
//    }
//
//    public static <T extends org.bukkit.Keyed> Set<T> deserializeRegisteredObjectSet(Object rawDataObject, Set<T> defaultValue, final RegistryKey<T> registryKey) {
//        Set<NamespacedKey> resultTmp = deserializeNamespacedKeySet(rawDataObject, Set.of(), registryKey);
//        if (resultTmp.isEmpty()) {
//            return defaultValue;
//        }
//        Set<T> result = new HashSet<>(resultTmp.size());
//        final Registry<T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);
//        for (NamespacedKey namespacedKey : resultTmp) {
//            if (namespacedKey.namespace().startsWith("#")) {
//                throw new RuntimeException("Caught tag in parsed NamespacedKey list!");
//            }
//            T value = registry.get(namespacedKey);
//            if (value != null) {
//                result.add(value);
//            }
//        }
//        if (result.isEmpty()) {
//            return defaultValue;
//        }
//        return result;
//    }

    @Deprecated(forRemoval = true)
    /*
     * Deprecated, Material enum should not be used! Use NamespacedKey instead
     */
    public static EnumSet<Material> deserializeEnumSet(Object rawDataObject, EnumSet<Material> defaultValue) {
        EnumSet<Material> result = EnumSet.noneOf(Material.class);
        if (rawDataObject != null) {
            if (rawDataObject instanceof List list) {
                for (Object obj : list) {
                    if (obj instanceof Material material) {
                        result.add(material);
                    } else if (obj instanceof String string) {
                        result.addAll(Tags.parseMaterials(string));
                    }
                }
            } else if (rawDataObject instanceof Material) {
                result.add((Material) rawDataObject);
            } else if (rawDataObject instanceof String) {
                result.addAll(Tags.parseMaterials((String) rawDataObject));
            }
        }

        if (result.isEmpty()) {
            result.addAll(defaultValue);
        }
        return result;
    }

    public static List<String> deserializeStringList(Object rawDataObject, List<String> defaultValue) {
        List<String> typeList;
        try {
            typeList = (List<String>) rawDataObject;
            if (!typeList.isEmpty()) {
                List<String> tmpList = new ArrayList<>(typeList);
                typeList.clear();
                final List<String> typeListTmp = typeList;
                tmpList.forEach(s -> {
                    typeListTmp.add(s.toUpperCase());
                });
            }
        } catch(ClassCastException cce) {
            typeList = List.copyOf(defaultValue);
        }
        if (typeList == null) {
            typeList = List.copyOf(defaultValue);
        }
        return typeList;
    }

    public static boolean deserializeBoolean(Object rawDataObject, boolean defaultValue) {
        boolean result = defaultValue;
        if (rawDataObject != null && (rawDataObject instanceof Boolean)) {
            result = ((Boolean) rawDataObject).booleanValue();
        }
        return result;
    }

    public static boolean deserializeBoolean(String key, Map<String, Object> rawDataMap, boolean defaultValue) {
        Object raw = rawDataMap.getOrDefault(key, null);
        return deserializeBoolean(raw, defaultValue);
    }

    public static String deserializeString(String key, Map<String, Object> rawData, String defaultValue) {
        return String.valueOf(rawData.getOrDefault(key, defaultValue));
    }

}
