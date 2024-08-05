// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import uk.ac.starlink.topcat.AbstractHtmlPanel;
import uk.ac.starlink.topcat.Executor;
import uk.ac.starlink.topcat.HtmlWindow;
import uk.ac.starlink.util.URLUtils;

/**
 * Displays URLs in web browsers.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Jun 2005
 */
public class Browsers {

    private static HtmlWindow htmlWindow_;
    private static Boolean canDesktop_;

    /**
     * Private constructor prevents instantiation.
     */
    private Browsers() {
    }

    /**
     * Displays a URL in a basic HTML viewer.
     * This is only likely to work for HTML, text or RTF data.
     * The browser can follow hyperlinks and has simple forward/back
     * buttons, but lacks the sophistication of a proper WWW browser
     * application.
     *
     * @param  url  location of the document to display
     * @return  short log message
     */
    public static String basicBrowser( String url ) {
        if ( url == null || url.trim().length() == 0 ) {
            return null;
        }
        URL url1 = null;
        try {
            File f = new File( url );
            if ( f.exists() ) {
                url1 = f.toURI().toURL();
            }
        }
        catch ( IOException e ) {
            // not a file
        }
        if ( url1 == null ) {
            try {
                url1 = URLUtils.newURL( url );
            }
            catch ( MalformedURLException e ) {
                return "Bad URL: " + url;
            }
        }
        getHtmlWindow().setURL( url1 );
        return url;
    }

    /**
     * Attempts to display a URL in the system's default web browser.
     * Exactly what couts as the default web browser is system dependent,
     * as is whether this function will work properly.
     *
     * @param  url  location of the document to display
     * @return  short log message
     */
    public static String systemBrowser( String url ) {
        if ( canDesktop_ == null ) {
            boolean canDesktop;
            if ( Desktop.isDesktopSupported() ) {
                canDesktop = Desktop.getDesktop()
                            .isSupported( Desktop.Action.BROWSE );
            }
            else {
                canDesktop = false;
            }
            canDesktop_ = Boolean.valueOf( canDesktop );
        }
        if ( canDesktop_.booleanValue() ) {
            try {
                Desktop.getDesktop().browse( new URI( url ) );
                return url;
            }
            catch ( Throwable e ) {
                return "Error: " + e;
            }
        }
        else {
            return "No system browser - try basic?";
        }
    }

    /**
     * Displays a URL in a Mozilla web browser.
     * Probably only works on Unix-like operating systems, and only if
     * Mozilla is already running.
     *
     * @param   url   location of the document to display
     * @return  short log message
     */
    public static String mozilla( String url ) {
        return mozalike( "mozilla", url );
    }

    /**
     * Displays a URL in a Firefox web browser.
     * Probably only works on Unix-like operating systems, and only
     * if Firefox is already running.
     *
     * @param   url   location of the document to display
     * @return  short log message
     */
    public static String firefox( String url ) {
        return mozalike( "firefox", url );
    }

    /**
     * Displays a URL in a Netscape web browser.
     * Probably only works on Unix-like operating systems, and only
     * if Netscape is already running.
     *
     * @param   url   location of the document to display
     * @return  short log message
     */
    public static String netscape( String url ) {
        return mozalike( "netscape", url );
    }

    /** 
     * Displays a URL in a web browser from the Mozilla family;
     * it must support flags of the type 
     * "<code>-remote openURL(</code><em>url</em><code>)</code>".
     * Probably only works on Unix-like operating systems, and only
     * if the browser is already running.
     *
     * @param   cmd  name or path of the browser command
     * @param   url  location of the document to display
     * @return  short log message
     */
    public static String mozalike( String cmd, String url ) {
        if ( url == null || url.trim().length() == 0 ) {
            return null;
        }
        String[] argv = { cmd, "-remote", "openURL(" + url + ")", };
        return uk.ac.starlink.topcat.func.System
              .execute( Executor.createExecutor( argv ) );
    }

    /**
     * Returns a basic HTML viewer window.
     *
     * @return  html window
     */
    private static HtmlWindow getHtmlWindow() {
        if ( htmlWindow_ == null ) {
            htmlWindow_ = new HtmlWindow( (Component) null,
                                          AbstractHtmlPanel.createPanel() );
        }
        if ( ! htmlWindow_.isShowing() ) {
            htmlWindow_.setVisible( true );
        }
        return htmlWindow_;
    }
}
