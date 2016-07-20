package uk.ac.starlink.ttools.task;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.layer.Combiner;
import uk.ac.starlink.ttools.task.MapEnvironment;

public class SkyDensityMapTest extends TestCase {

    private final String dummyUcd_;

    public SkyDensityMapTest() {
        Logger.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        dummyUcd_ = "pos.not-a-ucd";
    }

    public void testMap() throws Exception {
        StarTable t =
            new StarTableFactory( true )
           .makeStarTable( SkyDensityMapTest.class
                          .getResource( "ngc1275.fits.gz" ).toString(),
                           "fits" );
        // not guaranteed to work.
        ((DefaultValueInfo) t.getColumnInfo( 1 )).setUCD( dummyUcd_ );
        assertEquals( "X_IMAGE", t.getColumnInfo( 1 ).getName() );
        assertEquals( dummyUcd_, t.getColumnInfo( 1 ).getUCD() );

        for ( int level = 0; level < 4; level++ ) {
            int npix = 12<<2*level;
            StarTable tm1 = runMap( t, level, true );
            assertEquals( tm1.getRowCount(), npix );
            StarTable tm2 = runMap( t, level, false );
            long nr2 = Tables.randomTable( tm2 ).getRowCount();
            assertTrue( nr2 > 0.25 * npix && nr2 < 0.75 * npix );
        }
    }

    private StarTable runMap( StarTable t, int level, boolean isComplete )
            throws Exception {
        MapEnvironment env = new MapEnvironment()
           .setValue( "in", t )
           .setValue( "tiling", "hpx" + level )
           .setValue( "lon", "x_image" )                 // fake
           .setValue( "lat", "abs((y_image-180)%90)" )   // fake
           .setValue( "count", Boolean.TRUE )
           .setValue( "combine", Combiner.MAX )
           .setValue( "cols", "number x_image 0.5*gid_2" )
           .setValue( "complete", Boolean.valueOf( isComplete ) );
        new SkyDensityMap().createExecutable( env ).execute();
        StarTable skymap = env.getOutputTable( "omode" );
        int ncol = 5;
        assertEquals( ncol, skymap.getColumnCount() );
        ValueInfo numInfo = skymap.getColumnInfo( 2 );
        assertTrue( "number".equalsIgnoreCase( numInfo.getName() ) );
        assertEquals( Integer.class, numInfo.getContentClass() );
        double[] sums = new double[ ncol ];
        ValueInfo ximInfo = skymap.getColumnInfo( 3 );
        assertTrue( "x_image".equalsIgnoreCase( ximInfo.getName() ) );
        assertEquals( Float.class, ximInfo.getContentClass() );
        assertEquals( "pixel", ximInfo.getUnitString() );
        assertEquals( dummyUcd_ + ";stat.max", ximInfo.getUCD() );
        double[] maxs = new double[ ncol ];
        Arrays.fill( maxs, Double.NEGATIVE_INFINITY );
        RowSequence rseq = skymap.getRowSequence();
        for ( int ir = 0; rseq.next(); ir++ ) {
            Object[] row = rseq.getRow();
            if ( isComplete ) {
                assertEquals( ir, ((Number) row[ 0 ]).intValue() );
            }
            for ( int ic = 0; ic < ncol; ic++ ) {
                Object c = row[ ic ];
                double d = c == null ? Double.NaN : ((Number) c).doubleValue();
                if ( !Double.isNaN( d ) ) {
                    sums[ ic ] += d;
                    maxs[ ic ] = Math.max( maxs[ ic ], d );
                }
            }
        }
        rseq.close();
        assertEquals( t.getRowCount(), sums[ 1 ] );
        assertEquals( 352.25, maxs[ 3 ], 0.001 );
        assertEquals( 13.5, maxs[ 4 ] );
        return skymap;
    }
}
