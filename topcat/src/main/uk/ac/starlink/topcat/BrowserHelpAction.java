package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import uk.ac.starlink.topcat.interop.TopcatServer;

/**
 * Action which invokes help by attempting to display a page from the 
 * help document in a web browser.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2008
 */
public class BrowserHelpAction extends AbstractAction {

    private final Component parent_;
    private final URL helpUrl_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );
    private static final TopcatServer server_ = getTopcatServer();
    private static final Desktop desktop_ = TopcatUtils.getBrowserDesktop();

    /**
     * Constructor.
     *
     * @param  helpUrl  help page URL 
     * @param  parent   parent window - may be used for positioning
     */
    private BrowserHelpAction( URL helpUrl, Component parent ) {
        helpUrl_ = helpUrl;
        parent_ = parent;
        setEnabled( helpUrl_ != null && server_ != null && desktop_ != null );
    }

    public void actionPerformed( ActionEvent evt ) {
        try {
            desktop_.browse( helpUrl_.toURI() );
        }
        catch ( Throwable e ) {
            logger_.log( Level.WARNING, "Browser invocation failed: " + e, e );
        }
    }

    /**
     * Returns a new action displaying help for a given help ID.
     *
     * @param  helpId  help ID
     * @param  parent   parent window - may be used for positioning
     * @return  help action
     */
    public static Action createIdAction( String helpId, Component parent ) {
        Action action =
            new BrowserHelpAction( getHelpUrl( helpId + ".html" ), parent );
        action.putValue( NAME, "Help for Window in Browser" );
        action.putValue( SMALL_ICON, ResourceIcon.HELP_BROWSER );
        action.putValue( SHORT_DESCRIPTION,
                         "Attempt to display help for this window"
                       + " in a web browser" );
        return action;
    }

    /**
     * Returns a new action displaying help for the whole application as
     * a multi-page HTML document
     *
     * @param  parent   parent window - may be used for positioning
     * @return  help action
     */
    public static Action createManualAction( Component parent ) {
        Action action =
            new BrowserHelpAction( getHelpUrl( "index.html" ), parent );
        action.putValue( NAME, "Help in Browser" );
        action.putValue( SMALL_ICON, ResourceIcon.MANUAL_BROWSER );
        action.putValue( SHORT_DESCRIPTION,
                         "Attempt to display help for the application"
                       + " as a multiple-page HTML document in a web browser" );
        return action;
    }

    /**
     * Returns a new action displaying help for the whole application as
     * a single-page HTML document
     *
     * @param  parent   parent window - may be used for positioning
     * @return  help action
     */
    public static Action createManual1Action( Component parent ) {
        Action action =
            new BrowserHelpAction( getHelpUrl( "sun253.html" ), parent );
        action.putValue( NAME,
                         "Help in Browser (single page)" );
        action.putValue( SMALL_ICON, ResourceIcon.MANUAL1_BROWSER );
        action.putValue( SHORT_DESCRIPTION,
                         "Attempt to display help for the application"
                       + " as a single-page HTML document in a web browser" );
        return action;
    }

    /**
     * Returns a TopcatServer instance, or null;
     * called only once.
     *
     * @return   server, or null
     */
    private static TopcatServer getTopcatServer() {
        try {
            return TopcatServer.getInstance();
        }
        catch ( IOException e ) {
            return null;
        }
        catch ( SecurityException e ) {
            return null;
        }
    }

    /**
     * Returns an internal URL corresponding to a relative URL
     * (rooted at uk/ac/starlink/topcat/ in the classpath).
     *
     * @param  relUrl   relative path
     * @return  URL, or null in case of problem
     */
    public static URL getHelpUrl( String relUrl ) {
        return server_ == null
             ? null
             : server_.getRelativeUrl( "sun253/" + relUrl );
    }

    /**
     * Returns an internal URI corresponding to a relative URL
     * (rooted at uk/ac/starlink/topcat/ in the classpath).
     *
     * @param  relUrl   relative path
     * @return  URI, or null in case of problem
     */
    public static URI getHelpUri( String relUrl ) {
        URL url = getHelpUrl( relUrl );
        if ( url == null ) {
            return null;
        }
        else {
            try {
                return url.toURI();
            }
            catch ( URISyntaxException e ) {
                logger_.log( Level.WARNING, "Bad URI: " + url, e );
                return null;
            }
        }
    }
}
