package uk.ac.starlink.array;

/**
 * Toy ArrayImpl implementation not backed by real data.
 * It is read-only, and its pixels depend only on the offset at which
 * they are read.  Mainly for testing purposes.
 * The {@link #offsetToValue} method may be overridden to provide 
 * different determistic array data.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DeterministicArrayImpl implements ArrayImpl {

    private final Type type;
    private final OrderedNDShape shape;
    private final Number badValue;
    private final BadHandler badHandler;

    /**
     * Constructs a DeterministicArrayImpl of the given type and shape.
     * @param   shape  the shape of the array.  If it is an OrderedNDShape
     *                 its pixel ordering will be used, otherwise 
     *                 a default ordering of Order.COLUMN_MAJOR will be used
     */
    public DeterministicArrayImpl( NDShape shape, Type type ) {
        this.type = type;
        this.shape = ( shape instanceof OrderedNDShape ) 
                         ? (OrderedNDShape) shape 
                         : new OrderedNDShape( shape, Order.COLUMN_MAJOR );
        this.badValue = type.defaultBadValue();
        this.badHandler = BadHandler.getHandler( type, badValue );
    }

    /**
     * Maps the array offset to the value at that offset.
     * The DeterministicArrayImpl class implements it as a simple linear 
     * function of the offset, with a smattering of bad values.
     * Override it to get different contents.
     *
     * @param   off     the offset into the array
     * @return  the pixel value at offset.  Double.NaN is permitted and
     *          will be converted to a bad value.
     */
    protected double offsetToValue( long off ) {
        return ( ( off + 5 ) % 23 ) == 0 ? Double.NaN : (double) off;
    }

    public OrderedNDShape getShape() {
        return shape;
    }
    public Type getType() {
        return type;
    }
    public Number getBadValue() {
        return badValue;
    }
    public boolean isReadable() {
        return true;
    }
    public boolean isWritable() {
        return false;
    }
    public boolean isRandom() {
        return true;
    }
    public boolean canMap() {
        return false;
    }
    public Object getMapped() {
        return null;
    }
    public boolean multipleAccess() {
        return true;
    }
    public void open() {
    }
    public void close() {
    }

    public AccessImpl getAccess() {
        return new AccessImpl() {
            private long offset = 0L;
            public void setOffset( long off ) {
                this.offset = off;
            }
            public void read( Object buffer, int start, int size ) {
                for ( ; size-- > 0; start++ ) {
                    double val = offsetToValue( offset++ );
                    if ( Double.isNaN( val ) ) {
                        badHandler.putBad( buffer, start );
                    }
                    else {
                        if ( type == Type.BYTE ) {
                            ((byte[]) buffer)[ start ] = (byte) val;
                        }
                        else if ( type == Type.SHORT ) {
                            ((short[]) buffer)[ start ] = (short) val;
                        }
                        else if ( type == Type.INT ) {
                            ((int[]) buffer)[ start ] = (int) val;
                        }
                        else if ( type == Type.FLOAT ) {
                            ((float[]) buffer)[ start ] = (float) val;
                        }
                        else if ( type == Type.DOUBLE ) {
                            ((double[]) buffer)[ start ] = val;
                        }
                        else {
                            assert false;
                        }
                    }
                }
            }
            public void write( Object buffer, int start, int size ) {
                assert false;
            }
            public boolean isMapped() {
                return false;
            }
            public Object getMapped() {
                throw new AssertionError();
            }
            public void close() {
            }
        };
    }
}
