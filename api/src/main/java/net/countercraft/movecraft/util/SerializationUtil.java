package net.countercraft.movecraft.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.*;

public class SerializationUtil {

    public static Object serializeEnumSet(EnumSet<Material> materials) {
        List<String> result = new ArrayList<>(materials.size());
        materials.forEach(m -> result.add(m.name()));
        return result;
    }

    static void parseInternal(Set<NamespacedKey> collectorNormal, Set<NamespacedKey> collectorTag, Object object) {
        String workingString = null;
        if (object instanceof Keyed keyed) {
            if (keyed.key() instanceof NamespacedKey namespacedKey) {
                // NamespacedKeys CAN NOT have # in the namespace, so it is always normal
                collectorNormal.add(namespacedKey);
            }
            if (keyed instanceof TagKey<?> tagKey) {
                collectorTag.add(new NamespacedKey(tagKey.key().namespace(), tagKey.key().value()));
            }
            else {
                workingString = keyed.key().toString();
            }
        } else if (object instanceof String string) {
            workingString = string;
        }
        if (workingString != null) {
            NamespacedKey namespacedKey = null;
            Set<NamespacedKey> collection = null;
            if (workingString.startsWith("#")) {
                collection = collectorTag;
                namespacedKey = NamespacedKey.fromString(workingString.substring(1));
            } else {
                collection = collectorNormal;
                namespacedKey = NamespacedKey.fromString(workingString);
            }
            if (namespacedKey != null && collection != null) {
                collection.add(namespacedKey);
            }
        }
    }

    // Use TagKey and similar according to https://copilot.microsoft.com/shares/V2YcA9LmddP53ZsBdnAZG
    // Primarily, parse tag key with "#" marker, remove marker, create TagKey object and use that
    // Careful: Registry.getTag() throws an exception if the tag does not exist!
    private static <T extends org.bukkit.Keyed> void parseTagInternalTypeAware(Set<T> result, TagKey<T> namespacedKey, Registry<T> registry) {
        // If we dont even know that tag, we can quit early
        if (!registry.hasTag(namespacedKey)) {
            return;
        }

        Queue<TagKey> tagQueue = new LinkedList<>();
        final Set<TagKey> visited = new HashSet<>();
        tagQueue.add(namespacedKey);
        visited.add(namespacedKey);
        while (!tagQueue.isEmpty()) {
            final TagKey polledKey = tagQueue.poll();
            if (!registry.hasTag(polledKey)) {
                // TODO: Log warning that the tag does not exist for this registry!
                continue;
            }
            Tag<T> tag = registry.getTag(polledKey);
            for (T key : tag.resolve(RegistryAccess.registryAccess().getRegistry(tag.registryKey()))) {
                if (registry.get(key.getKey()) == null) {
                    // TODO: Is this necessary?
                    TagKey tagKeyTmp = TagKey.create(tag.registryKey(), key.key());
                    if (visited.add(tagKeyTmp)) {
                        tagQueue.add(tagKeyTmp);
                    }
                } else {
                    result.add(key);
                }
            }
        }
    }

    private static <T extends org.bukkit.Keyed> void parseTagInternal(Set<NamespacedKey> result, TagKey<T> namespacedKey, Registry<T> registry) {
        Set<T> resultTmp = new HashSet<>();
        parseTagInternalTypeAware(resultTmp, namespacedKey, registry);
        for (T tmp : resultTmp) {
            result.add(tmp.getKey());
        }
    }

    public static <T extends org.bukkit.Keyed> Set<NamespacedKey> deserializeNamespacedKeySet(Object rawDataObject, Set<NamespacedKey> defaultValue, RegistryKey<T>... registryKeys) {
        Set<NamespacedKey> resultTmp = new HashSet<>();
        Set<NamespacedKey> tagsTmp = new HashSet<>();
        if (rawDataObject != null) {
            if (rawDataObject instanceof List list) {
                for (Object obj : list) {
                    parseInternal(resultTmp, tagsTmp, obj);
                }
            } else {
                parseInternal(resultTmp, tagsTmp, rawDataObject);
            }
        }

        Set<NamespacedKey> result = new HashSet<>();
        for (RegistryKey<T> registryKey : registryKeys) {
            final Registry<T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);
            for (NamespacedKey namespacedKey : resultTmp) {
                T value = registry.get(namespacedKey);
                if (value != null) {
                    result.add(namespacedKey);
                } else {
                   System.err.println("Unable to lookup value for key <" + namespacedKey.toString() + "> in registry <" + registryKey.toString() + ">!");
                }
            }
            for (NamespacedKey tagNamespacedKey : tagsTmp) {
                parseTagInternal(result, TagKey.create(registryKey, tagNamespacedKey), registry);
            }
        }

        if (result.isEmpty()) {
            return defaultValue;
        }
        return result;
    }

    public static <T extends org.bukkit.Keyed> Set<T> deserializeRegisteredObjectSet(Object rawDataObject, Set<T> defaultValue, final RegistryKey<T> registryKey) {
        Set<NamespacedKey> resultTmp = deserializeNamespacedKeySet(rawDataObject, Set.of(), registryKey);
        if (resultTmp.isEmpty()) {
            return defaultValue;
        }
        Set<T> result = new HashSet<>(resultTmp.size());
        final Registry<T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);
        for (NamespacedKey namespacedKey : resultTmp) {
            T value = registry.get(namespacedKey);
            if (value != null) {
                result.add(value);
            } else {
                // Tries to parse the key as tag, quits early if no such tag exists
                parseTagInternalTypeAware(result, TagKey.create(registryKey, namespacedKey), registry);
            }
        }
        if (result.isEmpty()) {
            return defaultValue;
        }
        return result;
    }

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
