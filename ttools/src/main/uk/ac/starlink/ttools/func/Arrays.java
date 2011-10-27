// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.io.IOException;
import java.lang.reflect.Array;
import uk.ac.starlink.ttools.filter.QuantCalc;

/**
 * Functions which perform aggregating operations on array-valued cells.
 * The functions in this class such as <code>mean</code>, <code>sum</code>,
 * <code>maximum</code> etc can only be used on values which are already arrays.
 * In most cases that means on values in table columns which are declared
 * as array-valued.  FITS and VOTable tables can have columns which contain
 * array values, but other formats such as CSV cannot.
 *
 * <p>There is also a set of functions named <code>array</code> with various
 * numbers of arguments, which let you assemble an array value from a list
 * of scalar numbers.  This can be used for instance to get the mean of
 * a set of three magnitudes by using an expression like
 * "<code>mean(array(jmag, hmag, kmag))</code>".
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
     * Returns the population variance of all the non-blank elements
     * in the array.  If <code>array</code> is not a numeric array,
     * <code>null</code> is returned.
     *
     * @param  array  array of numbers
     * @return  variance of the numeric values in <code>array</code>
     */
    public static double variance( Object array ) {
        try {
            int n = Array.getLength( array );
            double sum = 0;
            double sum2 = 0;
            int count = 0;
            for ( int i = 0; i < n; i++ ) {
                double d = Array.getDouble( array, i );
                if ( ! Double.isNaN( d ) ) {
                    sum += d;
                    sum2 += d * d;
                    count++;
                }
            }
            double mean = sum / (double) count;
            return sum2 / (double) count - mean * mean;
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }

    /**
     * Returns the population standard deviation of all the non-blank elements
     * in the array.  If <code>array</code> is not a numeric array,
     * <code>null</code> is returned.
     *
     * @param   array  array of numbers
     * @return  standard deviation of the numeric values in <code>array</code>
     */
    public static double stdev( Object array ) {
        return Math.sqrt( variance( array ) );
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
     * Returns the median of the non-blank elements in the array.
     * If <code>array</code> is not a numeric array, <code>null</code>
     * is returned.
     *
     * @param   array  array of numbers
     * @return   median of the numeric values in <code>array</code>
     */
    public static double median( Object array ) {
        return quantile( array, 0.5 );
    }

    /**
     * Returns a quantile value of the non-blank elements in the array.
     * Which quantile is determined by the <code>quant</code> value;
     * values of 0, 0.5 and 1 give the minimum, median and maximum
     * respectively.  A value of 0.99 would give the 99th percentile.
     * 
     * @param   array  array of numbers
     * @param   quant  number in the range 0-1 deterining which quantile
     *                 to calculate
     * @return   quantile corresponding to <code>quant</code>
     */
    public static double quantile( Object array, double quant ) {
        try {
            int n = Array.getLength( array );
            QuantCalc qc = QuantCalc.createInstance( Double.class, n );
            for ( int i = 0; i < n; i++ ) {
                qc.acceptDatum( new Double( Array.getDouble( array, i ) ) );
            }
            qc.ready();
            Number value = qc.getQuantile( quant );
            return value instanceof Number ? ((Number) value).doubleValue()
                                           : Double.NaN;
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
        catch ( IOException e ) {
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

    /**
     * Returns the number of non-blank elements in the array.
     * If <code>array</code> is not an array, zero is returned.
     *
     * @param  array   array (may or may not be numeric)
     * @return   number of non-blank elements in <code>array</code>
     */
    public static int count( Object array ) {
        try {
            int n = Array.getLength( array );
            int count = 0;
            for ( int i = 0; i < n; i++ ) {
                double d = Array.getDouble( array, i );
                if ( ! Double.isNaN( d ) ) {
                    count++;
                }
            }
            return count;
        }
        catch ( RuntimeException e ) {
            return 0;
        }
    }

    /**
     * Returns a string composed of concatenating all the elements of an
     * array, separated by a joiner string.
     * If <code>array</code> is not an array, null is returned.
     *
     * @example <code>join(array(1.5,2.1,-3.9), "; ") = "1.5; 2.1; -3.9"</code>
     *
     * @param  array   array of numbers or strings
     * @param  joiner  text string to interpose between adjacent elements
     * @return  string composed of <code>array</code> elements separated by
     *          <code>joiner</code> strings
     */
    public static String join( Object array, String joiner ) {
        StringBuilder sbuf = new StringBuilder();
        try {
            int n = Array.getLength( array );
            for ( int i = 0; i < n; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( joiner );
                }
                sbuf.append( Array.get( array, i ) );
            }
            return sbuf.toString();
        }
        catch ( RuntimeException e ) {
            return null;
        }
    }

    /**
     * Returns a numeric array built from a given element.
     *
     * @param   x1   array element 1
     * @return  1-element array
     */
    public static double[] array( double x1 ) {
        return new double[] { x1, };
    }

    /**
     * Returns a numeric array built from given elements.
     *
     * @param   x1   array element 1
     * @param   x2   array element 2
     * @return  2-element array
     */
    public static double[] array( double x1, double x2 ) {
        return new double[] { x1, x2, };
    }

    /**
     * Returns a numeric array built from given elements.
     *
     * @param   x1   array element 1
     * @param   x2   array element 2
     * @param   x3   array element 3
     * @return  3-element array
     */
    public static double[] array( double x1, double x2, double x3 ) {
        return new double[] { x1, x2, x3, };
    }

    /**
     * Returns a numeric array built from given elements.
     *
     * @param   x1   array element 1
     * @param   x2   array element 2
     * @param   x3   array element 3
     * @param   x4   array element 4
     * @return  4-element array
     */
    public static double[] array( double x1, double x2, double x3, double x4 ) {
        return new double[] { x1, x2, x3, x4, };
    }

    /**
     * Returns a numeric array built from given elements.
     *
     * @param   x1   array element 1
     * @param   x2   array element 2
     * @param   x3   array element 3
     * @param   x4   array element 4
     * @param   x5   array element 5
     * @return  5-element array
     */
    public static double[] array( double x1, double x2, double x3, double x4,
                                  double x5 ) {
        return new double[] { x1, x2, x3, x4, x5, };
    }

    /**
     * Returns a numeric array built from given elements.
     *
     * @param   x1   array element 1
     * @param   x2   array element 2
     * @param   x3   array element 3
     * @param   x4   array element 4
     * @param   x5   array element 5
     * @param   x6   array element 6
     * @return  6-element array
     */
    public static double[] array( double x1, double x2, double x3, double x4,
                                  double x5, double x6 ) {
        return new double[] { x1, x2, x3, x4, x5, x6, };
    }

    /**
     * Returns a numeric array built from given elements.
     *
     * @param   x1   array element 1
     * @param   x2   array element 2
     * @param   x3   array element 3
     * @param   x4   array element 4
     * @param   x5   array element 5
     * @param   x6   array element 6
     * @param   x7   array element 7
     * @return  7-element array
     */
    public static double[] array( double x1, double x2, double x3, double x4,
                                  double x5, double x6, double x7 ) {
        return new double[] { x1, x2, x3, x4, x5, x6, x7, };
    }

    /**
     * Returns a numeric array built from given elements.
     *
     * @param   x1   array element 1
     * @param   x2   array element 2
     * @param   x3   array element 3
     * @param   x4   array element 4
     * @param   x5   array element 5
     * @param   x6   array element 6
     * @param   x7   array element 7
     * @param   x8   array element 8
     * @return  8-element array
     */
    public static double[] array( double x1, double x2, double x3, double x4,
                                  double x5, double x6, double x7, double x8 ) {
        return new double[] { x1, x2, x3, x4, x5, x6, x7, x8, };
    }
}
