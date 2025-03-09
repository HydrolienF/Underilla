package fr.formiko.mc.underilla.core.vector;

public class LocatedHolder<T extends Number, H> {

    // FIELDS
    private final Vector<T> vector_;
    private final H value_;


    // CONSTRUCTORS
    public LocatedHolder(Vector<T> coordinates, H value) {
        this.vector_ = coordinates;
        this.value_ = value;
    }


    // GETTERS
    public T x() { return this.vector_.x(); }
    public T y() { return this.vector_.y(); }
    public T z() { return this.vector_.z(); }
    public Vector<T> vector() { return this.vector_.clone(); }
    public H value() { return this.value_; }
}
