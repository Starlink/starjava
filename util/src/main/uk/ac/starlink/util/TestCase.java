package uk.ac.starlink.util;

import java.lang.reflect.Array;
import java.util.Random;
import junit.framework.AssertionFailedError;

/**
 * This class extends {@link junit.framework.TestCase}, providing some 
 * additional assertions and methods for providing test data for convenience.
 * All the existing methods of <tt>TestCase</tt> are simply delegated 
 * to the superclass.
 * <p>
 * Some of the methods are concerned with providing random values;
 * these are deterministic in that the random seed is set to a fixed
 * value when the test case is initialised, so a given test should
 * always be working with the same data, though the same call twice
 * in a given test will provide different random data.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TestCase extends junit.framework.TestCase {

    private Random rand = new Random( 23L );

    /**
     * Constructs a test case with the given name.
     *
     * @param  test case name
     */
    public TestCase( String name ) {
        super( name );
    }

    /**
     * Asserts that two arrays have exactly the same contents.
     * For the assertion to be true, <tt>expected</tt> and <tt>actual</tt>
     * must both be array objects whose component types are of the same
     * class; they must have the same number of elements; and the
     * <i>i</i>th element of <tt>expected</tt> must be `equal' to the 
     * <i>i</i>th element of <tt>actual</tt>.  `Equality' in this sense
     * depends on the types of the arrays: if they are arrays of a primitive
     * type, the primitive values must be equal, and if they are 
     * object arrays equality is assessed using the 
     * {@link java.lang.Object#equals} method.
     * In the case of <tt>float[]</tt> and <tt>double[]</tt> arrays,
     * elements which both have the value <tt>NaN</tt> are taken to match.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the expected array object
     * @param  actual    the actual array object
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( String message, Object expected, 
                                   Object actual ) {

        /* Check both objects have the same class. */
        assertEquals( combineMessages( message, 
                                       "array component class mismatch" ),
                     expected.getClass(), actual.getClass() );
        Class clazz = actual.getClass();

        /* Check both objects are arrays. */
        Class ctype = clazz.getComponentType();
        assertNotNull( combineMessages( message, "not array objects" ),
                       ctype );

        /* Check both arrays are the same length. */
        assertEquals( combineMessages( message, "array length mismatch" ),
                      Array.getLength( expected ), Array.getLength( actual ) );
        int nel = Array.getLength( actual );

        /* Test all the elements of each array.  Actual assertion testing
         * is done in-line, and the the superclass assertion methods are
         * only invoked in case of failure, at which point they handle
         * the message handling and error throwing and so on.
         * It is done like this for efficiency, to prevent generating a 
         * lot of unnecesary message strings. */
        if ( ctype == boolean.class ) {
            for ( int i = 0; i < nel; i++ ) {
                boolean v1 = ((boolean[]) expected)[ i ];
                boolean v2 = ((boolean[]) actual)[ i ];
                if ( v1 != v2 ) { 
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else if ( ctype == byte.class ) {
            for ( int i = 0; i < nel; i++ ) {
                byte v1 = ((byte[]) expected)[ i ];
                byte v2 = ((byte[]) actual)[ i ];
                if ( v1 != v2 ) { 
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else if ( ctype == short.class ) {
            for ( int i = 0; i < nel; i++ ) {
                short v1 = ((short[]) expected)[ i ];
                short v2 = ((short[]) actual)[ i ];
                if ( v1 != v2 ) { 
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else if ( ctype == int.class ) {
            for ( int i = 0; i < nel; i++ ) {
                int v1 = ((int[]) expected)[ i ];
                int v2 = ((int[]) actual)[ i ];
                if ( v1 != v2 ) { 
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else if ( ctype == long.class ) {
            for ( int i = 0; i < nel; i++ ) {
                long v1 = ((long[]) expected)[ i ];
                long v2 = ((long[]) actual)[ i ];
                if ( v1 != v2 ) { 
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else if ( ctype == float.class ) {
            for ( int i = 0; i < nel; i++ ) {
                float v1 = ((float[]) expected)[ i ];
                float v2 = ((float[]) actual)[ i ];
                if ( v1 != v2 && 
                     ! ( Float.isNaN( v1 ) && Float.isNaN( v2 ) ) ) { 
                    assertEquals( itemMismatchMessage( message, i ), 
                                  v1, v2, 0.0f );
                }
            }
        }
        else if ( ctype == double.class ) {
            for ( int i = 0; i < nel; i++ ) {
                double v1 = ((double[]) expected)[ i ];
                double v2 = ((double[]) actual)[ i ];
                if ( v1 != v2 &&
                     ! ( Double.isNaN( v1 ) && Double.isNaN( v2 ) ) ) { 
                    assertEquals( itemMismatchMessage( message, i ), 
                                  v1, v2, 0.0f );
                }
            }
        }
        else if ( ctype == char.class ) {
            for ( int i = 0; i < nel; i++ ) {
                char v1 = ((char[]) expected)[ i ];
                char v2 = ((char[]) actual)[ i ];
                if ( v1 != v2 ) { 
                    assertEquals( itemMismatchMessage( message, i ), 
                                  v1, v2, 0.0 );
                }
            }
        }
        else {
            // it's an array of objects
            for ( int i = 0; i < nel; i++ ) {
                Object v1 = Array.get( expected, i );
                Object v2 = Array.get( actual, i );
                if ( v1 != v2 ) { 
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
    }

    /**
     * Asserts that two objects are not arrays with the same contents.
     * This assertion has exactly the opposite sense to that of 
     * {@link #assertArrayEquals(String,Object,Object)}.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the expected array object
     * @param  actual    the actual array object,
     *                   asserted <i>not</i> to match <tt>expected</tt>
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayNotEquals( String message, Object expected,
                                      Object actual ) {
        boolean equal;
        try {
            assertArrayEquals( message, expected, actual );
            equal = true;
        }
        catch ( AssertionFailedError e ) {
            equal = false;
        }
        assertTrue( message, ! equal );
    }

    /**
     * Asserts that two arrays have exactly the same contents.
     * For the assertion to be true, <tt>expected</tt> and <tt>actual</tt>
     * must both be array objects whose component types are of the same
     * class; they must have the same number of elements; and the
     * <i>i</i>th element of <tt>expected</tt> must be `equal' to the 
     * <i>i</i>th element of <tt>actual</tt>.  `Equality' in this sense
     * depends on the types of the arrays: if they are arrays of a primitive
     * type, the primitive values must be equal, and if they are 
     * object arrays equality is assessed using the 
     * {@link java.lang.Object#equals} method.
     * In the case of <tt>float[]</tt> and <tt>double[]</tt> arrays,
     * elements which both have the value <tt>NaN</tt> are taken to match.
     *
     * @param  expected  the expected array object
     * @param  actual    the actual array object
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( Object expected, Object actual ) {
        assertArrayEquals( null, expected, actual );
    }

    /**
     * Asserts that two objects are not arrays with the same contents.
     * This assertion has exactly the opposite sense to that of 
     * {@link #assertArrayEquals(Object,Object)}.
     *
     * @param  expected  the expected array object
     * @param  actual    the actual array object,
     *                   asserted <i>not</i> to match <tt>expected</tt>
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayNotEquals( Object expected, Object actual ) {
        assertArrayNotEquals( null, expected, actual );
    }


    /**
     * Asserts that two numeric arrays have the same contents 
     * within a given tolerance.
     * For the assertion to be true, <tt>expected</tt> and <tt>actual</tt>
     * must both be primitive numeric array objects whose 
     * component types are of the same
     * class; they must have the same number of elements; and the
     * <i>i</i>th element of <tt>expected</tt> must be `equal' to the 
     * <i>i</i>th element of <tt>actual</tt>.
     * Equality in this sense normally means 
     * <tt>(abs(expected[i]-actual[i]&lt;delta)</tt>, 
     * but in the case of <tt>float[]</tt> and <tt>double[]</tt> arrays,
     * elements which both have the value <tt>NaN</tt> are taken to match,
     * and if the expected element is infinite, then <tt>delta</tt>
     * is ignored.
     * <p>
     * If <tt>delta&lt;1</tt> and the arrays are of integer type, 
     * this method does the same as
     * {@link #assertArrayEquals(String,Object,Object)}.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( String message, Object expected, 
                                   Object actual, double delta ) {
        
        /* Check both objects have the same class. */
        assertEquals( combineMessages( message, 
                                       "array component class mismatch" ),
                     expected.getClass(), actual.getClass() );
        Class clazz = actual.getClass();

        /* Check both objects are arrays. */
        Class ctype = clazz.getComponentType();
        assertNotNull( combineMessages( message, "not array objects" ),
                       ctype );

        /* Check both arrays are the same length. */
        assertEquals( combineMessages( message, "array length mismatch" ),
                      Array.getLength( expected ), Array.getLength( actual ) );
        int nel = Array.getLength( actual );

        /* Test all the elements of each array.  Actual assertion testing
         * is done in-line, and the the superclass assertion methods are
         * only invoked in case of failure, at which point they handle
         * the message handling and error throwing and so on.
         * It is done like this for efficiency, to prevent generating a 
         * lot of unnecesary message strings. */
        delta = Math.abs( delta );
        if ( ctype == byte.class ) {
            for ( int i = 0; i < nel; i++ ) {
                byte v1 = ((byte[]) expected)[ i ];
                byte v2 = ((byte[]) actual)[ i ];
                if ( Math.abs( v1 - v2 ) > delta ) {
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else if ( ctype == short.class ) {
            for ( int i = 0; i < nel; i++ ) {
                short v1 = ((short[]) expected)[ i ];
                short v2 = ((short[]) actual)[ i ];
                if ( Math.abs( v1 - v2 ) > delta ) {
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else if ( ctype == int.class ) {
            for ( int i = 0; i < nel; i++ ) {
                int v1 = ((int[]) expected)[ i ];
                int v2 = ((int[]) actual)[ i ];
                if ( Math.abs( v1 - v2 ) > delta ) {
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else if ( ctype == long.class ) {
            for ( int i = 0; i < nel; i++ ) {
                long v1 = ((long[]) expected)[ i ];
                long v2 = ((long[]) actual)[ i ];
                if ( Math.abs( v1 - v2 ) > delta ) {
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else if ( ctype == float.class ) {
            for ( int i = 0; i < nel; i++ ) {
                float v1 = ((float[]) expected)[ i ];
                float v2 = ((float[]) actual)[ i ];
                boolean finite = ! Float.isInfinite( v1 );
                if ( ( ! finite && ( v1 == v2 ) ) ||
                     ( finite && Math.abs( v1 - v2 ) <= delta ) ||
                     ( Float.isNaN( v1 ) && Float.isNaN( v2 ) ) ) {
                    // elements match, no action
                }
                else {
                    assertEquals( itemMismatchMessage( message, i ), 
                                  v1, v2, (float) delta );
                }
            }
        }
        else if ( ctype == double.class ) {
            for ( int i = 0; i < nel; i++ ) {
                double v1 = ((double[]) expected)[ i ];
                double v2 = ((double[]) actual)[ i ];
                boolean finite = ! Double.isInfinite( v1 );
                if ( ( ! finite && v1 == v2 ) ||
                     ( finite && Math.abs( v1 - v2 ) <= delta ) ||
                     ( Double.isNaN( v1 ) && Double.isNaN( v2 ) ) ) {
                    // elements match, no action
                }
                else {
                    assertEquals( itemMismatchMessage( message, i ),
                                  v1, v2, delta );
                }
            }
        }
        else if ( ctype == char.class ) {
            for ( int i = 0; i < nel; i++ ) {
                char v1 = ((char[]) expected)[ i ];
                char v2 = ((char[]) actual)[ i ];
                if ( Math.abs( v1 - v2 ) > delta ) {
                    assertEquals( itemMismatchMessage( message, i ), v1, v2 );
                }
            }
        }
        else {
            throw new IllegalArgumentException( 
                "Non-numeric array " + ctype + "[] not permitted" );
        }
    }

    /**
     * Asserts that two objects are not numeric arrays having the same
     * contents within a given tolerance.
     * This assertion has exactly the opposite sense to that of 
     * {@link #assertArrayEquals(String,Object,Object,double)}.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values,
     *                   asserted <i>not</i> to match <tt>expected</tt>
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayNotEquals( String message, Object expected,
                                      Object actual, double delta ) {
        boolean equal;
        try {
            assertArrayEquals( message, expected, actual, delta );
            equal = true;
        }
        catch ( AssertionFailedError e ) {
            equal = false;
        }
        assertTrue( message, ! equal );
    }

    /**
     * Asserts that two numeric arrays have the same contents 
     * within a given tolerance.
     * For the assertion to be true, <tt>expected</tt> and <tt>actual</tt>
     * must both be primitive numeric array objects whose component 
     * types are of the same
     * class; they must have the same number of elements; and the
     * <i>i</i>th element of <tt>expected</tt> must be `equal' to the 
     * <i>i</i>th element of <tt>actual</tt>.
     * Equality in this sense normally means 
     * <tt>(abs(expected[i]-actual[i]&lt;delta)</tt>, 
     * but in the case of <tt>float[]</tt> and <tt>double[]</tt> arrays,
     * elements which both have the value <tt>NaN</tt> are taken to match,
     * and if the expected element is infinite, then <tt>delta</tt>
     * is ignored.
     * <p>
     * If <tt>delta&lt;1</tt> and the arrays are of integer type, 
     * this method does the same as
     * {@link #assertArrayEquals(Object,Object)}.
     *
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( Object expected, Object actual,
                                   double delta ) {
        assertArrayEquals( null, expected, actual, delta );
    }

    /**
     * Asserts that two objects are not numeric arrays having the same
     * contents within a given tolerance.
     * This assertion has exactly the opposite sense to that of 
     * {@link #assertArrayEquals(Object,Object,double)}.
     *
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values,
     *                   asserted <i>not</i> to match <tt>expected</tt>
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayNotEquals( Object expected, Object actual,
                                      double delta ) {
        assertArrayNotEquals( null, expected, actual, delta );
    }
   
    /**
     * Asserts that two <tt>float</tt>s are exactly equal.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the expected value
     * @param  actual    the actual value
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertEquals( String message, float expected, float actual ) {
        assertEquals( message, expected, actual, 0.0f );
    }

    /**
     * Asserts that two <tt>float</tt>s are exactly equal.
     *
     * @param  expected  the expected value
     * @param  actual    the actual value
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertEquals( float expected, float actual ) {
        assertEquals( expected, actual, 0.0f );
    }

    /**
     * Asserts that two <tt>double</tt>s are exactly equal.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the expected value
     * @param  actual    the actual value
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertEquals( String message, double expected, double actual ) {
        assertEquals( message, expected, actual, 0.0 );
    }

    /**
     * Asserts that two <tt>double</tt>s are exactly equal.
     *
     * @param  expected  the expected value
     * @param  actual    the actual value
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertEquals( double expected, double actual ) {
        assertEquals( expected, actual, 0.0 );
    }


    /**
     * Fills a given array with random numbers between two floating point
     * values.
     * If the supplied minimum and maximum values are outside the 
     * range appropriate for the primitive type in question the range
     * will be suitably clipped.
     *
     * @param  array  an array of primitives to be filled with 
     *                random values
     * @param  min    the smallest value which will be used
     *                (will be converted to the appropriate primitive type)
     * @param  max    the largest value which will be used
     *                (will be converted to the appropriate primitive type)
     * @throws IllegalArgumentException  if <tt>array</tt> is not an array
     *         of a suitable primitive type
     */
    public void fillRandom( Object array, double min, double max ) {
        Class clazz = array.getClass().getComponentType();
        int size = Array.getLength( array );
        if ( clazz == byte.class ) {
            min = Math.max( min, (double) Byte.MIN_VALUE );
            max = Math.min( max, (double) Byte.MAX_VALUE );
            double range = max - min;
            byte[] arr = (byte[]) array;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = (byte) ( min + rand.nextDouble() * range );
            }
        }
        else if ( clazz == short.class ) {
            min = Math.max( min, (double) Short.MIN_VALUE );
            max = Math.min( max, (double) Short.MAX_VALUE );
            double range = max - min;
            short[] arr = (short[]) array;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = (short) ( min + rand.nextDouble() * range );
            }
        }
        else if ( clazz == int.class ) {
            min = Math.max( min, (double) Integer.MIN_VALUE );
            max = Math.min( max, (double) Integer.MAX_VALUE );
            double range = max - min;
            int[] arr = (int[]) array;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = (int) ( min + rand.nextDouble() * range );
            }
        }
        else if ( clazz == long.class ) {
            min = Math.max( min, (double) Long.MIN_VALUE );
            max = Math.min( max, (double) Long.MAX_VALUE );
            double range = max - min;
            long[] arr = (long[]) array;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = (long) ( min + rand.nextDouble() * range );
            }
        }
        else if ( clazz == float.class ) {
            min = Math.max( min, (double) -Float.MAX_VALUE );
            max = Math.min( max, (double) Float.MAX_VALUE );
            double range = max - min;
            float[] arr = (float[]) array;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = (float) ( min + rand.nextDouble() * range );
            }
        }
        else if ( clazz == double.class ) {
            double range = max - min;
            double[] arr = (double[]) array;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = (double) ( min + rand.nextDouble() * range );
            }
        }
        else {
            throw new IllegalArgumentException( 
                "Unsupported array type or not an array " + array.getClass() );
        }
    }

    /**
     * Fills a given array with random numbers between two integer values.
     * 
     * If the supplied minimum and maximum values are outside the 
     * range appropriate for the primitive type in question the range
     * will be suitably clipped.
     *
     * @param  array  an array of primitives to be filled with 
     *                random values
     * @param  min    the smallest value which will be used
     *                (will be converted to the appropriate primitive type)
     * @param  max    the largest value which will be used
     *                (will be converted to the appropriate primitive type)
     * @throws IllegalArgumentException  if <tt>array</tt> is not an array
     *         of a suitable primitive type
     */
    public void fillRandom( Object array, int min, int max ) {
        fillRandom( array, (double) min, (double) max + 0.99 );
    }

    /**
     * Fills a given array with a regular pattern of integer values.
     * The elements of the array will take the values 
     * <tt>min, min+1, min+2 .. max-1, min, min+1, min+2..</tt> and so on.
     * If the <tt>max&lt;min</tt> then the values will start at <tt>min</tt>
     * and keep increasing.
     * <p>
     * The results might not be as expected if you use a <tt>min</tt> and
     * <tt>max</tt> values outside the range of the numeric type in question.
     *
     * @param  array   an array of primitives to be filled with cycling values
     * @param  min     the first value
     * @param  max     the highest value, or if less than <tt>min</tt> an
     *                 indication that there is no maximum
     * @throws IllegalArgumentException  if <tt>array</tt> is not an array
     *         of a suitable primitive type
     */
    public void fillCycle( Object array, int min, int max ) {
        Class clazz = array.getClass().getComponentType();
        int size = Array.getLength( array );
        if ( clazz == byte.class ) {
            byte[] arr = (byte[]) array;
            byte val = (byte) min;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = val++;
                if ( val > max ) val = (byte) min;
            }
        }
        else if ( clazz == short.class ) {
            short[] arr = (short[]) array;
            short val = (short) min;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = val++;
                if ( val > max ) val = (short) min;
            }
        }
        else if ( clazz == int.class ) {
            int[] arr = (int[]) array;
            int val = min;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = val++;
                if ( val > max ) val = min;
            }
        }
        else if ( clazz == long.class ) {
            long[] arr = (long[]) array;
            long val = min;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = val++;
                if ( val > max ) val = min;
            }
        }
        else if ( clazz == float.class ) {
            float[] arr = (float[]) array;
            float val = min;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = val++;
                if ( val > max ) val = min;
            }
        }
        else if ( clazz == double.class ) {
            double[] arr = (double[]) array;
            double val = min;
            for ( int i = 0; i < size; i++ ) {
                arr[ i ] = val++;
                if ( val > max ) val = min;
            }
        }
        else {
            throw new IllegalArgumentException(
                "Unsupported array type or not an array " + array.getClass() );
        }
    }

    private String combineMessages( String msg, String detail ) {
        return ( msg == null ) ? ( msg + " - " + detail )
                               : detail;
    }

    private String itemMismatchMessage( String msg, int ix ) {
        return combineMessages( msg, "element [" + ix + "] mismatch" );
    }

}
