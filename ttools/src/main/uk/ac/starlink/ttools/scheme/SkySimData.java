package uk.ac.starlink.ttools.scheme;

import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.healpix.VerticesAndPathComputer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.LongFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ToDoubleFunction;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.cone.CdsHealpixUtil;

/**
 * Stores information required to simulate a sky catalogue.
 * It contains data for a number of rows, each row representing
 * a region of the sky with the capability to generate representative
 * catalogue quantities in that region.
 * The current implementation uses HEALPix tiles.
 *
 * <p>The data is held in arrays in memory, so that access including
 * concurrent access will be fast.  It is therefore only intended
 * to hold information for a modest number of sky areas.
 *
 * @author   Mark Taylor
 * @since    6 Aug 2020
 */
public class SkySimData {

    private final int nrow_;
    private final HealpixTableInfo hpxInfo_;
    private final double[] cprobs_;
    private final Xyz xyz_;
    private final Qstat[] qstats_;
    private final boolean useGaussianPosition_;

    /**
     * Constructor.
     *
     * @param  nrow  row count
     * @param  hpxInfo   information about HEALPix scheme for tile indices
     * @param  cprobs   array of cumulative probabilities for each row
     * @param  xyz      information about the sky position of each row,
     *                  encoded as unit vectors
     * @param  qstats   per-row quantities for which data can be generated
     */
    private SkySimData( int nrow, HealpixTableInfo hpxInfo,
                        double[] cprobs, Xyz xyz, Qstat[] qstats ) {
        nrow_ = nrow;
        hpxInfo_ = hpxInfo;
        cprobs_ = cprobs;
        xyz_ = xyz;
        qstats_ = qstats;
        useGaussianPosition_ = true;
    }

    /**
     * Returns the number of sky areas represented.
     *
     * @return  row count
     */
    public int getRowCount() {
        return nrow_;
    }

    /**
     * Returns the HEALPix setup of the statistics file on which this
     * simulation data is based.
     *
     * @return   healpix info
     */
    public HealpixTableInfo getHealpixInfo() {
        return hpxInfo_;
    }

    /**
     * Returns a sky area at random.  The returned value is weighted by
     * the number of objects in the original data set, so that the
     * sky distribution of the original data set can be replicated
     *
     * @param   seed  random seed
     * @return   weighted random row index in this data set
     */
    public int getRandomRowIndex( long seed ) {
        double rndval = new Random( seed ).nextDouble();
        int ipos = Arrays.binarySearch( cprobs_, rndval );
        if ( ipos >= 0 ) {
            return ipos;
        }
        else {
            int ix = -2 - ipos;
            while( ix < cprobs_.length - 1 &&
                   cprobs_[ ix + 1 ] == cprobs_[ ix ] ) {
                ix++;
            }
            return ix;
        }
    }

    /**
     * Returns a column object that can give simulated sky positions.
     *
     * @param  isLat  true for latitude, false for longitude
     */
    public Col createCoordColumn( boolean isLat ) {
        int level = hpxInfo_.getLevel();
        final String name;
        final String description;
        final String ucd;
        switch ( hpxInfo_.getCoordSys() ) {
            case GALACTIC:
                name = isLat ? "b" : "l";
                ucd = "pos.galactic." + ( isLat ? "lat" : "lon" );
                description = "Galactic " + ( isLat ? "latitude" : "longitude");
                break; 
            case ECLIPTIC:
                name = isLat ? "ecl_lat" : "ecl_lon";
                ucd = "pos.ecliptic." + ( isLat ? "lat" : "lon" );
                description = "Ecliptic " + ( isLat ? "latitude" : "longitude");
                break;
            case CELESTIAL:
            default:
                name = isLat ? "dec" : "ra";
                ucd = "pos.eq." + ( isLat ? "dec" : "ra" );
                description = ( isLat ? "Right Ascension" : "Declination" )
                            + " J2000";
        }
        ColumnInfo info = new ColumnInfo( name, Double.class, description );
        info.setUnitString( "deg" );
        info.setUCD( ucd );
        final double levelFact = 1. / ( 1 << ( 2 * level ) );
        final ToDoubleFunction<Random> rndAdjust =
            useGaussianPosition_ ? rnd -> 18 * ( rnd.nextGaussian() )
                                 : rnd -> 60 * ( rnd.nextDouble() - 0.5 );
        final double[] xs = xyz_.xs_;
        final double[] ys = xyz_.ys_;
        final double[] zs = xyz_.zs_;
        return new Col( info ) {
            public double getValue( int irow, long seed ) {
                double x = xs[ irow ];
                double y = ys[ irow ];
                double z = zs[ irow ];
                Random rnd = new Random( seed );
                x += rndAdjust.applyAsDouble( rnd ) * levelFact;
                y += rndAdjust.applyAsDouble( rnd ) * levelFact;
                z += rndAdjust.applyAsDouble( rnd ) * levelFact;
                double r1 = 1.0 / Math.sqrt( x * x + y * y + z * z );
                x *= r1;
                y *= r1;
                z *= r1;
                if ( isLat ) {
                    double theta = Math.acos( z );
                    return Math.toDegrees( 0.5 * Math.PI - theta );
                }
                else {
                    return Math.toDegrees( Math.atan2( y, x ) );
                }
            }
        };
    }

