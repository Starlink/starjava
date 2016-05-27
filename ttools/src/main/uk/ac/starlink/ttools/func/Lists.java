// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.io.IOException;
import uk.ac.starlink.ttools.filter.QuantCalc;

/**
 * Functions which operate on lists of values.
 *
 * <p>Some of these resemble similar functions in the <code>Arrays</code> class,
 * and in some cases are interchangeable, but these are easier to use
 * on non-array values because you don't have to explicitly wrap up
 * lists of arguments as an array.
 * However, for implementation reasons, most of the functions defined here
 * can be used on values which are already <code>double[]</code> arrays
 * (for instance array-valued columns) rather than as comma-separated
 * lists of floating point values.
 *
 * @author   Mark Taylor
 * @since    6 Jan 2015
 */
public class Lists {

    /**
     * Private constructor prevents instantiation.
     */
    private Lists() {
    }

    /**
     * Returns the sum of all the non-blank supplied arguments.
     *
     * @example  <code>sum(1, 3, 99) = 103</code>
     * @example  <code>sum(1, 3, NaN) = 4</code>
     *
     * @param   values   one or more numeric values
     * @return   sum of <code>values</code>
     */
    public static double sum( double... values ) {
        if ( values == null ) {
            return Double.NaN;
        }
        double sum = 0;
        for ( double v : values ) {
            if ( ! Double.isNaN( v ) ) {
                sum += v;
            }
        }
        return sum;
    }

    /**
     * Returns the mean of all the non-blank supplied arguments.
     *
     * @example   <code>mean(2, 4, 6, 8) = 5</code>
     * @example   <code>mean(100.5, 99.5, NaN) = 100</code>
     *
     * @param  values  one or more numeric values
     * @return   mean of <code>values</code>
     */
    public static double mean( double... values ) {
        if ( values == null ) {
            return Double.NaN;
        }
        int n = values.length;
        double sum = 0;
        double count = 0;
        for ( int i = 0; i < n; i++ ) {
            double v = values[ i ];
            if ( ! Double.isNaN( v ) ) {
                count++;
                sum += v;
            }
        }
        return count == 0 ? Double.NaN : sum / (double) count;
    }

    /**
     * Returns the population variance of the non-blank supplied arguments.
     *
     * @example  <code>variance(0, 3, 4, 3, 0) = 2.8</code>
     * @example  <code>variance(0, 3, NaN, 3, NaN) = 2</code>
     *
     * @param   values  one or more numeric values
     * @return   population variance of <code>values</code>
     */
    public static double variance( double... values ) {
        if ( values == null ) {
            return Double.NaN;
        }
        int n = values.length; 
        double sum = 0;
        double sum2 = 0; 
        int count = 0;
        for ( int i = 0; i < n; i++ ) {
            double v = values[ i ];
            if ( ! Double.isNaN( v ) ) {
                sum += v;
                sum2 += v * v;
                count++;
             }
        }
        if ( count == 0 ) {
            return Double.NaN;
        }
        else {
            double mean = sum / (double) count;
            return sum2 / (double) count - mean * mean;
        }
    }

    /**
     * Returns the population standard deviation of the non-blank supplied
     * arguments.
     *
     * @example  <code>stdev(-3, -2, 0, 0, 1, 2, 3, 4, 5, 6) = 2.8</code>
     *
     * @param  values  one or more numeric values
     * @return   population standard deviation of <code>values</code>
     */
    public static double stdev( double... values ) {
        return Math.sqrt( variance( values ) );
    }

    /**
     * Returns the minimum of all the non-blank supplied arguments.
     *
     * @example   <code>min(20, 25, -50, NaN, 101) = -50</code>
     *
     * @param   values   one or more numeric values
     * @return   minimum of <code>values</code>
     */
    public static double min( double... values ) {
        double min = Double.NaN;
        if ( values != null ) {
            for ( double v : values ) {
                if ( Double.isNaN( min ) || v < min ) {
                    min = v;
                }
            }
        }
        return min;
    }

    /**
     * Returns the maximum of all the non-blank supplied arguments.
     *
     * @example  <code>max(20, 25, -50, NaN, 101) = 101</code>
     *
     * @param   values  one or more numeric values
     * @return   maximum of <code>values</code>
     */
    public static double max( double... values ) {
        double max = Double.NaN;
        if ( values != null ) {
            for ( double v : values ) {
                if ( Double.isNaN( max ) || v > max ) {
                    max = v;
                }
            }
        }
        return max;
    }

    /**
     * Returns the median of all the non-blank supplied arguments.
     *
     * @example   <code>median(-100000, 5, 6, 7, 8) = 6</code>
     *
     * @param  values  one or more numeric values
     * @return  median of <code>values</code>
     */
    public static double median( double... values ) {
        if ( values == null ) {
            return Double.NaN;
        }
        QuantCalc qc;
        try {
            qc = QuantCalc.createInstance( Double.class, values.length );
        }
        catch ( IOException e ) {
            assert false;
            return Double.NaN;
        }
        for ( double v : values ) {
            qc.acceptDatum( new Double( v ) );
        }
        qc.ready();
        Number median = qc.getQuantile( 0.5 );
        return median instanceof Number ? ((Number) median).doubleValue()
                                        : Double.NaN;
    }

    /**
     * Returns the number of true values in a list of boolean arguments.
     * Note if any of the values are blank, the result may be blank as well.
     *
     * @example  <code>countTrue(false, false, true, false, true) = 2</code>
     *
     * @param  values  one or more true/false values
     * @return   number of elements of <code>values</code> that are true
     */
    public static int countTrue( boolean... values ) {
        int count = 0;
        if ( values != null ) {
            for ( boolean b : values ) {
                if ( b ) {
                    count++;
                }
            }
        }
        return count;
    }
}
