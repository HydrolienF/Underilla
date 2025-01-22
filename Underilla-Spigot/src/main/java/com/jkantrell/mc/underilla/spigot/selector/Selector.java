package com.jkantrell.mc.underilla.spigot.selector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import com.jkantrell.mc.underilla.spigot.Underilla;

// Upgraded version from WorldSelectorH.
// TODO test if we can improve performance by iterating region per region instead of moving one x and one z at a time. And inside each region, iterate by chunks.
public class Selector implements Serializable {
    private final int xMin;
    private final int zMin;
    private final int yMin; // = -64
    private final int yMax; // = 319
    private final int xMax;
    private final int zMax;
    private Vector3 currentBlock;
    private long processedBlocks = 0l;
    private final int blocksPerColumn;
    private final int blockPerChunk;
    private final UUID worldUUID;

    /**
     * Create a new selector
     * 
     * @param x1        The first x coordinate (inclusive)
     * @param y1        The first y coordinate (inclusive)
     * @param z1        The first z coordinate (inclusive)
     * @param x2        The second x coordinate (exclusive)
     * @param y2        The second y coordinate (exclusive)
     * @param z2        The second z coordinate (exclusive)
     * @param worldUUID The world UUID
     */
    public Selector(int x1, int y1, int z1, int x2, int y2, int z2, UUID worldUUID) {
        xMin = Math.min(x1, x2);
        yMin = Math.min(y1, y2);
        zMin = Math.min(z1, z2);
        // Minus one to exclude the last block
        xMax = Math.max(x1, x2) - 1;
        yMax = Math.max(y1, y2) - 1;
        zMax = Math.max(z1, z2) - 1;
        this.worldUUID = worldUUID;
        currentBlock = new Vector3(xMin, yMin, zMin);

        blocksPerColumn = yMax - yMin + 1;
        blockPerChunk = blocksPerColumn * Underilla.CHUNK_SIZE * Underilla.CHUNK_SIZE;
    }
    public Selector(Selector selector) {
        this(selector.xMin, selector.yMin, selector.zMin, selector.xMax, selector.yMax, selector.zMax, selector.worldUUID);
    }

    public long getColumnsCount() { return (xMax - xMin + 1l) * (zMax - zMin + 1l); }
    public long getBlocksCount() { return getColumnsCount() * blocksPerColumn; }
    public double progress() { return (double) processedBlocks / (double) getBlocksCount(); }
    public UUID getWorldUUID() { return worldUUID; }
    public World getWorld() { return Bukkit.getWorld(worldUUID); }


    public boolean hasNextBlock() { return processedBlocks < getBlocksCount(); }
    public boolean hasNextColumn() { return processedBlocks < getBlocksCount(); }
    public Block nextBlock() {
        Block b = getWorld().getBlockAt(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ());
        processedBlocks++;
        currentBlock.setY(currentBlock.getY() + 1);
        if (currentBlock.getY() > yMax) {
            currentBlock.setY(yMin);
            currentBlock.setZ(currentBlock.getZ() + 1);
            if (currentBlock.getZ() > zMax) {
                currentBlock.setZ(zMin);
                currentBlock.setX(currentBlock.getX() + 1);
                // if (currentBlock.getX() > xMax) {
                // return null;
                // }
            }
        }
        return b;
    }
    public Block nextColumn() {
        Block b = getWorld().getBlockAt(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ());
        processedBlocks += blocksPerColumn;
        currentBlock.setY(yMin);
        currentBlock.setZ(currentBlock.getZ() + 1);
        if (currentBlock.getZ() > zMax) {
            currentBlock.setZ(zMin);
            currentBlock.setX(currentBlock.getX() + 1);
            // if (currentBlock.getX() > xMax) {
            // return null;
            // }
        }
        return b;
    }
    public Chunk nextChunk() {
        Chunk c = getWorld().getChunkAt(currentBlock.getX() / Underilla.CHUNK_SIZE, currentBlock.getZ() / Underilla.CHUNK_SIZE);
        processedBlocks += blockPerChunk;
        currentBlock.setZ(currentBlock.getZ() + Underilla.CHUNK_SIZE);
        if (currentBlock.getZ() - (currentBlock.getZ() % Underilla.CHUNK_SIZE) > zMax) {
            currentBlock.setZ(zMin);
            currentBlock.setX(currentBlock.getX() + Underilla.CHUNK_SIZE);
            // if (currentBlock.getX() - (currentBlock.getX() % Underilla.CHUNK_SIZE) > xMax) {
            // return null;
            // }
        }
        return c;
    }

    // Save to file & load from file
    public void saveIn(String path) {
        // Use serialization to save the selector
        File file = new File(getSaveFolder(), path + ".dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
            Underilla.info("Selector saved to file: " + file.getAbsolutePath());
        } catch (IOException e) {
            Underilla.error("Error while saving selector to file: " + file.getAbsolutePath());
        }
    }
    public static Selector loadFrom(String path) {
        File file = new File(Underilla.getInstance().getDataFolder(), path + ".dat");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Selector selector = (Selector) ois.readObject();
            Underilla.info("Selector loaded from file: " + file.getAbsolutePath());
            return selector;
        } catch (IOException | ClassNotFoundException e) {
            Underilla.error("Error while loading selector from file: " + file.getAbsolutePath());
        }
        return null;
    }
    private File getSaveFolder() { return Underilla.getInstance().getDataFolder(); }
}
