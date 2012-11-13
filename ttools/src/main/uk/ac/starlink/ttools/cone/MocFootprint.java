package uk.ac.starlink.ttools.cone;

import cds.moc.HealpixImpl;
import cds.moc.HealpixMoc;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for Footprint implementations based on MOC
 * (HEALPix Multi-Order Coverage) objects.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2012
 */
public abstract class MocFootprint implements Footprint {

    private final HealpixImpl hpi_;
    private volatile HealpixMoc moc_;
    private volatile Coverage coverage_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param   hpi  HEALPix implementation to use for calculations
     */
    protected MocFootprint( HealpixImpl hpi ) {
        hpi_ = hpi;
    }

    /**
     * Constructs the MOC which will define this object's footprint.
     * This method, which may be time-consuming, will be called a
     * maximum of once by the {@link #initFootprint} method of
     * {@link MocFootprint}, and should not be called by anyone else.
     *
     * @return  new MOC defining footprint
     */
    protected abstract HealpixMoc createMoc() throws IOException;

    public synchronized void initFootprint() throws IOException {
        if ( coverage_ == null ) {
            assert moc_ == null;
            try {
                moc_ = createMoc();
            }
            finally {
                coverage_ = determineCoverage( moc_ );
                assert coverage_ != null;
            }
        }
    }

    public Coverage getCoverage() {
        return coverage_;
    }

    public boolean discOverlaps( double alphaDeg, double deltaDeg,
                                 double radiusDeg ) {
        checkInitialised();
        Boolean knownResult = coverage_.getKnownResult();
        if ( knownResult != null ) {
            return knownResult.booleanValue();
        }
        HealpixMoc overlapMoc;
        try {
            overlapMoc = moc_.queryDisc( hpi_, alphaDeg, deltaDeg, radiusDeg );
        }
        catch ( Exception e ) {
            logger_.log( Level.WARNING, "Unexpected MOC error - fail safe", e );
            return true;
        }
        return overlapMoc.getSize() > 0;
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
        if ( coverage_ == null ) {
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
           .append( "Coverage: " )
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
     * Returns the coverage type for a given Moc.
     *
     * @param   moc, may be null
     * @return   coverage type
     */
    private static Coverage determineCoverage( HealpixMoc moc ) {
        if ( moc == null ) {
            return Coverage.NO_DATA;
        }
        else {
            double frac = moc.getCoverage();
            if ( frac == 0 ) {
                return Coverage.NO_SKY;
            }
            else if ( frac == 1 ) {
                return Coverage.ALL_SKY;
            }
            else {
                return Coverage.SOME_SKY;
            }
        }
    }
}
