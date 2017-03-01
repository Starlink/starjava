package uk.ac.starlink.ttools.build;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

public class Plot2ExampleTest extends TestCase {

    public Plot2ExampleTest() {
        Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.SEVERE );
    }

    public void testExamples() throws Exception {
        String dataDir = System.getProperty( "plot2.figdata.dir", "." );
        Plot2Example.checkExamples( new File( dataDir ) );
    }

}
