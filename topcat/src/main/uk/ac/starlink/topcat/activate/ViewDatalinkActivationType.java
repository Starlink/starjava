package uk.ac.starlink.topcat.activate;

import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.DatalinkPanel;
import uk.ac.starlink.topcat.LinkRowPanel;
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
        DatalinkInvoker invoker = new DatalinkInvoker( tinfo );
        return new UrlDatalinkConfigurator( tinfo, invoker );
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

        /* Load a links table from the given location. */
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

        /* If that worked, load the table into the datalink panel.
         * We have to do this synchronously so that (a) we can work out
         * whether auto-invoke is in effect before proceeding and
         * (b) in the case of auto-invoke we need to return the result of
         * the invocation rather than just the result of loading the table. */
        try {
            SwingUtilities
           .invokeAndWait( () -> dlPanel.setLinksDoc( linksDoc ) );
        }
        catch ( InterruptedException | InvocationTargetException e ) {
            return Outcome.failure( e );
        }
        LinkRowPanel linkPanel = dlPanel.getLinkRowPanel();
        boolean autoInvoke = linkPanel.isAutoInvoke();

        /* If there's no auto-invoke, make the window visible so the user
         * can interact with it.  In the case of auto-invoke that's not
         * generally necessary and it's annoying because it brings the window
         * to the front and grabs focus, but if it's been closed altogther
         * bring it back, since otherwise there's no way for the user to
         * recover it. */
        if ( window != null &&
             ( ! autoInvoke ||
               ( autoInvoke && ! window.isVisible() ) ) ) {
            SwingUtilities.invokeLater( () -> window.setVisible( true ) );
        }

        /* In case of auto-invoke, invoke the row and return the result
         * of doing so; otherwise just return success. */
        return autoInvoke
             ? linkPanel.invokeRow()
             : Outcome.success( "Loaded " + nrow + " rows (" + loc + ")" );
    }

    /**
     * Consumes Datalink URLs to present the referenced DataLink tables
     * in a GUI.
     */
    private static class DatalinkInvoker {

        private final DatalinkPanel dlPanel_;
        private final JFrame window_;

        /**
         * Constructor.
         *
         * @param  tinfo   topcat model information
         */
        DatalinkInvoker( TopcatModelInfo tinfo ) { 
            dlPanel_ = new DatalinkPanel( true, true );
            String title = "TOPCAT(" + tinfo.getTopcatModel().getID() + "): "
                         + "Activation - View Datalink Table";
            window_ = new JFrame( title );
            window_.getContentPane().add( dlPanel_ );
            window_.pack();
        }

        /**
         * Loads the file/URL at a given location into this invoker's GUI.
         *
         * @param  loc  location of Datalink {links}-response file
         * @return  outcome
         */
        public Outcome invokeLocation( String loc ) {
            return ViewDatalinkActivationType
                  .invokeLocation( loc, dlPanel_, window_ );
        }
    }

    /**
     * Configurator implementation in which the user supplies the full URL
     * for a Datalink table.
     */
    private static class UrlDatalinkConfigurator extends UrlColumnConfigurator {

        private final DatalinkInvoker invoker_;

        /**
         * Constructor.
         *
         * @param  tinfo  topcat model information
         * @param  invoker  consumes datalink URLs
         */
        UrlDatalinkConfigurator( TopcatModelInfo tinfo,
                                 DatalinkInvoker invoker ) {
            super( tinfo, "Datalink",
                   new ColFlag[] { ColFlag.DATALINK, ColFlag.URL, } );
            invoker_ = invoker;
        }

        protected Activator createActivator( ColumnData cdata ) {
            return new LocationColumnActivator( cdata, false ) {
                protected Outcome activateLocation( String loc, long lrow ) {
                    return invoker_.invokeLocation( loc );
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
