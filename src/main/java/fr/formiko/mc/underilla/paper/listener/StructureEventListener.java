package fr.formiko.mc.underilla.paper.listener;

import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.SetStructureKeys;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureSpawnEvent;

public class StructureEventListener implements Listener {
    private final Map<String, Integer> structureCount;

    public StructureEventListener() { structureCount = new HashMap<>(); }

    public Map<String, Integer> getStructureCount() { return structureCount; }

    @EventHandler(ignoreCancelled = true)
    public void onStructureSpawn(AsyncStructureSpawnEvent e) {
        // If in the list of structure to keep then log & count else cancel the event.
        if (Underilla.getUnderillaConfig().isStructureInSet(SetStructureKeys.SURUCTURE_ONLY, e.getStructure())) {
            NamespacedKey structureKey = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE).getKey(e.getStructure());
            String structureName = structureKey == null ? "null" : structureKey.asString();
            Underilla.debug(() -> structureName + " spawned at " + e.getChunkX() + " " + e.getChunkZ() + " in biome "
                    + e.getWorld().getBiome(e.getChunkX() * 16, 0, e.getChunkZ() * 16));
            structureCount.put(structureName, structureCount.getOrDefault(structureName, 0) + 1);
        } else {
            e.setCancelled(true);
        }
    }

}
