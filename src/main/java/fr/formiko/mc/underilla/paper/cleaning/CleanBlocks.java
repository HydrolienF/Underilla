package fr.formiko.mc.underilla.paper.cleaning;

import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.BooleanKeys;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.MapMaterialKeys;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

public class CleanBlocks {
    private static Set<Material> returnToDirt = Set.of(Material.GRASS_BLOCK, Material.PODZOL, Material.DIRT_PATH);

    public static void cleanBlocks(Chunk chunk) {
        World world = chunk.getWorld();
        LevelReader levelReader = ((CraftWorld) world).getHandle();

        int startX = chunk.getX() << 4;
        int startZ = chunk.getZ() << 4;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    cleanBlock(world.getBlockAt(startX + x, y, startZ + z), levelReader);
                }
            }
        }
    }

    public static void cleanBlocks(WorldInfo worldInfo, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
        int minY = worldInfo.getMinHeight();
        int maxY = worldInfo.getMaxHeight();
        int minX = limitedRegion.getCenterBlockX() - limitedRegion.getBuffer() / 2;
        int maxX = limitedRegion.getCenterBlockX() + limitedRegion.getBuffer() / 2;
        int minZ = limitedRegion.getCenterBlockZ() - limitedRegion.getBuffer() / 2;
        int maxZ = limitedRegion.getCenterBlockZ() + limitedRegion.getBuffer() / 2;

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (!limitedRegion.isInRegion(x, y, z)) {
                        Underilla.warning("Block " + x + ", " + y + ", " + z + " is not in the region " + limitedRegion.getBuffer());
                        continue;
                    }
                    BlockData blockData = limitedRegion.getBlockData(x, y, z);
                    Material startMaterial = blockData.getMaterial();

                    // If there is no block to support, do not load the underCurrentBlock to save time
                    if (!Underilla.getUnderillaConfig().getMapMaterial(MapMaterialKeys.CLEAN_BLOCK_TO_SUPPORT).isEmpty() && y > minY) {
                        // Block underCurrentBlock = currentBlock.getRelative(BlockFace.DOWN);
                        BlockData underCurrentBlock = limitedRegion.getBlockData(x, y - 1, z);
                        if (!underCurrentBlock.getMaterial().isSolid() && !(startMaterial == Material.AIR)) {
                            // if currentBlock is a block to support (sand, gravel, etc) then replace it by the support block
                            Material toSupport = Underilla.getUnderillaConfig().getMaterialFromMap(MapMaterialKeys.CLEAN_BLOCK_TO_SUPPORT,
                                    startMaterial);
                            if (toSupport != null) {
                                limitedRegion.setBlockData(x, y, z, toSupport.createBlockData());
                            }
                        }
                    }


                    Material toReplace = Underilla.getUnderillaConfig().getMaterialFromMap(MapMaterialKeys.CLEAN_BLOCK_TO_REPLACE,
                            startMaterial);
                    if (toReplace != null) {
                        limitedRegion.setBlockData(x, y, z, toReplace.createBlockData());
                    }
                }
            }
        }
    }


    public static void cleanBlock(Block currentBlock, LevelReader levelReader) {
        Material startMaterial = currentBlock.getType();
        // Check with NMS that the block is stable, else remove it. (Need to be done once the chunk have been generated, else we can't use
        // canSurvive())
        if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.CLEAN_BLOCKS_REMOVE_UNSTABLE_BLOCKS)) {
            //
            CleanBlocks.removeUnstableBlock(currentBlock, startMaterial, levelReader);
        }

        // Final transformation that can be override by other plugins
        if (Underilla.getInstance().hasEndBlockTransformer()) {
            Underilla.getInstance().getEndBlockTransformer().accept(currentBlock);
        }
    }

    public static void removeUnstableBlock(Block currentBlock, Material currentBlockMaterial, LevelReader levelReader) {
        BlockPos blockPos = new BlockPos(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ());
        net.minecraft.world.level.block.state.BlockState blockState = levelReader.getBlockState(blockPos);
        if (!blockState.canSurvive(levelReader, blockPos)) {
            if (returnToDirt.contains(currentBlockMaterial)) {
                currentBlock.setType(Material.DIRT);
            } else {
                currentBlock.breakNaturally();
            }
        }
    }
}

