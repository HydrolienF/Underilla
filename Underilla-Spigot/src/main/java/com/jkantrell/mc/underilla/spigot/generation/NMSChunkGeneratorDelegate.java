// package com.jkantrell.mc.underilla.spigot.generation;

// import java.util.List;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.Executor;
// import javax.annotation.Nonnull;
// import com.jkantrell.mc.underilla.spigot.Underilla;
// import com.mojang.serialization.Codec;
// import com.mojang.serialization.MapCodec;
// import net.minecraft.core.BlockPos;
// import net.minecraft.server.level.WorldGenRegion;
// import net.minecraft.world.level.LevelHeightAccessor;
// import net.minecraft.world.level.NoiseColumn;
// import net.minecraft.world.level.StructureManager;
// import net.minecraft.world.level.WorldGenLevel;
// import net.minecraft.world.level.biome.BiomeManager;
// import net.minecraft.world.level.chunk.ChunkAccess;
// import net.minecraft.world.level.chunk.ChunkGenerator;
// import net.minecraft.world.level.levelgen.GenerationStep.Carving;
// import net.minecraft.world.level.levelgen.Heightmap.Types;
// import net.minecraft.world.level.levelgen.RandomState;
// import net.minecraft.world.level.levelgen.blending.Blender;


// public class NMSChunkGeneratorDelegate extends ChunkGenerator {
// // private final com.dfsek.terra.api.world.chunk.generation.ChunkGenerator delegate;

// private final ChunkGenerator vanilla;

// public NMSChunkGeneratorDelegate(ChunkGenerator vanilla) {
// super(vanilla.getBiomeSource());
// // this.delegate = pack.getGeneratorProvider().newInstance(pack);
// this.vanilla = vanilla;
// }

// @Override
// protected @Nonnull MapCodec<? extends ChunkGenerator> codec() { return ChunkGenerator.CODEC; }

// @Override
// public void applyCarvers(@Nonnull WorldGenRegion chunkRegion, long seed, @Nonnull RandomState noiseConfig, @Nonnull BiomeManager world,
// @Nonnull StructureManager structureAccessor, @Nonnull ChunkAccess chunk, @Nonnull Carving carverStep) {
// vanilla.applyCarvers(chunkRegion, seed, noiseConfig, world, structureAccessor, chunk, carverStep);
// }

// @Override
// public void buildSurface(@Nonnull WorldGenRegion region, @Nonnull StructureManager structures, @Nonnull RandomState noiseConfig,
// @Nonnull ChunkAccess chunk) {
// vanilla.buildSurface(region, structures, noiseConfig, chunk);
// }

// @Override
// public void applyBiomeDecoration(@Nonnull WorldGenLevel world, @Nonnull ChunkAccess chunk,
// @Nonnull StructureManager structureAccessor) {
// Underilla.getInstance().getLogger().info("Applying biome decoration");
// vanilla.applyBiomeDecoration(world, chunk, structureAccessor);
// }

// @Override
// public void spawnOriginalMobs(@Nonnull WorldGenRegion region) { vanilla.spawnOriginalMobs(region); }

// @Override
// public int getGenDepth() { return vanilla.getGenDepth(); }

// @Override
// public @Nonnull CompletableFuture<ChunkAccess> fillFromNoise(@Nonnull Executor executor, @Nonnull Blender blender,
// @Nonnull RandomState noiseConfig, @Nonnull StructureManager structureAccessor, @Nonnull ChunkAccess chunk) {
// return vanilla.fillFromNoise(executor, blender, noiseConfig, structureAccessor, chunk);
// }

// @Override
// public int getSeaLevel() { return vanilla.getSeaLevel(); }

// @Override
// public int getMinY() { return vanilla.getMinY(); }

// @Override
// public int getBaseHeight(int x, int z, @Nonnull Types heightmap, @Nonnull LevelHeightAccessor world, @Nonnull RandomState noiseConfig) {
// return vanilla.getBaseHeight(x, z, heightmap, world, noiseConfig);
// }

// @Override
// public @Nonnull NoiseColumn getBaseColumn(int x, int z, @Nonnull LevelHeightAccessor world, @Nonnull RandomState noiseConfig) {
// return vanilla.getBaseColumn(x, z, world, noiseConfig);
// }

// @Override
// public void addDebugScreenInfo(@Nonnull List<String> text, @Nonnull RandomState noiseConfig, @Nonnull BlockPos pos) {
// vanilla.addDebugScreenInfo(text, noiseConfig, pos);
// }
// }
