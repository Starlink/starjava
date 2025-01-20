package uk.ac.starlink.topcat.join;

import cds.moc.SMoc;
import java.io.IOException;
import uk.ac.starlink.ttools.cone.MocCoverage;
import uk.ac.starlink.ttools.cone.CdsHealpix;

/**
 * Footprint implementation that represents the overlap of two other
 * footprints.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2012
 */
public class OverlapCoverage extends MocCoverage {

    private final MocCoverage[] coverages_;
    
    /**
     * Constructor.
     * The supplied coverages must all be based on MOCs, there must be
     * at least one of them, and none must be null.
     *
     * @param   coverages whose intersection defines the new coverage
     */
    public OverlapCoverage( MocCoverage[] coverages ) {
        super( CdsHealpix.getInstance() );
        coverages_ = coverages;
    }

    @Override
    protected SMoc createMoc() throws IOException {
        MocCoverage cov0 = coverages_[ 0 ];
        cov0.initCoverage();
        SMoc moc = cov0.getMoc();
        for ( int i = 1; i < coverages_.length; i++ ) {
            MocCoverage cov1 = coverages_[ i ];
            cov1.initCoverage();
            SMoc moc1 = cov1.getMoc();
            if ( moc1 == null ) {
                return null;
            }
            try {
                moc = moc.intersection( moc1 );
            }
            catch ( Exception e ) {
                throw (IOException)
                      new IOException( "MOC trouble" ).initCause( e );
            }
        }
        return moc;
    } 
}
