package com.jkantrell.mc.underilla.core.generation;

import com.jkantrell.mc.underilla.core.api.ChunkData;
import com.jkantrell.mc.underilla.core.reader.ChunkReader;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

interface Merger {

    // void merge(ChunkReader reader, ChunkData chunkData);
    void mergeLand(@Nonnull ChunkReader reader, @Nonnull ChunkData chunkData, @Nullable ChunkReader cavesReader);
}
