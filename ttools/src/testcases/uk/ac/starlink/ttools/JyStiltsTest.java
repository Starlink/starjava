package uk.ac.starlink.ttools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import junit.framework.TestCase;
import org.python.util.PythonInterpreter;
import org.python.core.PySystemState;
import uk.ac.starlink.util.LogUtils;

/**
 * Testcase which runs python scripts to test JyStilts functionality.
 * Any *.py scripts in the same directory are executed.
 */
public class JyStiltsTest extends TestCase {

    private final PythonInterpreter interp_;
    private final File testDir_;

    public JyStiltsTest() {
        LogUtils.getLogger( "uk.ac.starlink.ttools.filter" )
                .setLevel( Level.SEVERE );
        PySystemState.initialize();
        File basedir = new File( System.getProperty( "basedir", "." ) );
        testDir_ = new File( basedir, "src/testcases/uk/ac/starlink/ttools" );
        String stiltsDir = new File( basedir, "build/etc" ).toString();
        String cwd =
            new File( basedir, "build/testcases/uk/ac/starlink/ttools" )
           .toString();
                        
        interp_ = new PythonInterpreter();
        interp_.exec( "import sys" );
        interp_.exec( "import os" );
        interp_.exec( "sys.path.insert(0,'" + stiltsDir + "')" );
        interp_.set( "testdir", testDir_.toString() );
        interp_.set( "cwd", cwd );
        
        interp_.exec( "import stilts" );
        interp_.exec( "import unittest" );
    }

    public void testScripts() throws IOException {
        File[] pyScripts = testDir_.listFiles( new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return name.endsWith( ".py" );
            }
        } );
        for ( int i = 0; i < pyScripts.length; i++ ) {
            execute( pyScripts[ i ] );
        }
    }

    private void execute( File file ) throws IOException {
        interp_.execfile( file.toString() );
    }
}
