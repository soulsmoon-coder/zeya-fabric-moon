package dev.zeya.fabricmoon;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.gizmos.Gizmos.TemporaryCollection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class MoonHitboxRenderer {
   private static final double MAX_DISTANCE_SQUARED = 4096.0;
   private static final int HITBOX_COLOR = -52429;
   private static final GizmoStyle HITBOX_STYLE = GizmoStyle.stroke(-52429, 1.5F);
   private static final GizmoStyle TARGET_HITBOX_STYLE = GizmoStyle.stroke(-10934, 2.0F);
   private static final SimpleGizmoCollector COLLECTOR = new SimpleGizmoCollector();

   private MoonHitboxRenderer() {
   }

   public static void register() {
      LevelRenderEvents.BEFORE_GIZMOS.register(MoonHitboxRenderer::render);
   }

   private static void render(LevelRenderContext context) {
      Minecraft client = Minecraft.getInstance();
      if (MoonConfig.featuresEnabled && MoonConfig.entityHitboxes && client.level != null) {
         COLLECTOR.drainGizmos();
         Entity cameraEntity = client.getCameraEntity();
         if (cameraEntity != null) {
            Vec3 cameraPosition = cameraEntity.position();
            boolean hideCameraEntity = client.options.getCameraType() == CameraType.FIRST_PERSON;
            Entity targetedEntity = client.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
            AABB searchArea = new AABB(
               cameraPosition.x() - Math.sqrt(4096.0),
               cameraPosition.y() - Math.sqrt(4096.0),
               cameraPosition.z() - Math.sqrt(4096.0),
               cameraPosition.x() + Math.sqrt(4096.0),
               cameraPosition.y() + Math.sqrt(4096.0),
               cameraPosition.z() + Math.sqrt(4096.0)
            );
            TemporaryCollection ignored = Gizmos.withCollector(COLLECTOR);

            try {
               for (Entity entity : client.level.getEntities(cameraEntity, searchArea)) {
                  if (!entity.isInvisible() && (!hideCameraEntity || entity != cameraEntity) && !(entity.position().distanceToSqr(cameraPosition) > 4096.0)) {
                     float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(!client.level.tickRateManager().isEntityFrozen(entity));
                     Vec3 interpolatedPosition = entity.getPosition(partialTick);
                     Vec3 offset = interpolatedPosition.subtract(entity.position());
                     Gizmos.cuboid(entity.getBoundingBox().move(offset), entity == targetedEntity ? TARGET_HITBOX_STYLE : HITBOX_STYLE);
                  }
               }
            } catch (Throwable var14) {
               if (ignored != null) {
                  try {
                     ignored.close();
                  } catch (Throwable var13) {
                     var14.addSuppressed(var13);
                  }
               }

               throw var14;
            }

            if (ignored != null) {
               ignored.close();
            }

            context.levelRenderer().addMainThreadGizmos(COLLECTOR.drainGizmos());
         }
      }
   }
}
