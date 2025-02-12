package com.jkantrell.mc.underilla.spigot.impl;

import fr.formiko.mc.biomeutils.NMSBiomeUtils;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;
import com.jkantrell.mc.underilla.core.api.Biome;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.core.api.ChunkData;
import com.jkantrell.mc.underilla.core.vector.VectorIterable;
import com.jkantrell.mc.underilla.spigot.Underilla;


public class BukkitRegionChunkData implements ChunkData {

    // FIELDS
    private final LimitedRegion region_;
    private final int minHeight_, maxHeight_, chunkX_, chunkZ_, absX_, absZ_;


    // CONSTRUCTORS
    public BukkitRegionChunkData(LimitedRegion region, int chunkX, int chunkZ, int minHeight, int maxHeight) {
        this.region_ = region;
        this.minHeight_ = minHeight;
        this.maxHeight_ = maxHeight;
        this.chunkX_ = chunkX;
        this.chunkZ_ = chunkZ;
        this.absX_ = this.chunkX_ * 16;
        this.absZ_ = this.chunkZ_ * 16;
    }


    // GETTERS
    public LimitedRegion getRegion() { return this.region_; }


    // IMPLEMENTATIONS
    @Override
    public int getMaxHeight() { return this.maxHeight_; }
    @Override
    public int getMinHeight() { return this.minHeight_; }
    public int getChunkX() { return this.chunkX_; }
    public int getChunkZ() { return this.chunkZ_; }
    @Override
    public Block getBlock(int x, int y, int z) {
        // TODO also save state of block if needed (for structure chests).
        // this.region_.getBlockState(this.absX_ + x, y, this.absZ_ + z);
        BlockData d = this.region_.getBlockData(this.absX_ + x, y, this.absZ_ + z);
        return new BukkitBlock(d);
    }
    @Override
    public Biome getBiome(int x, int y, int z) {
        org.bukkit.block.Biome b = this.region_.getBiome(this.absX_ + x, y, this.absZ_ + z);
        return new BukkitBiome(b.name());
    }
    @Override
    public void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Block block) {
        new VectorIterable(xMin, xMax, yMin, yMax, zMin, zMax).forEach(v -> this.setBlock(v, block));
    }
    @Override
    public void setBlock(int x, int y, int z, Block block) {
        if (!(block instanceof BukkitBlock bukkitBlock)) {
            return;
        }
        this.region_.setBlockData(this.absX_ + x, y, this.absZ_ + z, bukkitBlock.getBlockData());
        // TODO nexts lines are never called
        if (bukkitBlock.getSpawnedType().isPresent()) {
            if (region_.getWorld().getBlockAt(this.absX_ + x, y,
                    this.absZ_ + z) instanceof org.bukkit.block.CreatureSpawner creatureSpawner) {
                creatureSpawner.setSpawnedType(bukkitBlock.getSpawnedType().get());
                creatureSpawner.update();
                Underilla.info("\nSet spawner type to " + bukkitBlock.getSpawnedType().get());
            }
        }
    }
    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        if (!(biome instanceof BukkitBiome bukkitBiome)) {
            return;
        }
        // this.region_.setBiome(this.absX_ + x, y, this.absZ_ + z, bukkitBiome.getBiome());
        NMSBiomeUtils.setCustomBiome(bukkitBiome.getName(), this.absX_ + x, y, this.absZ_ + z, this.region_.getWorld());
    }
}
