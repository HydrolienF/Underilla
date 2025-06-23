package fr.formiko.mc.underilla.core.vector;

import fr.formiko.mc.underilla.core.api.Block;

public class LocatedBlock extends LocatedHolder<Integer, Block> {
    public LocatedBlock(Vector<Integer> coordinates, Block block) { super(coordinates, block); }
    public LocatedBlock(int x, int y, int z, Block block) { super(new IntVector(x, y, z), block); }
}
