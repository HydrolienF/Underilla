package com.jkantrell.mc.underilla.spigot.generation;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldReader;
import com.jkantrell.mc.underilla.spigot.impl.CustomBiomeSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class WorldInitListener implements Listener {
    private final BukkitWorldReader worldSurfaceReader;
    private final BukkitWorldReader worldCavesReader;
    private CustomBiomeSource customBiomeSource;

    public WorldInitListener(BukkitWorldReader worldSurfaceReader, BukkitWorldReader worldCavesReader) {
        this.worldSurfaceReader = worldSurfaceReader;
        this.worldCavesReader = worldCavesReader;
    }

    public CustomBiomeSource getCustomBiomeSource() { return customBiomeSource; }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        Underilla.getInstance().getLogger().info("Preparing to take over the world: " + event.getWorld().getName());
        CraftWorld craftWorld = (CraftWorld) event.getWorld();
        ServerLevel serverLevel = craftWorld.getHandle();

        // ConfigPack pack = bukkitChunkGeneratorWrapper.getPack();

        ChunkGenerator vanilla = serverLevel.getChunkSource().getGenerator();
        BiomeSource vanillaBiomeSource = vanilla.getBiomeSource();
        customBiomeSource = new CustomBiomeSource(vanillaBiomeSource, worldSurfaceReader, worldCavesReader);

        serverLevel.getChunkSource().chunkMap.generator = new NMSExtendedChunkGenerator(vanilla, customBiomeSource);
    }

}
