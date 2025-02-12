package com.jkantrell.mc.underilla.spigot.impl;

import org.bukkit.block.data.BlockData;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.core.api.ChunkData;

public class BukkitChunkData implements ChunkData {

    // FIELDS
    private org.bukkit.generator.ChunkGenerator.ChunkData chunkData;


    // CONSTRUCTORS
    public BukkitChunkData(org.bukkit.generator.ChunkGenerator.ChunkData chunkData) { this.chunkData = chunkData; }


    @Override
    public int getMinHeight() { return this.chunkData.getMinHeight(); }
    @Override
    public int getChunkX() { throw new UnsupportedOperationException(); }
    @Override
    public int getChunkZ() { throw new UnsupportedOperationException(); }

    @Override
    public Block getBlock(int x, int y, int z) {
        BlockData data = this.chunkData.getBlockData(x, y, z);
        return new BukkitBlock(data);
    }

    @Override
    public int getMaxHeight() { return this.chunkData.getMaxHeight(); }

    @Override
    public com.jkantrell.mc.underilla.core.api.Biome getBiome(int x, int y, int z) {
        return new BukkitBiome(this.chunkData.getBiome(x, y, z).getKey());
    }

    @Override
    public void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Block block) {
        if (!(block instanceof BukkitBlock bukkitBlock)) {
            return;
        }
        this.chunkData.setRegion(xMin, yMin, zMin, xMax, yMax, zMax, bukkitBlock.getBlockData());
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        if (!(block instanceof BukkitBlock bukkitBlock)) {
            return;
        }


        this.chunkData.setBlock(x, y, z, bukkitBlock.getBlockData());

        if (bukkitBlock.getMaterial().equals(org.bukkit.Material.SPAWNER)) {
            // // CraftBlock craftBlock = ((CraftBlock) world.getBlockAt(x, y, z));
            // // world.getBlockAt(x, y, z).getState().update();
            // org.bukkit.block.Block bblock = world.getBlockAt(x, y, z);
            // bblock.getState().setType(org.bukkit.Material.SPAWNER);
            // bblock.getState().update(true, true);
            // CraftBlock craftBlock = (CraftBlock) bblock;
            // CraftBlockState blockState = (CraftBlockState) craftBlock.getState();


            // Underilla.info("setBlock: Spawner block detected at " + x + ", " + y + ", " + z + " with class " + bblock.getClass()
            // + ", material " + bblock.getType() + ", state " + bblock.getState() + ", blockData " + bblock.getBlockData()
            // + ", blockData class " + bblock.getBlockData().getClass() + ", blockData material "
            // + bblock.getBlockData().getMaterial() + ", blockData class " + bblock.getBlockData().getClass());
            // // CreatureSpawner creatureSpawner = new CraftCreatureSpawner(world,
            // // new SpawnerBlockEntity(craftBlock.getPosition(), craftBlock.getState()));
            // // creatureSpawner.

            // // TODO it's not a CreatureSpawner, it's stay at the previous block type (Deepslate, mostly).
            // if (bblock.getState() instanceof CreatureSpawner creatureSpawner) {
            // creatureSpawner.setSpawnedType(bukkitBlock.getSpawnedType().orElse(org.bukkit.entity.EntityType.ZOMBIE));
            // // creatureSpawner.update();
            // Underilla.info("setBlock: Spawner type set to " + creatureSpawner.getSpawnedType());
            // }
        }

    }

    @Override
    public void setBiome(int x, int y, int z, com.jkantrell.mc.underilla.core.api.Biome biome) {
        // No need to set biome for chunk. It's done by the generator.
    }
}
