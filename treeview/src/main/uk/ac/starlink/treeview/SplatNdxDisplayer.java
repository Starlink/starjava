package uk.ac.starlink.treeview;

import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.NDXSpecDataImpl;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.SplatBrowserMain;
import uk.ac.starlink.splat.util.SplatException;

/**
 * NDX displayer which will display using SPLAT classes.
 * Although a public constructor is available, the {@link #getInstance}
 * method is also provided for singleton-liek use of the class.
 */
public class SplatNdxDisplayer extends NdxDisplayer {

    private SplatBrowser browser;
    private static SplatNdxDisplayer instance;

    public SplatNdxDisplayer() {
        super( "http://localhost:8081/services/SplatSOAPServices",
               "displayNDX" );
    }

    public synchronized static SplatNdxDisplayer getInstance() {
        if ( instance == null ) {
            instance = new SplatNdxDisplayer();
        }
        return instance;
    }

    public boolean canDisplay( Ndx ndx ) {
        return super.canDisplay( ndx ) && NodeUtil.hasAST();
    }

    public boolean localDisplay( Ndx ndx, boolean embedded ) {
        try {
            SpecData spectrum = new SpecData( new NDXSpecDataImpl( ndx ) );
            SplatBrowser browser = getBrowser( embedded );
            browser.addSpectrum( spectrum );
            browser.displaySpectrum( spectrum );
            return true;
        }
        catch ( SplatException e ) {
            return false;
        }
    }

    private synchronized SplatBrowser getBrowser( boolean embedded ) {
        if ( browser == null ) {
            //  Application-level initialisations for SPLAT.
            SplatBrowserMain.guessProperties( false );

            //  Create the browser.
            browser = new SplatBrowser( embedded );
        }
        browser.setVisible( true );
        return browser;
    }
 
}
