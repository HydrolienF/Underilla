package fr.formiko.mc.underilla.core.generation;

import com.jkantrell.mca.MCAUtil;
import fr.formiko.mc.underilla.core.api.Block;
import fr.formiko.mc.underilla.core.api.ChunkData;
import fr.formiko.mc.underilla.core.api.HeightMapType;
import fr.formiko.mc.underilla.core.api.WorldInfo;
import fr.formiko.mc.underilla.core.reader.ChunkReader;
import fr.formiko.mc.underilla.core.reader.WorldReader;
import fr.formiko.mc.underilla.core.vector.LocatedBlock;
import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.impl.BukkitBlock;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.BooleanKeys;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Generator {


    // FIELDS
    private final WorldReader worldSurfaceReader;
    private final Merger merger;
    public static Map<String, Long> times;

    // CONSTRUCTORS
    public Generator(WorldReader worldSurfaceReader) {
        this.worldSurfaceReader = worldSurfaceReader;
        // No matter what strategy is used, the AbsoluteMerger is always used to merge the land.
        // ABSOLUTE strategy is used in the surface y level function only.
        // NONE stategy is used in
        this.merger = new AbsoluteMerger(worldSurfaceReader);
        times = new HashMap<>();
    }

    // TODO fix issue with short grass making village houses 1 block higher
    // (Not an issue for custom map without grass on it or grass removed at the world generation)
    public int getBaseHeight(WorldInfo worldInfo, int x, int z, HeightMapType heightMap) {
        int chunkX = MCAUtil.blockToChunk(x), chunkZ = MCAUtil.blockToChunk(z);
        ChunkReader chunkReader = this.worldSurfaceReader.readChunk(chunkX, chunkZ).orElse(null);
        if (chunkReader == null) {
            return 0;
        }

        // From 320, while it's not check, go down.
        Predicate<Block> check = switch (heightMap) {
            case WORLD_SURFACE, WORLD_SURFACE_WG -> Block::isAir;
            case OCEAN_FLOOR, OCEAN_FLOOR_WG, MOTION_BLOCKING -> block -> !block.isSolid();
            case MOTION_BLOCKING_NO_LEAVES -> b -> (!b.isSolid() || b.getName().toLowerCase().contains("leaves"));
        };
        int y = chunkReader.airSectionsBottom();
        Block b;
        do {
            y--;
            if (y < worldInfo.getMinHeight()) {
                break;
            }
            b = chunkReader.blockAt(Math.floorMod(x, 16), y, Math.floorMod(z, 16)).orElse(BukkitBlock.AIR);
        } while (check.test(b));
        return y + 1;
    }

    public void generateSurface(@Nonnull ChunkReader reader, @Nonnull ChunkData chunkData, @Nullable ChunkReader cavesReader) {
        this.merger.mergeLand(reader, chunkData, cavesReader);
    }

    public void reInsertLiquidsOverWorldSurface(WorldReader worldReader, ChunkData chunkData) {
        ChunkReader reader = worldReader.readChunk(chunkData.getChunkX(), chunkData.getChunkZ()).orElse(null);
        if (reader == null) {
            Underilla.warning(() -> String.format("No reader found for chunk %d, %d. Skipping liquid reinsertion.", chunkData.getChunkX(),
                    chunkData.getChunkZ()));
        } else {
            // Getting watter and lava blocks in the chunk
            // Filter blocks that are not over the surface
            List<LocatedBlock> locations = reader.locationsOf(Block::isLiquid).stream()
                    .filter(l -> l.y() > worldReader.getLowerBlockOfSurfaceWorldYLevel(chunkData.getChunkX() * Underilla.CHUNK_SIZE + l.x(),
                            chunkData.getChunkZ() * Underilla.CHUNK_SIZE + l.z()))
                    .toList();

            // Placing them back
            locations.forEach(l -> {
                Block b = chunkData.getBlock(l.vector());
                b.waterlog();
                chunkData.setBlock(l.vector(), b);
            });
        }
    }

    public boolean shouldGenerateNoise(int chunkX, int chunkZ) {
        return Underilla.getUnderillaConfig().getMergeStrategy() != MergeStrategy.NONE;
    }

    public boolean shouldGenerateSurface(int chunkX, int chunkZ) {
        // Must always return true, bedrock and deepslate layers are generated in this step
        return true;
    }

    public boolean shouldGenerateCaves(int chunkX, int chunkZ) {
        return Underilla.getUnderillaConfig().getBoolean(BooleanKeys.CARVERS_ENABLED);
    }

    public boolean shouldGenerateDecorations(int chunkX, int chunkZ) {
        return Underilla.getUnderillaConfig().getBoolean(BooleanKeys.VANILLA_POPULATION_ENABLED);
    }

    public boolean shouldGenerateMobs(int chunkX, int chunkZ) { return true; }

    public boolean shouldGenerateStructures(int chunkX, int chunkZ) {
        return Underilla.getUnderillaConfig().getBoolean(BooleanKeys.STRUCTURES_ENABLED);
    }

    public static void addTime(String name, long startTime) {
        times.put(name, times.getOrDefault(name, 0l) + (System.currentTimeMillis() - startTime));
    }
}
