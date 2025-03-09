package fr.formiko.mc.underilla.core.generation;

import fr.formiko.mc.underilla.core.api.Block;
import fr.formiko.mc.underilla.core.api.ChunkData;
import fr.formiko.mc.underilla.core.reader.ChunkReader;
import fr.formiko.mc.underilla.core.reader.WorldReader;
import fr.formiko.mc.underilla.core.vector.Vector;
import fr.formiko.mc.underilla.core.vector.VectorIterable;
import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.impl.BukkitBlock;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.IntegerKeys;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.MapMaterialKeys;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.SetMaterialKeys;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Material;

public class AbsoluteMerger implements Merger {

    // FIELDS
    private final WorldReader worldSurfaceReader;

    // CONSTRUCTORS
    AbsoluteMerger(WorldReader worldSurfaceReader) { this.worldSurfaceReader = worldSurfaceReader; }


    // IMPLEMENTATIONS
    @Override
    public void mergeLand(@Nonnull ChunkReader surfaceReader, @Nonnull ChunkData chunkData, @Nullable ChunkReader cavesReader) {
        long startTime = System.currentTimeMillis();
        int airColumn = surfaceReader.airSectionsBottom();
        chunkData.setRegion(0, airColumn, 0, Underilla.CHUNK_SIZE, chunkData.getMaxHeight(), Underilla.CHUNK_SIZE, BukkitBlock.AIR);

        VectorIterable iterable = new VectorIterable(0, Underilla.CHUNK_SIZE,
                Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_Y), airColumn, 0, Underilla.CHUNK_SIZE);
        int columnHeigth = Underilla.getUnderillaConfig().getInt(IntegerKeys.MAX_HEIGHT_OF_CAVES);
        int lastX = -1;
        int lastZ = -1;
        Generator.addTime("Create VectorIterable to merge land", startTime);
        for (Vector<Integer> v : iterable) {
            startTime = System.currentTimeMillis();
            Block customBlock = surfaceReader.blockAt(v).orElse(BukkitBlock.AIR);
            customBlock = replaceSurfaceBlockIfNecessary(customBlock);
            Generator.addTime("Read block data from custom world", startTime);
            startTime = System.currentTimeMillis();
            Block vanillaBlock = cavesReader == null ? chunkData.getBlock(v) : cavesReader.blockAt(v).orElse(BukkitBlock.AIR);
            Generator.addTime("Read block data from vanilla world", startTime);

            // For every collumn of bloc calculate the lower block to remove that migth be lower than height_.
            startTime = System.currentTimeMillis();
            if (v.x() != lastX || v.z() != lastZ) {
                lastX = v.x();
                lastZ = v.z();
                columnHeigth = getLowerBlockToRemove(surfaceReader.getGlobalX(v.x()), surfaceReader.getGlobalZ(v.z()));
            }
            Generator.addTime("Calculate lower block to remove", startTime);

            startTime = System.currentTimeMillis();
            // Place the custom world bloc over 55 (or -64 if is preseved biome) or if it is a custom ore or if it is air,
            // or if vanilla world have watter or grass or sand over 30
            // and do not replace liquid vanilla blocks by air. (to preserve water and lava lackes)
            if (((v.y() > columnHeigth) // block over surface or close to surface are kept from custom surface world.
                    || (isCustomWorldOreOutOfVanillaCaves(customBlock, vanillaBlock)) // custom world ores are kept from custom world.
            // No need to remove surface block anymore since we can use datapack to have much higher caves.
            )
            // // Keep custom block if it's air to preserve custom world caves if there is any. (If vanilla block is liquid, we
            // // preserve vanilla block as we want to avoid holes in vanilla underground lakes)
            // || (customBlock.isAir() && !vanillaBlock.isLiquid())
            ) {
                // Use custom block
                chunkData.setBlock(v, customBlock);
            } else {
                if (cavesReader != null) {
                    // Use vanilla from caves world
                    chunkData.setBlock(v, vanillaBlock);
                }
                // Use vanilla block from current world
            }

            Generator.addTime("Merge block or not", startTime);
        }
    }

    /** return the 1st block mergeDepth_ blocks under surface or heigth_ */
    private int getLowerBlockToRemove(int x, int z) {
        return worldSurfaceReader.getLowerBlockOfSurfaceWorldYLevel(x, z);
    }

    // private --------------------------------------------------------------------------------------------------------
    /**
     * Return true if this block is a block to preserve from the custom world and a solid block in vanilla world (This avoid to have ores
     * floating in the air in vanilla caves).
     * 
     * @param customBlock  the block to check if is ore
     * @param vanillaBlock the block in vanilla world
     */
    private boolean isCustomWorldOreOutOfVanillaCaves(Block customBlock, Block vanillaBlock) {
        return (Underilla.getUnderillaConfig().isMaterialInSet(SetMaterialKeys.BLOCK_TO_KEEP_FROM_SURFACE_WORLD_IN_CAVES,
                Material.getMaterial(customBlock.getName().toUpperCase())) && (vanillaBlock == null || vanillaBlock.isSolid()));
    }

    private Block replaceSurfaceBlockIfNecessary(Block block) {
        Material replaceWith = Underilla.getUnderillaConfig().getMaterialFromMap(MapMaterialKeys.SURFACE_WORLD_BLOCK_TO_REPLACE,
                Material.getMaterial(block.getName().toUpperCase()));
        if (replaceWith != null) {
            return new BukkitBlock(replaceWith.createBlockData());
        }
        return block;
    }
}
