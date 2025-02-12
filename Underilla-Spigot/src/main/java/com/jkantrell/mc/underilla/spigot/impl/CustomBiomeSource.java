package com.jkantrell.mc.underilla.spigot.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.BooleanKeys;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.IntegerKeys;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.SetBiomeStringKeys;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.StringKeys;

public class CustomBiomeSource {
    private BiomeProvider vanillaBiomeSource;
    private final BukkitWorldReader worldSurfaceReader;
    private final BukkitWorldReader worldCavesReader;
    private final Map<String, Long> biomesPlaced;
    private long lastInfoPrinted = 0;
    private long lastWarnningPrinted = 0;

    public CustomBiomeSource(@Nonnull BukkitWorldReader worldSurfaceReader, @Nullable BukkitWorldReader worldCavesReader) {
        this.worldSurfaceReader = worldSurfaceReader;
        this.worldCavesReader = worldCavesReader;
        this.biomesPlaced = new ConcurrentHashMap<>();
    }

    public Map<String, Long> getBiomesPlaced() { return biomesPlaced; }

    /**
     * Get biome at x, y, z.
     * 
     * @param worldInfo World information.
     * @param x         Actual world coordinate.
     * @param y         Actual world coordinate.
     * @param z         Actual world coordinate.
     * @return
     */
    public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        // Needed to get surface biome & test if caves biome will override a preserved biome.
        // Use the top biome from the surface world only if configured.
        int surfaceWorldBiomeY = Underilla.getUnderillaConfig().getBoolean(BooleanKeys.SURFACE_WORLD_BIOME_USE_TOP_Y_VALUE_ONLY)
                ? Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_Y)
                : y;
        String surfaceWorldBiomeName = worldSurfaceReader.getBiomeName(x, surfaceWorldBiomeY, z);

        if (vanillaBiomeSource == null) {
            CraftWorld worldFinal = (CraftWorld) Bukkit.getWorld(Underilla.getUnderillaConfig().getString(StringKeys.FINAL_WORLD_NAME));
            vanillaBiomeSource = worldFinal == null ? null : worldFinal.vanillaBiomeProvider();
            Underilla.info("VanillaBiomeSource was null. It is now set to " + vanillaBiomeSource);
        }

        if (vanillaBiomeSource != null && surfaceWorldBiomeName != null && !Underilla.getUnderillaConfig()
                .isBiomeInSet(SetBiomeStringKeys.SURFACE_WORLD_ONLY_ON_THIS_BIOMES, surfaceWorldBiomeName)) {
            Biome vanillaBiome = vanillaBiomeSource.getBiome(worldInfo, x, y, z);
            String vanillaBiomeName = vanillaBiome.getKey().asString();
            // info("Currently tested vanillaBiome: " + vanillaBiomeName + " at " + x + " " + y + " " + z);
            // If is a cave biome that we should preserve & is below the surface of surface world.
            if (vanillaBiomeName != null && Underilla.getUnderillaConfig()
                    .isBiomeInSet(SetBiomeStringKeys.BIOME_MERGING_FROM_CAVES_GENERATION_ONLY_ON_BIOMES, vanillaBiomeName)
                    && isUnderSurface(worldSurfaceReader, x, y, z)) {
                String key = "cavesGeneration:" + vanillaBiomeName;
                debug("Use vanillaBiome because it's a cavesGeneration biome: " + vanillaBiomeName + " at " + x + " " + y + " " + z);
                biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
                return vanillaBiome;
            }
        }

        // // Get biome from cave world if it's in the list of transferWorldFromCavesWorld.
        // // & surface biome does not have a preserved biome here.
        // // & it's below the surface.
        // if (Underilla.CONFIG.transferBiomesFromCavesWorld && worldCavesReader != null
        // && (surfaceWorldBiome == null || !Underilla.CONFIG.preserveBiomes.contains(surfaceWorldBiome.getName()))
        // && y < Underilla.CONFIG.mergeLimit - Underilla.CONFIG.mergeDepth && y < 50) {
        // // For now there is as 50 hard max limits.
        // BukkitBiome cavesWorldBiome = (BukkitBiome) worldCavesReader.biomeAt(x, y, z).orElse(null);
        // if (cavesWorldBiome != null && Underilla.CONFIG.transferCavesWorldBiomes.contains(cavesWorldBiome.getName())) {
        // info("Use cavesWorldBiome because it's a transferedCavesWorldBiomes: " + cavesWorldBiome.getName() + " at " + x + " " + y
        // + " " + z);
        // String key = "caves:" + cavesWorldBiome.getName();
        // biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
        // return cavesWorldBiome.getBiome();
        // }
        // }

        // Get biome from surface world.
        if (surfaceWorldBiomeName != null) {
            debug("Use surfaceWorldBiome: " + surfaceWorldBiomeName + " at " + x + " " + y + " " + z);
            biomesPlaced.put("surface:" + surfaceWorldBiomeName, biomesPlaced.getOrDefault("surface:" + surfaceWorldBiomeName, 0L) + 1);
            // return surfaceWorldBiome.getBiome();
            return io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.BIOME)
                    .get(NamespacedKey.fromString(surfaceWorldBiomeName));
        }

        // If no other biome found, use vanilla biome.
        warning("Use vanilla because no other biome found at " + x + " " + y + " " + z);
        String key = "error:" + BukkitBiome.DEFAULT.getName();
        biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
        return BukkitBiome.DEFAULT.getBiome();
    }

    private boolean isUnderSurface(BukkitWorldReader worldSurfaceReader, int x, int y, int z) {
        if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.BIOME_MERGING_FROM_CAVES_GENERATION_ONLY_UNDER_SURFACE)) {
            // Merging biome below the surface only.
            x = x - x % 4;
            z = z - z % 4;
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    // If the block is over the merge limit, it's not under the surface.
                    if (y >= topYOfSurfaceWorld(worldSurfaceReader, x + i, z + j)) {
                        return false;
                    }
                }
            }
        }
        // All blocks are under the surface (or it's configured not to check).
        return true;
    }

    private int topYOfSurfaceWorld(BukkitWorldReader worldSurfaceReader, int x, int z) {
        return worldSurfaceReader.getLowerBlockOfSurfaceWorldYLevel(x, z);
    }

    private synchronized void debug(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInfoPrinted > Underilla.MS_PER_SECOND) {
            Underilla.debug(message);
            lastInfoPrinted = currentTime;
        }
    }
    private synchronized void info(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInfoPrinted > Underilla.MS_PER_SECOND) {
            Underilla.info(message);
            lastInfoPrinted = currentTime;
        }
    }
    private synchronized void warning(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWarnningPrinted > Underilla.MS_PER_SECOND) {
            Underilla.warning(message);
            lastWarnningPrinted = currentTime;
        }
    }
}
