package uk.ac.starlink.treeview;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import junit.framework.AssertionFailedError;
import uk.ac.starlink.util.TestCase;

public class TreeviewTest extends TestCase {

    /** Name of the file giving the correct output of 'treeview -demo -text'. */
    public static String DEMOTXT_FILE = "demotxt.cmp";

    public TreeviewTest( String name ) {
        super( name );
    }

    public void setUp() {
        String basedir = System.getProperty( "ant.basedir" );
        System.setProperty( "uk.ac.starlink.treeview.cmdname", "treeview" );
        System.setProperty( "uk.ac.starlink.treeview.demodir",
                            basedir + File.separator + 
                            "etc" + File.separator +
                            "treeview" + File.separator +
                            "demo" );
    }

    public void testTextMode() throws IOException {
        PrintStream sysout = System.out;
        ByteArrayOutputStream bstrm = new ByteArrayOutputStream();
        PrintStream ostrm = new PrintStream( bstrm );
        System.setOut( ostrm );
        Driver.main( new String[] { "-text", "-demo" } );
        ostrm.close();
        System.setOut( sysout );

        byte[] bytes = bstrm.toByteArray();
        String[] lines1 = 
            getLines( new ByteArrayInputStream( bytes ) );
        String[] lines2 = 
            getLines( getClass().getResourceAsStream( DEMOTXT_FILE ) );
        try {
            assertArrayEquals( lines1, lines2 );
        }
        catch ( AssertionFailedError e ) {
            System.out.println( 
                "Regression test failed; treeview -demo -text did not match " 
              + DEMOTXT_FILE );
            System.out.println(
                "Result of treeview -demo -text from this test was: " );
            for ( int i = 0; i < lines1.length; i++ ) {
                 System.out.println( lines1[ i ] );
            }
            throw e;
        }
    }

    private String[] getLines( InputStream strm ) throws IOException {
        List lines = new ArrayList();
        BufferedReader rdr = 
            new BufferedReader( new InputStreamReader( strm ) );
        for ( String line; ( line = rdr.readLine() ) != null; ) {

            /** Just exclude special case - see build.xml. */
            if ( ! line.matches( "^ *- \\[FIL\\] Origins *$" ) ) {
                lines.add( line );
            }
        }
        rdr.close();
        return (String[]) lines.toArray( new String[ 0 ] );
    }

    public void testGUIMode() {
        Driver.main( new String[] { "-demo" } );
        try {
            Thread.currentThread().sleep( 1000 );
        }
        catch ( InterruptedException e ) {
        }
    }
}
