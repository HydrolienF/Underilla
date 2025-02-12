package com.jkantrell.mc.underilla.core.reader;

import java.io.File;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import com.jkantrell.mc.underilla.core.api.Biome;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.core.generation.MergeStrategy;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.impl.BukkitBlock;
import com.jkantrell.mc.underilla.spigot.io.Tools;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.IntegerKeys;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.SetBiomeStringKeys;
import com.jkantrell.mca.Chunk;
import com.jkantrell.mca.MCAFile;
import com.jkantrell.mca.MCAUtil;

public abstract class WorldReader implements Reader {

    // CONSTANTS
    private static final String REGION_DIRECTORY = "region";

    // FIELDS
    private final File world_;
    private final File regions_;
    private final RLUCache<MCAFile> regionCache_;
    private final RLUCache<ChunkReader> chunkCache_;
    private final RLUCache<Integer> yLevelCache_;
    private final RLUCacheTriple<String> biomeCache_;

    // CONSTRUCTORS
    protected WorldReader(String worldPath) throws NoSuchFieldException { this(new File(worldPath)); }
    protected WorldReader(String worldPath, int cacheSize) throws NoSuchFieldException { this(new File(worldPath), cacheSize); }
    protected WorldReader(File worldDir) throws NoSuchFieldException {
        this(worldDir, Underilla.getUnderillaConfig().getInt(IntegerKeys.CACHE_SIZE));
    }
    protected WorldReader(File worldDir, int cacheSize) throws NoSuchFieldException {
        if (!(worldDir.exists() && worldDir.isDirectory())) {
            throw new NoSuchFieldException("World directory '" + worldDir.getPath() + "' does not exist.");
        }
        File regionDir = new File(worldDir, WorldReader.REGION_DIRECTORY);
        if (!(regionDir.exists() && regionDir.isDirectory())) {
            throw new NoSuchFieldException("World '" + worldDir.getName() + "' doesn't have a 'region' directory.");
        }
        this.world_ = worldDir;
        this.regions_ = regionDir;
        this.regionCache_ = new RLUCache<>(cacheSize);
        // There is 32*32 chunks in a region. We probably don't need to cache all of them.
        int chunkCacheSize = cacheSize * 64;
        this.chunkCache_ = new RLUCache<>(chunkCacheSize);
        // Cache as many y levels as it can fit in all the loaded chunks.
        this.yLevelCache_ = new RLUCache<>(chunkCacheSize * Underilla.CHUNK_SIZE * Underilla.CHUNK_SIZE);
        // Cache as many biomes as it can fit in all the loaded chunks.
        this.biomeCache_ = new RLUCacheTriple<>(chunkCacheSize * 4 * 4);
    }


    // GETTERS
    public String getWorldName() { return this.world_.getName(); }


    // UTIL
    @Override
    public Optional<Block> blockAt(int x, int y, int z) {
        int chunkX = MCAUtil.blockToChunk(x), chunkZ = MCAUtil.blockToChunk(z);
        return this.readChunk(chunkX, chunkZ).flatMap(c -> c.blockAt(Math.floorMod(x, 16), y, Math.floorMod(z, 16)));
    }
    @Override
    public Optional<Biome> biomeAt(int x, int y, int z) {
        int chunkX = MCAUtil.blockToChunk(x), chunkZ = MCAUtil.blockToChunk(z);
        return this.readChunk(chunkX, chunkZ).flatMap(c -> c.biomeAt(Math.floorMod(x, 16), y, Math.floorMod(z, 16)));
    }
    public Optional<ChunkReader> readChunk(int x, int z) {
        // This is the step were we read the chunk from the region file.
        // by setting the x and z to 0, we are able to read only 1 chunk.
        // x = 0;
        // z = 0;
        ChunkReader chunkReader = this.chunkCache_.get(x, z);
        if (chunkReader != null) {
            return Optional.of(chunkReader);
        }
        MCAFile r = this.readRegion(x >> 5, z >> 5);
        if (r == null) {
            return Optional.empty();
        }
        Chunk chunk = r.getChunk(Math.floorMod(x, 32), Math.floorMod(z, 32));
        if (chunk == null) {
            return Optional.empty();
        }
        chunkReader = this.newChunkReader(chunk);
        this.chunkCache_.put(x, z, chunkReader);
        return Optional.of(chunkReader);
    }

