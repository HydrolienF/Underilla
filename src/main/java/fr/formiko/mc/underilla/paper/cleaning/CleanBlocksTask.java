package fr.formiko.mc.underilla.paper.cleaning;

import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.BooleanKeys;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.MapMaterialKeys;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.StringKeys;
import fr.formiko.mc.underilla.paper.selector.Selector;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.scheduler.BukkitRunnable;

public class CleanBlocksTask extends FollowableProgressTask {
    private LevelReader levelReader;
    public CleanBlocksTask(int taskID, int tasksCount) {
        super(taskID, tasksCount);
        levelReader = ((CraftWorld) Bukkit.getWorld(selector.getWorldUUID())).getHandle();
    }
    public CleanBlocksTask(int taskID, int tasksCount, Selector selector) {
        super(taskID, tasksCount, selector);
        levelReader = ((CraftWorld) Bukkit.getWorld(selector.getWorldUUID())).getHandle();
    }

    public void run() {
        final long startTime = System.currentTimeMillis();
        final Map<Material, Map<Material, Long>> replacedBlock = new EnumMap<>(Material.class);
        final Map<Material, Long> finalBlock = new EnumMap<>(Material.class);
        new BukkitRunnable() {
            private long processedBlocks = 0;
            @Override
            public void run() {
                long execTime = System.currentTimeMillis();

                while (execTime + 45 > System.currentTimeMillis() && selector.hasNextBlock() && !stop) {
                    Block currentBlock = selector.nextBlock();
                    Block underCurrentBlock = currentBlock.getRelative(BlockFace.DOWN);
                    Material startMaterial = currentBlock.getType();


                    // if (underCurrentBlock.isEmpty() && !currentBlock.isEmpty()) {
                    if (!underCurrentBlock.isSolid() && !currentBlock.isEmpty()) {
                        // if currentBlock is a block to support (sand, gravel, etc)
                        // replave it by the support block
                        Material toSupport = Underilla.getUnderillaConfig().getMaterialFromMap(MapMaterialKeys.CLEAN_BLOCK_TO_SUPPORT,
                                startMaterial);
                        if (toSupport != null) {
                            currentBlock.setType(toSupport);
                        }
                    }

                    // Replace currentBlock by an other one if it is need.
                    Material toReplace = Underilla.getUnderillaConfig().getMaterialFromMap(MapMaterialKeys.CLEAN_BLOCK_TO_REPLACE,
                            startMaterial);
                    if (toReplace != null) {
                        currentBlock.setType(toReplace);
                    }

                    // Check with NMS that the block is stable, else remove it.
                    if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.CLEAN_BLOCKS_REMOVE_UNSTABLE_BLOCKS)) {
                        removeUnstableBlock(currentBlock, startMaterial);
                    }

                    // Final transformation that can be override by other plugins
                    if (Underilla.getInstance().hasEndBlockTransformer()) {
                        Underilla.getInstance().getEndBlockTransformer().accept(currentBlock);
                    }


                    // Keep track of removed and final blocks
                    Material finalMaterial = currentBlock.getType();
                    if (startMaterial != finalMaterial) {
                        // add 1 finalMaterial to replacedBlock
                        if (!replacedBlock.containsKey(finalMaterial)) {
                            replacedBlock.put(finalMaterial, new EnumMap<>(Material.class));
                        }
                        replacedBlock.get(finalMaterial).put(startMaterial,
                                replacedBlock.get(finalMaterial).getOrDefault(startMaterial, 0L) + 1L);
                        processedBlocks++;
                    }
                    finalBlock.put(finalMaterial, finalBlock.getOrDefault(finalMaterial, 0L) + 1L);
                }

                if (selector == null || selector.progress() >= 1 || stop) {
                    printProgress(processedBlocks, startTime);

                    Underilla.info(
                            "Cleaning blocks task " + taskID + " finished in " + Duration.ofMillis(System.currentTimeMillis() - startTime));
                    Underilla.info("Replaced blocks: " + replacedBlock);
                    Underilla.info("Final blocks: " + finalBlock);
                    cancel();
                    Underilla.getInstance().validateTask(StringKeys.STEP_CLEANING_BLOCKS);
                    return;
                } else {
                    printProgressIfNeeded(processedBlocks, startTime);
                }

            }

            private Set<Material> returnToDirt = Set.of(Material.GRASS_BLOCK, Material.PODZOL, Material.DIRT_PATH);
            // private Set<Material> returnToWater = Set.of(Material.FERN, Material.LARGE_FERN, Material.SEAGRASS, Material.KELP_PLANT);
            private void removeUnstableBlock(Block currentBlock, Material currentBlockMaterial) {
                BlockPos blockPos = new BlockPos(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ());
                net.minecraft.world.level.block.state.BlockState blockState = levelReader.getBlockState(blockPos);
                if (!blockState.canSurvive(levelReader, blockPos)) {
                    if (returnToDirt.contains(currentBlockMaterial)) {
                        currentBlock.setType(Material.DIRT);
                        // if is in water
                        // } else if (currentBlock.getBlockData() instanceof org.bukkit.block.data.Waterlogged waterLoggedData
                        // && waterLoggedData.isWaterlogged()) {
                        // currentBlock.setType(Material.WATER);
                    } else {
                        // currentBlock.setType(Material.AIR);
                        currentBlock.breakNaturally();
                    }
                }
            }

        }.runTaskTimer(Underilla.getInstance(), 0, 1);
    }


}
