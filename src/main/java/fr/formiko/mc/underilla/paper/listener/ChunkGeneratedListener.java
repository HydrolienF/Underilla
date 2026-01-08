package fr.formiko.mc.underilla.paper.listener;

import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.cleaning.CleanEntities;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Call both cleaning method after chunk generation
 */
public class ChunkGeneratedListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onChunkGenerated(ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            if (Underilla.getUnderillaConfig().getBoolean(UnderillaConfig.BooleanKeys.CLEAN_ENTITIES_ENABLED)) {
                CleanEntities.cleanEntities(event.getChunk());
            }
        }
    }
}
