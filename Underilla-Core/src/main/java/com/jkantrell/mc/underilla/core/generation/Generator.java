package com.jkantrell.mc.underilla.core.generation;

import java.util.List;
import java.util.function.Predicate;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.core.api.ChunkData;
import com.jkantrell.mc.underilla.core.api.HeightMapType;
import com.jkantrell.mc.underilla.core.api.WorldInfo;
import com.jkantrell.mc.underilla.core.reader.ChunkReader;
import com.jkantrell.mc.underilla.core.reader.WorldReader;
import com.jkantrell.mc.underilla.core.vector.LocatedBlock;
import com.jkantrell.mca.MCAUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class Generator {


    // FIELDS
    private final WorldReader worldReader_;
    private final Merger merger_;
    private final GenerationConfig config_;
    // public static Map<String, Long> times;

    // CONSTRUCTORS
    public Generator(WorldReader worldReader, GenerationConfig config) {
        this.worldReader_ = worldReader;
        this.config_ = config;
        this.merger_ = switch (config_.mergeStrategy) {
            case RELATIVE -> new RelativeMerger(this.worldReader_, config_.mergeUpperLimit, config_.mergeLowerLimit, config_.mergeDepth,
                    config_.mergeBlendRange, config_.keptUndergroundBiomes, config_.preserveBiomes, config_.keptReferenceWorldBlocks);
            case SURFACE, ABSOLUTE, NONE -> new AbsoluteMerger(config_.mergeStrategy.equals(MergeStrategy.NONE) ? -64 : config_.mergeLimit,
                    config_.preserveBiomes, config.ravinBiomes, config_.keptReferenceWorldBlocks,
                    config_.mergeStrategy.equals(MergeStrategy.SURFACE) ? config_.mergeDepth : 0);
        };
        // times = new HashMap<>();
    }

    public int getBaseHeight(WorldInfo worldInfo, int x, int z, HeightMapType heightMap) {
        int chunkX = MCAUtil.blockToChunk(x), chunkZ = MCAUtil.blockToChunk(z);
        ChunkReader chunkReader = this.worldReader_.readChunk(chunkX, chunkZ).orElse(null);
        if (chunkReader == null) {
            return 0;
        }

        Predicate<Block> check = switch (heightMap) {
            case WORLD_SURFACE, WORLD_SURFACE_WG -> Block::isAir;
            case OCEAN_FLOOR, OCEAN_FLOOR_WG, MOTION_BLOCKING -> Block::isSolid;
            case MOTION_BLOCKING_NO_LEAVES -> b -> b.isSolid() || b.getName().toLowerCase().contains("leaves");
        };
        int y = chunkReader.airSectionsBottom();
        Block b, airBlock = chunkReader.blockFromTag(MCAUtil.airBlockTag()).get();
        do {
            y--;
            if (y < worldInfo.getMinHeight()) {
                break;
            }
            b = chunkReader.blockAt(Math.floorMod(x, 16), y, Math.floorMod(z, 16)).orElse(airBlock);
        } while (check.test(b));
        return y + 1;
    }

    public void generateSurface(@Nonnull ChunkReader reader, @Nonnull ChunkData chunkData, @Nullable ChunkReader cavesReader) {
        this.merger_.mergeLand(reader, chunkData, cavesReader);
        // The only configuration where we need to merge biome here is when we want to transfer biomes from the reference world
        // & keep underground biomes.
        if (config_.needToMixBiomes()) {
            // long time = System.currentTimeMillis();
            this.merger_.mergeBiomes(reader, chunkData);
            // addTime("mergeBiomes", time);
        }
    }

    public void reInsertLiquids(ChunkReader reader, ChunkData chunkData) {
        // Getting watter and lava blocks in the chunk
        List<LocatedBlock> locations = reader.locationsOf(Block::isLiquid);

        // Placing them back
        locations.forEach(l -> {
            Block b = chunkData.getBlock(l.vector());
            b.waterlog();
            chunkData.setBlock(l.vector(), b);
        });
    }

    public boolean shouldGenerateNoise(int chunkX, int chunkZ) { return !this.config_.mergeStrategy.equals(MergeStrategy.NONE); }

    public boolean shouldGenerateSurface(int chunkX, int chunkZ) {
        // Must always return true, bedrock and deepslate layers are generated in this step
        return true;
    }

    public boolean shouldGenerateCaves(int chunkX, int chunkZ) { return this.config_.generateCaves; }

    public boolean shouldGenerateDecorations(int chunkX, int chunkZ) { return this.config_.vanillaPopulation; }

    public boolean shouldGenerateMobs(int chunkX, int chunkZ) { return true; }

    public boolean shouldGenerateStructures(int chunkX, int chunkZ) { return this.config_.generateStructures; }

    // public static void addTime(String name, long startTime) {
    // times.put(name, times.getOrDefault(name, 0l) + (System.currentTimeMillis() - startTime));
    // }
}
