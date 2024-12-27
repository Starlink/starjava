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
     * Appends all the elements of a second list to this list.
     *
     * @param  other  other list
     * @return   true iff this collection changed as a result of the call
     */
    public boolean addAll( IntList other ) {
        return super.addAll( other );
    }

    /**
     * Appends all the elements of a primitive array to this list.
     *
     * @param   array  array to append
     * @return   true iff this collection changed as a result of the call
     */ 
    public boolean addAll( int[] array ) {
        return super.addArrayElements( array, array.length );
    }

    /**
     * Returns the contents of this list as an array.
     *
     * @return   copy of list contents
     */
    public int[] toIntArray() {
        return (int[]) toArray();
    }

    /**
     * Returns the array currently used to store the contents of this list.
     * Its length will be greater than or equal to the length of this list.
     * The identity of the returned array may change as this list is mutated.
     *
     * @return  storage array
     */
    public int[] getIntBuffer() {
        return (int[]) array_;
    }
}
