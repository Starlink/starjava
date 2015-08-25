// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.io.IOException;
import java.lang.reflect.Array;
import uk.ac.starlink.ttools.filter.QuantCalc;

/**
 * Functions which operate on array-valued cells.
 * The array parameters of these functions can only be used on values
 * which are already arrays (usually, numeric arrays).
 * In most cases that means on values in table columns which are declared
 * as array-valued.  FITS and VOTable tables can have columns which contain
 * array values, but other formats such as CSV cannot.
 *
 * <p>The functions fall into a number of categories:
 * <ul>
 * <li>Aggregating operations, which map an array value to a scalar, including
 *     <code>size</code>,
 *     <code>count</code>,
 *     <code>maximum</code>,
 *     <code>minimum</code>,
 *     <code>sum</code>,
 *     <code>mean</code>,
 *     <code>median</code>,
 *     <code>quantile</code>,
 *     <code>stdev</code>,
 *     <code>variance</code>,
 *     <code>join</code>.
 *     </li>
 * <li>Operations on one or more arrays which produce array results, including
 *     <code>add</code>,
 *     <code>subtract</code>,
 *     <code>multiply</code>,
 *     <code>divide</code>,
 *     <code>reciprocal</code>,
 *     <code>condition</code>.
 *     </li>
 * <li>A set of functions named <code>array</code> with various
 *     numbers of arguments, which let you assemble an array value from a list
 *     of scalar numbers.  This can be used for instance to get the mean of
 *     a set of three magnitudes by using an expression like
 *     "<code>mean(array(jmag, hmag, kmag))</code>".
 *     </li>
 * </ul>
 *
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
     * Returns the result of adding two numeric arrays element by element.
     * Both arrays must be numeric, and the arrays must have the same length.
     * If either of those conditions is not true, <code>null</code> is returned.
     * The types of the arrays do not need to be the same,
     * so for example it is permitted to add an integer array
     * to a floating point array.
     *
     * @example  <code>add(array(1,2,3), array(0.1,0.2,0.3))
     *                 = [1.1, 2.2, 3.3]</code>
     *
     * @param   array1  first array of numeric values
     * @param   array2  second array of numeric values
     * @return    element-by-element sum of
     *            <code>array1</code> and <code>array2</code>,
     *            the same length as the input arrays
     */
    public static double[] add( Object array1, Object array2 ) {
        int n = getNumericArrayLength( array1 );
        if ( n >= 0 && getNumericArrayLength( array2 ) == n ) {
            double[] out = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                out[ i ] = Array.getDouble( array1, i )
                         + Array.getDouble( array2, i );
            }
            return out;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the result of adding a constant value to every element of
     * a numeric array.
     * If the supplied <code>array</code> argument is not a numeric array,
     * <code>null</code> is returned.
     *
     * @example  <code>add(array(1,2,3), 10) = [11,12,13]</code>
     *
     * @param  array   array input
     * @param  constant   value to add to each array element
     * @return   array output,
     *           the same length as the <code>array</code> parameter
     */
    public static double[] add( Object array, double constant ) {
        int n = getNumericArrayLength( array );
        if ( n >= 0 ) {
            double[] out = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                out[ i ] = Array.getDouble( array, i ) + constant;
            }
            return out;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the result of subtracting one numeric array from the other
     * element by element.
     * Both arrays must be numeric, and the arrays must have the same length.
     * If either of those conditions is not true, <code>null</code> is returned.
     * The types of the arrays do not need to be the same,
     * so for example it is permitted to subtract an integer array
     * from a floating point array.
     *
     * @example  <code>subtract(array(1,2,3), array(0.1,0.2,0.3))
     *                 = [0.9, 1.8, 2.7]</code>
     *
     * @param   array1  first array of numeric values
     * @param   array2  second array of numeric values
     * @return    element-by-element difference of
     *            <code>array1</code> and <code>array2</code>,
     *            the same length as the input arrays
     */
    public static double[] subtract( Object array1, Object array2 ) {
        int n = getNumericArrayLength( array1 );
        if ( n >= 0 && getNumericArrayLength( array2 ) == n ) {
            double[] out = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                out[ i ] = Array.getDouble( array1, i )
                         - Array.getDouble( array2, i );
            }
            return out;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the result of multiplying two numeric arrays element by element.
     * Both arrays must be numeric, and the arrays must have the same length.
     * If either of those conditions is not true, <code>null</code> is returned.
     * The types of the arrays do not need to be the same,
     * so for example it is permitted to multiply an integer array
     * by a floating point array.
     *
     * @example  <code>multiply(array(1,2,3), array(2,4,6)) = [2, 8, 18]</code>
     *
     * @param   array1  first array of numeric values
     * @param   array2  second array of numeric values
     * @return    element-by-element product of
     *            <code>array1</code> and <code>array2</code>,
     *            the same length as the input arrays
     */
    public static double[] multiply( Object array1, Object array2 ) {
        int n = getNumericArrayLength( array1 );
        if ( n >= 0 && getNumericArrayLength( array2 ) == n ) {
            double[] out = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                out[ i ] = Array.getDouble( array1, i )
                         * Array.getDouble( array2, i );
            }
            return out;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the result of multiplying every element of a numeric array
     * by a constant value.
     * If the supplied <code>array</code> argument is not a numeric array,
     * <code>null</code> is returned.
     *
     * @example  <code>multiply(array(1,2,3), 2) = [2, 4, 6]</code>
     *
     * @param  array   array input
     * @param  constant   value by which to multiply each array element
     * @return   array output,
     *           the same length as the <code>array</code> parameter
     */
    public static double[] multiply( Object array, double constant ) {
        int n = getNumericArrayLength( array );
        if ( n >= 0 ) {
            double[] out = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                out[ i ] = Array.getDouble( array, i ) * constant;
            }
            return out;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the result of dividing two numeric arrays element by element.
     * Both arrays must be numeric, and the arrays must have the same length.
     * If either of those conditions is not true, <code>null</code> is returned.
     * The types of the arrays do not need to be the same,
     * so for example it is permitted to divide an integer array
     * by a floating point array.
     *
     * @example  <code>divide(array(0,9,4), array(1,3,8)) = [0, 3, 0.5]</code>
     *
     * @param   array1  array of numerator values (numeric)
     * @param   array2  array of denominator values (numeric)
     * @return    element-by-element result of <code>array1[i]/array2[i]</code>
     *            the same length as the input arrays
     */
    public static double[] divide( Object array1, Object array2 ) {
        int n = getNumericArrayLength( array1 );
        if ( n >= 0 && getNumericArrayLength( array2 ) == n ) {
            double[] out = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                out[ i ] = Array.getDouble( array1, i )
                         / Array.getDouble( array2, i );
            }
            return out;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the result of taking the reciprocal of every element of
     * a numeric array.
     * If the supplied <code>array</code> argument is not a numeric array,
     * <code>null</code> is returned.
     *
     * @example  <code>reciprocal(array(1,2,0.25) = [1, 0.5, 4]</code>
     *
     * @param   array  array input
     * @return  array output,
     *          the same length as the <code>array</code> parameter
     */
    public static double[] reciprocal( Object array ) {
        int n = getNumericArrayLength( array );
        if ( n >= 0 ) {
            double[] out = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                out[ i ] = 1.0 / Array.getDouble( array, i );
            }
            return out;
        }
        else {
            return null;
        }
    }

    /**
     * Maps a boolean array to a numeric array by using supplied numeric
     * values to represent true and false values from the input array.
     *
     * <p>This has the same effect as applying the expression
     * <code>outArray[i] = flagArray[i] ? trueValue : falseValue</code>.
     *
     * @example   <code>condition([true, false, true], 1, 0) = [1, 0, 1]</code>
     *
     * @param   flagArray   array of boolean values
     * @param   trueValue   output value corresponding to an input true value
     * @param   falseValue  output value corresponding to an input false value
     * @return    output numeric array, same length as <code>flagArray</code>
     */
    public static double[] condition( boolean[] flagArray,
                                      double trueValue, double falseValue ) {
        int n = flagArray.length;
        double[] out = new double[ n ];
        for ( int i = 0; i < n; i++ ) {
            out[ i ] = flagArray[ i ] ? trueValue : falseValue;
        }
        return out;
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

    /**
     * Returns the length of a primitive numeric array.
     * If the supplied object is not a primitive numeric array,
     * -1 will be returned.
     *
     * @param   array   object
     * @return   length of array, or -1
     */
    private static int getNumericArrayLength( Object array ) {
        return ( array instanceof byte[] 
              || array instanceof short[]
              || array instanceof int[]
              || array instanceof long[]
              || array instanceof float[]
              || array instanceof double[] )
            ? Array.getLength( array )
            : -1;
    }
}
