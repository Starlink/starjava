package uk.ac.starlink.util;

import java.io.*;
import java.util.*;

/**
 * Provides methods for unit testing of other Java classes.
 * <p>
 * The normal way of using this class will be to subclass it and override 
 * the <code>testScript</code> method with a sequence of method invocations
 * and assertions specific to the unit being tested.  If any of the
 * assertions is false, or if any error or exception is thrown during
 * execution, an error will be logged.  On completion of the test a
 * further appropriate log message will be written, and the calling code
 * may enquire whether all tests were successful or not.
 * <p>
 * Log messages are written to a <code>PrintStream</code>.  Subclasses
 * can (and should) override the <code>testScript</code> method in such
 * a way that log messages make it clear which component was being tested
 * when any failure occurs.
 * <p>
 * An example executable subclass of <code>Tester</code> for testing a 
 * class called <code>Number</code> might look like this:
 * <pre>
 *   class NumberTester extends uk.ac.starlink.util.Tester {
 * 
 *      public static void main( String[] args ) {
 *         NumberTester tester = new NumberTester();
 *         tester.doTest();
 *      }  
 *
 *      public void testScript() throws Throwable {
 *         setComponent( "Constructor( int )" ); 
 *         Number two = new Number( 2 ); 
 *
 *         setComponent( "add2()" );
 *         assertTrue( two.add2() == 4 );
 *      }  
 *   }  
 * </pre>
 *
 * @deprecated  Use JUnit and {@link TestCase} instead
 * @author  Mark Taylor (STARLINK)
 * @version $Id$
 */
public class Tester {

    /*
     * Fields.
     */

    private boolean allOK;            // Have we avoided failure so far?
    private boolean testActive;       // Have we started the test?
    private String testComponent;     // Name of the component now being tested
    private PrintStream stream;       // Print stream for logging output

    /**
     * The maximum number of elements shown in array assertEqual 
     * failure messages.
     */
    public static final int MAX_SHOWN = 10;

    /*
     * Constructors.
     */

    /**
     * Initialises a <code>Tester</code> object to output to a given 
     * print stream.
     *
     * @param  stream  the <code>PrintStream</code> to which logging 
     *                 output will be directed.
     */
    public Tester( PrintStream stream ) {
        allOK = true;
        clearFailures();
        this.stream = stream;
    }

    /**
     * Initialises a <code>Tester</code> object to log to standard output.
     */
    public Tester() {
        this( System.out );
    }


    /*
     * Methods.
     */

    /**
     * Main method.  This runs the tests defined by <code>testScript</code>.
     * It needs to be shadowed for executable subclasses, but may be
     * invoked directly to test the <code>Tester</code> class itself.
     * There is no requirement for an implementation of <code>main</code>
     * in subclasses of <code>Tester</code>, but it will be useful if
     * unit testing is to be performed from a makefile.
     *
     * @param  args  ignored
     */
    public static void main( String[] args ) {
    /*
     * Although the form of this is probably adequate for many testing
     * purposes, it is no good just inheriting it because, as far as I
     * can tell, there is no way of writing code in a static function 
     * (such as an inheritable main) which will construct an instance 
     * of the extended class; inheriting this static function will simply
     * lead to calling doTest on class Tester not the subclass.
     */
        Tester tester = new Tester();
        tester.logMessage( "Running tests" );
        tester.doTest();
    }

    /**
     * Runs all the class-specific tests.
     * This method is intended to be overridden by subclasses of 
     * <code>Tester</code>.  It should contain one or more calls of
     * <code>setComponent</code>, each followed by a sequence of invocations
     * of the methods to be tested interspersed with <code>assert</code>
     * calls.
     *
     * @throws  Throwable  The code comprising the implementation of this 
     *                     method may throw any exception or error.
     *                     Any such Throwable which is thrown during 
     *                     execution will be caught and will 
     *                     terminate the <code>testScript</code> execution,
     *                     and register that a test failure has occurred.
     */
    protected void testScript() throws Throwable {
        setComponent( "Tester" );
    }

    /**
     * Clears the failure status of the tests run.
     */
    public void clearFailures() {
        allOK = true;
    }

