package fr.formiko.mc.underilla.core.vector;

public class IntVector extends Vector<Integer> {

    // CONSTRUCTORS
    public IntVector(Integer x, Integer y, Integer z) { super(x, y, z); }
    public IntVector(Vector<Integer> original) { super(original); }


    // IMPLEMENTATIONS
    @Override
    public IntVector clone() { return new IntVector(this); }
    @Override
    protected Integer add(Integer a, Integer b) { return a + b; }
}
