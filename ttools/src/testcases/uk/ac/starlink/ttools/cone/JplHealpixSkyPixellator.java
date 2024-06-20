package uk.ac.starlink.ttools.cone;

import java.util.function.Supplier;
import java.util.logging.Logger;
import healpix.essentials.HealpixBase;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import uk.ac.starlink.table.join.FixedRadiusConePixer;
import uk.ac.starlink.table.join.HealpixSkyPixellator;
import uk.ac.starlink.table.join.VariableRadiusConePixer;

/**
 * HEALpix pixellator based on semi-official HEALPix java library,
 * written by Martin Reinecker et al.
 * Maximum K value is 29.
 *
 * @author   Mark Taylor 
 * @since    23 Jul 2012
 */
public class JplHealpixSkyPixellator extends HealpixSkyPixellator {

    private final Scheme scheme_;
    private HealpixBase healpixBase_;
    private final int qdiscFactor_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );

    /**
     * Scale factor which determines the sky pixel size to use,
     * as a multiple of the angular scale, if no k value is set explicitly.
     * This is a tuning factor (any value will give correct results,
     * but performance may be affected).
     * The current value may not be optimal.
     */
    private static final int DEFAULT_SCALE_FACTOR = 8;

    /**
     * Constructs a pixellator using either the RING or NESTED HEALPix scheme.
     *
     * @param  nested  false for RING sche, true for NESTED
     * @param  qdiscFactor  resolution factor (<code>fact</code> parameter)
     *                      at which <code>queryDiscInclusive</code>
     *                      call is made; should be a power of 2 for nested,
     *                      4 is apparently a 'typical choice'
     */
    public JplHealpixSkyPixellator( boolean nested, int qdiscFactor ) {
        super( HealpixBase.order_max );
        assert HealpixBase.order_max == 29;
        scheme_ = nested ? Scheme.NESTED : Scheme.RING;
        qdiscFactor_ = qdiscFactor;
    }

    /**
     * Constructs a pixellator using the default scheme (RING).
     */
    public JplHealpixSkyPixellator() {
        this( false, 4 );
    }

    public Supplier<VariableRadiusConePixer>
            createVariableRadiusPixerFactory() {
        final HealpixBase healpixBase = healpixBase_;
        final int qdiscFactor = qdiscFactor_;
        return () -> new VariableRadiusConePixer() {
            public Long[] getPixels( double alpha, double delta,
                                     double radius ) {
                return getConePixels( healpixBase, qdiscFactor,
                                      alpha, delta, radius );
            }
        };
    }

    public Supplier<FixedRadiusConePixer>
            createFixedRadiusPixerFactory( final double radius ) {
        final HealpixBase healpixBase = healpixBase_;
        final int qdiscFactor = qdiscFactor_;
        return () -> new FixedRadiusConePixer() {
            public Long[] getPixels( double alpha, double delta ) {
                return getConePixels( healpixBase, qdiscFactor,
                                      alpha, delta, radius );
            }
        };
    }

    static Long[] getConePixels( HealpixBase healpixBase, int qdiscFactor,
                                 double alpha, double delta, double radius ) {
        double theta = Math.PI * 0.5 - delta;
        alpha = alpha % ( 2 * Math.PI );
        if ( alpha < 0 ) {
            alpha += 2 * Math.PI;
        }
        Pointing pointing = new Pointing( theta, alpha );
        RangeSet rset;
        try {
            rset = healpixBase
                  .queryDiscInclusive( pointing, radius, qdiscFactor );
        }
        catch ( Exception e ) {
            logger_.warning( "Healpix error for "
                           + alpha + ", " + delta + ", " + radius + ": " + e );
            rset = null;
        }
        if ( rset == null ) {
            return new Long[ 0 ];
        }
        else {
            int npix = (int) rset.nval();
            Long[] pixels = new Long[ npix ];
            int ip = 0;
            for ( RangeSet.ValueIterator vit = rset.valueIterator();
                  vit.hasNext(); ) {
                pixels[ ip++ ] = Long.valueOf( vit.next() );
            }
            assert ip == npix;
            return pixels;
        }
    }

    protected void configureK( int k ) {
        int nside = 1 << k;
        HealpixBase hpbase;
        try {
            hpbase = new HealpixBase( nside, scheme_ );
        }
        catch ( Exception e ) {
            long ns = Math.min( Math.max( 1, nside ), HealpixBase.ns_max );
            try {
                logger_.warning( "Unable to set HEALPix order to " + k );
                hpbase = new HealpixBase( ns, Scheme.NESTED );
            }
            catch ( Exception e1 ) {
                throw new AssertionError( "Serious HEALPix problem: " + e1 );
            }
        }
        healpixBase_ = hpbase;
    }

    public int calculateDefaultK( double scale ) {

        /* Calculate the HEALPix map resolution parameter appropriate for
         * the requested scale.  Any value is correct, the scale is just
         * a tuning parameter. */
        double pixelSize = DEFAULT_SCALE_FACTOR * scale;
        double pixelSizeArcSec = pixelSize * ( 180. * 60 * 60 / Math.PI );
        long nside = getNSide( pixelSizeArcSec );
        return (int) Math.round( Math.log( nside ) / Math.log( 2 ) );
    }

    /**
     * Mostly copied from PixTools source.
     * calculate requared nside given pixel size in arcsec
     * @param pixsize in arcsec
     * @return long nside parameter
     */
    public long getNSide(double pixsize) {
        long ns_max = HealpixBase.ns_max;
        int order_max = HealpixBase.order_max;
        long res = 0;
        double pixelArea = pixsize*pixsize;
        double degrad = Math.toDegrees(1.);
        double skyArea = 4.*Math.PI*degrad*degrad*3600.*3600.;
        long npixels = (long) (skyArea/pixelArea);
        long nsidesq = npixels/12;
        long nside_req = (long) Math.sqrt(nsidesq);
        long mindiff = ns_max;
        int indmin = 0;
        for (int i=0; i<order_max; i++) {
            long nside = 1L << i;
            if (Math.abs(nside_req - nside) <= mindiff) {
                mindiff = Math.abs(nside_req - nside);
                res = nside;
                indmin = i;
            }
            if ((nside_req > res) && (nside_req < ns_max)) {
                res = 1L << (indmin+1);
            }
            if (nside_req > ns_max ) {
                logger_.info( "Limit nside to " + ns_max );
                return ns_max;
            }
        }
        return res;
    }
}