    /**
     * Indicates whether any (uncleared) failures have occurred so far 
     * in this tester.
     *
     * @return   true if any failures have occurred, false otherwise
     */
    public boolean hasFailures() {
        return ! allOK;
    }

    /**
     * Runs the test script, and logs the output appropriately.  
     * This is the main method called by clients of this class and of its 
     * subclasses to execute the tests.
     */
    public void doTest() {
        testActive = true;
        setComponent( null );
        try {
            this.testScript();
        }
        catch ( Throwable thrown ) {
            registerFailure( thrown.toString() );
            thrown.printStackTrace( stream );
        }
        setComponent( null );
        testActive = false;
        if ( hasFailures() ) {
            logMessage( "Testing failed" );
            System.exit( 1 );
        }
        else {
            logMessage( "All tests successful" );
        }
    }

    /**
     * Indicates that the named component is about to be tested.
     * This method should be called within the <code>testScript</code>
     * method prior to a sequence of instructions which will test
     * a given component.  It enables logging messages to make clear
     * which components have been tested and where failures occur.
     *
     * @param  component  Indicates the component about to be tested.
     *                    May be any descriptive string, or <code>null</code>
     *                    to indicate that no specific component is 
     *                    being tested.
     */
    protected void setComponent( String component ) {
        testComponent = null;
        if ( component != null ) {
            logMessage( "Testing  " + component );
        }
        testComponent = component;
    }

    /**
     * Gets the name of the component currently being tested, as set by
     * the <code>setComponent</code> method.  Note this may be 
     * <code>null</code> if it has never been set or if it has been 
     * set explicitly to <code>null</code>. 
     *
     * @return  the name of the component being tested, or <code>null</code>.
     *          A return value of <code>null</code> is a legitimate indication
     *          that no component is currently under active testing.
     */
    protected String getComponent() {
        return testComponent;
    }

    /**
     * Asserts that a given Runnable will cause a Throwable to be thrown.
     * This method should be called within the <code>testScript</code>
     * method with the expectation that the <code>run</code> method of
     * the Runnable will throw a Throwable of the class indicated.
     * If no such Throwable is thrown, then a failure is registered,
     * which will result in a suitable message being logged, and
     * subsequent calls of the <code>hasFailures</code> method
     * returning true.
     *
     * @param   throwclass  the class of a Throwable which is expected to
     *                      get thrown by the <code>run</code> method of
     *                      runner
     * @param   runner      a Runnable whose <code>run</code> method is
     *                      expected to throw a Throwable of type 
     *                      <code>throwclass</code>
     */
    protected void assertThrows( Class throwclass, Runnable runner ) {
        boolean ok = false;
        try {
            runner.run();
            registerFailure( "No " + throwclass.getName() + " was thrown" );
        }
        catch ( Throwable e ) {
            if ( ! throwclass.isInstance( e ) ) {
                registerFailure( "A " + e.getClass().getName() 
                               + " was thrown, "
                               + "not a " + throwclass.getName() );
            }
        }
    }
   

    /**
     * Asserts that a given value should be true.
     * This method should be called within the <code>testScript</code>
     * method on a boolean value (e.g. the return value of method of the
     * class being tested) which is expected to be true.  If it is in
     * fact true, no action is taken.  If it is false, then a failure
     * is registered, which will result in a suitable message being 
     * logged, and subsequent calls of the <code>hasFailures</code>
     * method returning true.
     *
     * @param  success   Value which is asserted to be true
     */
    protected void assertTrue( boolean success ) {
        if ( ! success ) {
            registerFailure( "Assertion failed" );
        }
    }

    /**
     * Asserts that a given value should be true, assigning a number to the
     * assertion.
     * This method should be called within the <code>testScript</code>
     * method on a boolean value (e.g. the return value of method of the
     * class being tested) which is expected to be true.  If it is in
     * fact true, no action is taken.  If it is false, then a failure
     * is registered, which will result in a suitable message, which 
     * contains the assertion number being logged, and subsequent calls 
     * of the <code>hasFailures</code> method returning true.
     *
     * @param  success   Value which is asserted to be true
     */
    protected void assertTrue( int num, boolean success ) {
        if ( ! success ) {
            registerFailure( "Assertion # " + num + " failed" );
        }
    }

