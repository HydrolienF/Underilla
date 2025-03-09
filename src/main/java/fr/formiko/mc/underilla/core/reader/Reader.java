package fr.formiko.mc.underilla.core.reader;


import fr.formiko.mc.underilla.core.api.Biome;
import fr.formiko.mc.underilla.core.api.Block;
import fr.formiko.mc.underilla.core.vector.Vector;
import java.util.Optional;

public interface Reader {

    Optional<Block> blockAt(int x, int y, int z);
    Optional<Biome> biomeAt(int x, int y, int z);
    default Optional<Block> blockAt(Vector<Integer> vector) { return this.blockAt(vector.x(), vector.y(), vector.z()); }
    default Optional<Biome> biomeAt(Vector<Integer> vector) { return biomeAt(vector.x(), vector.y(), vector.z()); }
    default Optional<Biome> biomeAtCell(int x, int y, int z) { return biomeAt(x << 2, y << 2, z << 2); }
    default Optional<Biome> biomeAtCell(Vector<Integer> vector) { return this.biomeAtCell(vector.x(), vector.y(), vector.z()); }

}
