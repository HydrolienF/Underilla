package fr.formiko.mc.underilla.paper.generation;

import fr.formiko.mc.underilla.core.api.HeightMapType;
import fr.formiko.mc.underilla.core.generation.Generator;
import fr.formiko.mc.underilla.core.reader.ChunkReader;
import fr.formiko.mc.underilla.core.reader.WorldReader;
import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.impl.BukkitChunkData;
import fr.formiko.mc.underilla.paper.impl.BukkitRegionChunkData;
import fr.formiko.mc.underilla.paper.impl.BukkitWorldInfo;
import fr.formiko.mc.underilla.paper.impl.BukkitWorldReader;
import fr.formiko.mc.underilla.paper.impl.CustomBiomeSource;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.BooleanKeys;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.IntegerKeys;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.SetBiomeStringKeys;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

public class UnderillaChunkGenerator extends ChunkGenerator {
    // TODO : For performance reason, we should generate and empty world if transfer_world_from_caves_world==true

    // ASSETS
    private static final Map<HeightMap, HeightMapType> HEIGHTMAPS_MAP = Map.of(HeightMap.OCEAN_FLOOR, HeightMapType.OCEAN_FLOOR,
            HeightMap.OCEAN_FLOOR_WG, HeightMapType.OCEAN_FLOOR_WG, HeightMap.MOTION_BLOCKING, HeightMapType.MOTION_BLOCKING,
            HeightMap.MOTION_BLOCKING_NO_LEAVES, HeightMapType.MOTION_BLOCKING_NO_LEAVES, HeightMap.WORLD_SURFACE,
            HeightMapType.WORLD_SURFACE, HeightMap.WORLD_SURFACE_WG, HeightMapType.WORLD_SURFACE_WG);


    // FIELDS
    private final Generator delegate;
    private final @Nonnull fr.formiko.mc.underilla.core.reader.WorldReader worldSurfaceReader;
    private final @Nullable fr.formiko.mc.underilla.core.reader.WorldReader worldCavesReader;
    private static CustomBiomeSource customBiomeSource;
    private static ChunkGenerator outOfTheSurfaceWorldGenerator;


    // CONSTRUCTORS
    public UnderillaChunkGenerator(@Nonnull BukkitWorldReader worldSurfaceReader, @Nullable BukkitWorldReader worldCavesReader,
            @Nullable ChunkGenerator outOfTheSurfaceWorldGenerator) {
        this.worldSurfaceReader = worldSurfaceReader;
        this.worldCavesReader = worldCavesReader;
        this.outOfTheSurfaceWorldGenerator = outOfTheSurfaceWorldGenerator;
        this.delegate = new Generator(worldSurfaceReader);
    }


