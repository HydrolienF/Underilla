package fr.formiko.mc.underilla.core.api;

import fr.formiko.mc.underilla.core.vector.Vector;

public interface ChunkData {

    int getMaxHeight();
    int getMinHeight();
    int getChunkX();
    int getChunkZ();
    Block getBlock(int x, int y, int z);
    default Block getBlock(Vector<Integer> pos) { return this.getBlock(pos.x(), pos.y(), pos.z()); }
    Biome getBiome(int x, int y, int z);
    default Biome getBiome(Vector<Integer> pos) { return this.getBiome(pos.x(), pos.y(), pos.z()); }
    void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Block material);
    void setBlock(int x, int y, int z, Block block);
    default void setBlock(Vector<Integer> pos, Block block) { this.setBlock(pos.x(), pos.y(), pos.z(), block); }
    void setBiome(int x, int y, int z, Biome biome);
    default void setBiome(Vector<Integer> pos, Biome biome) { this.setBiome(pos.x(), pos.y(), pos.z(), biome); }

}
