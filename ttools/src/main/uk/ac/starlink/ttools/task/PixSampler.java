package uk.ac.starlink.ttools.task;

import gov.fnal.eag.healpix.PixTools;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Vector3d;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Interrogates a HEALPix all-sky map to sample pixel data.
 * The map is supplied in the form of a table (one row per pixel,
 * using HEALPix pixel indices), as used for instance by
 * <a href="http://lambda.gsfc.nasa.gov/">LAMBDA</a>.
 *
 * @author   Mark Taylor
 * @since    5 Dec 2011
 */
public class PixSampler {

    private final StarTable pixTable_;
    private final long nside_;
    private final boolean nested_;
    private final int ncol_;
    private final PixTools pixTools_;
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
     * @param   nside   HEALPix nside value
     * @param   nested  true for nested pixel order, false for ring
     * @throws  IOException  if the table has the wrong number of rows
     *                       or is not random access
     */
    public PixSampler( StarTable pixTable, long nside, boolean nested )
            throws IOException {
        if ( ! pixTable.isRandom() ) {
            throw new IOException( "Pixel data not random access" );
        }
        pixTools_ = PixTools.getInstance();
        long hasNrow = pixTable.getRowCount();
        long requireNrow = pixTools_.Nside2Npix( nside );
        if ( hasNrow != requireNrow  ) {
            throw new IOException( "Wrong number of rows for nside " + nside
                                 + " (" + hasNrow + "!=" + requireNrow + ")" );
        }
        pixTable_ = pixTable;
        nside_ = nside;
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
        double phiRad = alphaToPhi( alphaDeg );
        double thetaRad = deltaToTheta( deltaDeg );
        return nested_ ? pixTools_.ang2pix_nest( nside_, thetaRad, phiRad )
                       : pixTools_.ang2pix_ring( nside_, thetaRad, phiRad );
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
        double phiRad = alphaToPhi( alphaDeg );
        double thetaRad = deltaToTheta( deltaDeg );
        double radiusRad = radiusDeg * Math.PI / 180.;
        Vector3d vec = pixTools_.Ang2Vec( thetaRad, phiRad );
        List pixList = pixTools_.query_disc( nside_, vec, radiusRad,
                                             nested_ ? 1 : 0, 0 );
        long[] pixes = new long[ pixList.size() ];
        int ip = 0;
        for ( Iterator it = pixList.iterator(); it.hasNext(); ) {
            pixes[ ip++ ] = ((Number) it.next()).longValue();
        }
        assert ip == pixes.length;
        return pixes;
    }

    /**
     * Converts longitude in degrees to polar coordinate phi in radians.
     *
     * @param   longitude measured anticlockwise from meridian in degrees
     * @return   phi meansured anticlockwise from meridian in radians
     */
    private static double alphaToPhi( double alphaDeg ) {
        double alphaRad = alphaDeg * Math.PI / 180.;
        return alphaRad;
    }

    /**
     * Converts latitude in degrees to polar coordinate theta in radians.
     *
     * @param   deltaDeg  latitude measured North from equator in degrees
     * @return  theta measured South from North pole in radians
     */
    private static double deltaToTheta( double deltaDeg ) {
        double deltaRad = deltaDeg * Math.PI / 180.;
        return Math.PI * 0.5 - deltaRad;
    }

