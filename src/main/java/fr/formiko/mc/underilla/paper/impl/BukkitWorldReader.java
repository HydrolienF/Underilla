package fr.formiko.mc.underilla.paper.impl;

import com.jkantrell.mca.Chunk;
import fr.formiko.mc.underilla.core.reader.ChunkReader;
import java.io.File;

public class BukkitWorldReader extends fr.formiko.mc.underilla.core.reader.WorldReader {

    // CONSTRUCTORS
    public BukkitWorldReader(String worldPath) throws NoSuchFieldException { super(worldPath); }
    public BukkitWorldReader(String worldPath, int cacheSize) throws NoSuchFieldException { super(worldPath, cacheSize); }
    public BukkitWorldReader(File worldDir) throws NoSuchFieldException { super(worldDir); }
    public BukkitWorldReader(File worldDir, int cacheSize) throws NoSuchFieldException { super(worldDir, cacheSize); }


    // IMPLEMENTATIONS
    @Override
    protected ChunkReader newChunkReader(Chunk chunk) { return new BukkitChunkReader(chunk); }
}