    /**
     * Returns an array of column objects that can report statistically
     * defined quantities.
     *
     * @return  columns for quantities that can generate numeric values
     */
    public Col[] createQuantityColumns() {
        int nq = qstats_.length;
        Col[] qcols = new Col[ nq ];
        for ( int iq = 0; iq < nq; iq++ ) {
            qcols[ iq ] = createQuantityColumn( qstats_[ iq ] );
        }
        return qcols;
    }

    /**
     * Returns a column for a given quantity.
     *
     * @param  qstat  quantity generator definition
     */
    private Col createQuantityColumn( final Qstat qstat ) {
        ColumnInfo info = new ColumnInfo( qstat.meanInfo_ );
        info.setContentClass( Double.class );
        info.setDescription( null );
        return new Col( info ) {
            public double getValue( int irow, long seed ) {
                float mean = qstat.means_[ irow ];
                float stdev = qstat.stdevs_[ irow ];
                return ! Float.isNaN( mean ) && stdev >= 0
                     ? mean + new Random( seed ).nextGaussian() * stdev
                     : Double.NaN;
            }
        };
    }

    /**
     * Assembles a SkySimData instance by using statistical information
     * in a given HEALPix table (one with a row for each healpix tile).
     * As well as a column containing the healpix index
     * (as characterised by the HealpixTableInfo parameter),
     * it can have pairs of column <code>X</code>,
     * <code>X&lt;stdevSuffix&gt;</code> containing respectively the mean and
     * sample standard deviation respectively of quantities per region.
     *
     * @param  table   table containing statistical information
     * @param  hpxInfo   HEALPix characterisation of the table
     * @param  cnameWeight  name of the table column containing region weight
     *                      (for instance a source count)
     * @param  stdevSuffix   column name suffix indicating sample standard
     *                       deviation
     * @return   SkySimData instance
     */
    public static SkySimData readData( StarTable table,
                                       HealpixTableInfo hpxInfo,
                                       String cnameWeight, String stdevSuffix )
            throws IOException {
        int nrow = Tables.checkedLongToInt( table.getRowCount() );
        int level = hpxInfo.getLevel();

        /* Note column names in input table. */
        int ncol = table.getColumnCount();
        String[] colNames = new String[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            colNames[ icol ] = table.getColumnInfo( icol ).getName();
        }

        /* Prepare to store cumulative probabilities per row. */
        int icWeight = Arrays.asList( colNames ).indexOf( cnameWeight );
        double[] weights = new double[ nrow ];

        /* Identify available statistical quantities. */
        Qloc[] qlocs = findQlocs( colNames, stdevSuffix );
        int nq = qlocs.length;
        Qstat[] qstats = new Qstat[ nq ];
        for ( int iq = 0; iq < nq; iq++ ) {
            qstats[ iq ] =
                new Qstat( table.getColumnInfo( qlocs[ iq ].icMean_ ), nrow );
        }
        Xyz xyz = new Xyz( nrow );

        /* Prepare to store central sky position as unit vectors. */
        final VerticesAndPathComputer hnf = Healpix.getNestedFast( level );
        final HealpixNested hn = Healpix.getNested( level );
        final LongFunction<double[]> toLonLat =
              hpxInfo.isNest() ? hpx -> hnf.center( hpx )
                               : hpx -> hnf.center( hn.toRing( hpx ) );
        final int icHpx = Arrays.asList( colNames )
                         .indexOf( hpxInfo.getPixelColumnName() );
        final LongUnaryOperator irowToHpx;
        if ( icHpx < 0 ) {
            irowToHpx = irow -> irow;
        }
        else {
            final RowAccess racc = table.getRowAccess();
            irowToHpx = irow -> {
                try {
                    racc.setRowIndex( irow );
                    Object hpxObj = racc.getCell( icHpx );
                    return hpxObj instanceof Number
                         ? ((Number) hpxObj).longValue()
                         : -1;
                }
                catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            };
        }

        /* Iterate over rows and store data in arrays. */
        RowSequence rseq = table.getRowSequence();
        double[] v3 = new double[ 3 ];
        double wsum = 0;
        try {
            for ( int irow = 0; rseq.next(); irow++ ) {
                Object[] row = rseq.getRow();

                /* Weights. */
                Object weightObj = row[ icWeight ];
                double weight = weightObj instanceof Number
                              ? ((Number) weightObj).doubleValue()
                              : 0;
                weights[ irow ] = wsum;
                if ( weight > 0 ) {
                    wsum += weight;
                }

                /* Sky positions. */
                long hpx = irowToHpx.applyAsLong( irow );
                double[] lonlat = toLonLat.apply( hpx );
                CdsHealpixUtil.lonlatToVector( lonlat, v3 );
                xyz.xs_[ irow ] = v3[ 0 ];
                xyz.ys_[ irow ] = v3[ 1 ];
                xyz.zs_[ irow ] = v3[ 2 ];

                /* Statistical quantities. */
                for ( int iq = 0; iq < nq; iq++ ) {
                    Qloc qloc = qlocs[ iq ];
                    Qstat qstat = qstats[ iq ];
                    qstat.means_[ irow ] =
                        ((Number) row[ qloc.icMean_ ]).floatValue();
                    qstat.stdevs_[ irow ] =
                        ((Number) row[ qloc.icStdev_ ]).floatValue();
                }
            }
        }
        finally {
            rseq.close();
        }

        /* Scale weights to make cumulative probabilities. */
        double scale = 1.0 / wsum;
        for ( int i = 1; i < nrow; i++ ) {
            weights[ i ] *= scale;
        }
        assert weights[ 0 ] == 0;

        /* Return populated object. */
        return new SkySimData( nrow, hpxInfo, weights, xyz, qstats );
    }

