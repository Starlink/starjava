package uk.ac.starlink.treeview;

import java.io.IOException;
import uk.ac.starlink.jaiutil.HDXImage;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.sog.SOG;
import uk.ac.starlink.sog.SOGNavigatorImageDisplay;

/**
 * NDX displayer which will display using SoG classes.
 * Although a public constructor is available, the {@link #getInstance}
 * method is also provided for singleton-like use of the class.
 */
public class SogNdxDisplayer extends NdxDisplayer {

    private SOGNavigatorImageDisplay imageDisplay;
    private static SogNdxDisplayer instance;

    public SogNdxDisplayer() {
        super( "http://localhost:8082/services/SOGRemoteServices", "showNDX" );
    }

    public static synchronized SogNdxDisplayer getInstance() {
        if ( instance == null ) {
            instance = new SogNdxDisplayer();
        }
        return instance;
    }

    public boolean canDisplay( Ndx ndx ) {
        return ndx.isPersistent();
    }

    public boolean localDisplay( Ndx ndx ) {
        if ( ndx.isPersistent() ) {
            try {
                getImageDisplay().setHDXImage( new HDXImage( ndx ) );
                return true;
            }
            catch ( IOException e ) {
                return false;
            }
        }
        return false;
    }

    private synchronized SOGNavigatorImageDisplay getImageDisplay() {
        if ( imageDisplay == null ) {
            SOG sog = new SOG();
            imageDisplay = (SOGNavigatorImageDisplay) sog.getImageDisplay();
        }
        return imageDisplay;
    }
}
