package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Panel for display of a TAP query for a given TAP service.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2011
 */
public class TapQueryPanel extends JPanel {

    private final JEditorPane adqlPanel_;
    private final TableSetPanel tmetaPanel_;
    private final JLabel serviceLabel_;
    private final JLabel countLabel_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public TapQueryPanel() {
        super( new BorderLayout() );

        /* Prepare a panel to contain user-entered ADQL text. */
        adqlPanel_ = new JEditorPane();
        adqlPanel_.setEditable( true );
        JComponent adqlScroller = new JScrollPane( adqlPanel_ );
        adqlScroller.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "ADQL Text" ) );

        /* Prepare a panel for the TAP service heading. */
        serviceLabel_ = new JLabel();
        countLabel_ = new JLabel();
        JComponent tableHeading = Box.createHorizontalBox();
        tableHeading.add( new JLabel( "Service: " ) );
        tableHeading.add( serviceLabel_ );
        tableHeading.add( Box.createHorizontalStrut( 10 ) );
        tableHeading.add( countLabel_ );

        /* Prepare a panel for table metadata display. */
        tmetaPanel_ = new TableSetPanel();

        /* Arrange the components in a split pane. */
        final JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        JComponent servicePanel = new JPanel( new BorderLayout() );
        servicePanel.add( tableHeading, BorderLayout.NORTH );
        servicePanel.add( tmetaPanel_, BorderLayout.CENTER );
        servicePanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Table Metadata" ) );
        splitter.setTopComponent( servicePanel );
        splitter.setBottomComponent( adqlScroller );
        splitter.setResizeWeight( 0.5 );
        add( splitter, BorderLayout.CENTER );
    }

    /**
     * Returns the text panel used for the ADQL text entered by the user.
     *
     * @return   ADQL text entry component
     */
    public JEditorPane getAdqlPanel() {
        return adqlPanel_;
    }

    /**
     * Returns the text currently entered in the ADQL text component.
     *
     * @return  adql text supplied by user
     */
    public String getAdql() {
        return adqlPanel_.getText();
    }

    /**
     * Sets a short text string describing the TAP service used by this panel.
     *
     * @param  serviceHeading  short, human-readable label for the
     *         service this panel relates to
     */
    public void setServiceHeading( String serviceHeading ) {
        serviceLabel_.setText( serviceHeading );
    }

    /**
     * Sets the service URL for the TAP service used by this panel.
     * Calling this will initiate an asynchronous attempt to fill in
     * service metadata from the service at the given URL.
     *
     * @param  serviceUrl  base URL for a TAP service
     */
    public void setServiceUrl( final String serviceUrl ) {

        /* Prepare the URL where we can find the TableSet document. */
        URL url;
        try {
            url = new URL( serviceUrl );
        }
        catch ( MalformedURLException e ) {
            return;
        }
        final URL turl;
        try {
            turl = new URL( serviceUrl + "/tables" );
        }
        catch ( MalformedURLException e ) {
            throw new AssertionError( e );
        }

        /* Dispatch a request to acquire the table metadata from
         * the service. */
        setTables( null );
        tmetaPanel_.showFetchProgressBar( "Fetching Table Metadata" );
        new Thread( "Table metadata fetcher" ) {
            public void run() {
                final TableMeta[] tableMetas;
                try {
                    logger_.info( "Reading table metadata from " + turl );
                    tableMetas = TableSetSaxHandler.readTableSet( turl );
                }
                catch ( final Exception e ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            tmetaPanel_.showFetchFailure( turl, e );
                        }
                    } );
                    return;
                }

                /* On success, install this information in the GUI. */
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        setTables( tableMetas );
                    }
                } );
            }
        }.start();
    }

    /**
     * Sets the metadata panel to display a given set of table metadata.
     *
     * @param  tmetas  table metadata list; null if no metadata is available
     */
    private void setTables( TableMeta[] tmetas ) {
        tmetaPanel_.setTables( tmetas );
        String countText;
        if ( tmetas == null ) {
            countText = "";
        }
        else if ( tmetas.length == 1 ) {
            countText = "(1 table)";
        }
        else {
            countText = "(" + tmetas.length + " tables)";
        }
        countLabel_.setText( countText );
    }
}
