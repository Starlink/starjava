package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.net.URL;
import javax.swing.JPanel;

/**
 * Skeleton implementation for a panel that can display navigable HTML.
 * The currently displayed location can be monitored using the "url" property.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2019
 */
public abstract class AbstractHtmlPanel extends JPanel {

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
}
