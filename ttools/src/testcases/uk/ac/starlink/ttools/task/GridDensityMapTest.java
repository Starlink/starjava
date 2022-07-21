package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.LogUtils;

public class GridDensityMapTest extends TestCase {

    public GridDensityMapTest() {
        LogUtils.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
    }

    // There are a lot of options for bin sizing etc.
    // I haven't attempted to test them all.
    public void testMap() throws Exception {
        StarTable ngc1275 = new StarTableFactory( true )
           .makeStarTable( GridDensityMapTest.class
                          .getResource( "ngc1275.fits.gz" ).toString(),
                           "fits" );
        runTestMap( ngc1275, false );
        runTestMap( ngc1275, true );
    }

    private void runTestMap( StarTable ngc1275, boolean isLog )
            throws Exception {
        MapEnvironment env0 = new MapEnvironment()
           .setValue( "in", ngc1275 )
           .setValue( "icmd", "addcol x x_image; addcol y y_image" )
           .setValue( "coords", "X_IMAGE Y_IMAGE" )
           .setValue( "combine", "mean" )
           .setValue( "cols", "1;count;count gsize_3;sum;sum3 x y" );
        if ( isLog ) {
            env0.setValue( "logs", "true true" );
        }
        boolean hasMissing = false;
        for ( int npow = 1; npow < 5; npow++ ) {
            int nbin = 1 << npow;
            MapEnvironment env = new MapEnvironment( env0 );
            env.setValue( "nbins", "10 " + nbin );
            MapEnvironment envS = new MapEnvironment( env )
                                 .setValue( "sparse", Boolean.TRUE );
            MapEnvironment envF = new MapEnvironment( env )
                                 .setValue( "sparse", Boolean.FALSE );
            new GridDensityMap().createExecutable( envS ).execute();
            new GridDensityMap().createExecutable( envF ).execute();
            StarTable gridmapS = envS.getOutputTable( "omode" );
            StarTable gridmapF = envF.getOutputTable( "omode" );
            Tables.checkTable( gridmapS );
            Tables.checkTable( gridmapF );
            int nrowS = (int) gridmapS.getRowCount();
            int nrowF = (int) gridmapF.getRowCount();
            assertTrue( nrowF >= nrowS );
            hasMissing = hasMissing || nrowS < nrowF;
            double nbinFracF = 10. * nbin / gridmapS.getRowCount();
            assertTrue( nbinFracF >= 0.6 && nbinFracF <= 1.7 ); // about right
            checkResult( gridmapS, ngc1275, true );
            checkResult( gridmapF, ngc1275, false );
        }
        assertTrue( hasMissing );
    }

    private void checkResult( StarTable t, StarTable inTable, boolean isSparse )
            throws IOException {
        double csumS = 0;
        double sum3S = 0;
        for ( int ir = 0; ir < t.getRowCount(); ir++ ) {
            double x = getValue( t, "x", ir );
            double y = getValue( t, "y", ir );
            double c = getValue( t, "count", ir );
            if ( isSparse ) {
                assertTrue( c > 0 );
            }
            if ( c > 0 ) {
                csumS += c;
                assertTrue( x >= getValue( t, "x_image_lo", ir ) &&
                            x <= getValue( t, "x_image_hi", ir ) );
                assertTrue( y >= getValue( t, "y_image_lo", ir ) &&
                            y <= getValue( t, "y_image_hi", ir ) );
            }
            double s3 = getValue( t, "sum3", ir );
            if ( ! Double.isNaN( s3 ) ) {
                sum3S += s3;
            }
        }
        assertEquals( inTable.getRowCount(), (long) csumS );
        assertEquals( 1413.0, sum3S );
    }

    private static double getValue( StarTable t, String colName, int irow )
            throws IOException {
        for ( int ic = 0; ic < t.getColumnCount(); ic++ ) {
            if ( colName.equalsIgnoreCase( t.getColumnInfo( ic ).getName() ) ) {
                Object value = t.getCell( irow, ic );
                return value instanceof Number ? ((Number) value).doubleValue()
                                               : Double.NaN;
            }
        }
        return Double.NaN;
    }
}
