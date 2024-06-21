package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.net.URL;
import javax.swing.JPanel;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.topcat.fx.FxHtmlPanel;

/**
 * Skeleton implementation for a panel that can display navigable HTML.
 * The currently displayed location can be monitored using the "url" property.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2019
 */
public abstract class AbstractHtmlPanel extends JPanel {

    private static Boolean hasFx_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor.
     */
    protected AbstractHtmlPanel() {
        super( new BorderLayout() );
    }

    /**
     * Sets the location which is to be displayed in this panel.
     *
     * @param  url  locator of resource to display
     */
    public abstract void setUrl( URL url );

    /**
     * Returns the location which is currently displayed in this panel.
     *
     * @return  locator of displayed resource
     */
    public abstract URL getUrl();

    /**
     * Indicates whether the JRE has access to JavaFX classes,
     * and hence whether the <code>createFxPanel</code> method ought to work.
     *
     * @return  true iff createFxPanel ought to work
     */
    public static boolean hasJavaFx() {
        if ( hasFx_ == null ) {
            try {
                Class.forName( "javafx.beans.Observable" ).toString();
                logger_.log( Level.CONFIG, "JavaFX is present" );
                hasFx_ = Boolean.TRUE;
            }
            catch ( Throwable e ) {
                logger_.log( Level.INFO, "No JavaFX: " + e );
                hasFx_ = Boolean.FALSE;
            }
        }
        return hasFx_.booleanValue();
    }

    /**
     * Attempts to create an HtmlPanel based on JavaFX classes.
     * If it fails (for example because JavaFX classes are not present),
     * null is returned and a report may be made through the loggint system.
     *
     * @return  JavaFX-based instance, or null
     */
    public static AbstractHtmlPanel createFxPanel() {
        if ( hasJavaFx() ) {
            try {

                /* Note: at time of writing this class is provided
                 * (with source) in the source package as a pre-built
                 * class in a separate jar file, to avoid problems with
                 * building it in an environment that may lack JavaFX. */
                return new FxHtmlPanel();
            }
            catch ( Throwable e ) {
                logger_.log( Level.WARNING,
                             "JavaFX browser creation failed: " + e, e );
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Creates an HtmlPanel based on Swing classes.  This should always work.
     *
     * @return  Swing-based instance
     */
    public static AbstractHtmlPanel createSwingPanel() {
        return new SwingHtmlPanel();
    }

    /**
     * Returns a best-efforts HtmlPanel.
     *
     * @return  instance based on JavaFX if possible,
     *          otherwise instance based on Swing
     */
    public static AbstractHtmlPanel createPanel() {
        AbstractHtmlPanel panel = createFxPanel();
        return panel != null ? panel : createSwingPanel();
    }
}
