package com.jkantrell.mc.underilla.spigot;

import fr.formiko.mc.biomeutils.NMSBiomeUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import com.jkantrell.mc.underilla.core.generation.Generator;
import com.jkantrell.mc.underilla.spigot.generation.UnderillaChunkGenerator;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldReader;
import com.jkantrell.mc.underilla.spigot.io.Config;
import com.jkantrell.mc.underilla.spigot.listener.StructureEventListener;

public final class Underilla extends JavaPlugin {

    public static final Config CONFIG = new Config("");
    private BukkitWorldReader worldSurfaceReader;
    private @Nullable BukkitWorldReader worldCavesReader;
    private com.jkantrell.mc.underilla.spigot.generation.WorldInitListener worldInitListener;


    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (this.worldSurfaceReader == null) {
            this.getServer().getLogger().warning("No world with name '" + Underilla.CONFIG.referenceWorldName + "' found");
            return super.getDefaultWorldGenerator(worldName, id);
        }
        this.getServer().getLogger().info("Using Underilla as world generator!");
        return new UnderillaChunkGenerator(this.worldSurfaceReader, this.worldCavesReader);
    }

    @Override
    public void onEnable() {
        // save default config
        this.saveDefaultConfig();

        // Loading config
        Underilla.CONFIG.setFilePath(this.getDataFolder() + File.separator + "config.yml");
        try {
            Underilla.CONFIG.load();
            Underilla.CONFIG.transferCavesWorldBiomes = NMSBiomeUtils.normalizeBiomeNameList(Underilla.CONFIG.transferCavesWorldBiomes);
            Underilla.CONFIG.preserveBiomes = NMSBiomeUtils.normalizeBiomeNameList(Underilla.CONFIG.preserveBiomes);
            Underilla.CONFIG.ravinBiomes = NMSBiomeUtils.normalizeBiomeNameList(Underilla.CONFIG.ravinBiomes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Loading reference world
        try {
            this.worldSurfaceReader = new BukkitWorldReader(Underilla.CONFIG.referenceWorldName);
            this.getServer().getLogger().info("World + '" + Underilla.CONFIG.referenceWorldName + "' found.");
        } catch (NoSuchFieldException e) {
            this.getServer().getLogger().warning("No world with name '" + Underilla.CONFIG.referenceWorldName + "' found");
            e.printStackTrace();
        }
        // Loading caves world if we should use it.
        if (Underilla.CONFIG.transferBlocksFromCavesWorld || Underilla.CONFIG.transferBiomesFromCavesWorld) {
            try {
                this.getServer().getLogger().info("Loading caves world");
                this.worldCavesReader = new BukkitWorldReader(Underilla.CONFIG.cavesWorldName);
            } catch (NoSuchFieldException e) {
                this.getServer().getLogger().warning("No world with name '" + Underilla.CONFIG.cavesWorldName + "' found");
                e.printStackTrace();
            }
        }

        // Registering listeners
        if (CONFIG.generateStructures) {
            this.getServer().getPluginManager().registerEvents(new StructureEventListener(CONFIG.structureBlackList), this);
        }
        if (CONFIG.transferBiomes && CONFIG.customBiomeEnabled) {
            worldInitListener = new com.jkantrell.mc.underilla.spigot.generation.WorldInitListener(worldSurfaceReader, worldCavesReader);
            this.getServer().getPluginManager().registerEvents(worldInitListener, this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (Generator.times != null) {
                long totalTime = Generator.times.entrySet().stream().mapToLong(Map.Entry::getValue).sum();
                for (Map.Entry<String, Long> entry : Generator.times.entrySet()) {
                    this.getServer().getLogger()
                            .info(entry.getKey() + " took " + entry.getValue() + "ms (" + (entry.getValue() * 100 / totalTime) + "%)");
                }
            }
            Map<String, Long> biomesPlaced = worldInitListener != null ? worldInitListener.getCustomBiomeSource().getBiomesPlaced()
                    : UnderillaChunkGenerator.getBiomesPlaced();
            this.getServer().getLogger()
                    .info("Map of chunks: " + biomesPlaced.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                            .map(entry -> entry.getKey() + ": " + entry.getValue()).reduce((a, b) -> a + ", " + b).orElse(""));
        } catch (Exception e) {
            this.getServer().getLogger().info("Fail to print times or biomes placed.");
            e.printStackTrace();
        }
    }

    public static Underilla getInstance() { return getPlugin(Underilla.class); }
}
