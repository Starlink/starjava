package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import cds.moc.HealpixMoc;

/**
 * MOC coverage implementation which uses the ASCII serialization.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2023
 * @see  <a href="https://www.ivoa.net/documents/MOC/">MOC v2.0 sec 4.2.3</a>
 */
public class AsciiMocCoverage extends MocCoverage {

    private final String asciiMoc_;

    /**
     * Constructor.
     *
     * @param  asciiMoc  MOC encoded using the ASCII MOC serialization
     */
    public AsciiMocCoverage( String asciiMoc ) {
        asciiMoc_ = asciiMoc;
    }

    @Override
    protected HealpixMoc createMoc() throws IOException {
        try {
            return new HealpixMoc( asciiMoc_ );
        }
        catch ( Exception e ) {
            throw new IOException( "MOC ASCII format error", e );
        }
    }
}
