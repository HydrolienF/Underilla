package com.jkantrell.mc.underilla.core.reader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import com.jkantrell.mc.underilla.core.api.Biome;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.core.vector.LocatedBlock;
import com.jkantrell.mca.Chunk;
import com.jkantrell.mca.MCAUtil;
import com.jkantrell.mca.PaletteContainer;
import com.jkantrell.mca.Section;
import com.jkantrell.nbt.tag.CompoundTag;
import com.jkantrell.nbt.tag.StringTag;

public abstract class ChunkReader implements Reader {

    // ASSETS
    public static final int CHUNK_SIZE = 16;
    public static final int MAXIMUM_SECTION_HEIGHT = 20, MINIMUM_SECTION_HEIGHT = -4;
    public static final int MAXIMUM_HEIGHT = MAXIMUM_SECTION_HEIGHT * CHUNK_SIZE, MINIMUM_HEIGHT = MINIMUM_SECTION_HEIGHT * CHUNK_SIZE;


    // FIELDS
    private final Chunk chunk_;
    private Integer airColumnHeight_ = null;


    // CONSTRUCTORS
    protected ChunkReader(Chunk chunk) { this.chunk_ = chunk; }


    // GETTERS
    public int getX() { return this.chunk_.getX(); }
    public int getZ() { return this.chunk_.getZ(); }
    public int getGlobalX(int localX) { return this.chunk_.getX() * CHUNK_SIZE + localX; }
    public int getGlobalZ(int localZ) { return this.chunk_.getZ() * CHUNK_SIZE + localZ; }


    // UTIL
    @Override
    public Optional<Block> blockAt(int x, int y, int z) {
        if (this.chunk_ == null) {
            return Optional.empty();
        }
        CompoundTag tag = this.chunk_.getBlockStateAt(x, y, z);
        CompoundTag blockEntity = this.chunk_.getBlockEntity(getGlobalX(x), y, getGlobalZ(z));
        return this.blockFromTag(tag, blockEntity);
    }
    @Override
    public Optional<Biome> biomeAt(int x, int y, int z) {
        // Declaring variables
        Map<Integer, Section> sectionMap = this.chunk_.getSectionMap();
        int height = MCAUtil.blockToChunk(y);
        Section section = sectionMap.get(height);
        StringTag biomeTag;

        // Returning biome if section exists
        if (section != null) {
            biomeTag = section.getBiomeAt(x, Math.floorMod(y, 16), z);
            if (biomeTag != null) {
                return this.biomeFromTag(biomeTag);
            }
        }

        // If sections doesn't exist trying to pull it from the above section
        int i = height + 1;
        while (i < MAXIMUM_SECTION_HEIGHT) {
            section = sectionMap.get(i);
            if (section != null) {
                biomeTag = section.getBiomeAt(x, Math.floorMod(y, 16), z);
                if (biomeTag != null) {
                    return this.biomeFromTag(biomeTag);
                }
            }
            i++;
        }

        // If no above section exists, trying to pull it from the under section
        i = height - 1;
        while (i >= MINIMUM_SECTION_HEIGHT) {
            section = sectionMap.get(i);
            if (section != null) {
                biomeTag = section.getBiomeAt(x, Math.floorMod(y, 16), z);
                if (biomeTag != null) {
                    return this.biomeFromTag(biomeTag);
                }
            }
            i--;
        }

        // Returning empty
        return Optional.empty();
    }
    public int airSectionsBottom() {
        if (this.airColumnHeight_ != null) {
            return this.airColumnHeight_;
        }

        Predicate<Section> isAir = s -> {
            PaletteContainer<CompoundTag> paletteContainer = s.getBlockStatePalette();
            if (paletteContainer.getPalette().size() > 1) {
                return false;
            }
            CompoundTag b = s.getBlockStatePalette().get(0);
            if (b == null) {
                return true;
            }
            return this.blockFromTag(b).map(Block::isAir).orElse(true);
        };

        Section s;
        int lowest = MAXIMUM_HEIGHT - 1;
        while (lowest > MINIMUM_HEIGHT - 1) {
            s = this.chunk_.getSection(lowest);
            if (s == null || isAir.test(s)) {
                lowest--;
            } else {
                break;
            }
        }

        this.airColumnHeight_ = (lowest + 1) * 16;
        return this.airColumnHeight_;
    }
    public List<LocatedBlock> locationsOf(Predicate<Block> checker, int under, int above) {
        Stream<Section> sectionStream = this.chunk_.getSections().stream().filter(s -> {
            int lower = s.getHeight() * 16, upper = lower + 15;
            if (under <= lower) {
                return false;
            }
            return above < upper;
        });
        Predicate<LocatedBlock> locationCheck = l -> l.y() > above && l.y() < under;
        return this.locationsOf(checker, locationCheck, sectionStream);
    }
    public List<LocatedBlock> locationsOf(Predicate<Block> checker) {
        return this.locationsOf(checker, l -> true, this.chunk_.getSections().stream());
    }
    public List<LocatedBlock> locationsOf(Block block) { return this.locationsOf(block::equals); }
    public List<LocatedBlock> locationsOf(Block... blocks) {
        List<Block> materialList = Arrays.asList(blocks);
        return this.locationsOf(materialList::contains);
    }


    // ABSTRACT
    public abstract Optional<Block> blockFromTag(CompoundTag tag);
    public abstract Optional<Block> blockFromTag(CompoundTag tag, CompoundTag entityTag);
    public abstract Optional<Biome> biomeFromTag(StringTag tag);


    // PRIVATE UTIL
    private List<LocatedBlock> locationsOf(Predicate<Block> checker, Predicate<LocatedBlock> secondChecker, Stream<Section> sections) {
        return sections.flatMap(s -> s.getBlockLocations(t -> {
            Block b = this.blockFromTag(t).orElse(null);
            if (b == null) {
                return false;
            }
            return checker.test(b);
        }).stream().map(l -> new LocatedBlock(l.x(), l.y() + (s.getHeight() * 16), l.z(), this.blockFromTag(l.tag()).get())))
                .filter(secondChecker).toList();
    }
}
