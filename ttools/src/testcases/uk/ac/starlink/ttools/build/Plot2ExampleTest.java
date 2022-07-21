package uk.ac.starlink.ttools.build;

import java.io.File;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.util.LogUtils;

public class Plot2ExampleTest extends TestCase {

    public Plot2ExampleTest() {
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.votable" ).setLevel( Level.SEVERE );
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot2" )
                .setLevel( Level.WARNING );
    }

    public void testExamples() throws Exception {
        String dataDir = System.getProperty( "plot2.figdata.dir", "." );
        Plot2Example.checkExamples( new File( dataDir ) );
    }

}
