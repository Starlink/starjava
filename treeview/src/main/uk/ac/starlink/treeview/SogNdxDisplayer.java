package uk.ac.starlink.treeview;

import java.io.IOException;
import uk.ac.starlink.jaiutil.HDXImage;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.sog.SOG;
import uk.ac.starlink.sog.SOGNavigatorImageDisplay;
import uk.ac.starlink.sog.SOGNavigatorImageDisplayFrame;

/**
 * NDX displayer which will display using SoG classes.
 * Although a public constructor is available, the {@link #getInstance}
 * method is also provided for singleton-like use of the class.
 */
public class SogNdxDisplayer extends NdxDisplayer {

    private SOG sog;
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
        return true;
    }

    public boolean localDisplay( Ndx ndx, boolean embedded ) {
        try {
            getImageDisplay( embedded ).setHDXImage( new HDXImage( ndx ) );
            return true;
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return false;
        }
    }

    private synchronized SOGNavigatorImageDisplay 
        getImageDisplay( boolean embedded ) {

        SOGNavigatorImageDisplay imageDisplay = null;
        if ( sog == null ) {
            sog = new SOG();
            sog.setDoExit( ! embedded );
            imageDisplay = (SOGNavigatorImageDisplay) sog.getImageDisplay();
        }
        else {

            //  Get a new window each time.
            imageDisplay = (SOGNavigatorImageDisplay) sog.getImageDisplay();

            SOGNavigatorImageDisplayFrame frame =
                (SOGNavigatorImageDisplayFrame) imageDisplay.newWindow();

            imageDisplay = (SOGNavigatorImageDisplay) 
                frame.getImageDisplayControl().getImageDisplay();
            imageDisplay.setDoExit( ! embedded );
        }
        return imageDisplay;
    }
}
