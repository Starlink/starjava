package uk.ac.starlink.ttools.task;

import cds.healpix.FlatHashIterator;
import cds.healpix.HashComputer;
import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.healpix.HealpixNestedBMOC;
import cds.healpix.VerticesAndPathComputer;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.func.CoordsRadians;
import uk.ac.starlink.util.LongList;

/**
 * Interrogates a HEALPix all-sky map to sample pixel data.
 * The map is supplied in the form of a table (one row per pixel,
 * using HEALPix pixel indices), as used for instance by
 * <a href="https://lambda.gsfc.nasa.gov/">LAMBDA</a>.
 *
 * @author   Mark Taylor
 * @since    5 Dec 2011
 */
public class PixSampler {

    private final StarTable pixTable_;
    private final int order_;
    private final HashComputer hasher_;
    private final HealpixNested hnested_;
    private final boolean nested_;
    private final int ncol_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.calc" );

    /** StatMode for making point samples. */
    public static final StatMode POINT_MODE = new PointStatMode( "Point" );

    /** StatMode for taking an average. */
    public static final StatMode MEAN_MODE = new MeanStatMode( "Mean" );

    /**
     * Constructor.
     *
     * @param   pixTable  random access HEALPix-format table
     *                    (one row per pixel)
     * @param   nested  true for nested pixel order, false for ring
     * @param   order   HEALPix order
     * @throws  IOException  if the table has the wrong number of rows
     *                       or is not random access
     */
    public PixSampler( StarTable pixTable, boolean nested, int order )
            throws IOException {
        if ( ! pixTable.isRandom() ) {
            throw new IOException( "Pixel data not random access" );
        }
        order_ = order;
        hnested_ = Healpix.getNested( order_ );
        hasher_ = Healpix.getNestedFast( order_ );
        long hasNrow = pixTable.getRowCount();
        long requireNrow = 12L << ( 2 * order_ );
        if ( hasNrow != requireNrow  ) {
            throw new IOException( "Wrong number of rows for order " + order
                                 + " (" + hasNrow + "!=" + requireNrow + ")" );
        }
        pixTable_ = pixTable;
        nested_ = nested;
        ncol_ = pixTable.getColumnCount();
    }

    /**
     * Samples a single value from a given sky position.
     *
     * @param   icol   column index of value to sample
     * @param   alphaDeg  longitude position in degrees
     * @param   deltaDeg  latitude position in degrees
     * @param   radiusDeg  radius of disc over which statistics will be
     *                     gathered (ignored for point-like statMode)
     * @param   statMode   mode for sampling statistics 
     * @return   sampled value at given point
     */
    public Object sampleValue( int icol, double alphaDeg, double deltaDeg,
                               double radiusDeg, StatMode statMode )
            throws IOException {

        /* Check the given position is on the sky. */
        if ( alphaDeg >= -360 && alphaDeg <= +360 &&
             deltaDeg >= -90 && deltaDeg <= +90 ) {
            final Object[] samples;

            /* If point-like statistic, just sample at the given position. */
            if ( statMode.isPoint() ) {
                long irow = getPixIndex( alphaDeg, deltaDeg );
                samples = new Object[] { pixTable_.getCell( irow, icol ) };
            }

            /* Otherwise, get samples from all pixels within given radius. */
            else {
                long[] irows = getPixIndices( alphaDeg, deltaDeg, radiusDeg );
                int nr = irows.length;
                samples = new Object[ nr ];
                for ( int ir = 0; ir < nr; ir++ ) {
                    samples[ ir ] = pixTable_.getCell( ir, icol );
                }
            }

            /* Combine acquired samples to get the result. */
            return statMode.getResult( samples );
        }
        else {
            return null;
        }
    }

