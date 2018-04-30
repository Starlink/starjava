package uk.ac.starlink.topcat.activate;

import java.net.URL;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.func.Browsers;

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
        return new BrowserColumnConfigurator( tinfo );
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
         */
        BrowserColumnConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, "Web Page",
                   new ColFlag[] { ColFlag.HTML, ColFlag.URL, } );
            JComponent queryPanel = getQueryPanel();
            browserChooser_ = new JComboBox();
            for ( Browser b : Browser.values() ) {
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
            return browser instanceof Browser
                 ? new UrlColumnActivator( cdata, false ) {
                       protected Outcome activateUrl( URL url ) {
                           String turl = url.toString();
                           ((Browser) browser).showLocation( turl );
                           return Outcome.success( turl );
                       }
                   }
                 : null;
        }

        protected String getConfigMessage( ColumnData cdata ) {
            return null;
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
     * Enumeration of known browsers.
     */
    private static enum Browser {
        SYSTEM( "system browser" ) {
            public void showLocation( String url ) {
                Browsers.systemBrowser( url );
            }
        },
        BASIC( "basic browser" ) {
            public void showLocation( String url ) {
                Browsers.basicBrowser( url );
            }
        },
        FIREFOX( "firefox" ) {
            public void showLocation( String url ) {
                Browsers.mozalike( "firefox", url );
            }
        },
        MOZILLA( "mozilla" ) {
            public void showLocation( String url ) {
                Browsers.mozalike( "mozilla", url );
            }
        };
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
         * Loads the given URL in this browser.
         *
         * @param  url  location
         */
        abstract void showLocation( String url );

        @Override
        public String toString() {
            return name_;
        }
    }
}
