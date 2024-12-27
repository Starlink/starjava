package uk.ac.starlink.util;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Random;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import org.opentest4j.AssertionFailedError;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static org.junit.jupiter.api.Assertions.*;


/**
 * This class provides some additional assertions and methods for providing
 * test data for convenience.
 * <p>
 * Some of the methods are concerned with providing random values;
 * these are deterministic in that the random seed is set to a fixed
 * value when the test case is initialised, so a given test should
 * always be working with the same data, though the same call twice
 * in a given test will provide different random data.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TestCase {

    private Random rand = new Random( 23L );

    /** Flags for {@link #assertDOMEquals} */
    public static final short IGNORE_ATTRIBUTE_PRESENCE = 1;
    public static final short IGNORE_ATTRIBUTE_VALUE = 2;
    public static final short IGNORE_WHITESPACE = 4;    
    public static final short IGNORE_COMMENTS = 8;
 
    static private javax.xml.parsers.DocumentBuilder docParser;

    /**
     * Asserts that two arrays have exactly the same contents.
     * For the assertion to be true, <code>expected</code> and
     * <code>actual</code>
     * must both be array objects whose component types are of the same
     * class; they must have the same number of elements; and the
     * <i>i</i>th element of <code>expected</code> must be `equal' to the 
     * <i>i</i>th element of <code>actual</code>.  `Equality' in this sense
     * depends on the types of the arrays: if they are arrays of a primitive
     * type, the primitive values must be equal, and if they are 
     * object arrays equality is assessed using the 
     * {@link java.lang.Object#equals} method.
     * In the case of <code>float[]</code> and <code>double[]</code> arrays,
     * elements which both have the value <code>NaN</code> are taken to match.
     *
     * @param  message   the message to output if the assertion fails
     * @param  expected  the expected array object
     * @param  actual    the actual array object
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( String message, Object expected,
                                   Object actual ) {

        /* OK if both are null. */
        if ( expected == null && actual == null ) {
            return;
        }

        /* Check both objects have the same class. */
        assertEquals(
                getClassName( expected ),
                getClassName( actual ),
                combineMessages( message, "array component class mismatch" )
        );
        Class<?> clazz = actual.getClass();

        /* Check both objects are arrays. */
        Class<?> ctype = clazz.getComponentType();

        assertNotNull(ctype,
                combineMessages( message, "not array objects" ));

        /* Test all the elements of each array.  Actual assertion testing
         * is done in-line, and the superclass assertion methods are
         * only invoked in case of failure, at which point they handle
         * the message handling and error throwing and so on.
         * It is done like this for efficiency, to prevent generating a 
         * lot of unnecessary message strings. */
        int nel = Math.min( Array.getLength( actual ),
                            Array.getLength( expected ) );
        if ( ctype == boolean.class ) {
            for ( int i = 0; i < nel; i++ ) {
                boolean v1 = ( (boolean[]) expected)[ i ];
                boolean v2 = ( (boolean[]) actual)[ i ];
                if ( v1 != v2 ) {
                    assertEquals(v1,v2, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == byte.class ) {
            for ( int i = 0; i < nel; i++ ) {
                byte v1 = ( (byte[]) expected)[ i ];
                byte v2 = ( (byte[]) actual)[ i ];
                if ( v1 != v2 ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == short.class ) {
            for ( int i = 0; i < nel; i++ ) {
                short v1 = ( (short[]) expected)[ i ];
                short v2 = ( (short[]) actual)[ i ];
                if ( v1 != v2 ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == int.class ) {
            for ( int i = 0; i < nel; i++ ) {
                int v1 = ( (int[]) expected)[ i ];
                int v2 = ( (int[]) actual)[ i ];
                if ( v1 != v2 ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == long.class ) {
            for ( int i = 0; i < nel; i++ ) {
                long v1 = ( (long[]) expected)[ i ];
                long v2 = ( (long[]) actual)[ i ];
                if ( v1 != v2 ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == float.class ) {
            for ( int i = 0; i < nel; i++ ) {
                float v1 = ( (float[]) expected)[ i ];
                float v2 = ( (float[]) actual)[ i ];
                if ( v1 != v2 && 
                     ! ( Float.isNaN( v1 ) && Float.isNaN( v2 ) ) ) { 
                    assertEquals(v1, v2, 0.0f, itemMismatchMessage( message, i ) );
                }
            }
        }
        else if ( ctype == double.class ) {
            for ( int i = 0; i < nel; i++ ) {
                double v1 = ( (double[]) expected)[ i ];
                double v2 = ( (double[]) actual)[ i ];
                if ( v1 != v2 &&
                     ! ( Double.isNaN( v1 ) && Double.isNaN( v2 ) ) ) { 
                    assertEquals(v1, v2, 0.0f, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == char.class ) {
            for ( int i = 0; i < nel; i++ ) {
                char v1 = ( (char[]) expected)[ i ];
                char v2 = ( (char[]) actual)[ i ];
                if ( v1 != v2 ) {
                    //assertEquals( itemMismatchMessage( message, i ), v1, v2, 0.0 );
                    assertEquals( v1, v2, 0.0, itemMismatchMessage( message, i ));
                }
            }
        }
        else {
            // it's an array of objects
            for ( int i = 0; i < nel; i++ ) {
                Object v1 = Array.get( expected, i );
                Object v2 = Array.get( actual, i );
                if ( v1 == null && v2 == null ) {
                    // ok
                }
                else if ( v1 == null || v2 == null ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
                else if ( v1.getClass().isArray() && v2.getClass().isArray() ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
                else if ( ! v1.equals( v2 ) ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
            }
        }

        /* Finally check both arrays are the same length. */
        assertEquals(
                Array.getLength( expected ),
                Array.getLength( actual ), combineMessages( message, "array length mismatch" )
        );
    }

    /**
     * Asserts that two objects are not arrays with the same contents.
     * This assertion has exactly the opposite sense to that of
     * {@link #assertArrayEquals(String,Object,Object)}.
     *
     * @param  message   the message to output if the assertion fails
     * @param  expected  the expected array object
     * @param  actual    the actual array object,
     *                   asserted <i>not</i> to match <code>expected</code>
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public static void assertArrayNotEquals( String message, Object[] expected,
                                      Object[] actual ) {
        boolean equal;
        try {
            assertEquals( expected, actual, message );
            equal = true;
        }
        catch ( AssertionFailedError e ) {
            equal = false;
        }
        assertFalse(equal, message);
    }

    /**
     * Asserts that two objects are not arrays with the same contents.
     * This assertion has exactly the opposite sense to that of 
     * {@link #assertArrayEquals(String,Object,Object)}.
     *
     * @param  message   the message to output if the assertion fails
     * @param  expected  the expected array object
     * @param  actual    the actual array object,
     *                   asserted <i>not</i> to match <code>expected</code>
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayNotEquals( String message, Object expected,
                                      Object actual ) {
        boolean equal;
        try {
            assertEquals( expected, actual, message );
            equal = true;
        }
        catch ( AssertionFailedError e ) {
            equal = false;
        }
        assertFalse(equal, message);
    }

    /**
     * Asserts that two arrays have exactly the same contents.
     * For the assertion to be true,
     * <code>expected</code> and <code>actual</code>
     * must both be array objects whose component types are of the same
     * class; they must have the same number of elements; and the
     * <i>i</i>th element of <code>expected</code> must be `equal' to the 
     * <i>i</i>th element of <code>actual</code>.  `Equality' in this sense
     * depends on the types of the arrays: if they are arrays of a primitive
     * type, the primitive values must be equal, and if they are 
     * object arrays equality is assessed using the 
     * {@link java.lang.Object#equals} method.
     * In the case of <code>float[]</code> and <code>double[]</code> arrays,
     * elements which both have the value <code>NaN</code> are taken to match.
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
     *                   asserted <i>not</i> to match <code>expected</code>
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayNotEquals( Object expected, Object actual ) {
        assertArrayNotEquals( null, expected, actual );
    }


    /**
     * Asserts that two numeric arrays have the same contents 
     * within a given tolerance.
     * For the assertion to be true,
     * <code>expected</code> and <code>actual</code>
     * must both be primitive numeric array objects whose 
     * component types are of the same
     * class; they must have the same number of elements; and the
     * <i>i</i>th element of <code>expected</code> must be `equal' to the 
     * <i>i</i>th element of <code>actual</code>.
     * Equality in this sense normally means 
     * <code>(abs(expected[i]-actual[i]&lt;delta)</code>, 
     * but in the case of <code>float[]</code> and <code>double[]</code> arrays,
     * elements which both have the value <code>NaN</code> are taken to match,
     * and if the expected element is infinite, then <code>delta</code>
     * is ignored.
     * <p>
     * If <code>delta&lt;1</code> and the arrays are of integer type, 
     * this method does the same as
     * {@link #assertArrayEquals(String,Object,Object)}.
     *
     * @param  message   the message to output if the assertion fails
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayEquals( String message, Object expected, 
                                   Object actual, double delta ) {

        /* OK if both are null. */
        if ( expected == null && actual == null ) {
            return;
        }

        /* Check both objects have the same class. */
        assertEquals(
                getClassName( expected ),
                getClassName( actual ),
                combineMessages( message, "array component class mismatch" )
        );
        Class<?> clazz = actual.getClass();

        /* Check both objects are arrays. */
        Class<?> ctype = clazz.getComponentType();
        assertNotNull(
                ctype,
                combineMessages( message, "not array objects" )
        );

        /* Check both arrays are the same length. */
        assertEquals(
                Array.getLength( expected ),
                Array.getLength( actual ),
                combineMessages( message, "array length mismatch" )
        );
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
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == short.class ) {
            for ( int i = 0; i < nel; i++ ) {
                short v1 = ((short[]) expected)[ i ];
                short v2 = ((short[]) actual)[ i ];
                if ( Math.abs( v1 - v2 ) > delta ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == int.class ) {
            for ( int i = 0; i < nel; i++ ) {
                int v1 = ((int[]) expected)[ i ];
                int v2 = ((int[]) actual)[ i ];
                if ( Math.abs( v1 - v2 ) > delta ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == long.class ) {
            for ( int i = 0; i < nel; i++ ) {
                long v1 = ((long[]) expected)[ i ];
                long v2 = ((long[]) actual)[ i ];
                if ( Math.abs( v1 - v2 ) > delta ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
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
                    assertEquals(v1, v2, (float) delta, itemMismatchMessage( message, i ));
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
                    assertEquals(v1, v2, delta, itemMismatchMessage( message, i ));
                }
            }
        }
        else if ( ctype == char.class ) {
            for ( int i = 0; i < nel; i++ ) {
                char v1 = ((char[]) expected)[ i ];
                char v2 = ((char[]) actual)[ i ];
                if ( Math.abs( v1 - v2 ) > delta ) {
                    assertEquals(v1, v2, itemMismatchMessage( message, i ));
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
     * @param  message   the message to output if the assertion fails
     * @param  expected  the array object containing the expected values
     * @param  actual    the array object containing the actual values,
     *                   asserted <i>not</i> to match <code>expected</code>
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
        assertFalse(equal, message);
    }

    /**
     * Asserts that two numeric arrays have the same contents 
     * within a given tolerance.
     * For the assertion to be true,
     * <code>expected</code> and <code>actual</code>
     * must both be primitive numeric array objects whose component 
     * types are of the same
     * class; they must have the same number of elements; and the
     * <i>i</i>th element of <code>expected</code> must be `equal' to the 
     * <i>i</i>th element of <code>actual</code>.
     * Equality in this sense normally means 
     * <code>(abs(expected[i]-actual[i]&lt;delta)</code>, 
     * but in the case of <code>float[]</code> and <code>double[]</code> arrays,
     * elements which both have the value <code>NaN</code> are taken to match,
     * and if the expected element is infinite, then <code>delta</code>
     * is ignored.
     * <p>
     * If <code>delta&lt;1</code> and the arrays are of integer type, 
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
     *                   asserted <i>not</i> to match <code>expected</code>
     * @param  delta     the tolerance acceptable between values
     * @throws AssertionFailedError  if the assertion is untrue
     */
    public void assertArrayNotEquals( Object expected, Object actual,
                                      double delta ) {
        assertArrayNotEquals( null, expected, actual, delta );
    }

    /**
     * Asserts that a DOM is equivalent to the XML in a given URL.
     * Equivalent to <code>assertDOMEquals(dom, actual, filename, 0)</code> with
     * the first argument being the document element of the DOM read
     * from the URL, and the third argument being the last part
     * (the `file name') of the URL.
     *
     * @param url pointing to an XML file -- the document element of
     * this file is the expected value
     * @param actual the node which is being compared
     * @throws java.io.IOException if the file cannot be found
     * @throws org.xml.sax.SAXException if there is a problem parsing the XML
     * @throws javax.xml.parsers.ParserConfigurationException if the
     *            XML parser cannot be initialised
     * @throws AssertionFailedError if the assertion is untrue
     * @see #assertDOMEquals(Node,Node,String,int)
     */
    public static void assertDOMEquals( URL url, Node actual )
            throws java.io.IOException,
            org.xml.sax.SAXException,
            javax.xml.parsers.ParserConfigurationException {
        assertDOMEquals( url.openStream(),
                        actual, 
                        url.toString().replaceFirst( ".*/", ".../" )+":", 0 );
    }

    /**
     * Asserts that a DOM is equivalent to the XML in a given URL.
     * Equivalent to <code>assertDOMEquals(dom, actual, context, flags)</code> with
     * the first argument being the document element of the DOM read
     * from the URL, and the third argument being the last part
     * (the `file name') of the URL.
     *
     * @param url     pointing to an XML file -- the document element of
     *                this file is the expected value
     * @param actual  the node which is being compared
     * @param context a string indicating the context of this; if
     *                <code>null</code>, it defaults to `string:'
     * @param flags   a set of flags controlling the comparison; see
     *                {@link #assertDOMEquals(Node,Node,String,int)}
     * @throws java.io.IOException if the file cannot be found
     * @throws org.xml.sax.SAXException if there is a problem parsing the XML
     * @throws javax.xml.parsers.ParserConfigurationException if the
     *            XML parser cannot be initialised
     * @throws AssertionFailedError if the assertion is untrue
     * @see #assertDOMEquals(Node,Node,String,int)
     */
    public static void assertDOMEquals( URL url,
                                 Node actual,
                                 String context,
                                 int flags ) 
            throws java.io.IOException,
            org.xml.sax.SAXException,
            javax.xml.parsers.ParserConfigurationException {
        assertDOMEquals( url.openStream(),
                         actual,
                         context,
                         flags );
    }

    /**
     * Asserts that a DOM is equivalent to the DOM implied by the XML
     * in a given string.
     * 
     * @see #assertDOMEquals(String,Node,String,int)
     */
    public static void assertDOMEquals( String s, Node n )
            throws
            java.io.IOException,
            org.xml.sax.SAXException,
            javax.xml.parsers.ParserConfigurationException {
        assertDOMEquals( s, n, "string:", 0 );
    }

    /**
     * Asserts that a DOM is equivalent to the DOM implied by the XML
     * in a given string.
     *
     * @param expected a string containing XML -- the document element of
     * this file is the expected value
     * @param actual the node which is compared
     * @param context a string indicating the context of this; if
     * <code>null</code>, it defaults to `string:'
     * @param flags a set of flags controlling the comparison; see
     * {@link #assertDOMEquals(Node,Node,String,int)}
     * 
     * @throws AssertionFailedError if the assertion is untrue
     * @see #assertDOMEquals(Node,Node,String,int)
     */
    public static void assertDOMEquals( String expected,
                                 Node actual,
                                 String context,
                                 int flags )
            throws
            java.io.IOException,
            org.xml.sax.SAXException,
            javax.xml.parsers.ParserConfigurationException {
        java.io.ByteArrayInputStream bais
                = new java.io.ByteArrayInputStream( expected.getBytes() );
        assertDOMEquals( bais,
                        actual,
                        (context == null ? "string:" : context),
                        flags );
    }

    /**
     * Asserts that a DOM is equivalent to the DOM read from a given stream.
     * 
     *
     * @param s a stream from which XML may be read -- the document element of
     * the resulting DOM is the expected value
     * @param actual the node which is compared
     * @param context a string indicating the context of this; may be
     * <code>null</code>
     * @param flags a set of flags controlling the comparison; see
     * {@link #assertDOMEquals(Node,Node,String,int)}
     * 
     * @throws AssertionFailedError if the assertion is untrue
     * @see #assertDOMEquals(Node,Node,String,int)
     */
    public static void assertDOMEquals( java.io.InputStream s, Node actual,
                                String context, int flags ) 
            throws
            java.io.IOException,
            org.xml.sax.SAXException,
            javax.xml.parsers.ParserConfigurationException {
        if ( docParser == null ) {
            javax.xml.parsers.DocumentBuilderFactory factory
                    = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware( true );
            docParser = factory.newDocumentBuilder();
        }
        Document doc = docParser.parse( s );
        assertDOMEquals( doc.getDocumentElement(), actual, context, flags );
    }
    
    /**
     * Asserts that two DOMs are equal.
     *
     * @param expected the Node containing the expected DOM
     * @param actual the Node to be tested
     * @throws AssertionFailedError if the assertion is untrue
     * @see #assertDOMEquals(Node,Node,String,int)
     */
    public static void assertDOMEquals( Node expected, Node actual ) {
        assertDOMEquals( expected, actual, null, 0 );
    }

    /**
     * Asserts that two XML {@link javax.xml.transform.Source} objects
     * represent the same XML Infoset.  Differences in whitespace and
     * in comments may optionally be ignored.
     *
     * @param   expected  the Source object containing the expected infoset
     * @param   actual    the Source object containing the actual infoset,
     *                    asserted to match <code>expected</code>
     * @param context a string indicating the context of this; may be
     *                    <code>null</code>
     * @param flags a set of flags indicating which node tests to
     *                    omit.  Passing as zero includes all tests.
     * @throws  AssertionFailedError if the assertion is untrue
     * @see #assertDOMEquals(Node,Node,String,int)
     */
    public static void assertSourceEquals( Source expected,
                                    Source actual,
                                    String context,
                                    int flags ) {
                 /* OK if both null. */
        if ( expected == null && actual == null ) {
            return;
        }

        /* Get a transformer. */
        Transformer trans;
        try {
            trans = TransformerFactory.newInstance().newTransformer();
        }
        catch ( TransformerConfigurationException e ) {
            throw new RuntimeException( "Unexpected configuration error", e );
        }

        /* Transform both sources into DOMs, and compare */
        DOMResult res1 = new DOMResult();
        DOMResult res2 = new DOMResult();
        try {
            trans.transform( expected, res1 );
            trans.transform( actual, res2 );
            assertDOMEquals( res1.getNode(), res2.getNode(), context, flags );
        }
        catch ( TransformerException e ) {
            throw (AssertionFailedError)
                  new AssertionFailedError( "At least one source could not be "
                                          + "transformed" )
                 .initCause( e );
        }
    }
    
    /**
     * Asserts that two DOMs are equal.
     *
     * <p>If an assertion fails, the method indicates the location by
     * showing in the failure message the location of the mismatched
     * node, so that
     * <pre>
     * AssertionFailedError: .../test.xml:/[1]ndx/[2]data
     * expected: ...
     * </pre>
     * indicates that the assertion failed when examining the second child node
     * (which was a <code>&lt;data&gt;</code> element) of the first
     * child of the file <code>test.xml</code>
     *
     * <p>If the <code>flags</code> argument is non-zero, it indicates
     * a set of tests on the DOM to omit.  The value is ORed together
     * from the following constants:
     * <dl>
     * <dt><code>TestCase.IGNORE_ATTRIBUTE_PRESENCE</code>
     * <dt>do not check whether attributes match
     * <dt><code>TestCase.IGNORE_ATTRIBUTE_VALUE</code>
     * <dd>check that
     * the same attributes are present on the corresponding elements
     * in the tree, but do not check their values
     * <dt><code>TestCase.IGNORE_WHITESPACE</code>
     * <dd>skip whitespace-only text nodes
     * <dt><code>TestCase.IGNORE_COMMENTS</code>
     * <dd>skip comment nodes
     * </dl>
     *
     * @param expected the Node containing the expected DOM
     * @param actual the Node to be tested
     * @param context a string indicating the context, which will be
     * used in assertion failure reports.  May be null
     * @param flags a set of flags indicating which node tests to
     * omit.  Passing as zero includes all tests.
     * @throws AssertionFailedError if the assertion is untrue
     */
    public static void assertDOMEquals(Node expected,
                                       Node actual,
                                       String context,
                                       int flags) {
        if ( context == null )
            context = "TOP:";
        context = context + expected.getNodeName();
        assertNotNull( expected, context );
        assertNotNull( actual, context );
        if ( expected.getNodeType() != actual.getNodeType() ) {
            StringBuffer msg = new StringBuffer( context );
            msg.append( ": expected " )
                    .append( DOMUtils.mapNodeType( expected.getNodeType() ))
                    .append( "='" )
                    .append( expected.getNodeValue() )
                    .append( "', got " )
                    .append( DOMUtils.mapNodeType( actual.getNodeType() ))
                    .append( "='" )
                    .append( actual.getNodeValue() )
                    .append( "'" );
            fail( msg.toString() );
        }

        assertEquals(expected.getNodeType(), actual.getNodeType(), context+"(type)");

        /*
         * Comparing Nodes: 
         *
         *   - Namespaces must be equal (or both null)
         *
         *   - If namespaces are null, then compare getNodeName
         *
         *     Don't compare localname in this case: getLocalName is
         *     documented to return null if the element was created
         *     with a DOM1 method such as Document.createElement
         *     (rather than createElementNS).  That is, getLocalName
         *     will return null or not depending on how the DOM was
         *     constructed, and so, if you compare the return values
         *     of getLocalName(), two equivalent DOMs could test
         *     unequal (because one getLocalName is null and the other
         *     isn't) if the two DOMs happened to be constructed by
         *     different routes.
         *
         *   - If namespaces are not null, then compare getLocalName
         *
         *     Don't compare prefixes, since these are defined to be arbitrary.
         *
         */
        String expectedNS = expected.getNamespaceURI();
        assertEquals(
                expectedNS,
                actual.getNamespaceURI(),
                context+"(ns)"
        );
        if ( expectedNS == null ) {
            assertEquals(
                    expected.getNodeName(),
                    actual.getNodeName(),
                    context+"(name)"
            );
        } else {
            assertEquals(
                    expected.getLocalName(),
                    actual.getLocalName(),
                    context+"(localName)"
            );
        }

        assertEquals(
                expected.getNodeValue(),
                actual.getNodeValue(),
                context+"(value)"
        );

        if ( (flags & IGNORE_ATTRIBUTE_PRESENCE) == 0 ) {
            NamedNodeMap okatts = expected.getAttributes();
            if ( okatts != null ) {
                NamedNodeMap testatts = actual.getAttributes();
                assertNotNull( testatts, context );
                assertEquals(
                        okatts.getLength(),
                        testatts.getLength(),
                        context+"(natts)"
                );
                for (int i=0; i<okatts.getLength(); i++) {
                    Attr okatt = (Attr)okatts.item(i);
                    Attr testatt = (Attr)testatts.getNamedItem( okatt.getName() );
                    assertNotNull( testatt );
                    if ( (flags & IGNORE_ATTRIBUTE_VALUE) == 0 )
                        assertEquals( context+'@'+okatt.getName(),
                                     okatt.getValue(), testatt.getValue() );
                }
            }
        }

        Node okkid = nextIncludedNode( expected.getFirstChild(), flags );
        Node testkid = nextIncludedNode( actual.getFirstChild(), flags );
        int kidno = 1;
        while ( okkid != null ) {
            assertNotNull( testkid, context+" too few kid elements" );
            assertDOMEquals
                    ( okkid,
                     testkid,
                     context + "/["+Integer.toString( kidno )+']',
                     flags );
            okkid = nextIncludedNode( okkid.getNextSibling(), flags );
            testkid = nextIncludedNode( testkid.getNextSibling(), flags );
            kidno++;
        }
        assertNull( testkid, context+" extra kids: "+testkid );
    }

    /**
     * Returns the first node from the set of this node and its
     * following siblings which is in the included set.
     * 
     * @param n the node to follow
     * @param flags the set of nodes to include; if zero, the input
     *              node is returned unconditionally (ie, this method
     *              is a no-op)
     * @return the next interesting node, or null if
     *         there are none
     */
    private static Node nextIncludedNode(Node n, int flags) {
        if ( flags == 0 )
            return n;           // trivial case -- no omissions at all

        for ( /* no init */ ; n != null; n = n.getNextSibling() ) {
            boolean veto = false;
                
            switch ( n.getNodeType() ) {
              case Node.TEXT_NODE:
                if ( (flags & IGNORE_WHITESPACE) != 0
                     && n.getNodeValue().trim().length() == 0 )
                    veto = true;
                break;
                
              case Node.COMMENT_NODE:
                if ( (flags & IGNORE_COMMENTS) != 0 )
                    veto = true;
                break;
                
              default:
                // do nothing -- this node is OK
            }
            if ( !veto )
                return n;       // JUMP OUT
        }
        assert n == null;

        return null;            // found nothing
    }

    /**
     * Asserts that the contents of a stream are valid XML.
     * The stream is passed through a validating XML parser.
     * Badly-formed XML, or failure to conform to any DTD or schema referenced 
     * in the document's declaration will result in a SAXParseException.
     * <p>
     * Entity resolution is done using an instance of 
     * {@link StarEntityResolver}.
     *
     * @param  message  message associated with assertion failure
     * @param  isrc  input source containing an XML document
     * @throws  IOException  if there is an error reading <code>strm</code>
     * @throws  SAXException  if the document in <code>strm</code>
     *                        is badly-formed or invalid
     */
    public static void assertValidXML( String message, InputSource isrc )
            throws IOException, SAXException {
        final String prefix = message == null ? "" : ( message + ": " );

        /* Obtain a validating parser. */
        SAXParser parser;
        try {
            SAXParserFactory sfact = SAXParserFactory.newInstance();
            sfact.setValidating( true );
            parser = sfact.newSAXParser();
        }
        catch ( ParserConfigurationException e ) {
            throw new RuntimeException( prefix + "Unexpected failure to get " +
                                        "validating SAX parser", e );
        }
        assertTrue( parser.isValidating(), "Check parser is validating" );

        /* Prepare to use the custom entity resolver which knows about some
         * useful entities. */
        final EntityResolver resolver = StarEntityResolver.getInstance();

        /* Set up a handler which rethrows parse errors as 
         * AssertionFailedErrors.  If a custom handler along these lines
         * is not used then validation errors are simply ignored. */
        DefaultHandler handler = new DefaultHandler() {
            public void warning( SAXParseException e ) throws SAXException {
                rethrow( e );
            }
            public void error( SAXParseException e ) throws SAXException {
                rethrow( e );
            }
            public void fatalError( SAXParseException e ) throws SAXException {
                rethrow( e );
            }
            private void rethrow( SAXParseException e ) throws SAXException {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( "Parse error" )
                    .append( e.getMessage() );
                String sysid = e.getSystemId();
                if ( sysid != null && sysid.length() > 0 ) {
                    sbuf.append( " in " + sysid );
                }
                sbuf.append( " at line " + e.getLineNumber() );
                sbuf.append( " column " + e.getColumnNumber() );
                throw new SAXException( sbuf.toString(), e );
            }

            public InputSource resolveEntity( String publicId, String systemId )
                    throws SAXException {
                try {
                    return resolver.resolveEntity( publicId, systemId );
                }
                catch ( IOException e ) {
                    throw new SAXException( e.getMessage(), e );
                }
            }
        };

        /* Do the parse. */
        parser.parse( isrc, handler );
    }

    /**
     * Asserts that the contents of a stream are valid XML.
     * The stream is passed through a validating XML parser.
     * Badly-formed XML, or failure to conform to any DTD or schema
     * referenced in the 
     * document's declaration will result in a SAXParseException.
     *
     * @param  isrc  input stream containing an XML document
     * @throws  IOException  if there is an error reading <code>strm</code>
     * @throws  SAXException  if the document in <code>strm</code>
     *                        is badly-formed or invalid
     */
    public static void assertValidXML( InputSource isrc )
            throws IOException, SAXException {
        assertValidXML( null, isrc );
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
     * @throws IllegalArgumentException  if <code>array</code> is not an array
     *         of a suitable primitive type
     */
    public void fillRandom( Object array, double min, double max ) {
        Class<?> clazz = array.getClass().getComponentType();
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
                arr[ i ] = min + rand.nextDouble() * range;
            }
        }
        else {
            throw new IllegalArgumentException( 
                "Unsupported array type or not an array " + 
                getClassName( array ) );
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
     * @throws IllegalArgumentException  if <code>array</code> is not an array
     *         of a suitable primitive type
     */
    public void fillRandom( Object array, int min, int max ) {
        fillRandom( array, (double) min, (double) max + 0.99 );
    }

    /**
     * Fills a given array with a regular pattern of integer values.
     * The elements of the array will take the values 
     * <code>min, min+1, min+2 .. max-1, min, min+1, min+2..</code> and so on.
     * If the <code>max&lt;min</code> then the values will start at
     * <code>min</code> and keep increasing.
     * <p>
     * The results might not be as expected if you use a <code>min</code> and
     * <code>max</code> values outside the range
     * of the numeric type in question.
     *
     * @param  array   an array of primitives to be filled with cycling values
     * @param  min     the first value
     * @param  max     the highest value, or if less than <code>min</code> an
     *                 indication that there is no maximum
     * @throws IllegalArgumentException  if <code>array</code> is not an array
     *         of a suitable primitive type
     */
    public static void fillCycle( Object array, int min, int max ) {
        Class<?> clazz = array.getClass().getComponentType();
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
                "Unsupported array type or not an array " + 
                getClassName( array ) );
        }
    }

    /**
     * Fills a given array with a pattern of values taken from another one.
     * <code>destArray</code> is filled up with copies of
     * <code>sourceArray</code>.
     * <code>destArray</code> and <code>sourceArray</code> must be arrays of the
     * same class (but can be different lengths of course).
     *
     * @param  destArray    array to be filled with items
     * @param  sourceArray  array containing source items
     */
    public void fillCycle( Object destArray, Object sourceArray ) {
        Class<?> clazz = destArray.getClass();
        if ( ! clazz.isArray() || ! clazz.equals( sourceArray.getClass() ) ) {
            throw new IllegalArgumentException();
        }
        int nsrc = Array.getLength( sourceArray );
        int ndst = Array.getLength( destArray );
        for ( int start = 0; start < ndst; start += nsrc ) {
            System.arraycopy( sourceArray, 0, destArray, start,
                              Math.min( nsrc, ndst - start ) );
        }
    }

    /**
     * Tests whether or not a display, keyboard and mouse can in fact
     * be supported in this environment.   This differs from the
     * {@link java.awt.GraphicsEnvironment#isHeadless} method in that
     * this one tries to do some graphics and if it catches a throwable
     * as a consequence it will return true.  The only time that
     * the <code>GraphicsEnvironment</code> call returns true in practice
     * is if you start java with the property
     * <code>java.awt.headless=true</code>.
     *
     * @return  <code>true</code> if graphics type stuff will fail
     */
    public static boolean isHeadless() {

        /* See if we know we're headless. */
        if ( GraphicsEnvironment.isHeadless() ) {
            return true;
        }

        /* Do something you can't do on a headless display. 
         * The code inside here may need some tweaking - seems to do 
         * the trick on linux & solaris.  Possibly it ought to check 
         * separately for presence of a mouse and keyboard as well? */
        try {
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        }
        catch ( Throwable e ) {
            return true;
        }
        return false;
    }

    /**
     * Returns the classname of an object.  Returns something sensible 
     * if <code>o</code> is null.
     *
     * @param  o  object
     * @return  name of <code>o</code>'s class, or "(null)"
     */
    private static String getClassName( Object o ) {
        return o == null ? "(null)" : o.getClass().getName();
    }

    private String combineMessages( String msg, String detail ) {
        return ( msg != null ) ? ( msg + " - " + detail )
                               : detail;
    }

    private String itemMismatchMessage( String msg, int ix ) {
        return combineMessages( msg, "element [" + ix + "] mismatch" );
    }

}
