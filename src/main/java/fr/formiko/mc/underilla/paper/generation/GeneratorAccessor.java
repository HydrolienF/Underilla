package fr.formiko.mc.underilla.paper.generation;

import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.StringKeys;
import fr.formiko.mc.voidworldgenerator.VoidWorldGeneratorPlugin;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class GeneratorAccessor {
    public static ChunkGenerator getOutOfTheSurfaceWorldGenerator(String worldName, String id) {
        String outOfTheSurfaceWorldGeneratorName = Underilla.getUnderillaConfig().getString(StringKeys.OUT_OF_THE_SURFACE_WORLD_GENERATOR);
        ChunkGenerator outOfTheSurfaceWorldGenerator;
        if (outOfTheSurfaceWorldGeneratorName == null || "VANILLA".equals(outOfTheSurfaceWorldGeneratorName)) {
            outOfTheSurfaceWorldGenerator = null;
        } else if ("VoidWorldGenerator".equals(outOfTheSurfaceWorldGeneratorName)) {
            outOfTheSurfaceWorldGenerator = JavaPlugin.getProvidingPlugin(VoidWorldGeneratorPlugin.class)
                    .getDefaultWorldGenerator(worldName, id);
        } else {
            outOfTheSurfaceWorldGenerator = null;
        }
        return outOfTheSurfaceWorldGenerator;
    }
}
