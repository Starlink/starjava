package uk.ac.starlink.array;

/**
 * Dummy ArrayImpl backed by no data.  It is read-only, and all its pixels
 * have the bad value.  This class is package private since it probably
 * doesn't need extending - the more convenient DummyNDArray class
 * is provided for applications to use.
 *
 * @author   Mark Taylor (Starlink)
 * @see   DeterministicArrayImpl
 * @see   DummyArrayImpl
 */
class DummyArrayImpl implements ArrayImpl {
    private final OrderedNDShape oshape;
    private final Type type;
    private final Number badValue;
    private final BadHandler bh;

    /**
     * Construct a new DummyArrayImpl out of a shape, type and bad value.
     *
     * @param  oshape  the shape
     * @param  type    the type
     * @param  badValue the bad value
     */
    public DummyArrayImpl( OrderedNDShape oshape, Type type, Number badValue ) {
        this.oshape = oshape;
        this.type = type;
        this.badValue = badValue;
        this.bh = BadHandler.getHandler( type, badValue );
    }

    public OrderedNDShape getShape() { return oshape; }
    public Type getType() { return type; }
    public Number getBadValue() { return badValue; }
    public boolean isReadable() { return true; }
    public boolean isWritable() { return false; }
    public boolean isRandom() { return true; }
    public boolean multipleAccess() { return true; }
    public void open() {}
    public boolean canMap() { return false; }
    public Object getMapped() { return null; }
    public void close() {}
    public AccessImpl getAccess() {
        return new AccessImpl() {
            public void setOffset( long off ) {}
            public void write( Object buffer, int start, int size ) {}
            public void close() {}
            public void read( Object buffer, int start, int size ) {
                bh.putBad( buffer, start, size );
            }
        };
    }
}
