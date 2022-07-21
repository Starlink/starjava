package uk.ac.starlink.ttools.task;

import java.net.URL;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLDataSource;

public class CombinedMatchTest extends TestCase {

    final URL tmassLoc_ = getClass().getResource( "p_2mass.fits" );
    final URL tychoLoc_ = getClass().getResource( "p_tycho.fits" );
    final StarTable tmass_;
    final StarTable tycho_;

    public CombinedMatchTest() throws Exception {
        LogUtils.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
        StarTableFactory tf = new StarTableFactory( true );
        tmass_ = tf.makeStarTable( new URLDataSource( tmassLoc_ ) );
        tycho_ = tf.makeStarTable( new URLDataSource( tychoLoc_ ) );
    }

    public void testSkyX() throws Exception {
        double skyErr = 0.4;
        double magErr = 1.3;
        MapEnvironment env = new MapEnvironment();
        env.setValue( "matcher", "sky+1d" );
        env.setValue( "find", "all" );
        env.setValue( "progress", "none" );
        env.setValue( "in1", tmass_ );
        env.setValue( "in2", tycho_ );
        env.setValue( "values1", "ra2mass de2mass jmag" );
        env.setValue( "values2", "raTycho deTycho btmag" );
        env.setValue( "params", "" + skyErr + " " + magErr );
        env.setValue( "ocmd",
                      "addcol skydist "
                    + "skyDistanceDegrees(ra2mass,de2mass,raTycho,deTycho)"
                    + "/(" + skyErr + "/3600.); "
                    + "addcol magdist abs(jmag-btmag)/" + magErr + "; "
                    + "addcol sep_manual hypot(skydist,magdist); "
                    + "keepcols 'Separation sep_manual'" );
        new TableMatch2().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );

        // Checks that the separation is actually calculated as documented
        // in the "Sky + X" matcher description in SUN/253.
        RowSequence rseq = result.getRowSequence();
        while ( rseq.next() ) {
            double sep1 = ((Number) rseq.getCell( 0 )).doubleValue();
            double sep2 = ((Number) rseq.getCell( 1 )).doubleValue();
            assertTrue( Math.abs( sep1 - sep2 ) / sep1 < 1e-8 );
        }

        // This figure is certainly reasonable, but I haven't checked it
        // for sure - so this is a regression test, if it fails you should
        // investigate the possibility that 23 is not the right answer.
        assertEquals( 23, result.getRowCount() );
    }

    public void testSkyXY() throws Exception {
        double skyErr = 0.6;
        double mag1Err = 1.1;
        double mag2Err = 0.9;
        MapEnvironment env = new MapEnvironment();
        env.setValue( "matcher", "sky+2d_anisotropic" );
        env.setValue( "find", "all" );
        env.setValue( "progress", "none" );
        env.setValue( "in1", tmass_ );
        env.setValue( "in2", tycho_ );
        env.setValue( "values1", "ra2mass de2mass jmag hmag" );
        env.setValue( "values2", "raTycho deTycho btmag vtmag" );
        env.setValue( "runner", RowRunner.PARTEST );
        env.setValue( "params", "" + skyErr + " " + mag1Err + " " + mag2Err );
        env.setValue( "ocmd",
                      "addcol skydist "
                    + "skyDistanceDegrees(ra2mass,de2mass,raTycho,deTycho)"
                    + "/(" + skyErr + "/3600.); "
                    + "addcol mag1dist abs(jmag-btmag)/" + mag1Err + "; "
                    + "addcol mag2dist abs(hmag-vtmag)/" + mag2Err + "; "
                    + "addcol sep_manual hypot(skydist,mag1dist,mag2dist); "
                    + "keepcols 'Separation sep_manual'" );
        new TableMatch2().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );

        // Checks that the separation is actually calculated as documented
        // in the "Sky + XY" matcher description in SUN/253.
        RowSequence rseq = result.getRowSequence();
        while ( rseq.next() ) {
            double sep1 = ((Number) rseq.getCell( 0 )).doubleValue();
            double sep2 = ((Number) rseq.getCell( 1 )).doubleValue();
            assertEquals( 0.0, Math.abs( sep1 - sep2 ) / sep1, 1e-8 );
        }

        // This figure is certainly reasonable, but I haven't checked it
        // for sure - so this is a regression test, if it fails you should
        // investigate the possibility that 19 is not the right answer.
        assertEquals( 19, result.getRowCount() );
    }
}
