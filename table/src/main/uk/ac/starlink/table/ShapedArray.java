package uk.ac.starlink.table;

import java.lang.reflect.Array;

/**
 * Represents an array of objects with an additional imposed 
 * N-dimensional shape.  This class is not intended to be highly 
 * functional (hence there are no methods for reading/writing 
 * given elements etc), its job is to associate an N-dimensional 
 * shape with a data array for the purposes of putting in a table cell.
 *
 * <p>For a more functional container for N-dimensional data, see
 * {@link uk.ac.starlink.array.NDArray}.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ShapedArray {

    private final Object data;
    private final int[] dims;
    private final int nel;

    /**
     * Constructs a shaped array object from its data array and a
     * specification of its dimensions.  <tt>data</tt> must be a
     * java array with a number of elements equal to the product 
     * of the elements of <tt>dims</tt>.
     *
     * @param   data  a java array containing the object data
     * @param   dims  an array giving the dimensions imposed on <tt>data</tt>.
     *          It is interpreted in column major order; the first index
     *          varies fastest
     * @throws  IllegalArgumentException  if <tt>dims</tt> has elements &lt;=0
     *          or doesn't match the length of <tt>data</tt>
     */
    public ShapedArray( Object data, int[] dims ) {
        this.dims = (int[]) dims.clone();
        this.data = data;
        long n = 1L;
        for ( int i = 0; i < dims.length; i++ ) {
            if ( dims[ i ] <= 0 ) {
                throw new IllegalArgumentException( 
                   "Dimension " + i + " is " + dims[ i ] + "(<=0)" );
            }
            n *= dims[ i ];
        }
        this.nel = (int) n;
        if ( Array.getLength( data ) != nel ||
             n > Integer.MAX_VALUE /* unlikely but possible */ ) {
            throw new IllegalArgumentException(
                "Size of data does not match dims (" + 
                nel + " != " + Array.getLength( data ) );
        }
    }

    /**
     * Returns the data array.  This object is a java array.
     *
     * @return  the data array
     */
    public Object getData() {
        return data;
    }

    /**
     * Returns the array of dimensions associated with the data array
     * held by this object.
     *
     * @return  the dimensions array
     */
    public int[] getDims() {
        return (int[]) dims.clone();
    }
}