    /**
     * Asserts that two String values should be roughly equal
     * upper/lower case is ignored).
     * This method should be called within the <code>testScript</code>
     * method on a pair of strings expected to be equal.  If they are
     * equal, no action is taken.  If they are not equal then a failure
     * is registered, which will result in a suitable message being 
     * logged, and subsequent calls of the <code>hasFailures</code>
     * method returning true.
     *
     * @param  str1  first string
     * @param  str2  second string
     */
    protected void assertEqual( String str1, String str2 ) {
        if ( ! str1.equalsIgnoreCase( str2 ) ) {
            registerFailure( 
                "Assertion failed: \"" + str1 + "\" != \"" + str2 + "\"" );
        }
    }

    /**
     * Asserts that two integer values should be equal.
     * This method should be called within the <code>testScript</code>
     * method on a pair of values expected to be equal.  If they are
     * equal, no action is taken.  If they are not equal then a failure
     * is registered, which will result in a suitable message being 
     * logged, and subsequent calls of the <code>hasFailures</code>
     * method returning true.
     *
     * @param  value1  first value
     * @param  value2  second value
     */
    protected void assertEqual( int value1, int value2 ) {
        if ( value1 != value2 ) {
            registerFailure( "Assertion failed: " + value1 + " != " + value2 );
        }
    }

    /**
     * Asserts that two floating point values should be nearly equal.
     * This method should be called within the <code>testScript</code>
     * method on a pair of values expected to be equal.  If they are
     * equal, no action is taken.  If they are not equal then a failure
     * is registered, which will result in a suitable message being 
     * logged, and subsequent calls of the <code>hasFailures</code>
     * method returning true.
     *
     * @param  value1  first value
     * @param  value2  second value
     */
    protected void assertEqual( double value1, double value2 ) {
        if ( Math.abs( value1 - value2 ) > Float.MIN_VALUE * 10.0 ) {
            registerFailure( "Assertion failed: " + (float) value1 + " != " 
                                                  + (float) value2 );
        }
    }

    /**
     * Asserts that two integer arrays should be the same.
     * This method should be called within the <code>testScript</code>
     * method on a pair of arrays expected to be equal.  If they are
     * equal, no action is taken.  If they are not equal then a failure
     * is registered, which will result in a suitable message being 
     * logged, and subsequent calls of the <code>hasFailures</code>
     * method returning true.
     *
     * @param  array1  first array
     * @param  array2  second array
     */
    protected void assertEqual( int[] array1, int[] array2 ) {
        if ( ! Arrays.equals( array1, array2 ) ) {
            String v1 = " ";
            String v2 = " ";
            for ( int i = 0; i < Math.min( array1.length, MAX_SHOWN ); i++ ) {
                v1 += array1[ i ] + " ";
            }
            for ( int i = 0; i < Math.min( array2.length, MAX_SHOWN ); i++ ) {
                v2 += array2[ i ] + " ";
            }
            registerFailure( "Assertion failed: (" + v1 + ") != (" + v2 + ")" );
        }
    }

    /**
     * Asserts that two long arrays should be the same.
     * This method should be called within the <code>testScript</code>
     * method on a pair of arrays expected to be equal.  If they are
     * equal, no action is taken.  If they are not equal then a failure
     * is registered, which will result in a suitable message being 
     * logged, and subsequent calls of the <code>hasFailures</code>
     * method returning true.
     *
     * @param  array1  first array
     * @param  array2  second array
     */
    protected void assertEqual( long[] array1, long[] array2 ) {
        if ( ! Arrays.equals( array1, array2 ) ) {
            String v1 = " ";
            String v2 = " ";
            for ( int i = 0; i < Math.min( array1.length, MAX_SHOWN ); i++ ) {
                v1 += array1[ i ] + " ";
            }
            for ( int i = 0; i < Math.min( array2.length, MAX_SHOWN ); i++ ) {
                v2 += array2[ i ] + " ";
            }
            registerFailure( "Assertion failed: (" + v1 + ") != (" + v2 + ")" );
        }
    }