    /** 
     * Samples values from all columns in given table at a given sky position.
     *
     * @param   alphaDeg  longitude position in degrees
     * @param   deltaDeg  latitude position in degrees
     * @param   radiusDeg  radius of disc over which statistics will be
     *                     gathered (ignored for point-like statMode)
     * @param   statMode   mode for sampling statistics 
     * @return  array of sampled column values at given point
     */
    public Object[] sampleValues( double alphaDeg, double deltaDeg,
                                  double radiusDeg, StatMode statMode )
            throws IOException {

        /* Check the given position is on the sky. */
        if ( alphaDeg >= -360 && alphaDeg <= +360 &&
             deltaDeg >= -90 && deltaDeg <= +90 ) {
            final Object[][] samples;

            /* If point-like statistic, just sample at the given position. */
            if ( statMode.isPoint() ) {
                long irow = getPixIndex( alphaDeg, deltaDeg );
                samples = new Object[ ncol_ ][ 1 ];
                Object[] row = pixTable_.getRow( irow );
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    samples[ icol ][ 0 ] = row[ icol ];
                }
            }

            /* Otherwise, get samples from all pixels within given radius. */
            else {
                long[] irows = getPixIndices( alphaDeg, deltaDeg, radiusDeg );
                int nr = irows.length;
                samples = new Object[ ncol_ ][ nr ];
                for ( int ir = 0; ir < nr; ir++ ) {
                    Object[] row = pixTable_.getRow( irows[ ir ] );
                    for ( int icol = 0; icol < ncol_; icol++ ) {
                        samples[ icol ][ ir ] = row[ icol ];
                    }
                }
            }

            /* Combine acquired samples to get the result. */
            Object[] outRow = new Object[ ncol_ ];
            for ( int ic = 0; ic < ncol_; ic++ ) {
                outRow[ ic ] = statMode.getResult( samples[ ic ] );
            }
            return outRow;
        }
        else {
            return new Object[ ncol_ ];
        }
    }

    /**
     * Returns the metadata for the columns output by the sampler.
     *
     * @param  statMode   mode for sampling statistics
     * @return   array of output metadata objects, one for each output column
     */
    public ColumnInfo[] getValueInfos( StatMode statMode ) {
        int ncol = pixTable_.getColumnCount();
        ColumnInfo[] infos = new ColumnInfo[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            infos[ icol ] =
                statMode.getResultInfo( pixTable_.getColumnInfo( icol ) );
        }
        return infos;
    }

    /**
     * Returns the index of row in the pixel table corresponding to the
     * given sky position.
     *
     * @param   alphaDeg   longitude in degrees
     * @param   deltaDeg   latitude in degrees
     * @return   table row index
     */
    private long getPixIndex( double alphaDeg, double deltaDeg ) {
        long inest = hasher_.hash( Math.toRadians( alphaDeg ),
                                   Math.toRadians( deltaDeg ) );
        return nested_ ? inest
                       : hnested_.toRing( inest );
    }

    /**
     * Returns an array of pixel table row indices for pixels at least
     * partly covered by a disc on the sky.
     *
     * @param   alphaDeg  longitude in degrees of disc centre
     * @param   deltaDeg  latitude in degrees of disc centre
     * @param   radiusDeg  radius in degrees of disc
     * @return   array of pixel indices
     */
    private long[] getPixIndices( double alphaDeg, double deltaDeg,
                                  double radiusDeg ) {
        double alphaRad = Math.toRadians( alphaDeg );
        double deltaRad = Math.toRadians( deltaDeg );
        double radiusRad = Math.toRadians( radiusDeg );

        /* Get the overlapping pixels; this may contain false positives. */
        HealpixNestedBMOC bmoc =
            hnested_.newConeComputerApprox( Math.toRadians( radiusDeg ) )
                    .overlappingCenters( Math.toRadians( alphaDeg ),
                                         Math.toRadians( deltaDeg ) );

        /* Convert the result into an array of longs, eliminating false
         * positives. */
        LongList pixList = new LongList( (int) bmoc.computeDeepSize() );
        VerticesAndPathComputer vpc = hnested_.newVerticesAndPathComputer();
        double[] cpos = new double[ 2 ];
        for ( FlatHashIterator fhit = bmoc.flatHashIterator();
              fhit.hasNext(); ) {
            long lpix = fhit.next();
            vpc.center( lpix, cpos );
            if ( CoordsRadians
                .skyDistanceRadians( alphaRad, deltaRad, cpos[ 0 ], cpos[ 1 ] ) 
                 <= radiusRad ) {
                pixList.add( lpix );
            }
        }
        long[] pixes = pixList.toLongArray();

        /* Convert to ring scheme if required. */
        if ( ! nested_ ) {
            for ( int ip = 0; ip < pixes.length; ip++ ) {
                pixes[ ip ] = hnested_.toRing( pixes[ ip ] );
            }
        }
        return pixes;
    }

    /**
     * Constructs a PixSampler from a given table.
     * The current implementation works with any table having a row count
     * corresponding to a HEALPix pixel count, the order is inferred.
     *
     * @param   pixTable   random access table containing HEALPix pixels 
     * @return  PixSampler object taking data from table
     * @throws  IOException  if table is not random access or does not
     *                       appear to contain HEALPix data
     */
    public static PixSampler createPixSampler( StarTable pixTable ) 
            throws IOException {
        if ( ! pixTable.isRandom() ) {
            throw new IOException( "Pixel data not random access" );
        }
        if ( ! HealpixTableInfo.isHealpix( pixTable.getParameters() ) ) {
            logger_.warning( "Table doesn't look like a HEALPix map" );
        }
        int order = inferOrder( pixTable );
        Boolean isNested = inferNested( pixTable );
        final boolean nested;
        if ( isNested == null ) {
            logger_.warning( "Cannot determine HEALPix ordering scheme"
                           + " - assuming nested" );
            nested = true;
        }
        else {
            nested = isNested.booleanValue();
        }
        return new PixSampler( pixTable, nested, order );
    }

    /**
     * Tries to work out whether a given table uses the nested or ring
     * HEALPix ordering scheme.
     *
     * @param  pixTable  pixel data table
     * @return  TRUE for nested, FALSE for ring, null for don't know
     */
    public static Boolean inferNested( StarTable pixTable ) {
        Object orderObj = Tables.getValue( pixTable.getParameters(),
                                           HealpixTableInfo.HPX_ISNEST_INFO );
        return orderObj instanceof Boolean
             ? (Boolean) orderObj
             : null;
    }

    /**
     * Tries to work out the HEALPix order parameter for a pixel data table.
     * Mainly it looks at the row count, but if the table obeys HEALPix
     * header conventions any discrepancies between declared and apparent
     * order result in an error.
     *
     * @param   pixTable  pixel data table
     * @return  HEALPix order
     */
    public static int inferOrder( StarTable pixTable ) throws IOException {

        /* Work out order from row count. */
        if ( ! pixTable.isRandom() ) {
            throw new IOException( "Pixel data not random access" );
        }
        long nrow = pixTable.getRowCount();
        int level = -1;
        for ( int l = 0; level < 0 && l < Healpix.DEPTH_MAX; l++ ) {
            if ( nrow == 12L << ( 2 * l ) ) {
                level = l;
            }
        }
        if ( level < 0 ) {
            throw new IOException( "Unsuitable number of rows for all-sky "
                                 + "HEALPix map (" + nrow + ")" );
        }

        /* Read and check declared order if present. */
        List<DescribedValue> pixParams = pixTable.getParameters();
        DescribedValue levelParam =
            pixTable.getParameterByName( HealpixTableInfo.HPX_LEVEL_INFO
                                                         .getName() );
        long dLevel = -1;
        if ( levelParam != null ) {
            Object levelObj = levelParam.getValue();
            if ( levelObj instanceof Integer || levelObj instanceof Long ) {
                dLevel = ((Number) levelObj).intValue();
            }
        }
        if ( dLevel >= 0 && dLevel != level ) {
            String msg = "Order mismatch: declared order (" + dLevel + ")"
                       + " != count (" + level + ")";
            if ( HealpixTableInfo.isHealpix( pixParams ) ) {
                throw new IOException( msg );
            }
            else {
                logger_.warning( msg );
            }
        }

        /* Return order. */
        return level;
    }

    /**
     * Defines how statistics are to be acquired from a pixel or set of pixels.
     */
    public interface StatMode {

        /**
         * Provides a column metadata object describing the output of this
         * mode, given the metadata of the input.
         *
         * @param  baseInfo  input data metadata
         * @return  output data metadata
         */
        ColumnInfo getResultInfo( ColumnInfo baseInfo );

        /**
         * Returns the result of some statistical operation on a set of
         * supplied values.
         * 
         * @param   values  array of input values
         * @return   result of statistical operation
         */
        Object getResult( Object[] values );

        /**
         * Indicates whether this operation is point-like.
         * If so, only a single value should be supplied to the
         * {@link #getResult} method.
         *
         * @return  true iff this operation is point-like
         */
        boolean isPoint();
    }

    /**
     * Partial StatMode implementation.
     */
    private static abstract class AbstractStatMode implements StatMode {
        private final String name_;
        private final boolean isPoint_;

        /**
         * Constructor.
         *
         * @param   name   mode name
         * @parma   isPoint  true iff operation is point-like
         */
        protected AbstractStatMode( String name, boolean isPoint ) {
            name_ = name;
            isPoint_ = isPoint;
        }

        public boolean isPoint() {
            return isPoint_;
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * StatMode implementation for point sampling.
     */
    private static class PointStatMode extends AbstractStatMode {
        PointStatMode( String name ) {
            super( name, true );
        }
        public ColumnInfo getResultInfo( ColumnInfo baseInfo ) {
            return baseInfo;
        }
        public Object getResult( Object[] values ) {
            return values != null && values.length > 0 ? values[ 0 ] : null;
        }
    };

    /**
     * StatMode implementation for area averaging.
     */
    private static class MeanStatMode extends AbstractStatMode {
        MeanStatMode( String name ) {
            super( name, false );
        }
        public ColumnInfo getResultInfo( ColumnInfo baseInfo ) {
            String baseDesc = baseInfo.getDescription();
            String desc = baseDesc == null || baseDesc.trim().length() == 0
                        ? "Mean value"
                        : baseDesc + ", spatial mean";
            return new ColumnInfo( baseInfo.getName(), Double.class, desc );
        }
        public Object getResult( Object[] values ) {
            double sum = 0;
            double count = 0;
            for ( int i = 0; i < values.length; i++ ) {
                Object val = values[ i ];
                if ( val instanceof Number ) {
                    double dval = ((Number) val).doubleValue();
                    if ( ! Double.isNaN( dval ) ) {
                        sum += dval;
                        count++;
                    }
                }
            }
            return count > 0 ? new Double( sum / count )
                             : Double.NaN;
        }
    };
}