    public int getLowerBlockOfSurfaceWorldYLevel(int globalX, int globalZ) {
        // Hard limit for NONE & ABSOLUTE merge strategy.
        if (Underilla.getUnderillaConfig().getMergeStrategy() == MergeStrategy.ABSOLUTE
                || Underilla.getUnderillaConfig().getMergeStrategy() == MergeStrategy.NONE) {
            return Underilla.getUnderillaConfig().getInt(IntegerKeys.MAX_HEIGHT_OF_CAVES);
        }
        // Computed limit for SURFACE merge startegy.
        Integer cached = yLevelCache_.get(globalX, globalZ);
        if (cached != null) {
            return cached;
        }
        int r;

        int maxHeightOfCaves = Underilla.getUnderillaConfig().getInt(IntegerKeys.MAX_HEIGHT_OF_CAVES);
        int minimalPossibleY = Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_Y);
        if (maxHeightOfCaves <= minimalPossibleY) {
            r = minimalPossibleY;
            yLevelCache_.put(globalX, globalZ, r);
            return r;
        }
        int mergeDepth = Underilla.getUnderillaConfig().getInt(IntegerKeys.MERGE_DEPTH);

        // Optional<Biome> optionalBiome = surfaceReader.biomeAt(globalX, 0, globalZ);
        // Unkown biome or preserved biome.
        if (Underilla.getUnderillaConfig().isBiomeInSet(SetBiomeStringKeys.SURFACE_WORLD_ONLY_ON_THIS_BIOMES,
                getBiomeName(globalX, globalZ))) {
            r = minimalPossibleY;
            yLevelCache_.put(globalX, globalZ, r);
            return r;
        }


        // While is AIR, LEAVES, non solid block, etc, go down.
        int lbtr = maxHeightOfCaves + mergeDepth;
        while (!blockAt(globalX, lbtr, globalZ).orElse(BukkitBlock.AIR).isSolidAndSurfaceBlock() && lbtr > minimalPossibleY) {
            lbtr--;
        }

        // Cliffs surface fixer to avoid cave blocks being visible on cliffs.
        int extraAdaptativeMaxMergeDepth = Underilla.getUnderillaConfig().getInt(IntegerKeys.ADAPTATIVE_MAX_MERGE_DEPTH) - mergeDepth;
        final int finalDepth;
        if (extraAdaptativeMaxMergeDepth > 0) {
            int minHiddenBlocksDepth = Underilla.getUnderillaConfig().getInt(IntegerKeys.ADAPTATIVE_MIN_HIDDEN_BLOCKS_MERGE_DEPTH);
            int countHowManyBlocksAreExposed = 0;
            // While there is air or grass etc near the block, go down.
            while (extraAdaptativeMaxMergeDepth > 0 && haveNonSolidNeighbour(globalX, lbtr - countHowManyBlocksAreExposed, globalZ)
                    && lbtr > minimalPossibleY) {
                countHowManyBlocksAreExposed++;
                extraAdaptativeMaxMergeDepth--;
            }
            finalDepth = Math.max(mergeDepth, countHowManyBlocksAreExposed + minHiddenBlocksDepth);
        } else {
            finalDepth = mergeDepth;
        }


