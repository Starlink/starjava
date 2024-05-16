package uk.ac.starlink.ttools.task;

import java.util.Arrays;
import java.util.logging.Level;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.ConstantColumn;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.func.Tilings;
import uk.ac.starlink.ttools.plot2.layer.Combiner;
import uk.ac.starlink.ttools.plot2.layer.SolidAngleUnit;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.util.LogUtils;

public class SkyDensityMapTest extends TestCase {

    private final String dummyUcd_;

    public SkyDensityMapTest() {
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.util" ).setLevel( Level.WARNING );
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
            long npix = 12L<<2*level;
            StarTable tm1 = runMap( t, level, true );
            assertEquals( tm1.getRowCount(), npix );
            StarTable tm2 = runMap( t, level, false );
            long nr2 = Tables.randomTable( tm2 ).getRowCount();
            assertTrue( nr2 > 0.25 * npix && nr2 < 0.75 * npix );
        }
    }

    public void testQuantiles() throws Exception {
        MapEnvironment env = new MapEnvironment()
           .setValue( "in", ":skysim:10000" )
           .setValue( "icmd", "select dec>0" )
           .setValue( "tiling", "hpx1" )
           .setValue( "complete", Boolean.TRUE )
           .setValue( "lon", "ra" )
           .setValue( "lat", "dec" )
           .setValue( "cols", "gmag;min;q_00A gmag;q.0;q_00B "
                            + "gmag;q1;q_25A; gmag;q.250;q_25B" )
           .setValue( "ocmd", "keepcols q_*" );
        new SkyDensityMap().createExecutable( env ).execute();
        StarTable skymap = env.getOutputTable( "omode" );
        Tables.checkTable( skymap );
        int ndiff = 0;
        int nsameMin = 0;
        int nsameQ25 = 0;
        try ( RowSequence rseq = skymap.getRowSequence() ) {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                float minA = toFloat( row[ 0 ] );
                float minB = toFloat( row[ 1 ] );
                float q25A = toFloat( row[ 2 ] );
                float q25B = toFloat( row[ 3 ] );
                if ( ! ( Float.isNaN( minA ) && Float.isNaN( minB ) ) ) {
                    assertEquals( minA, minB );
                    nsameMin++;
                }
                if ( ! ( Float.isNaN( q25A ) && Float.isNaN( q25B ) ) ) {
                    assertEquals( q25A, q25B );
                    nsameQ25++;
                }
                if ( q25A > minA ) {
                    ndiff++;
                }
            }
        }
        long nrow = skymap.getRowCount();
        assertTrue( ndiff > nrow / 2 && ndiff < nrow );
        assertTrue( nsameMin > nrow / 2 && nsameMin < nrow );
        assertTrue( nsameQ25 > nrow / 2 && nsameQ25 < nrow );
    }

    private static float toFloat( Object obj ) {
        return obj == null ? Float.NaN : ((Number) obj).floatValue();
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
           .setValue( "cols", "number x_image 0.5*gid_2 "
                            + "number;count;ncount number;hit;nhit" )
           .setValue( "complete", Boolean.valueOf( isComplete ) );
        new SkyDensityMap().createExecutable( env ).execute();
        StarTable skymap = env.getOutputTable( "omode" );
        Tables.checkTable( skymap );
        int ncol = 7;
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
        assertEquals( "ncount", skymap.getColumnInfo( 5 ).getName() );
        assertEquals( "nhit", skymap.getColumnInfo( 6 ).getName() );
        double[] maxs = new double[ ncol ];
        Arrays.fill( maxs, Double.NEGATIVE_INFINITY );
        RowSequence rseq = skymap.getRowSequence();
        long nr = 0;
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
            nr++;
        }
        rseq.close();
        assertEquals( t.getRowCount(), sums[ 1 ] );
        assertEquals( 352.25, maxs[ 3 ], 0.001 );
        assertEquals( 13.5, maxs[ 4 ] );
        assertEquals( t.getRowCount(), sums[ 5 ] );
        assertEquals( 1, maxs[ 6 ] );

        HealpixTableInfo hpxInfo =
            HealpixTableInfo.fromParams( skymap.getParameters() );
        assertEquals( level, hpxInfo.getLevel() );
        assertEquals( true, hpxInfo.isNest() );
        assertEquals( skymap.getColumnInfo( 0 ).getName(),
                      hpxInfo.getPixelColumnName() );
        long nsky = 12L << 2*level;
        if ( isComplete ) {
            assertEquals( nsky, nr );
        }
        else {
            assertTrue( nr < nsky );
        }

        return skymap;
    }

    public void testGrid() throws Exception {

        // Table with the same value in every HEALPix cell at a given level.
        final int tLevel = 4;
        double cval = 6;
        int nrow = (int) ( 12L << ( 2 * tLevel ) );
        ColumnStarTable t = ColumnStarTable.makeTableWithRows( nrow );
        t.addColumn( new ColumnData( new ColumnInfo( "ra", Double.class,
                                                     null ) ) {
            public Object readValue( long irow ) {
                return new Double( Tilings.healpixNestLon( tLevel, irow ) );
            }
        } );
        t.addColumn( new ColumnData( new ColumnInfo( "dec", Double.class,
                                                     null ) ) {
            public Object readValue( long irow ) {
                return new Double( Tilings.healpixNestLat( tLevel, irow ) );
            }
        } );
        t.addColumn( new ConstantColumn( new ColumnInfo( "c", Double.class,
                                                         null ),
                                         cval ) );
        SolidAngleUnit anyUnit = SolidAngleUnit.STERADIAN;
        double perDeg = 1.0 / Tilings.healpixSqdeg( tLevel );

        checkGrid( t, tLevel - 2, Combiner.MEAN, anyUnit, cval );
        checkGrid( t, tLevel - 2, Combiner.MEDIAN, anyUnit, cval );
        checkGrid( t, tLevel - 2, Combiner.SAMPLE_STDEV, anyUnit, 0 );
        checkGrid( t, tLevel - 2, Combiner.MIN, anyUnit, cval );
        checkGrid( t, tLevel - 2, Combiner.MAX, anyUnit, cval );
        checkGrid( t, tLevel - 2, Combiner.HIT, anyUnit, 1.0 );
        checkGrid( t, tLevel, Combiner.COUNT, anyUnit, 1.0 );
        checkGrid( t, tLevel - 2, Combiner.COUNT, anyUnit, 16.0 );
        checkGrid( t, tLevel, Combiner.SUM, anyUnit, cval );
        checkGrid( t, tLevel - 2, Combiner.SUM, anyUnit, 16 * cval );

        checkGrid( t, tLevel, Combiner.DENSITY,
                   SolidAngleUnit.DEGREE2, perDeg );
        checkGrid( t, tLevel - 2, Combiner.DENSITY,
                   SolidAngleUnit.DEGREE2, perDeg );
        checkGrid( t, tLevel - 1, Combiner.DENSITY,
                   SolidAngleUnit.ARCMIN2, perDeg / 3600. );

        checkGrid( t, tLevel, Combiner.WEIGHTED_DENSITY,
                   SolidAngleUnit.DEGREE2, cval * perDeg );
        checkGrid( t, tLevel - 2, Combiner.WEIGHTED_DENSITY,
                   SolidAngleUnit.DEGREE2, cval * perDeg );
        checkGrid( t, tLevel - 1, Combiner.WEIGHTED_DENSITY,
                   SolidAngleUnit.ARCSEC2, cval * perDeg / ( 3600. * 3600. ) );
    }

    private void checkGrid( StarTable inTable, int mLevel,
                            Combiner combiner, SolidAngleUnit unit,
                            double cellValue )
            throws Exception {
        MapEnvironment env = new MapEnvironment()
           .setValue( "in", inTable )
           .setValue( "tiling", "hpx" + mLevel )
           .setValue( "lon", "ra" )
           .setValue( "lat", "dec" )
           .setValue( "cols", "c" )
           .setValue( "count", Boolean.FALSE )
           .setValue( "combine", combiner )
           .setValue( "complete", Boolean.FALSE )
           .setValue( "perunit", unit );
        new SkyDensityMap().createExecutable( env ).execute();
        StarTable outTable = env.getOutputTable( "omode" );
        Tables.checkTable( outTable );
        assertEquals( 2, outTable.getColumnCount() );
        int iDataCol = 1;
        assertEquals( "c", outTable.getColumnInfo( iDataCol ).getName() );
        RowSequence rseq = outTable.getRowSequence();
        double dval = Double.NaN;
        int irow = 0;
        while ( rseq.next() ) {
            double d = ((Number) rseq.getCell( iDataCol )).doubleValue();
            if ( irow++ == 0 ) {
                dval = d;
            }
            else {
                assertEquals( dval, d );
            }
        }
        rseq.close();
        assertEquals( 12L << ( 2 * mLevel ), irow );
        assertEquals( dval, cellValue, cellValue * 1e-10 );
    }
}
