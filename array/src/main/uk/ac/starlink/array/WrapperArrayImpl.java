package uk.ac.starlink.array;

import java.io.IOException;

/**
 * Base class for NDArray-wrapping implementations of ArrayImpl.
 * Instances of this class present an ArrayImpl interface based on an
 * NDArray object; in a sense doing the reverse of what BridgeNDArray does.
 * On its own this is not useful, but subclasses of this class can 
 * arrange to modify some of the method implementations to represent
 * an array object with different qualities based on the wrapped NDArray.
 * The resulting object can then be turned into a new NDArray by 
 * passing it to the BridgeNDArray constructor.  In this way it is
 * quite easy to produce a new NDArray from a base NDArray (typically
 * with data generated on the fly from the accessor of the base),
 * giving a virtual view of the base NDArray with modified properties
 * such as shape or type.  Doing it via an ArrayImpl rather than 
 * wrapping the NDArray directly makes the implementation much less
 * effort; for instance tile read/write methods do not need to be
 * implemented and guarantees about the validity of many method
 * parameters can be assumed.
 *
 * @author   Mark Taylor (Starlink)
 * @see BridgeNDArray
 */
public class WrapperArrayImpl implements ArrayImpl {

    private final NDArray baseNda;

    public WrapperArrayImpl( NDArray nda ) {

        /* Store the wrapped NDArray. */
        this.baseNda = nda;
    }

    public OrderedNDShape getShape() {
        return baseNda.getShape();
    }
    public Type getType() {
        return baseNda.getType();
    }
    public Number getBadValue() {
        return baseNda.getBadHandler().getBadValue();
    }
    public boolean isReadable() {
        return baseNda.isReadable();
    }
    public boolean isWritable() {
        return baseNda.isWritable();
    }
    public boolean isRandom() {
        return baseNda.isRandom();
    }
    public boolean multipleAccess() {
        return baseNda.multipleAccess();
    }
    public void open() {
    }
    public boolean canMap() {
        return false;
    }
    public Object getMapped() {
        return null;
    }
    public AccessImpl getAccess() throws IOException {
        return new AccessImpl() {
            private ArrayAccess baseAcc = baseNda.getAccess();
            public void setOffset( long off ) throws IOException {
                baseAcc.setOffset( off );
            }
            public void read( Object buffer, int start, int size ) 
                    throws IOException {
                baseAcc.read( buffer, start, size );
            }
            public void write( Object buffer, int start, int size )
                    throws IOException {
                baseAcc.write( buffer, start, size );
            }
            public void close() throws IOException {
                baseAcc.close();
            }
        };
    }
    public void close() throws IOException {
        baseNda.close();
    }

    /**
     * Returns the NDArray which this Wrapper is wrapping.
     *
     * @return  the underlying NDArray
     */
    public NDArray getWrapped() {
        return baseNda;
    }

    public String toString() {
        return getClass().toString() 
             + " wrapping " 
             + getWrapped().toString();
    }

}
