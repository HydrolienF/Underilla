package com.jkantrell.mc.underilla.spigot.cleaning;

import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.SetEntityTypeKeys;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.StringKeys;
import com.jkantrell.mc.underilla.spigot.selector.Selector;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

public class CleanEntitiesTask extends FollowableProgressTask {
    public CleanEntitiesTask(int taskID, int tasksCount) { super(taskID, tasksCount); }
    public CleanEntitiesTask(int taskID, int tasksCount, Selector selector) { super(taskID, tasksCount, selector); }

    public void run() {
        final long startTime = System.currentTimeMillis();
        final Map<EntityType, Long> removedEntity = new EnumMap<>(EntityType.class);
        final Map<EntityType, Long> finalEntity = new EnumMap<>(EntityType.class);
        new BukkitRunnable() {
            private long processedEntities = 0;
            @Override
            public void run() {
                long execTime = System.currentTimeMillis();
                while (selector != null && execTime + 45 > System.currentTimeMillis() && selector.hasNextBlock() && !stop) {
                    Chunk currentChunk = selector.nextChunk();
                    for (Entity entity : currentChunk.getEntities()) {
                        if (Underilla.getUnderillaConfig().isEntityTypeInSet(SetEntityTypeKeys.CLEAN_ENTITY_TO_REMOVE, entity.getType())) {
                            entity.remove();
                            removedEntity.put(entity.getType(), removedEntity.getOrDefault(entity.getType(), 0l) + 1);
                        } else {
                            // Final transformation that can be override by other plugins
                            if (Underilla.getInstance().hasEndEntityTransformer()) {
                                Underilla.getInstance().getEndEntityTransformer().accept(entity);
                            }
                            finalEntity.put(entity.getType(), finalEntity.getOrDefault(entity.getType(), 0l) + 1);
                        }
                    }
                }

                if (selector == null || selector.progress() >= 1 || stop) {
                    printProgress(processedEntities, startTime);
                    String finishOrStop = stop ? "stopped" : "finished";
                    Underilla.info("Cleaning entities task " + taskID + " " + finishOrStop + " in "
                            + Duration.ofMillis(System.currentTimeMillis() - startTime));
                    Underilla.info("Removed entities: " + removedEntity);
                    Underilla.info("Final entities: " + finalEntity);
                    cancel();
                    Underilla.getInstance().validateTask(StringKeys.STEP_CLEANING_ENTITIES);
                    return;
                } else {
                    printProgressIfNeeded(processedEntities, startTime);
                }

            }

        }.runTaskTimer(Underilla.getInstance(), 0, 1);
    }

}
