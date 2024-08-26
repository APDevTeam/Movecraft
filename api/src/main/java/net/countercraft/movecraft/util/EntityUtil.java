package net.countercraft.movecraft.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.EnumSet;

public class EntityUtil {
    public static EnumSet<EntityType> getClassEntities(Class<? extends Entity> clazz) {
        var entityList = EnumSet.noneOf(EntityType.class);

        for(EntityType type : EntityType.values()) {
            var entityClazz = type.getEntityClass();

            if (entityClazz == null)
                continue;

            if (entityClazz.isAssignableFrom(clazz)) {
                entityList.add(type);
            }
        }

        return entityList;
    }
}
