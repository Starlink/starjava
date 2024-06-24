package uk.ac.starlink.array;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * ArrayImpl implementation which wraps a primitive numeric java array.
 * The type of the resulting ArrayImpl is determined by the component
 * type of the java array.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ArrayArrayImpl implements ArrayImpl {
 
    private Object data;
    private OrderedNDShape oshape;
    private Type type;
    private Number badValue;

    /**
     * Constructs a new ArrayArrayImpl backed by a given java primitive 
     * numeric array.
     *
     * @param   data  the java primitive array; must be a numeric
     *          type corresponding to one of the {@link Type} instances
     * @param   oshape  the shape of the array; must have the same number
     *          of pixels as <code>data</code>
     * @param   badValue  the bad data value; must match the type of 
     *          <code>data</code> if it is not <code>null</code>
     * @throws  IllegalArgumentException  if <code>data</code> is not a suitable
     *          primitive numeric type or <code>oshape</code> has the wrong 
     *          number of pixels
     */
    public ArrayArrayImpl( Object data, OrderedNDShape oshape, 
                           Number badValue ) {
        this.data = data;
        this.oshape = oshape;
        this.badValue = badValue;
        this.type = Type.getType( data.getClass().getComponentType() );
        if ( oshape.getNumPixels() != (long) Array.getLength( data ) ) {
            throw new IllegalArgumentException( 
                "Primitive array has wrong number of elements " + 
                Array.getLength( data ) + " for shape " + oshape );
        }
        if ( type == null ) {
            throw new IllegalArgumentException(
                "Unsupported primitive numeric element array of type " + 
                data.getClass() );
        }
    }

    public OrderedNDShape getShape() {
        return oshape;
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
        return true;
    }

    public boolean isRandom() {
        return true;
    }

    public boolean multipleAccess() {
        return true;
    }

    public void open() {
    }

    public boolean canMap() {
        return true;
    }

    public Object getMapped() {
        return data;
    }

    public void close() {
    }

    public AccessImpl getAccess() {
        return new AccessImpl() {
            private int off = 0;
            public void setOffset( long off ) {
                this.off = (int) off;
            }
            public void read( Object buf, int start, int size ) {
                System.arraycopy( data, off, buf, start, size );
                off += size;
            }
            public void write( Object buf, int start, int size ) {
                System.arraycopy( buf, start, data, off, size );
                off += size;
            }
            public void close() {
            }
        };
    }
}
