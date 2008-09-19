package uk.ac.starlink.topcat;

import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import uk.ac.starlink.topcat.interop.TopcatServer;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Action which invokes help by attempting to display a page from the 
 * help document in a web browser.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2008
 */
public class BrowserHelpAction extends AbstractAction {

    private final Component parent_;
    private final TopcatServer server_;
    private final URL helpUrl_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );
    private static BrowserLauncher launcher_;

    /**
     * Constructor.
     *
     * @param  helpId  help ID string
     * @param  parent   parent window - may be used for positioning
     */
    public BrowserHelpAction( String helpId, Component parent ) {
        parent_ = parent;
        TopcatServer server;
        try {
            server = TopcatServer.getInstance();
        }
        catch ( IOException e ) {
            server = null;
        }
        server_ = server;
        putValue( NAME, helpId == null ? "Help in Browser"
                                       : "Help for window in Brower" );
        putValue( SMALL_ICON, helpId == null ? ResourceIcon.MANUAL_BROWSER
                                             : ResourceIcon.HELP_BROWSER );
        putValue( SHORT_DESCRIPTION,
                  helpId == null
                  ? "Attempt to display help in web browser"
                  : "Attempt to display help for this window in web browser" );
        if ( server_ != null ) {
            URL helpUrl;
            try {
                helpUrl = new URL( server_.getTopcatPackageUrl(), 
                                   "sun253/" + ( helpId == null ? "index"
                                                    : helpId )
                                             + ".html" );
                if ( ! server_.isFound( helpUrl ) ) {
                    logger_.warning( "Can't locate help URL: " + helpUrl );
                    setEnabled( false );
                }
            }
            catch ( MalformedURLException e ) {
                assert false;
                helpUrl = null;
                setEnabled( false );
            }
            helpUrl_ = helpUrl;
        }
        else {
            helpUrl_ = null;
            setEnabled( false );
        }
    }

    public void actionPerformed( ActionEvent evt ) {
        if ( launcher_ == null ) {
            try {
                launcher_ = new BrowserLauncher();
                launcher_.setNewWindowPolicy( false );
            }
            catch ( BrowserLaunchingInitializingException e ) {
                ErrorDialog.showError( parent_, "Browser Error", e );
                return;
            }
            catch ( UnsupportedOperatingSystemException e ) {
                ErrorDialog.showError( parent_, "Browser Error", e );
                return;
            }
        }
        launcher_.openURLinBrowser( helpUrl_.toString() );
    }
}
