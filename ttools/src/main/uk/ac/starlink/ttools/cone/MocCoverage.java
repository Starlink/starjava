package uk.ac.starlink.ttools.cone;

import cds.healpix.Healpix;
import cds.healpix.HealpixNestedBMOC;
import cds.moc.HealpixImpl;
import cds.moc.SMoc;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for Coverage implementations based on MOC
 * (HEALPix Multi-Order Coverage) objects.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2012
 */
public abstract class MocCoverage implements Coverage {

    private final HealpixImpl hpi_;
    private volatile SMoc moc_;
    private volatile Amount amount_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /** Default Healpix implementation. */
    public static final HealpixImpl DFLT_HPI = CdsHealpix.getInstance();

    /**
     * Constructs a MocCoverage with default HEALPix implementation.
     */
    protected MocCoverage() {
        this( DFLT_HPI );
    }

    /**
     * Constructs a MocCoverage with specified HEALPix implementation.
     *
     * @param   hpi  HEALPix implementation to use for calculations
     */
    protected MocCoverage( HealpixImpl hpi ) {
        hpi_ = hpi;
    }

    /**
     * Constructs the MOC which will define this object's coverage.
     * This method, which may be time-consuming, will be called a
     * maximum of once by the {@link #initCoverage} method of
     * {@link MocCoverage}, and should not be called by anyone else.
     *
     * @return  new MOC defining footprint, or null
     */
    protected abstract SMoc createMoc() throws IOException;

    public synchronized void initCoverage() throws IOException {
        if ( amount_ == null ) {
            assert moc_ == null;
            try {
                moc_ = createMoc();
            }
            finally {
                amount_ = determineAmount( moc_ );
                assert amount_ != null;
            }
        }
    }

    public Amount getAmount() {
        return amount_;
    }

    public boolean discOverlaps( double alphaDeg, double deltaDeg,
                                 double radiusDeg ) {
        checkInitialised();
        Boolean knownResult = amount_.getKnownResult();
        if ( knownResult != null ) {
            return knownResult.booleanValue();
        }
        int mocOrder = moc_.getMocOrder();
        double alphaRad = Math.toRadians( alphaDeg );
        double deltaRad = Math.toRadians( deltaDeg );
        double radiusRad = Math.toRadians( radiusDeg );
        try {

            /* If the cell at the MOC's deepest order into which the given
             * position falls is in the MOC, it's an overlap. */
            long centerPixel = hpi_.ang2pix( mocOrder, alphaDeg, deltaDeg );
            if ( moc_.isIntersecting( mocOrder, centerPixel ) ) {
                return true;
            }
            if ( radiusDeg == 0 ) {
                return false;
            }

            /* Otherwise, create a BMOC corresponding to the disc,
             * and test whether each of its tiles overlaps with this MOC.
             * For the BMOC construction use the MOC maximum order under normal
             * circumstances, since that will give the most accurate result.  
             * However, place a limit on the order that scales with the
             * requested radius, since in the case of a BMOC with tiles
             * much smaller than the radius, it can lead to a very large
             * data structure and in extreme cases an OutOfMemoryError.
             * The effect of this is to introduce a small degradation of
             * the accuracy of the test near the edge of the disc. */
            int orderLimit = Healpix.getBestStartingDepth( radiusRad * 0.01 );
            int scanOrder = mocOrder;
            if ( scanOrder > orderLimit ) {
                logger_.config( "Limit MOC order " + scanOrder + " -> "
                              + orderLimit + " for radius "
                              + radiusDeg + "deg" );
                scanOrder = orderLimit;
            }
            HealpixNestedBMOC bmoc =
                Healpix.getNested( mocOrder )
                       .newConeComputerApprox( radiusRad )
                       .overlappingCells( alphaRad, deltaRad );
            for ( HealpixNestedBMOC.CurrentValueAccessor vac : bmoc ) {
                if ( moc_.isIntersecting( vac.getDepth(), vac.getHash() ) ) {
                    return true;
                }
            }
            return false;
        }
        catch ( Exception e ) {
            logger_.log( Level.WARNING, "Unexpected MOC error - fail safe", e );
            return true;
        }
    }

    /**
     * Returns the MOC object associated with this footprint.
     *
     * @return  moc
     */
    public SMoc getMoc() {
        return moc_;
    }

    /**
     * Checks that this object is initialised, and throws an exception if not.
     */
    private void checkInitialised() {
        if ( amount_ == null ) {
            throw new IllegalStateException( "Not initialised" );
        }
    }

    /**
     * Utility method to stringify a MOC.
     *
     * @param  moc  MOC
     * @return  string
     */
    static String summariseMoc( SMoc moc ) { 
        return new StringBuffer()
           .append( "Fraction: " )
           .append( (float) moc.getCoverage() )
           .append( ", " )
           .append( "Pixels: " )
           .append( moc.getNbCoding() )
           .append( ", " )
           .append( "Bytes: " )
           .append( moc.getMem() )
           .append( ", " )
           .append( "Order: " )
           .append( moc.getMocOrder() )
           .toString();
    }

    /**
     * Returns the amount category for a given Moc.
     *
     * @param   moc, may be null
     * @return   amount category
     */
    private static Amount determineAmount( SMoc moc ) {
        if ( moc == null ) {
            return Amount.NO_DATA;
        }
        else {
            double frac = moc.getCoverage();
            if ( frac == 0 ) {
                return Amount.NO_SKY;
            }
            else if ( frac == 1 ) {
                return Amount.ALL_SKY;
            }
            else {
                return Amount.SOME_SKY;
            }
        }
    }
}
