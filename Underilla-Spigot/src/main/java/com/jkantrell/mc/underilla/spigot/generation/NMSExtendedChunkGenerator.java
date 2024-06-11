package com.jkantrell.mc.underilla.spigot.generation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

public class NMSExtendedChunkGenerator extends ChunkGenerator {

    private final ChunkGenerator vanillaChunkGenerator;

    public NMSExtendedChunkGenerator(ChunkGenerator vanillaChunkGenerator, BiomeSource biomeSource) {
        super(biomeSource);
        this.vanillaChunkGenerator = vanillaChunkGenerator;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        try {
            Method method = vanillaChunkGenerator.getClass().getDeclaredMethod("codec");
            method.setAccessible(true);
            return (MapCodec<? extends ChunkGenerator>) method.invoke(vanillaChunkGenerator);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion chunkRegion, long seed, RandomState noiseConfig, BiomeManager biomeAccess,
            StructureManager structureAccessor, ChunkAccess chunk, Carving carverStep) {
        vanillaChunkGenerator.applyCarvers(chunkRegion, seed, noiseConfig, biomeAccess, structureAccessor, chunk, carverStep);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState noiseConfig, ChunkAccess chunk) {
        vanillaChunkGenerator.buildSurface(region, structures, noiseConfig, chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) { vanillaChunkGenerator.spawnOriginalMobs(region); }

    @Override
    public int getGenDepth() { return vanillaChunkGenerator.getGenDepth(); }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState noiseConfig,
            StructureManager structureAccessor, ChunkAccess chunk) {

        return vanillaChunkGenerator.fillFromNoise(executor, blender, noiseConfig, structureAccessor, chunk);
    }

    @Override
    public int getSeaLevel() { return vanillaChunkGenerator.getSeaLevel(); }

    @Override
    public int getMinY() { return vanillaChunkGenerator.getMinY(); }

    @Override
    public int getBaseHeight(int x, int z, Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
        return vanillaChunkGenerator.getBaseHeight(x, z, heightmap, world, noiseConfig);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig) {
        return vanillaChunkGenerator.getBaseColumn(x, z, world, noiseConfig);
    }

    @Override
    public void addDebugScreenInfo(List<String> text, RandomState noiseConfig, BlockPos pos) {
        vanillaChunkGenerator.addDebugScreenInfo(text, noiseConfig, pos);
    }

}
