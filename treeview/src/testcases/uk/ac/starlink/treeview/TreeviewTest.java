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
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.AssertionFailedError;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.util.TestCase;

/**
 * Tests Treeview.
 * This testcase is unusual in that it behaves differently according to whether
 * the property uk.ac.starlink.treeview.test.args is set or not.  If yes
 * it starts up treeview with those args, if not it performs its usual
 * non-interactive tests.
 */
public class TreeviewTest extends TestCase {

    static {
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.util" ).setLevel( Level.SEVERE );
    }

    /**
     *  Name of the file giving the correct output of command:
     *      treeview -demo -text
     */
    public String DEMOTXT_FILE = "demotxt.cmp";

    /** 
     * Name of the file giving the correct output of command:
     *     treeview -demo -text -path | awk '{print $NF}'
     */
    public String DEMOPATH_FILE = "demopath.cmp";

    private String[] args;

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
        String arg = System.getProperty( "uk.ac.starlink.treeview.test.args" );
        if ( arg == null || 
             arg.trim().length() == 0 || 
             arg.startsWith( "${" ) ) {
            args = null;
        }
        else {
            args = arg.split( "\\s+" );
        }
    }

    public void testTextMode() throws IOException {
        if ( args != null ) {
            return;
        }
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
        String msg = "diffing " + DEMOTXT_FILE + " and " +
                     "`treeview -demo -text`";
        assertArrayEquals( msg, lines2, lines1 );
    }

    public void testTextPaths() throws IOException {
        if ( args != null ) {
            return;
        }
        if ( File.separatorChar != '/' ) {
            System.out.println( "Skipping test for system with non-'/' " +
                                "separator" );
            return;
        }
        PrintStream sysout = System.out;
        ByteArrayOutputStream bstrm = new ByteArrayOutputStream();
        PrintStream ostrm = new PrintStream( bstrm );
        System.setOut( ostrm );
        Driver.main( new String[] { "-text", "-path", "-demo" } );
        ostrm.close();
        System.setOut( sysout );

        byte[] bytes = bstrm.toByteArray();
        String[] lines1 = 
            getLines( new ByteArrayInputStream( bytes ) );
        for ( int i = 0; i < lines1.length; i++ ) {
            String line = lines1[ i ];
            lines1[ i ] = line.substring( line.lastIndexOf( ' ' ) + 1 );
        }
        String[] lines2 =
            getLines( getClass().getResourceAsStream( DEMOPATH_FILE ) );
        String msg = "diffing " + DEMOPATH_FILE + " and " + 
                     "`treeview -demo -text -path | awk '{print $NF}'`";
        assertArrayEquals( msg, lines2, lines1 );
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

    public void testWithArgs() throws IOException {
        if ( args != null ) {
            Driver.main( args );
            System.in.read();
        }
    }
}
