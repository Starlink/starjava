package uk.ac.starlink.table.storage;

import java.lang.reflect.Array;
import uk.ac.starlink.table.ValueStore;

/**
 * ValueStore implementation which stores primitive values in a normal
 * java array.
 *
 * @author   Mark Taylor
 * @since    1 Nov 2005
 */
public class ArrayPrimitiveStore implements ValueStore {

    private final Class clazz_;
    private final Object data_;
    private final long size_;

    /**
     * Constructs a new store.  The supplied <code>clazz</code> must be
     * a primitive type such as <code>int.class</code>.
     *
     * @param  clazz  primitive type to store
     * @param  size   length of the vector
     */
    public ArrayPrimitiveStore( Class clazz, int size ) {
        if ( ! clazz.isPrimitive() ) {
            throw new IllegalArgumentException( clazz + " not primitive" );
        }
        clazz_ = clazz;
        size_ = (long) size;
        data_ = Array.newInstance( clazz, size );
    }

    public Class getType() {
        return clazz_;
    }

    public long getLength() {
        return size_;
    }

    public void put( long index, Object array, int ioff, int count ) {
        if ( index < Integer.MAX_VALUE ) {
            System.arraycopy( array, ioff, data_, (int) index, count );
        }
        else {
            throw new IllegalArgumentException( "Out of range" );
        }
    }

    public void get( long index, Object array, int ioff, int count ) {
        if ( index < Integer.MAX_VALUE ) {
            System.arraycopy( data_, (int) index, array, ioff, count );
        }
        else {
            throw new IllegalArgumentException( "Out of range" );
        }
    }
}
