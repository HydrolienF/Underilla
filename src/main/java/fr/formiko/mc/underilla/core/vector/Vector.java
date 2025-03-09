package fr.formiko.mc.underilla.core.vector;

public abstract class Vector<T extends Number> implements Cloneable, Comparable<Object> {

    // FIELDS
    private T x_, y_, z_;


    // CONSTRUCTORS
    protected Vector(T x, T y, T z) {
        this.x_ = x;
        this.z_ = z;
        this.y_ = y;
    }

    protected Vector(Vector<T> original) { this(original.x_, original.y_, original.z_); }


    // GETTER
    public T x() { return this.x_; }

    public T y() { return this.y_; }

    public T z() { return this.z_; }


    // SETTER
    public void setX(T x) { this.x_ = x; }

    public void setY(T y) { this.y_ = y; }

    public void setZ(T z) { this.z_ = z; }


    // UTIL
    @Override
    public abstract Vector<T> clone();
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof Vector<?> v)) {
            return 0;
        }
        return Double.compare(this.norm(), v.norm());
    }
    public Vector<T> addX(T x) {
        this.x_ = this.add(this.x_, x);
        return this;
    }
    public Vector<T> addY(T y) {
        this.y_ = this.add(this.y_, y);
        return this;
    }
    public Vector<T> addZ(T z) {
        this.z_ = this.add(this.z_, z);
        return this;
    }
    public Vector<T> add(Vector<T> vector) {
        this.x_ = this.add(this.x_, vector.x_);
        this.y_ = this.add(this.y_, vector.y_);
        this.z_ = this.add(this.z_, vector.z_);
        return this;
    }
    public double norm() {
        double x = Math.pow(this.x_.doubleValue(), 2), y = Math.pow(this.y_.doubleValue(), 2), z = Math.pow(this.z_.doubleValue(), 2);
        return Math.sqrt(x + y + z);
    }

    @Override
    public String toString() { return String.format("(%s, %s, %s)", this.x_, this.y_, this.z_); }


    // PRIVATE
    protected abstract T add(T a, T b);
}
