package com.jkantrell.mc.underilla.spigot.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;

public class CustomBiomeSource extends BiomeSource {
    private final BiomeSource vanillaBiomeSource;
    private final BukkitWorldReader worldSurfaceReader;
    private final BukkitWorldReader worldCavesReader;
    private final Map<String, Long> biomesPlaced;
    private long lastInfoPrinted = 0;
    private long lastWarnningPrinted = 0;

    public CustomBiomeSource(BiomeSource vanillaBiomeSource, BukkitWorldReader worldSurfaceReader, BukkitWorldReader worldCavesReader) {
        this.vanillaBiomeSource = vanillaBiomeSource;
        this.worldSurfaceReader = worldSurfaceReader;
        this.worldCavesReader = worldCavesReader;
        this.biomesPlaced = new ConcurrentHashMap<>();
    }

    public Map<String, Long> getBiomesPlaced() { return biomesPlaced; }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        try {
            Method method = BiomeSource.class.getDeclaredMethod("codec");
            method.setAccessible(true);
            return (MapCodec<? extends BiomeSource>) method.invoke(vanillaBiomeSource);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Underilla.getInstance().getLogger().warning("Failed to get codec field from BiomeSource");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        Registry<Biome> biomeRegistry = NMSBiomeUtils.getBiomeRegistry();
        Underilla.getInstance().getLogger().info(
                "Collecting possible biomes: " + biomeRegistry.stream().map(biome -> biomeRegistry.getKey(biome).toString()).toList());
        return biomeRegistry.stream().map(biome -> biomeRegistry.wrapAsHolder(biome));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, @Nonnull Sampler noise) {
        // Keep biome from vanilla noise biome generation if it's in the list of keptUndergroundBiomes.
        Holder<Biome> vanillaBiome = vanillaBiomeSource.getNoiseBiome(x, y, z, noise);
        // Edit value to mach actual world coordinates.
        x = x << 2;
        y = y << 2;
        z = z << 2;
        if (vanillaBiome != null && (!Underilla.CONFIG.transferBiomes
                || Underilla.CONFIG.keptUndergroundBiomes.contains(vanillaBiome.getRegisteredName()))) {
            info("Use vanillaBiome because we don't transfer biome or it's a keptUndergroundBiomes: " + vanillaBiome.getRegisteredName()
                    + " at " + x + " " + y + " " + z);
            String key = "noise:" + vanillaBiome.getRegisteredName();
            biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
            return vanillaBiome;
        }

        // Get biome from cave world if it's in the list of keptUndergroundBiomes.
        if (Underilla.CONFIG.transferWorldFromCavesWorld && worldCavesReader != null) {
            BukkitBiome cavesBiome = (BukkitBiome) worldCavesReader.biomeAt(x, y, z).orElse(null);
            if (cavesBiome != null && Underilla.CONFIG.transferCavesWorldBiomes.contains(cavesBiome.getName())) {
                info("Use cavesBiome because it's a transferedCavesWorldBiomes: " + cavesBiome.getName() + " at " + x + " " + y + " " + z);
                String key = "caves:" + cavesBiome.getName();
                biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
                return NMSBiomeUtils.getBiomeRegistry().wrapAsHolder(NMSBiomeUtils.getBiome(cavesBiome.getName()));
            }
        }

        // Get biome from surface world.
        BukkitBiome surfaceBiome = (BukkitBiome) worldSurfaceReader.biomeAt(x, y, z).orElse(null);
        if (surfaceBiome != null) {
            info("Use surfaceBiome: " + surfaceBiome.getName() + " at " + x + " " + y + " " + z);
            biomesPlaced.put("surface:" + surfaceBiome.getName(), biomesPlaced.getOrDefault("surface:" + surfaceBiome.getName(), 0L) + 1);
            return NMSBiomeUtils.getBiomeRegistry().wrapAsHolder(NMSBiomeUtils.getBiome(surfaceBiome.getName()));
        }

        // If no other biome found, use plain biome.
        warning("Use vanilla because no other biome found at " + x + " " + y + " " + z);
        if (vanillaBiome != null) {
            String key = "notfound:" + vanillaBiome.getRegisteredName();
            biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
            return vanillaBiome;
        } else {
            String key = "error:plains";
            biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
            return NMSBiomeUtils.getBiomeRegistry().wrapAsHolder(NMSBiomeUtils.getBiome("minecraft:plains"));
        }
    }

    private synchronized void info(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInfoPrinted > 1000) {
            Underilla.getInstance().getLogger().info(message);
            lastInfoPrinted = currentTime;
        }
    }
    private synchronized void warning(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWarnningPrinted > 1000) {
            Underilla.getInstance().getLogger().warning(message);
            lastWarnningPrinted = currentTime;
        }
    }

}