    /**
     * Asserts that two String arrays should be the same.
     * This method should be called within the <code>testScript</code>
     * method on a pair of arrays expected to be equal.  If they are
     * equal, no action is taken.  If they are not equal then a failure
     * is registered, which will result in a suitable message being 
     * logged, and subsequent calls of the <code>hasFailures</code>
     * method returning true.
     *
     * @param  array1  first array
     * @param  array2  second array
     */
    protected void assertEqual( String[] array1, String[] array2 ) {
        if ( ! Arrays.equals( array1, array2 ) ) {
            String v1 = " ";
            String v2 = " ";
            for ( int i = 0; i < Math.min( array1.length, MAX_SHOWN ); i++ ) {
                v1 += array1[ i ] + " ";
            }
            for ( int i = 0; i < Math.min( array2.length, MAX_SHOWN ); i++ ) {
                v2 += array2[ i ] + " ";
            }
            registerFailure( "Assertion failed: (" + v1 + ") != (" + v2 + ")" );
        }
    }

    /**
     * Asserts that two double precision arrays should be nearly the same.
     * This method should be called within the <code>testScript</code>
     * method on a pair of arrays expected to be equal.  If they are
     * equal, no action is taken.  If they are not equal then a failure
     * is registered, which will result in a suitable message being 
     * logged, and subsequent calls of the <code>hasFailures</code>
     * method returning true.
     *
     * @param  array1  first array
     * @param  array2  second array
     */
    protected void assertEqual( double[] array1, double[] array2 ) {
        boolean ok = ( array1.length == array2.length );
        if ( ok ) {
            for ( int i = 0; i < array1.length; i++ ) {
                if ( Math.abs( array1[ i ] - array2[ i ] ) 
                     > Float.MIN_VALUE * 10.0 ) {
                    ok = false;
                    break;
                }
            }
        }
        if ( ! ok ) {
            String v1 = " ";
            String v2 = " ";
            for ( int i = 0; i < Math.min( array1.length, MAX_SHOWN ); i++ ) {
                v1 += array1[ i ] + " ";
            }
            for ( int i = 0; i < Math.min( array2.length, MAX_SHOWN ); i++ ) {
                v2 += array2[ i ] + " ";
            }
            registerFailure( "Assertion failed: (" + v1 + ") != (" + v2 + ")" );
        }
    }

    /**
     * Asserts that two primitive array objects are identical. 
     * They have to be arrays of the same primitive type, of course.
     * This method should be called within the <code>testScript</code>
     * method on a pair of arrays expected to be equal.  If they are
     * equal, no action is taken.  If they are not equal then a failure
     * is registered, which will result in a suitable message being 
     * logged, and subsequent calls of the <code>hasFailures</code>
     * method returning true.
     *
     * @param  array1  first array
     * @param  array2  second array
     */
    protected void assertEqual( Object array1, Object array2 ) {
        Class itemType = array1.getClass().getComponentType();
        if ( itemType == byte.class ) {
            assertTrue( Arrays.equals( (byte[]) array1, (byte[]) array2 ) );
        }
        else if ( itemType == short.class ) {
            assertTrue( Arrays.equals( (short[]) array1, (short[]) array2 ) );
        }
        else if ( itemType == int.class ) {
            assertTrue( Arrays.equals( (int[]) array1, (int[]) array2 ) );
        }
        else if ( itemType == float.class ) {
            assertTrue( Arrays.equals( (float[]) array1, (float[]) array2 ) );
        }
        else if ( itemType == double.class ) {
            assertTrue( Arrays.equals( (double[]) array1, (double[]) array2 ) );
        }
    }

    /**
     * Logs a message about the testing.
     * An attempt is made to write the message in such a way that it is
     * clear at which stage of the testing (e.g. during testing of
     * which component) it has been generated.
     *
     * @param  message   Arbitrary message text to write.  
     *                   A <code>message</code> containing embedded newline
     *                   characters is permitted and will be output in a
     *                   tidy fashion.
     */
    protected void logMessage( String message ) {

        // Calculate indent according to what stage of testing we are at.
        String prefix = "";
        if ( testActive ) {
            prefix = prefix + "   ";
        }
        if ( testComponent != null ) {
            prefix = prefix + "   ";
        }

        // Split messages by newline characters and output with correct indents.
        StringTokenizer stok = new StringTokenizer( message, "\n" );
        while ( stok.hasMoreTokens() ) {
            stream.println( prefix + stok.nextToken() );
        }
    }

    private void registerFailure( String detail ) {
        allOK = false;
        logMessage( detail );
    }
}

