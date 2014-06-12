package uk.ac.starlink.ttools.cone;

import cds.moc.HealpixImpl;
import cds.moc.HealpixMoc;
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
    private volatile HealpixMoc moc_;
    private volatile Amount amount_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
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
     * @return  new MOC defining footprint
     */
    protected abstract HealpixMoc createMoc() throws IOException;

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
        int mocOrder = moc_.getMaxOrder();
        try {
            long centerPixel = hpi_.ang2pix( mocOrder, alphaDeg, deltaDeg );
            if ( moc_.isInTree( mocOrder, centerPixel ) ) {
                return true;
            }
            if ( radiusDeg == 0 ) {
                return false;
            }
            int discOrder =
                Math.min( PixtoolsHealpix
                         .nsideToOrder( PixtoolsHealpix.getInstance()
                                       .sizeToNside( radiusDeg ) ),
                          mocOrder );
            long[] discPixels =
                hpi_.queryDisc( discOrder, alphaDeg, deltaDeg, radiusDeg );
            for ( int i = 0; i < discPixels.length; i++ ) {
                if ( moc_.isInTree( discOrder, discPixels[ i ] ) ) {
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
    public HealpixMoc getMoc() {
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
    static String summariseMoc( HealpixMoc moc ) { 
        return new StringBuffer()
           .append( "Amount: " )
           .append( (float) moc.getCoverage() )
           .append( ", " )
           .append( "Pixels: " )
           .append( moc.getSize() )
           .append( ", " )
           .append( "Bytes: " )
           .append( moc.getMem() )
           .toString();
    }

    /**
     * Returns the amount category for a given Moc.
     *
     * @param   moc, may be null
     * @return   amount category
     */
    private static Amount determineAmount( HealpixMoc moc ) {
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
