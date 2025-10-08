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
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;

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


    public static void cleanBlock(Block currentBlock, LevelReader levelReader) {
        Material startMaterial = currentBlock.getType();

        // If there is no block to support, do not load the underCurrentBlock to save time
        if (!Underilla.getUnderillaConfig().getMapMaterial(MapMaterialKeys.CLEAN_BLOCK_TO_SUPPORT).isEmpty()) {
            Block underCurrentBlock = currentBlock.getRelative(BlockFace.DOWN);
            if (!underCurrentBlock.isSolid() && !currentBlock.isEmpty()) {
                // if currentBlock is a block to support (sand, gravel, etc) then replace it by the support block
                Material toSupport = Underilla.getUnderillaConfig().getMaterialFromMap(MapMaterialKeys.CLEAN_BLOCK_TO_SUPPORT,
                        startMaterial);
                if (toSupport != null) {
                    currentBlock.setType(toSupport);
                }
            }
        }


        // Replace currentBlock by an other one if it is need.
        Material toReplace = Underilla.getUnderillaConfig().getMaterialFromMap(MapMaterialKeys.CLEAN_BLOCK_TO_REPLACE, startMaterial);
        if (toReplace != null) {
            currentBlock.setType(toReplace);
        }

        // Check with NMS that the block is stable, else remove it.
        if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.CLEAN_BLOCKS_REMOVE_UNSTABLE_BLOCKS)) {
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