    /**
     * Constructs a PixSampler from a given table.
     * The current implementation works with any table having a row count
     * corresponding to a HEALPix pixel count, the nside is inferred.
     * Parameters are interrogated in accordance with the conventions used by,
     * for instance the FITS files used at NASA's
     * <a href="http://lambda.gsfc.nasa.gov/">LAMBDA</a> archive,
     * but other HEALPix tables that work in a more or less similar way
     * will probably work.
     *
     * <p>I don't know of any proper reference for encoding of HEALPix maps
     * in FITS files, but the documentation for the HPIC package
     * (<a href="http://cmb.phys.cwru.edu/hpic/"
     *          >http://cmb.phys.cwru.edu/hpic/</a>)
     * has a useful list of heuristics (manual section 2.10.1).
     * One of these acknowledges the fact that some HEALPix FITS files
     * have columns which are 1024-element arrays
     * (<code>TFORMn = '1024E'</code>).  This routine does not currently
     * support this rather perverse convention.  If somebody requests it,
     * maybe I'll consider implementing it.
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
        int nside = inferNside( pixTable );
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
        return new PixSampler( pixTable, nside, nested );
    }

    /**
     * Tries to work out whether a given table uses the nested or ring
     * HEALPix ordering scheme.
     * Parameters are interrogated in accordance with the conventions used by,
     * for instance the FITS files used at NASA's
     * <a href="http://lambda.gsfc.nasa.gov/">LAMBDA</a> archive.
     *
     * @param  pixTable  pixel data table
     * @return  TRUE for nested, FALSE for ring, null for don't know
     */
    public static Boolean inferNested( StarTable pixTable ) {
        String orderKey = "ORDERING";
        String ordering = getStringParam( pixTable, orderKey );
        if ( ordering != null ) {
            String hval = "Header " + orderKey + "=\"" + ordering.trim() + "\"";
            if ( ordering.toUpperCase().startsWith( "NEST" ) ) {
                logger_.info( hval + " - inferring NESTED HEALPix ordering" );
                return Boolean.TRUE;
            }
            else if ( ordering.toUpperCase().startsWith( "RING" ) ) {
                logger_.info( hval + " - inferring RING HEALPix ordering" );
                return Boolean.FALSE;
            }
            else {
                logger_.warning( hval + " - unknown value" );
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Tries to work out the HEALPix nside parameter for a pixel data table.
     * Mainly it looks at the row count, but if the table obeys HEALPix
     * header conventions any discrepancies between declared and apparent
     * nside result in an error.
     *
     * @param   pixTable  pixel data table
     * @return  HEALPix nside
     */
    public static int inferNside( StarTable pixTable ) throws IOException {

        /* Work out nside from row count. */
        if ( ! pixTable.isRandom() ) {
            throw new IOException( "Pixel data not random access" );
        }
        long nrow = pixTable.getRowCount();
        long nside;
        try {
            nside = PixTools.getInstance().Npix2Nside( nrow );
        }
        catch ( RuntimeException e ) {
            nside = -1;
        }
        if ( ! ( nside > 0 ) ) {
            throw new IOException( "Unsuitable number of rows for all-sky "
                                 + "HEALPix map (" + nrow + ")" );
        }

        /* Read and check declared nside if present. */
        String pixtype = getStringParam( pixTable, "PIXTYPE" );
        boolean dHealpix = "HEALPIX".equalsIgnoreCase( pixtype );
        double dNside = getNumericParam( pixTable, "NSIDE" );
        if ( dNside >= 0 && dNside != nside ) {
            String msg = "NSIDE mismatch: declared (" + dNside + ")"
                       + " != count (" + nside + ")";
            if ( dHealpix ) {
                throw new IOException( msg );
            }
            else {
                logger_.warning( msg );
            }
        }

        /* Return nside. */
        return Tables.checkedLongToInt( nside );
    }

    /**
     * Returns the string value of a table parameter, if one exists.
     *
     * @param  table  table whose parameter list is to be examined
     * @param  key  requested parameter name
     * @return  string value of parameter <code>key</code>, or null
     */
    private static String getStringParam( StarTable table, String key ) {
        DescribedValue dval = table.getParameterByName( key );
        if ( dval != null ) {
            Object value = dval.getValue();
            if ( value instanceof String ) {
                return (String) value;
            }
        }
        return null;
    }

    /**
     * Returns the floating point value of a table parameter, if one exists.
     *
     * @param  table  table whose parameter list is to be examined
     * @param  key  requested parameter name
     * @return  numeric value of parameter <code>key</code>, or NaN
     */
    private static double getNumericParam( StarTable table, String key ) {
        DescribedValue dval = table.getParameterByName( key );
        if ( dval != null ) {
            Object value = dval.getValue();
            if ( value instanceof Number ) {
                return ((Number) value).doubleValue();
            }
        }
        return Double.NaN;
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