        r = lbtr - finalDepth;
        yLevelCache_.put(globalX, globalZ, r);
        return r;
    }

    private boolean haveNonSolidNeighbour(int x, int y, int z) {
        // Is there blocks next to that block that are not solid (air, leaves, etc). If no block are found, return false.
        return Stream.of(blockAt(x + 1, y, z), blockAt(x - 1, y, z), blockAt(x, y, z + 1), blockAt(x, y, z - 1)).filter(Optional::isPresent)
                .map(Optional::get).filter(b -> !b.isSolid()).findAny().isPresent();
    }

    public String getBiomeName(int globalX, int globalY, int globalZ) {
        // make globalX and globalZ multiple of BIOME_AREA_SIZE ot avoid storing duplicate data.
        globalX = globalX - globalX % Underilla.BIOME_AREA_SIZE;
        globalY = globalY - globalY % Underilla.BIOME_AREA_SIZE;
        globalZ = globalZ - globalZ % Underilla.BIOME_AREA_SIZE;

        String cached = biomeCache_.get(globalX, globalY, globalZ);
        if (cached != null) {
            return cached;
        }

        Optional<Biome> optionalBiome = biomeAt(globalX, globalY, globalZ);
        String r = optionalBiome.isEmpty() ? null : optionalBiome.get().getName();
        biomeCache_.put(globalX, globalY, globalZ, r);
        return r;
    }
    public String getBiomeName(int globalX, int globalZ) {
        return getBiomeName(globalX, Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_Y), globalZ);
    }


    // ABSTRACT
    protected abstract ChunkReader newChunkReader(Chunk chunk);


    // PRIVATE UTIL
    private MCAFile readRegion(int x, int z) {
        MCAFile region = this.regionCache_.get(x, z);
        if (region != null) {
            return region;
        }
        File regionFile = new File(this.regions_, "r." + x + "." + z + ".mca");
        if (!regionFile.exists()) {
            return null;
        }
        try {
            region = MCAUtil.read(regionFile);
            this.regionCache_.put(x, z, region);
            return region;
        } catch (Exception e) {
            Underilla.error(() -> "Failed to read region file '" + regionFile.getPath() + "'");
            Underilla.error(() -> Tools.exceptionToString(e));
            return null;
        }
    }


    // CLASSES
    public static class RLUCache<T> {

        // FIELDS
        private final Map<Pair<Integer, Integer>, T> map_ = new HashMap<>();
        private final Deque<Pair<Integer, Integer>> queue_ = new LinkedList<>();
        private final int capacity_;


        // CONSTRUCTOR
        RLUCache(int capacity) { this.capacity_ = capacity; }


        // UTIL
        // We synchronized the methode to avoid concurrent access to the cache.
        // Concurrent access cause queue_ and map_ to grow without never being reduced.
        // We might win few ms by reducing the part of the code that is synchronized, but I don't think it's worth the potential bugs.
        T get(int x, int z) {
            Pair<Integer, Integer> pair = ImmutablePair.of(x, z);
            synchronized (this) {
                // Set the pair to the front of the queue to avoid it to be removed soon.
                // T cached = this.map_.get(pair);
                // if (cached == null) {
                // return null;
                // }
                // this.queue_.remove(pair);
                // this.queue_.addFirst(pair);
                // return cached;
                // Do not edit the queue to be faster.
                return this.map_.get(pair);
            }
        }
        void put(int x, int z, T file) {
            Pair<Integer, Integer> pair = ImmutablePair.of(x, z);
            synchronized (this) {
                if (map_.containsKey(pair)) {
                    this.queue_.remove(pair);
                } else if (this.queue_.size() >= this.capacity_) {
                    try {
                        Pair<Integer, Integer> temp = this.queue_.removeLast();
                        this.map_.remove(temp);
                    } catch (NoSuchElementException ignored) {}
                }
                this.map_.put(pair, file);
                this.queue_.addFirst(pair);
            }
        }
    }

    public static class RLUCacheTriple<T> {

        // FIELDS
        private final Map<Triple<Integer, Integer, Integer>, T> map_ = new HashMap<>();
        private final Deque<Triple<Integer, Integer, Integer>> queue_ = new LinkedList<>();
        private final int capacity_;


        // CONSTRUCTOR
        RLUCacheTriple(int capacity) { this.capacity_ = capacity; }


        // UTIL
        // We synchronized the methode to avoid concurrent access to the cache.
        // Concurrent access cause queue_ and map_ to grow without never being reduced.
        // We might win few ms by reducing the part of the code that is synchronized, but I don't think it's worth the potential bugs.
        T get(int x, int y, int z) {
            Triple<Integer, Integer, Integer> pair = ImmutableTriple.of(x, y, z);
            synchronized (this) {
                return this.map_.get(pair);
            }
        }
        void put(int x, int y, int z, T file) {
            Triple<Integer, Integer, Integer> pair = ImmutableTriple.of(x, y, z);
            synchronized (this) {
                if (map_.containsKey(pair)) {
                    this.queue_.remove(pair);
                } else if (this.queue_.size() >= this.capacity_) {
                    try {
                        Triple<Integer, Integer, Integer> temp = this.queue_.removeLast();
                        this.map_.remove(temp);
                    } catch (NoSuchElementException ignored) {}
                }
                this.map_.put(pair, file);
                this.queue_.addFirst(pair);
            }
        }
    }

}