    /**
     * Identifies the available statistical quantities and records
     * where their mean and standard deviation can be found.
     *
     * @param  colnames  list of column names
     * @param  stdevSuffix   suffix identifying a sample standard deviation
     *                       column name
     * @return  array characterising quantity locations
     */
    private static Qloc[] findQlocs( String[] colnames, String stdevSuffix ) {
        List<Qloc> qlocs = new ArrayList<>();
        for ( int icol = 0; icol < colnames.length; icol++ ) {
            String cname = colnames[ icol ];
            if ( cname.endsWith( stdevSuffix ) ) {
                String baseName =
                    cname.substring( 0, cname.length() - stdevSuffix.length() );
                int jcol = Arrays.asList( colnames ).indexOf( baseName );
                if ( jcol >= 0 ) {
                    qlocs.add( new Qloc( jcol, icol ) );
                }
            }
        }
        return qlocs.toArray( new Qloc[ 0 ] );
    }

    /**
     * Defines an object that can return a sample value for a given region.
     */
    public static abstract class Col {
        private final ColumnInfo info_;

        /**
         * Constructor.
         *
         * @param  info  column metadata
         */
        protected Col( ColumnInfo info ) {
            info_ = info;
        }

        /**
         * Returns column metadata.
         *
         * @return  column metadata
         */
        public ColumnInfo getInfo() {
            return info_;
        }

        /**
         * Returns a statistically sampled value for a given row.
         *
         * @param  irow  row index indicating sky region
         * @param  seed   random seed
         */
        public abstract double getValue( int irow, long seed );
    }

    /**
     * Structure that stores a number of 3-vectors.
     */
    private static class Xyz {
        final double[] xs_;
        final double[] ys_;
        final double[] zs_;

        /**
         * Constructor.
         *
         * @param  nrow   row count
         */
        Xyz( int nrow ) {
            xs_ = new double[ nrow ];
            ys_ = new double[ nrow ];
            zs_ = new double[ nrow ];
        }
    }

    /**
     * Characterises location of statistical columns in a table.
     */
    private static class Qloc {
        final int icMean_;
        final int icStdev_;

        /**
         * @param  icMean  column index of column giving mean
         * @param  icStdev  column index of column giving
         *                  sample standard deviation
         */
        Qloc( int icMean, int icStdev ) {
            icMean_ = icMean;
            icStdev_ = icStdev;
        }
    }

    /**
     * Stores statistical information for a quantity at each row.
     */
    private static class Qstat {
        final ColumnInfo meanInfo_;
        final float[] means_;
        final float[] stdevs_;

        /**
         * Constructor.
         *
         * @param   meanInfo  metadata for basic quantity
         * @param nrow  row count
         */
        Qstat( ColumnInfo meanInfo, int nrow ) {
            meanInfo_ = meanInfo;
            means_ = new float[ nrow ];
            stdevs_ = new float[ nrow ];
        }
    }
}
