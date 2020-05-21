package uk.ac.starlink.ttools.cone;

import cds.healpix.FlatHashIterator;
import cds.healpix.HashComputer;
import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.healpix.HealpixNestedBMOC;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;

/**
 * Tiling implementation based on the HEALPix scheme.
 *
 * @author   Mark Taylor
 * @since    12 Dec 2007
 */
public class HealpixTiling implements SkyTiling {

    private final int k_;
    private final boolean nest_;
    private final HashComputer hasher_;
    private final HealpixNested hnested_;

    /** Maximum healpix level permitted by CDS Healpix implementation (29). */
    public static final int MAX_LEVEL = Healpix.DEPTH_MAX;

    /**
     * Constructor.
     *
     * @param  k    Healpix level
     * @param  nest  true for nesting scheme, false for ring scheme
     */
    public HealpixTiling( int k, boolean nest ) {
        if ( k > MAX_LEVEL ) {
            throw new IllegalArgumentException( "k " + k + " too large" );
        }
        k_ = k;
        nest_ = nest;
        hasher_ = Healpix.getNestedFast( k );
        hnested_ = Healpix.getNested( k );
    }

    /**
     * Returns the HEALpix level.
     *
     * @return  log2(nside)
     */
    public int getHealpixK() {
        return k_;
    }

    /**
     * Indicates HEALPix ordering scheme.
     *
     * @return   true for NEST, false for RING
     */
    public boolean isNest() {
        return nest_;
    }

    public long getPixelCount() {
        return 12L << ( 2 * k_ );
    }

    public ValueInfo getIndexInfo() {
        String name = "hpx" + k_;
        Class<?> clazz = k_ <= 13 ? Integer.class : Long.class;
        String descrip = "HEALPix index at order " + k_;
        DefaultValueInfo info = new DefaultValueInfo( name, clazz, descrip );
        info.setUCD( "pos.healpix" );
        return info;
    }

    public long getPositionTile( double ra, double dec ) {
        long nestHash = hasher_.hash( Math.toRadians( ra ),
                                      Math.toRadians( dec ) );
        return nest_ ? nestHash
                     : hnested_.toRing( nestHash );
    }

    public long[] getTileRange( double ra, double dec, double radius ) {
        HealpixNestedBMOC bmoc =
            hnested_.newConeComputerApprox( Math.toRadians( radius ) )
                    .overlappingCells( Math.toRadians( ra ),
                                       Math.toRadians( dec ) );
        long lo = Long.MAX_VALUE;
        long hi = Long.MIN_VALUE;

        /* It should be possible in the nested case to do this more efficiently
         * by iterating over the MOC cells rather than the individual tiles,
         * but it would require a bit of work not apparently supported
         * by public library methods. */
        for ( FlatHashIterator fhit = bmoc.flatHashIterator();
              fhit.hasNext(); ) {
            long hash = fhit.next();
            if ( ! nest_ ) {
                hash = hnested_.toRing( hash );
            }
            lo = Math.min( lo, hash );
            hi = Math.max( hi, hash );
        }
        return lo <= hi ? new long[] { lo, hi }
                        : null;
    }
}