    // IMPLEMENTATIONS
    @Override
    public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z, HeightMap heightMap) {
        // Do not use base height from VoidWorldGenerator if it is outside of the surface world, else it broke structures generation.
        // We only use UnderillaChunkGenerator base height to avoid a bug with the structure generation height.
        BukkitWorldInfo info = new BukkitWorldInfo(worldInfo);
        return this.delegate.getBaseHeight(info, x, z, HEIGHTMAPS_MAP.get(heightMap));
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
            @NotNull ChunkData chunkData) {
        if (outOfTheSurfaceWorldGenerator != null
                && isOutsideOfTheSurfaceWorld(chunkX * Underilla.CHUNK_SIZE, chunkZ * Underilla.CHUNK_SIZE)) {
            outOfTheSurfaceWorldGenerator.generateSurface(worldInfo, random, chunkX, chunkZ, chunkData);
            return;
        }

        String biomeKey = getBiomeKeyStringFromChunkCoordinates(worldInfo, chunkX, chunkZ);
        // if is a biome that should not be carved OR surface should not be preserved from carvers (== merge the world before).
        if (!Underilla.getUnderillaConfig().isBiomeInSet(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES, biomeKey) || !Underilla
                .getUnderillaConfig().isBiomeInSet(SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_ONLY_ON_BIOMES, biomeKey)) {
            mergeSurfaceWorldAndCavesWorld(worldInfo, random, chunkX, chunkZ, chunkData);
        }
    }

    @Override
    public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        if (outOfTheSurfaceWorldGenerator != null
                && isOutsideOfTheSurfaceWorld(chunkX * Underilla.CHUNK_SIZE, chunkZ * Underilla.CHUNK_SIZE)) {
            outOfTheSurfaceWorldGenerator.generateCaves(worldInfo, random, chunkX, chunkZ, chunkData);
            return;
        }

        String biomeKey = getBiomeKeyStringFromChunkCoordinates(worldInfo, chunkX, chunkZ);
        // if is a biome that should be carved and surface should be preserved from carvers (== merge the world after).
        if (Underilla.getUnderillaConfig().isBiomeInSet(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES, biomeKey) && Underilla
                .getUnderillaConfig().isBiomeInSet(SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_ONLY_ON_BIOMES, biomeKey)) {
            mergeSurfaceWorldAndCavesWorld(worldInfo, random, chunkX, chunkZ, chunkData);
        }
    }

    private void mergeSurfaceWorldAndCavesWorld(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
            @NotNull ChunkData chunkData) {
        Optional<ChunkReader> reader = this.worldSurfaceReader.readChunk(chunkX, chunkZ);
        if (reader.isEmpty()) {
            return;
        }
        BukkitChunkData data = new BukkitChunkData(chunkData);
        ChunkReader cavesReader = null;
        if (this.worldCavesReader != null && Underilla.getUnderillaConfig().getBoolean(BooleanKeys.TRANSFER_BLOCKS_FROM_CAVES_WORLD)) {
            cavesReader = this.worldCavesReader.readChunk(chunkX, chunkZ).orElse(null);
        }
        this.delegate.generateSurface(reader.get(), data, cavesReader);
    }
    private static String getBiomeKeyStringFromChunkCoordinates(@NotNull WorldInfo worldInfo, int chunkX, int chunkZ) {
        return Bukkit.getWorld(worldInfo.getUID()).getBiome(chunkX * Underilla.CHUNK_SIZE, 0, chunkZ * Underilla.CHUNK_SIZE).getKey()
                .asString();
    }


    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        // Caves are vanilla generated, but they are carved underwater, this re-places the water blocks in case they were carved into.
        return List.of(new Populator(this.worldSurfaceReader, this.delegate));
    }

    @Override
    public boolean shouldGenerateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        if (outOfTheSurfaceWorldGenerator != null
                && isOutsideOfTheSurfaceWorld(chunkX * Underilla.CHUNK_SIZE, chunkZ * Underilla.CHUNK_SIZE)) {
            return outOfTheSurfaceWorldGenerator.shouldGenerateNoise(worldInfo, random, chunkX, chunkZ);
        }

        return this.delegate.shouldGenerateNoise(chunkX, chunkZ);
    }


    @Override
    public boolean shouldGenerateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        if (outOfTheSurfaceWorldGenerator != null
                && isOutsideOfTheSurfaceWorld(chunkX * Underilla.CHUNK_SIZE, chunkZ * Underilla.CHUNK_SIZE)) {
            return outOfTheSurfaceWorldGenerator.shouldGenerateSurface(worldInfo, random, chunkX, chunkZ);
        }

        // Must always return true, bedrock and deepslate layers are generated in this step
        return this.delegate.shouldGenerateSurface(chunkX, chunkZ);
    }


    /**
     * Should generate caves with carvers.
     */
    @Override
    public boolean shouldGenerateCaves(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        if (outOfTheSurfaceWorldGenerator != null
                && isOutsideOfTheSurfaceWorld(chunkX * Underilla.CHUNK_SIZE, chunkZ * Underilla.CHUNK_SIZE)) {
            return outOfTheSurfaceWorldGenerator.shouldGenerateCaves(worldInfo, random, chunkX, chunkZ);
        }

        String biomeKey = getBiomeKeyStringFromChunkCoordinates(worldInfo, chunkX, chunkZ);
        return Underilla.getUnderillaConfig().isBiomeInSet(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES, biomeKey);
    }

    @Override
    public boolean shouldGenerateDecorations(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        if (outOfTheSurfaceWorldGenerator != null
                && isOutsideOfTheSurfaceWorld(chunkX * Underilla.CHUNK_SIZE, chunkZ * Underilla.CHUNK_SIZE)) {
            return outOfTheSurfaceWorldGenerator.shouldGenerateDecorations(worldInfo, random, chunkX, chunkZ);
        }

        return this.delegate.shouldGenerateDecorations(chunkX, chunkZ);
    }

    @Override
    public boolean shouldGenerateMobs(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        if (outOfTheSurfaceWorldGenerator != null
                && isOutsideOfTheSurfaceWorld(chunkX * Underilla.CHUNK_SIZE, chunkZ * Underilla.CHUNK_SIZE)) {
            return outOfTheSurfaceWorldGenerator.shouldGenerateMobs(worldInfo, random, chunkX, chunkZ);
        }

        return this.delegate.shouldGenerateMobs(chunkX, chunkZ);
    }

    @Override
    public boolean shouldGenerateStructures(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        if (outOfTheSurfaceWorldGenerator != null
                && isOutsideOfTheSurfaceWorld(chunkX * Underilla.CHUNK_SIZE, chunkZ * Underilla.CHUNK_SIZE)) {
            return outOfTheSurfaceWorldGenerator.shouldGenerateStructures(worldInfo, random, chunkX, chunkZ);
        }

        return this.delegate.shouldGenerateStructures(chunkX, chunkZ);
    }

    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        int x = Math.min(Math.max(0, Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_X)),
                Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_X));
        int z = Math.min(Math.max(0, Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_Z)),
                Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_Z));
        return new Location(world, x, 100, z);
    }

    // Since 1.21.3 custom biomes are supported by paper.
    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        Underilla.info("Underilla Use the custom biome provider from file data. Structures will be generate in the right biome.");
        BiomeProvider outOfTheSurfaceWorldBiomeProdiver = outOfTheSurfaceWorldGenerator == null ? null
                : outOfTheSurfaceWorldGenerator.getDefaultBiomeProvider(worldInfo);
        return new BiomeProviderFromFile(outOfTheSurfaceWorldBiomeProdiver);
    }

    public static Map<String, Long> getBiomesPlaced() {
        if (customBiomeSource == null) {
            return null;
        }
        return customBiomeSource.getBiomesPlaced();
    }


    private boolean isOutsideOfTheSurfaceWorld(int x, int z) {
        return x < Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_X)
                || x >= Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_X)
                || z < Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_Z)
                || z >= Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_Z);
    }


    // CLASSES
    private static class Populator extends BlockPopulator {

        // FIELDS
        private final WorldReader worldReader_;
        private final Generator generator_;


        // CONSTRUCTORS
        public Populator(WorldReader reader, Generator generator) {
            this.worldReader_ = reader;
            this.generator_ = generator;
        }


        // OVERRITES
        @Override
        public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
            // If carvers are enabled in this biome & surface was not preserved from carvers & liquids are preserved from carvers.
            // => we need to re-insert the water blocks from surface world over the limits between the 2 world.

            if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.PRESERVE_LIQUID_FROM_CAVERS)) {
                String biomeKey = getBiomeKeyStringFromChunkCoordinates(worldInfo, chunkX, chunkZ);
                if (Underilla.getUnderillaConfig().isBiomeInSet(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES, biomeKey)
                        && !Underilla.getUnderillaConfig()
                                .isBiomeInSet(SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_ONLY_ON_BIOMES, biomeKey)) {

                    BukkitRegionChunkData chunkData = new BukkitRegionChunkData(limitedRegion, chunkX, chunkZ, worldInfo.getMinHeight(),
                            worldInfo.getMaxHeight());
                    this.generator_.reInsertLiquidsOverWorldSurface(this.worldReader_, chunkData);
                }
            }
        }
    }

    private class BiomeProviderFromFile extends BiomeProvider {
        private final BiomeProvider outOfTheSurfaceWorldBiomeProdiver;

        private BiomeProviderFromFile(BiomeProvider outOfTheSurfaceWorldBiomeProdiver) {
            this.outOfTheSurfaceWorldBiomeProdiver = outOfTheSurfaceWorldBiomeProdiver;
            customBiomeSource = new CustomBiomeSource(((BukkitWorldReader) worldSurfaceReader), ((BukkitWorldReader) worldCavesReader));
        }

        @Override
        public @Nonnull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
            if (outOfTheSurfaceWorldBiomeProdiver != null && isOutsideOfTheSurfaceWorld(x, z)) {
                return outOfTheSurfaceWorldBiomeProdiver.getBiome(worldInfo, x, y, z);
            }
            return customBiomeSource.getBiome(worldInfo, x, y, z);
        }

        @Override
        public @Nonnull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
            return io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.BIOME)
                    .stream().toList();
        }

    }
}
