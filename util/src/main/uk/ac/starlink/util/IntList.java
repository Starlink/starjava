package uk.ac.starlink.util;

/**
 * Extendable array of integers.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2006
 */
public class IntList extends PrimitiveList {

    /**
     * Constructs a new list.
     */
    public IntList() {
        this( DEFAULT_SIZE );
    }

    /**
     * Constructs a new list with a given initial capacity.
     *
     * @param   size  initial capacity
     */
    public IntList( int size ) {
        super( new int[ size ], 0 );
    }

    /**
     * Constructs a new list initialised to the contents of a given array.
     *
     * @param   array   array whose contents form initial contents of list
     */
    public IntList( int[] array ) {
        super( array.clone(), array.length );
    }

    /**
     * Returns the element at a given position.
     *
     * @param   i   index
     * @return   element at <code>i</code>
     */
    public int get( int i ) {
        checkIndex( i );
        return ((int[]) array_)[ i ];
    }

    /**
     * Sets the element at a given position.
     *
     * @param   i  index
     * @param   value   new value for element <code>i</code>
     */
    public void set( int i, int value ) {
        checkIndex( i );
        ((int[]) array_)[ i ] = value;
    }

    /**
     * Appends a value to the end of this list.
     *
     * @param   value  value to append
     */
    public void add( int value ) {
        expandSize( 1 );
        set( size() - 1, value );
    }

    /**
     * Returns the contents of this list as an array.
     *
     * @return   copy of list contents
     */
    public int[] toIntArray() {
        return (int[]) toArray();
    }
}
