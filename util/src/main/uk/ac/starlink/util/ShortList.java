package uk.ac.starlink.util;

/**
 * Extendable array of shorts.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2006
 */
public class ShortList extends PrimitiveList {

    /**
     * Constructs a new list.
     */
    public ShortList() {
        this( DEFAULT_SIZE );
    }

    /**
     * Constructs a new list with a given initial capacity.
     *
     * @param   size  initial capacity
     */
    public ShortList( int size ) {
        super( new short[ size ], 0 );
    }

    /**
     * Constructs a new list initialised to the contents of a given array.
     *
     * @param   array   array whose contents form initial contents of list
     */
    public ShortList( short[] array ) {
        super( array.clone(), array.length );
    }

    /**
     * Returns the element at a given position.
     *
     * @param   i   index
     * @return   element at <code>i</code>
     */ 
    public short get( int i ) {
        checkIndex( i );
        return ((short[]) array_)[ i ];
    }

    /**
     * Sets the element at a given position.
     *
     * @param   i  index
     * @param   value   new value for element <code>i</code>
     */
    public void set( int i, short value ) {
        checkIndex( i );
        ((short[]) array_)[ i ] = value;
    }

    /**
     * Appends a value to the end of this list.
     *
     * @param   value  value to append
     */
    public void add( short value ) {
        expandSize( 1 );
        set( size() - 1, value );
    }

    /**
     * Returns the contents of this list as an array.
     *
     * @return   copy of list contents
     */
    public short[] toShortArray() {
        return (short[]) toArray();
    }
}
