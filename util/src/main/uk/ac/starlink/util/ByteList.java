package uk.ac.starlink.util;

/**
 * Extendable array of bytes.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2006
 */
public class ByteList extends PrimitiveList {

    /**
     * Constructs a new list.
     */
    public ByteList() {
        this( DEFAULT_SIZE );
    }

    /**
     * Constructs a new list with a given initial capacity.
     *
     * @param   size  initial capacity
     */
    public ByteList( int size ) {
        super( new byte[ size ], 0 );
    }

    /**
     * Constructs a new list initialised to the contents of a given array.
     *
     * @param   array   array whose contents form initial contents of list
     */
    public ByteList( byte[] array ) {
        super( array.clone(), array.length );
    }

    /**
     * Returns the element at a given position.
     *
     * @param   i   index
     * @return   element at <code>i</code>
     */ 
    public byte get( int i ) {
        checkIndex( i );
        return ((byte[]) array_)[ i ];
    }

    /**
     * Sets the element at a given position.
     *
     * @param   i  index
     * @param   value   new value for element <code>i</code>
     */
    public void set( int i, byte value ) {
        checkIndex( i );
        ((byte[]) array_)[ i ] = value;
    }

    /**
     * Appends a value to the end of this list.
     *
     * @param   value  value to append
     */
    public void add( byte value ) {
        expandSize( 1 );
        set( size() - 1, value );
    }

    /**
     * Returns the contents of this list as an array.
     *
     * @return   copy of list contents
     */
    public byte[] toByteArray() {
        return (byte[]) toArray();
    }
}
