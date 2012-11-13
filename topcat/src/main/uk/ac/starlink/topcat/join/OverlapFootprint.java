package uk.ac.starlink.topcat.join;

import cds.moc.HealpixMoc;
import java.io.IOException;
import uk.ac.starlink.ttools.cone.MocFootprint;
import uk.ac.starlink.ttools.cone.PixtoolsHealpix;

/**
 * Footprint implementation that represents the overlap of two other
 * footprints.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2012
 */
public class OverlapFootprint extends MocFootprint {

    private final MocFootprint[] footprints_;
    
    /**
     * Constructor.
     * The supplied footprints must all be based on MOCs, there must be
     * at least one of them, and none must be null.
     *
     * @param   footprints whose intersection defines the new footprint
     */
    public OverlapFootprint( MocFootprint[] footprints ) {
        super( PixtoolsHealpix.getInstance() );
        footprints_ = footprints;
    }

    @Override
    protected HealpixMoc createMoc() throws IOException {
        MocFootprint fp0 = footprints_[ 0 ];
        fp0.initFootprint();
        HealpixMoc moc = fp0.getMoc();
        for ( int i = 1; i < footprints_.length; i++ ) {
            MocFootprint fp1 = footprints_[ i ];
            fp1.initFootprint();
            HealpixMoc moc1 = fp1.getMoc();
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
