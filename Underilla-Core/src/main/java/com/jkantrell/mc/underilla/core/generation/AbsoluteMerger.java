package com.jkantrell.mc.underilla.core.generation;

import java.util.List;
import com.jkantrell.mc.underilla.core.api.Biome;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.core.api.ChunkData;
import com.jkantrell.mc.underilla.core.reader.ChunkReader;
import com.jkantrell.mc.underilla.core.reader.Reader;
import com.jkantrell.mc.underilla.core.vector.IntVector;
import com.jkantrell.mc.underilla.core.vector.Vector;
import com.jkantrell.mc.underilla.core.vector.VectorIterable;
import com.jkantrell.mca.MCAUtil;
import jakarta.annotation.Nullable;

class AbsoluteMerger implements Merger {

    // FIELDS
    private final int height_;
    private final List<? extends Biome> preserveBiomes_, ravinBiomes_;
    private final List<String> keptReferenceWorldBlocks_;
    private final int mergeDepth_;

    // CONSTRUCTORS
    AbsoluteMerger(int height, List<? extends Biome> preserveBiomes, List<? extends Biome> ravinBiomes,
            List<String> keptReferenceWorldBlocks, int mergeDepth) {
        this.height_ = height;
        this.preserveBiomes_ = preserveBiomes;
        this.ravinBiomes_ = ravinBiomes;
        this.keptReferenceWorldBlocks_ = keptReferenceWorldBlocks;
        this.mergeDepth_ = mergeDepth;
    }


    // IMPLEMENTATIONS
    // @Override
    // public void merge(ChunkReader reader, ChunkData chunkData) {
    // this.mergeLand(reader, chunkData);
    // // this.mergeBiomes(reader, chunkData); // No need to set biome for chunk. It's done by the generator.
    // }
    @Override
    public void mergeLand(ChunkReader reader, ChunkData chunkData, @Nullable ChunkReader cavesReader) {
        // long startTime = System.currentTimeMillis();
        Block airBlock = reader.blockFromTag(MCAUtil.airBlockTag()).get();
        // int airColumn = Math.max(reader.airSectionsBottom(), -64);
        int airColumn = reader.airSectionsBottom();
        chunkData.setRegion(0, airColumn, 0, 16, chunkData.getMaxHeight(), 16, airBlock);

        VectorIterable iterable = new VectorIterable(0, 16, -64, airColumn, 0, 16);
        int columnHeigth = this.height_;
        int lastX = -1;
        int lastZ = -1;
        // Generator.addTime("Create VectorIterable to merge land", startTime);
        for (Vector<Integer> v : iterable) {
            // startTime = System.currentTimeMillis();
            // Generator.addTime("Read block data from custom world", startTime);
            // startTime = System.currentTimeMillis();
            // Generator.addTime("Read block data from vanilla world", startTime);

            // For every collumn of bloc calculate the lower block to remove that migth be lower than height_.
            // startTime = System.currentTimeMillis();
            if (v.x() != lastX || v.z() != lastZ) {
                lastX = v.x();
                lastZ = v.z();
                columnHeigth = (isPreservedBiome(reader, v) ? -64 : getLowerBlockToRemove(reader, v.x(), v.z(), airBlock));
            }
            // Generator.addTime("Calculate lower block to remove", startTime);

            // startTime = System.currentTimeMillis();
            // Place the custom world bloc over 55 (or -64 if is preseved biome) or if it is a custom ore or if it is air,
            // or if vanilla world have watter or grass or sand over 30
            // and do not replace liquid vanilla blocks by air. (to preserve water and lava lackes)
            Block b = reader.blockAt(v).orElse(airBlock);
            Block vanillaBlock = chunkData.getBlock(v);
            if (((v.y() > columnHeigth) || (isCustomWorldOreOutOfVanillaCaves(b, vanillaBlock))
                    || (v.y() > 30 && (vanillaBlock.isLiquid() || vanillaBlock.getName().equalsIgnoreCase("GRASS_BLOCK")
                            || vanillaBlock.getName().equalsIgnoreCase("SAND") || vanillaBlock.getName().equalsIgnoreCase("SAND_STONE")
                            || vanillaBlock.getName().equalsIgnoreCase("GRAVEL"))))
                    || (b.isAir() && !vanillaBlock.isLiquid())) {
                chunkData.setBlock(v, b);
            } else {
                if (cavesReader != null) {
                    b = cavesReader.blockAt(v).orElse(null);
                    if (b != null) {
                        chunkData.setBlock(v, b);
                    }
                }
            }

            // create ravines in biome that should have ravines
            if (v.y() == columnHeigth && vanillaBlock.isAir() && isRavinBiome(reader, v) && isAirCollumn(chunkData, v, 30)) {
                chunkData.setRegion(v.x(), columnHeigth, v.z(), v.x() + 1, airColumn, v.z() + 1, airBlock);
            }
            // Generator.addTime("Merge block or not", startTime);
        }
    }

    /** return the 1st block mergeDepth_ blocks under surface or heigth_ */
    private int getLowerBlockToRemove(Reader reader, int x, int z, Block defaultBlock) {
        int lbtr = this.height_ + mergeDepth_;
        while (!reader.blockAt(x, lbtr, z).orElse(defaultBlock).isSolid() && lbtr > -64) {
            lbtr--;
        }
        return lbtr - mergeDepth_;
    }

    @Override
    public void mergeBiomes(ChunkReader reader, ChunkData chunkData) {
        // No need to set biome for chunk. It's done by the generator.
    }


    // private --------------------------------------------------------------------------------------------------------
    /**
     * Return true if this block is a block to preserve from the custom world and a solid block in vanilla world (This avoid to have ores
     * floating in the air in vanilla caves).
     * 
     * @param b            the block to check if is ore
     * @param vanillaBlock the block in vanilla world
     */
    private boolean isCustomWorldOreOutOfVanillaCaves(Block b, Block vanillaBlock) {
        return keptReferenceWorldBlocks_.contains(b.getName().toUpperCase()) && (vanillaBlock == null || vanillaBlock.isSolid());
    }
    /** Return true if this biome need to be only custom world */
    private boolean isPreservedBiome(ChunkReader reader, Vector<Integer> v) {
        return this.preserveBiomes_.contains(reader.biomeAt(v).orElse(null));
    }

    private boolean isRavinBiome(ChunkReader reader, Vector<Integer> v) {
        return this.ravinBiomes_.contains(reader.biomeAt(v).orElse(null));
    }

    /** Return true if all the collumn is air. */
    private boolean isAirCollumn(ChunkData chunkData, Vector<Integer> v, int len) {
        for (int i = 0; i < len; i++) {
            if (!chunkData.getBlock(new IntVector(v.x(), v.y() - i, v.z())).isAir()) {
                return false;
            }
        }
        return true;
    }
}
