package fr.formiko.mc.underilla.core.generation;

import fr.formiko.mc.underilla.core.api.ChunkData;
import fr.formiko.mc.underilla.core.reader.ChunkReader;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The Merger interface is used to merge the custom world with the vanilla world.
 * Only blocks need to be merged here, biomes are merged in `UnderillaChunkGenerator.BiomeProviderFromFile` class.
 */
interface Merger {
    void mergeLand(@Nonnull ChunkReader reader, @Nonnull ChunkData chunkData, @Nullable ChunkReader cavesReader);
}
