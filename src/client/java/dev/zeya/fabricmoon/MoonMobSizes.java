package dev.zeya.fabricmoon;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

public final class MoonMobSizes {
   public static final String MAGMA_CUBE_ID = "minecraft:magma_cube";
   private static List<EntityType<?>> cachedMobs;

   private MoonMobSizes() {
   }

   public static List<EntityType<?>> resizableMobs() {
      if (cachedMobs == null) {
         Set<EntityType<?>> mobs = new LinkedHashSet<>();
         addVanillaMobs(mobs);
         addRegisteredMobs(mobs);
         mobs.remove(EntityTypes.MAGMA_CUBE);
         List<EntityType<?>> sorted = new ArrayList<>(mobs);
         sorted.sort(Comparator.comparing(type -> type.getDescription().getString(), String.CASE_INSENSITIVE_ORDER));
         cachedMobs = List.copyOf(sorted);
      }

      return cachedMobs;
   }

   private static void addVanillaMobs(Set<EntityType<?>> mobs) {
      for (Field field : EntityTypes.class.getDeclaredFields()) {
         if (Modifier.isStatic(field.getModifiers())
            && field.getType() == EntityType.class
            && field.getGenericType() instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();
            if (arguments.length == 1 && arguments[0] instanceof Class<?> entityClass && Mob.class.isAssignableFrom(entityClass)) {
               try {
                  if (field.get(null) instanceof EntityType<?> type) {
                     mobs.add(type);
                  }
               } catch (IllegalAccessException var10) {
               }
            }
         }
      }
   }

   private static void addRegisteredMobs(Set<EntityType<?>> mobs) {
      for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
         MobCategory category = type.getCategory();
         if (category != null && category != MobCategory.MISC) {
            mobs.add(type);
         }
      }
   }

   public static String idOf(EntityType<?> type) {
      Identifier id = EntityType.getKey(type);
      return id == null ? "" : id.toString();
   }

   public static String displayNameOf(EntityType<?> type) {
      return type.getDescription().getString();
   }

   public static void selectAllLargerThanPlayer() {
      float playerHeight = 1.8F * MoonConfig.mobSize;
      List<String> ids = new ArrayList<>();

      for (EntityType<?> type : resizableMobs()) {
         if (type.getHeight() > playerHeight) {
            ids.add(idOf(type));
         }
      }

      MoonConfig.addMobPlayerSizeTargets(ids);
   }

   public static int selectedCount() {
      int count = 0;

      for (EntityType<?> type : resizableMobs()) {
         if (MoonConfig.isMobPlayerSizeTarget(idOf(type))) {
            count++;
         }
      }

      return count;
   }
}
