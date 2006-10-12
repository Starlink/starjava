package uk.ac.starlink.util;

/**
 * Extendable array of doubles.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2006
 */
public class DoubleList extends PrimitiveList {

    /**
     * Constructs a new list.
     */
    public DoubleList() {
        this( DEFAULT_SIZE );
    }

    /**
     * Constructs a new list with a given initial capacity.
     *
     * @param   size  initial capacity
     */
    public DoubleList( int size ) {
        super( new double[ size ], 0 );
    }

    /**
     * Constructs a new list initialised to the contents of a given array.
     *
     * @param   array   array whose contents form initial contents of list
     */
    public DoubleList( double[] array ) {
        super( array.clone(), array.length );
    }

    /**
     * Returns the element at a given position.
     *
     * @param   i   index
     * @return   element at <code>i</code>
     */ 
    public double get( int i ) {
        checkIndex( i );
        return ((double[]) array_)[ i ];
    }

    /**
     * Sets the element at a given position.
     *
     * @param   i  index
     * @param   value   new value for element <code>i</code>
     */
    public void set( int i, double value ) {
        checkIndex( i );
        ((double[]) array_)[ i ] = value;
    }

    /**
     * Appends a value to the end of this list.
     *
     * @param   value  value to append
     */
    public void add( double value ) {
        expandSize( 1 );
        set( size() - 1, value );
    }

    /**
     * Returns the contents of this list as an array.
     *
     * @return   copy of list contents
     */
    public double[] toDoubleArray() {
        return (double[]) toArray();
    }
}
