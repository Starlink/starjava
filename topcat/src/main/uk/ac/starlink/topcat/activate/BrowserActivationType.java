package uk.ac.starlink.topcat.activate;

import java.awt.Desktop;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.HtmlWindow;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * ActivationType for displaying a resource in a browser.
 *
 * @author   Mark Taylor
 * @since    30 Jan 2018
 */
public class BrowserActivationType implements ActivationType {

    public String getName() {
        return "View in Web Browser";
    }

    public String getDescription() {
        return "Load the resource in a file or URL column"
             + " into an external web browser";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new BrowserColumnConfigurator( tinfo, createBrowserList() );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.tableHasFlag( ColFlag.HTML )
             ? Suitability.SUGGESTED
             : tinfo.getUrlSuitability();
    }

    /**
     * Implementation that displays a URL in a general purpose web browser.
     */
    private static class BrowserColumnConfigurator
                         extends UrlColumnConfigurator {
        private final JComboBox browserChooser_;
        private static final String BROWSER_KEY = "browser";

        /**
         * Constructor.
         *
         * @param  topcat model information
         * @param  browsers  available browser implementations
         */
        BrowserColumnConfigurator( TopcatModelInfo tinfo, Browser[] browsers ) {
            super( tinfo, "Web Page",
                   new ColFlag[] { ColFlag.HTML, ColFlag.URL, } );
            JComponent queryPanel = getQueryPanel();
            browserChooser_ = new JComboBox();
            for ( Browser b : browsers ) {
                browserChooser_.addItem( b );
            }
            browserChooser_.addActionListener( getActionForwarder() );
            JLabel browserLabel = new JLabel( "Browser Type: " );
            JComponent browserBox = Box.createHorizontalBox();
            browserBox.add( browserLabel );
            browserBox.add( browserChooser_ );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            queryPanel.add( browserBox );
        }

        protected Activator createActivator( ColumnData cdata ) {
            final Object browser = browserChooser_.getSelectedItem();
            final String label = getWindowLabel( cdata );
            return browser instanceof Browser
                 ? new UrlColumnActivator( cdata, false ) {
                       protected Outcome activateUrl( URL url, long lrow ) {
                           return ((Browser) browser).browse( url, label );
                       }
                   }
                 : null;
        }

        protected String getConfigMessage( ColumnData cdata ) {
            return browserChooser_.getSelectedItem() instanceof Browser
                 ? null
                 : "no browser??";
        }

        public Safety getSafety() {
            return Safety.UNSAFE;
        }

        public ConfigState getState() {
            ConfigState state = getUrlState();
            state.saveSelection( BROWSER_KEY, browserChooser_ );
            return state;
        }

        public void setState( ConfigState state ) {
            setUrlState( state );
            state.restoreSelection( BROWSER_KEY, browserChooser_ );
        }
    }

    /**
     * Returns a new list of browsers that can be used.
     *
     * @return  browsers
     */
    private final Browser[] createBrowserList() {
        List<Browser> list = new ArrayList<Browser>();
        Desktop desktop = TopcatUtils.getBrowserDesktop();
        if ( desktop != null ) {
            list.add( new DesktopBrowser( desktop ) );
        }
        list.add( new BasicBrowser() );
        return list.toArray( new Browser[ 0 ] );
    }

    /**
     * Browser implementation that uses java.awt.Desktop.
     */
    private static class DesktopBrowser extends Browser {
        private final Desktop desktop_;

        /**
         * Constructor.
         *
         * @param  desktop  desktop instance, assumed to support URI browsing
         */
        DesktopBrowser( Desktop desktop ) {
            super( "system browser" );
            desktop_ = desktop;
        }
        public Outcome browse( URL url, String label ) {
            try {
                desktop_.browse( url.toURI() );
            }
            catch ( Throwable e ) {
                return Outcome.failure( e );
            }
            return Outcome.success( url.toString() );
        }
    }

    /**
     * Internally implemented HTML display window.
     */
    private static class BasicBrowser extends Browser {
        private HtmlWindow htmlWin_;
        BasicBrowser() {
            super( "basic browser" );
        }
        public Outcome browse( URL url, String label ) {
            HtmlWindow win = getHtmlWindow( label );
            try {
                win.setURL( url );
            }
            catch ( Throwable e ) {
                return Outcome.failure( e );
            }
            return Outcome.success( url.toString() );
        }
        private HtmlWindow getHtmlWindow( String label ) {
            if ( htmlWin_ == null ) {
                htmlWin_ = new HtmlWindow( null );
            }
            htmlWin_.setTitle( label );
            if ( ! htmlWin_.isShowing() ) {
                htmlWin_.setVisible( true );
            }
            return htmlWin_;
        }
    }

    /**
     * Browser functionality interface
     */
    private static abstract class Browser {
        final String name_;

        /**
         * Constructor.
         *
         * @param  name  browser display name
         */
        Browser( String name ) {
            name_ = name;
        }

        /**
         * Attempts to display the given URL in a browser window.
         *
         * @param  url  resource to display
         * @param  label   label for window
         * @return   action outcome
         */
        public abstract Outcome browse( URL url, String label );

        @Override
        public String toString() {
            return name_;
        }
    }
}
