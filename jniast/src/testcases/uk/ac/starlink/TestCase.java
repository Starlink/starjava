package uk.ac.starlink;

import java.lang.reflect.Array;
import junit.framework.AssertionFailedError;

/**
 * This class extends {@link junit.framework.TestCase}, providing some 
 * additional assertions for convenience.  All the existing methods
 * of <tt>TestCase</tt> are simply delegated to the superclass.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TestCase extends junit.framework.TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @name  test case name
     */
    public TestCase( String name ) {
        super( name );
    }

    /**
     * Asserts that two arrays have the same contents.
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
        if ( ctype == char.class ) {
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
            // assert ctype == Object.class
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
     * Asserts that two arrays have the same contents.
     * For the assertion to be true, <tt>expected</tt> and <tt>actual</tt>
     * must both be array objects whose component types are of the same
     * class; they must have the same number of elements; and the
     * <i>i</i>th element of <tt>expected</tt> must be `equal' to the 
     * <i>i</i>th element of <tt>actual</tt>.  `Equality' in this sense
     * depends on the types of the arrays: if they are arrays of a primitive
     * type, the primitive values must be equal in the sense of the
     * <tt>==</tt> operator applied to said types, and if they are 
     * object arrays equality is assessed using the 
     * {@link java.lang.Object#equals} method.
     *
     * @param  expected  the expected array object
     * @param  actual    the actual array object
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( Object expected, Object actual ) {
        assertArrayEquals( null, expected, actual );
    }

    /**
     * Asserts that two arrays of type <tt>float[]</tt> have equal 
     * elements within a given tolerance.
     * Two elements which are both <tt>NaN</tt> are taken to match, and
     * if the expected element is infinity, the delta is ignored.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( String message, float[] expected, 
                                   float[] actual, float delta ) {
        
        /* Check both arrays are the same length. */
        assertEquals( combineMessages( message, "array length mismatch" ),
                      expected.length, actual.length );
        int nel = actual.length;

        /* Test all the elements of both arrays. */
        for ( int i = 0; i < nel; i++ ) {
            float v1 = expected[ i ];
            float v2 = actual[ i ];
            boolean finite = Float.isInfinite( v1 );
            if ( ( ! finite && ( v1 == v2 ) ) ||
                 ( finite && Math.abs( v1 - v2 ) <= delta ) ||
                 ( Float.isNaN( v1 ) && Float.isNaN( v2 ) ) ) {
                // elements match, no action
            }
            else {
                assertEquals( itemMismatchMessage( message, i ), 
                              v1, v2, delta );
            }
        }
    }

    /**
     * Asserts that two arrays of type <tt>float[]</tt> have equal 
     * elements within a given tolerance.
     * Two elements which are both <tt>NaN</tt> are taken to match, and
     * if the expected element is infinity, the delta is ignored.
     *
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( float[] expected, float[] actual, 
                                   float delta ) {
        assertArrayEquals( null, expected, actual, delta );
    }

    /**
     * Asserts that two arrays of type <tt>double[]</tt> have equal 
     * elements within a given tolerance.
     * Two elements which are both <tt>NaN</tt> are taken to match, and
     * if the expected element is infinity, the delta is ignored.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( String message, double[] expected, 
                                   double[] actual, double delta ) {

        /* Check both arrays are the same length. */
        assertEquals( combineMessages( message, "array length mismatch" ),
                      expected.length, actual.length );
        int nel = actual.length;

        /* Test all the elements of both arrays. */
        for ( int i = 0; i < nel; i++ ) {
            double v1 = expected[ i ];
            double v2 = expected[ i ];
            boolean finite = Double.isInfinite( v1 );
            if ( ( ! finite && ( v1 == v2 ) ) ||
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

    /**
     * Asserts that two arrays of type <tt>double[]</tt> have equal 
     * elements within a given tolerance.
     * Two elements which are both <tt>NaN</tt> are taken to match, and
     * if the expected element is infinity, the delta is ignored.
     *
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( double[] expected, double[] actual,
                                   double delta ) {
        assertArrayEquals( null, expected, actual, delta );
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
     * Asserts that two arrays of type <tt>float[]</tt> have exactly equal
     * elements.
     * Two elements which are both <tt>NaN</tt> are taken to match.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( String message, float[] expected, 
                                   float[] actual ) {
        assertArrayEquals( message, expected, actual, 0.0f );
    }

    /**
     * Asserts that two arrays of type <tt>float[]</tt> have exactly equal
     * elements.
     * Two elements which are both <tt>NaN</tt> are taken to match.
     *
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( float[] expected, float[] actual ) {
        assertArrayEquals( expected, actual, 0.0f );
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
     * Asserts that two <tt>double</tt>s are exactly equal apart.
     *
     * @param  expected  the expected value
     * @param  actual    the actual value
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertEquals( double expected, double actual ) {
        assertEquals( expected, actual, 0.0 );
    }

    /**
     * Asserts that two arrays of type <tt>double[]</tt> have exactly equal
     * elements.
     * Two elements which are both <tt>NaN</tt> are taken to match.
     *
     * @param  messsage  the message to output if the assertion fails
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( String message, double[] expected, 
                                   double[] actual ) {
        assertArrayEquals( message, expected, actual, 0.0 );
    }

    /**
     * Asserts that two arrays of type <tt>double[]</tt> have exactly equal
     * elements.
     * Two elements which are both <tt>NaN</tt> are taken to match.
     *
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( double[] expected, double[] actual ) {
        assertArrayEquals( expected, actual, 0.0 );
    }


    private String combineMessages( String msg, String detail ) {
        return ( msg == null ) ? ( msg + " - " + detail )
                               : detail;
    }

    private String itemMismatchMessage( String msg, int ix ) {
        return combineMessages( msg, "element [" + ix + "] mismatch" );
    }

}
