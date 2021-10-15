package uk.ac.starlink.topcat.activate;

import java.awt.Window;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.DatalinkPanel;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.vo.datalink.LinksDoc;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;

/**
 * Activation type for viewing a downloaded table as a DataLink file.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2018
 */
public class ViewDatalinkActivationType implements ActivationType {

    public String getName() {
        return "View Datalink Table";
    }

    public String getDescription() {
        return "View the data in a file or URL column as a DataLink table";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new DatalinkConfigurator( tinfo );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.tableHasFlag( ColFlag.DATALINK )
             ? Suitability.SUGGESTED
             : Suitability.AVAILABLE;
    }

    /**
     * Loads the file/URL at a given location into a given DataLink panel.
     *
     * @param  loc  location of Datalink {links}-response file
     * @param  dlPanel   display component for successfully-loaded datalink file
     * @param  window   window in which dlPanel is hosted;
     *                  if non-null, it will be set visible on load
     * @return  outcome
     */
    public static Outcome invokeLocation( String loc,
                                          final DatalinkPanel dlPanel,
                                          final Window window ) {
        VOElementFactory voelFact = new VOElementFactory();
        final LinksDoc linksDoc;
        try {
            VOElement voel = voelFact.makeVOElement( loc );
            linksDoc = LinksDoc.randomAccess( LinksDoc.createLinksDoc( voel ) );
        }
        catch ( SAXException e ) {
            return Outcome.failure( "XML parse failure: " + e.getMessage() );
        }
        catch ( IOException e ) {
            return Outcome.failure( e );
        }
        long nrow = linksDoc.getResultTable().getRowCount();
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                dlPanel.setLinksDoc( linksDoc );
                if ( window != null ) {

                    /* In the case of auto-invoke, don't generally setVisible.
                     * setVisible(true) (at least in some cases; this may be
                     * platform-dependent) brings the window to the front and
                     * grabs focus, but we want the thing to stay unobtrusive.
                     * If the window has been closed however, bring it back,
                     * since otherwise there's going to be no way for the user
                     * to recover it. */
                    if ( dlPanel.isAutoInvoke() ) {
                        if ( ! window.isVisible() ) {
                            window.setVisible( true );
                        }
                    }

                    /* If no auto-invoke, the user needs have the window made
                     * obvious so that they can optionally interact with it. */
                    else {
                        window.setVisible( true );
                    }
                }
            }
        } );
        return Outcome.success( "Loaded " + nrow + " rows (" + loc + ")" );
    }

    /**
     * Configurator implementation for Datalink.
     */
    public static class DatalinkConfigurator extends UrlColumnConfigurator {

        private final DatalinkPanel dlPanel_;
        private final JFrame window_;

        /**
         * Constructor.
         *
         * @param  tinfo  topcat model information
         */
        DatalinkConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, "Datalink",
                   new ColFlag[] { ColFlag.DATALINK, ColFlag.URL, } );
            dlPanel_ = new DatalinkPanel( true, true );
            String title = "TOPCAT(" + tinfo.getTopcatModel().getID() + "): "
                         + "Activation - View Datalink Table";
            window_ = new JFrame( title );
            window_.getContentPane().add( dlPanel_ );
            window_.pack();
        }

        protected Activator createActivator( ColumnData cdata ) {
            return new LocationColumnActivator( cdata, false ) {
                protected Outcome activateLocation( String loc, long lrow ) {
                    return invokeLocation( loc, dlPanel_, window_ );
                }
            };
        }

        protected String getConfigMessage( ColumnData cdata ) {
            return null;
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        public ConfigState getState() {
            return getUrlState();
        }

        public void setState( ConfigState state ) {
            setUrlState( state );
        }
    }
}
