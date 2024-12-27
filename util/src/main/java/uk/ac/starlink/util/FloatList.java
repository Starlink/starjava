package uk.ac.starlink.util;

/**
 * Extendable array of floats.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2006
 */
public class FloatList extends PrimitiveList {

    /**
     * Constructs a new list.
     */
    public FloatList() {
        this( DEFAULT_SIZE );
    }

    /**
     * Constructs a new list with a given initial capacity.
     *
     * @param   size  initial capacity
     */
    public FloatList( int size ) {
        super( new float[ size ], 0 );
    }

    /**
     * Constructs a new list initialised to the contents of a given array.
     *
     * @param   array   array whose contents form initial contents of list
     */
    public FloatList( float[] array ) {
        super( array.clone(), array.length );
    }

    /**
     * Returns the element at a given position.
     *
     * @param   i   index
     * @return   element at <code>i</code>
     */ 
    public float get( int i ) {
        checkIndex( i );
        return ((float[]) array_)[ i ];
    }

    /**
     * Sets the element at a given position.
     *
     * @param   i  index
     * @param   value   new value for element <code>i</code>
     */
    public void set( int i, float value ) {
        checkIndex( i );
        ((float[]) array_)[ i ] = value;
    }

    /**
     * Appends a value to the end of this list.
     *
     * @param   value  value to append
     */
    public void add( float value ) {
        expandSize( 1 );
        set( size() - 1, value );
    }

    /**
     * Appends all the elements of a second list to this list.
     *
     * @param  other  other list
     * @return   true iff this collection changed as a result of the call
     */
    public boolean addAll( FloatList other ) {
        return super.addAll( other );
    }

    /**
     * Appends all the elements of a primitive array to this list.
     *
     * @param   array  array to append
     * @return   true iff this collection changed as a result of the call
     */ 
    public boolean addAll( float[] array ) {
        return super.addArrayElements( array, array.length );
    }

    /**
     * Returns the contents of this list as an array.
     *
     * @return   copy of list contents
     */
    public float[] toFloatArray() {
        return (float[]) toArray();
    }

    /**
     * Returns the array currently used to store the contents of this list.
     * Its length will be greater than or equal to the length of this list.
     * The identity of the returned array may change as this list is mutated.
     *
     * @return  storage array
     */
    public float[] getFloatBuffer() {
        return (float[]) array_;
    }
}
