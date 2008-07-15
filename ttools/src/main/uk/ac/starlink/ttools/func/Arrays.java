// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.lang.reflect.Array;

/**
 * Functions which operate on array-valued cells.
 * You can only use these functions on values which are already arrays.
 * In most cases that means on values in table columns which are declared
 * as array-valued.  FITS and VOTable tables can have columns which contain
 * array values, but other formats such as CSV cannot.
 *
 * @author   Mark Taylor
 * @since    14 Jul 2008
 */
public class Arrays {

    /**
     * Private constructor prevents instantiation.
     */
    private Arrays() {
    }

    /**
     * Returns the sum of all the non-blank elements in the array.
     * If <code>array</code> is not a numeric array, <code>null</code>
     * is returned.
     *
     * @param   array  array of numbers
     * @return  sum of all the numeric values in <code>array</code>
     */
    public static double sum( Object array ) {
        try {
            int n = Array.getLength( array );
            double sum = 0;
            for ( int i = 0; i < n; i++ ) {
                double d = Array.getDouble( array, i );
                if ( ! Double.isNaN( d ) ) {
                    sum += d;
                }
            }
            return sum;
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }

    /**
     * Returns the mean of all the non-blank elements in the array.
     * If <code>array</code> is not a numeric array, <code>null</code>
     * is returned.
     *
     * @param  array  array of numbers
     * @return  mean of all the numeric values in <code>array</code>
     */
    public static double mean( Object array ) {
        try {
            int n = Array.getLength( array );
            double sum = 0;
            int count = 0;
            for ( int i = 0; i < n; i++ ) {
                double d = Array.getDouble( array, i );
                if ( ! Double.isNaN( d ) ) {
                    sum += d;
                    count++;
                }
            }
            return sum / (double) count;
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }

    /**
     * Returns the smallest of the non-blank elements in the array.
     * If <code>array</code> is not a numeric array, <code>null</code>
     * is returned.
     *
     * @param  array  array of numbers
     * @return  minimum of the numeric values in <code>array</code>
     */
    public static double minimum( Object array ) {
        try {
            int n = Array.getLength( array );
            double min = Double.NaN;
            for ( int i = 0; i < n; i++ ) {
                double d = Array.getDouble( array, i );
                if ( ! Double.isNaN( d ) && ! ( d > min ) ) {
                    min = d;
                }
            }
            return min;
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }

    /**
     * Returns the largest of the non-blank elements in the array.
     * If <code>array</code> is not a numeric array, <code>null</code>
     * is returned.
     *
     * @param   array  array of numbers
     * @return   maximum of the numeric values in <code>array</code>
     */
    public static double maximum( Object array ) {
        try {
            int n = Array.getLength( array );
            double max = Double.NaN;
            for ( int i = 0; i < n; i++ ) {
                double d = Array.getDouble( array, i );
                if ( ! Double.isNaN( d ) && ! ( d < max ) ) {
                    max = d;
                }
            }
            return max;
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }

    /**
     * Returns the number of elements in the array.
     * If <code>array</code> is not an array, zero is returned.
     *
     * @param  array  array
     * @return  size of <code>array</code>
     */
    public static int size( Object array ) {
        return array != null && array.getClass().isArray()
             ? Array.getLength( array )
             : 0;
    }
}
