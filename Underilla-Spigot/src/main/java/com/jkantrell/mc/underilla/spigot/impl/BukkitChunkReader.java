package com.jkantrell.mc.underilla.spigot.impl;

import java.util.Optional;
import org.bukkit.Material;
import com.jkantrell.mc.underilla.core.api.Biome;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.core.reader.ChunkReader;
import com.jkantrell.mc.underilla.core.reader.TagInterpreter;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.io.Tools;
import com.jkantrell.mca.Chunk;
import com.jkantrell.nbt.tag.CompoundTag;
import com.jkantrell.nbt.tag.StringTag;

public class BukkitChunkReader extends ChunkReader {
    // private Map<String, org.bukkit.block.Biome> customBiomes = new HashMap<>();

    // CONSTRUCTORS
    public BukkitChunkReader(Chunk chunk) { super(chunk); }


    // IMPLEMENTATION
    @Override
    public Optional<Block> blockFromTag(CompoundTag tag) {
        Material m = Optional.ofNullable(tag).map(t -> t.getString("Name")).map(Material::matchMaterial).orElse(null);
        if (m == null) {
            return Optional.empty();
        }

        CompoundTag properties = tag != null ? tag.getCompoundTag("Properties") : null;
        BukkitBlock block;
        if (properties == null) {
            block = new BukkitBlock(m.createBlockData());
            return Optional.of(block);
        } else {
            block = null;
        }

        // IllegalArgumentException might be thrown if block data is not compatible with current version of Minecraft
        // In such case, return plain block with no data
        try {
            String dataString = TagInterpreter.COMPOUND.interpretBlockDataString(properties);
            // if (dataString != null && dataString.length() > 90) {
            // Underilla.info(dataString);
            // }
            if (m.equals(Material.VINE)) {
                dataString = removeVineDownProperty(dataString);
            }
            block = new BukkitBlock(m.createBlockData(dataString));
        } catch (IllegalArgumentException e) {
            Underilla.warning("Failed to create block data " + m + ": " + e.getMessage());
            Underilla.warning(() -> Tools.exceptionToString(e));
            block = new BukkitBlock(m.createBlockData());
        }
        return Optional.of(block);
    }

    // From minecraft:vine[west=false,east=false,up=false,south=false,down=false,north=true]
    // To minecraft:vine[west=false,east=false,up=false,south=false,north=true]
    private String removeVineDownProperty(String dataString) { return dataString.replace(",down=false", ""); }

    // [21:29:47 INFO]: [Underilla] [STDOUT] Loaded Block Entities:
    // {"type":"ListTag","value":{"type":"CompoundTag","list":[{"MaxNearbyEntities":{"type":"ShortTag","value":6},
    // "RequiredPlayerRange":{"type":"ShortTag","value":16},"SpawnCount":{"type":"ShortTag","value":4},
    // "SpawnData":{"type":"CompoundTag","value":{"entity":{"type":"CompoundTag","value":{"id":{"type":"StringTag","value":"minecraft:cave_spider"}}}}},
    // "MaxSpawnDelay":{"type":"ShortTag","value":800},"Delay":{"type":"ShortTag","value":20},"keepPacked":{"type":"ByteTag","value":0},
    // "x":{"type":"IntTag","value":5332},"y":{"type":"IntTag","value":-27},"z":{"type":"IntTag","value":5988},
    // "id":{"type":"StringTag","value":"minecraft:mob_spawner"},"SpawnRange":{"type":"ShortTag","value":4},
    // "MinSpawnDelay":{"type":"ShortTag","value":200},"SpawnPotentials":{"type":"ListTag","value":{"type":"EndTag","list":[]}}}]}}
    @Override
    public Optional<Block> blockFromTag(CompoundTag tag, CompoundTag blockEntity) {
        Optional<Block> block = blockFromTag(tag);
        if (block.isPresent() && blockEntity != null) {
            // System.out.println("Loaded Block Entities: " + blockEntity);
            // if it's a spawner or a chest
            if (blockEntity.getString("id").equals("minecraft:mob_spawner")) {
                try {
                    // Underilla.info("Interesting Spawner: " + blockEntity);
                    String spawnedType = blockEntity.getCompoundTag("SpawnData").getCompoundTag("entity").getString("id");
                    // System.out.println("Spawner Type: " + spawnedType);
                    if (block.get() instanceof BukkitBlock bukkitBlock) {
                        // bukkitBlock.setBlockData(Material.SPAWNER.createBlockData());
                        bukkitBlock.setSpawnedType(spawnedType);
                        // Underilla.info("blockFromTag: " + bukkitBlock.getSpawnedType());
                    }

                    // } else if (blockEntity.getString("id").equals("minecraft:chest")) {
                    // System.out.println("Interesting Chest: " + blockEntity);
                } catch (Exception e) {
                    Underilla.warning("Failed to set the type of a spawner: " + blockEntity);
                    Underilla.warning(() -> Tools.exceptionToString(e));
                }

            } else if (blockEntity.getString("id").equals("minecraft:chest")) {

            }
            // System.out.println("Loaded Block Entities: " + blockEntity.getString("id") + ":\n" + blockEntity);
        }
        return block;
    }


    @Override
    public Optional<Biome> biomeFromTag(StringTag tag) { return Optional.of(new BukkitBiome(tag.getValue())); }
}
