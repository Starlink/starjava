package uk.ac.starlink.topcat;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.MessageHandler;
import org.astrogrid.samp.gui.DefaultSendActionManager;
import org.astrogrid.samp.gui.SendActionManager;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.URLUtils;

/**
 * Provides TOPCAT's SAMP functionality.
 *
 * @author   Mark Taylor
 * @since    29 Aug 2008
 */
public class TopcatSampConnector extends HubConnector {

    private final ControlWindow controlWindow_;
    private final Map idMap_;
    private final Map highlightMap_;

    /**
     * Constructor.
     *
     * @param   controlWindow  TOPCAT top-level window
     */
    public TopcatSampConnector( ControlWindow controlWindow )
            throws IOException {
        super( TopcatServer.getInstance().getProfile() );
        controlWindow_ = controlWindow;
        idMap_ = Collections.synchronizedMap( new HashMap() );
        highlightMap_ = Collections.synchronizedMap( new WeakHashMap() );
        StarTableOutput sto = controlWindow.getTableOutput();
        TopcatServer server = TopcatServer.getInstance();

        /* Declare metadata. */
        Metadata meta = new Metadata();
        meta.setName( "topcat" );
        meta.setDescriptionText( "Tool for OPerations "
                               + "on Catalogues And Tables" );
        meta.setDocumentationUrl( "http://www.starlink.ac.uk/topcat/" );
        meta.setIconUrl( "http://www.starlink.ac.uk/topcat/tc3.gif" );
        meta.put( "author.name", "Mark Taylor" );
        meta.put( "author.affiliation",
                  "Astrophysics Group, Bristol University" );
        meta.put( "author.email", "m.b.taylor@bristol.ac.uk" );
        meta.put( "topcat.version", TopcatUtils.getVersion() );
        declareMetadata( meta );

        /* Add MType-specific handlers and declare subscriptions. */
        MessageHandler[] handlers = createMessageHandlers();
        for ( int ih = 0; ih < handlers.length; ih++ ) {
            addMessageHandler( handlers[ ih ] );
        }
        declareSubscriptions( computeSubscriptions() );

        /* Set an autoconnect time and start looking out for a hub. */
        setAutoconnect( 5 );
    }

    /**
     * Returns the message handlers which implement SAMP message receipt.
     * This method is called from the constructor.
     *
     * @return  message handler array
     */
    private final MessageHandler[] createMessageHandlers() {
        return new MessageHandler[] {

            /* Load VOTable by reference. */
            new AbstractMessageHandler( "table.load.votable" ) {
                public Map processCall( HubConnection conn, String senderId,
                                        Message msg )
                        throws IOException {
                    StarTable table =
                        createTable( "votable",
                                     (String) msg.getRequiredParam( "url" ) );
                    loadTable( table, (String) msg.getParam( "table-id" ),
                               senderId );
                    return null;
                }
            },

            /* Load FITS table by reference. */
            new AbstractMessageHandler( "table.load.fits" ) {
                public Map processCall( HubConnection conn, String senderId,
                                        Message msg )
                        throws IOException {
                    StarTable table =
                        createTable( "fits",
                                     (String) msg.getRequiredParam( "url" ) );
                    loadTable( table, (String) msg.getParam( "table-id" ),
                               senderId );
                    return null;
                }
            },

            /* Highlight a single row. */
            new AbstractMessageHandler( "table.highlight.row" ) {
                public Map processCall( HubConnection conn, String senderId,
                                        Message msg )
                        throws MalformedURLException {
                    TableWithRows tr =
                        lookupTable( (String) msg.getParam( "table-id" ),
                                     (String) msg.getParam( "url" ) );
                    int irow =
                        SampUtils
                       .decodeInt( (String) msg.getRequiredParam( "row" ) );
                    highlightRow( tr, irow );
                    return null;
                }
            },

            /* Select a list of rows. */
            new AbstractMessageHandler( "table.select.rowList" ) {
                public Map processCall( HubConnection conn, String senderId,
                                        Message msg )
                        throws MalformedURLException {
                    TableWithRows tr =
                        lookupTable( (String) msg.getParam( "table-id" ),
                                     (String) msg.getParam( "url" ) );
                    List rowList = (List) msg.getRequiredParam( "row-list" );
                    int[] irows = new int[ rowList.size() ];
                    for ( int i = 0; i < irows.length; i++ ) {
                        irows[ i ] =
                            SampUtils.decodeInt( (String) rowList.get( i ) );
                    }
                    selectRows( tr, irows, senderId );
                    return null;
                }
            },
        };
    }

    /**
     * Loads a table into TOPCAT as the result of a SAMP message.
     *
     * @param   table  table to load
     * @param   key    table id for later reference, or null
     * @param   senderId  public identifier for sending client
     */
    private void loadTable( final StarTable table, final String key,
                            String senderId ) {
        String name = table.getName();
        final String title = name == null ? getClientName( senderId )
                                          : name;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                TopcatModel tcModel =
                    controlWindow_.addTable( table, title, true );
                if ( key != null && key.trim().length() > 0 ) {
                    idMap_.put( key, new TableWithRows( tcModel, null ) );
                }
            }
        } );
    }

    /**
     * Highlights a single row in a table as the result of a SAMP message.
     *
     * @param   tr   table annotated with row sequence
     * @param   irow   index of row to highlight
     */
    private void highlightRow( TableWithRows tr, int irow ) {
        final TopcatModel tcModel = tr.getTable();
        int[] rowMap = tr.getRowMap();
        long maxRow = rowMap == null ? tcModel.getDataModel().getRowCount()
                                     : rowMap.length;
        if ( irow < 0 || irow >= maxRow ) {
            throw new IllegalArgumentException( "Row index " + irow
                                              + " out of range" );
        }
        final long lrow = rowMap == null ? (long) irow
                                         : (long) rowMap[ irow ];

        /* Call a highlight on this row.  However, if it's the same
         * as the last-highlighted row for this table, do nothing.
         * The purpose of this is to avoid the possibility of
         * eternal SAMP ping-pong between two (or more)
         * applications.  It doesn't completely work though. */
        Long lastHigh = (Long) highlightMap_.get( tcModel );
        if ( lastHigh == null || lastHigh.longValue() != lrow ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    tcModel.highlightRow( lrow );
                }
            } );
        }
        highlightMap_.put( tcModel, new Long( lrow ) );
    }

    /**
     * Causes selection of a given list of row indices as the result of a
     * SAMP message.
     *
     * @param   tr   table annotated with row sequence
     * @param   irows  array of row indices to form the selection
     * @param   senderId  public identifier for sending client
     */
    private void selectRows( TableWithRows tr, int[] irows, String senderId ) {
        final TopcatModel tcModel = tr.getTable();
        int[] rowMap = tr.getRowMap();
        BitSet mask = new BitSet();
        for ( int i = 0; i < irows.length; i++ ) {
            int irow = irows[ i ];
            mask.set( rowMap == null ? irow : rowMap[ irow ] );
        }
        String subName = getClientName( senderId );
        final RowSubset rset = new BitsRowSubset( subName, mask );

        /* Currently, the behaviour is to co-opt an existing subset if one
         * of the right name exists and send a SHOW_SUBSET message.  
         * This is probably the best thing to do, but an alternative would 
         * be to generate a new subset and set it current, thus not 
         * overwriting an existing subset from the same source.  
         * Possibly provide the latter as an option?
         * See TopcatPlasticListener for implementation. */
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                tcModel.addSubset( rset );
            }
        } );
    }

    /**
     * Returns the human-readable name of a given registered client application.
     *
     * @param   clientId  public ID of registered client
     * @return   client name if available, or failing that some other string
     */
    private String getClientName( String clientId ) {
        Client client = (Client) getClientMap().get( clientId );
        if ( client != null ) {
            Metadata meta = client.getMetadata();
            if ( meta != null ) {
                String name = meta.getName();
                if ( name != null && name.trim().length() > 0 ) {
                    return name;
                }
            }
        }
        return "samp";
    }

    /**
     * Constructs a table given a format and a URL.
     *
     * @param   format  table format string (as used by StarTableFactory)
     * @param   url   table location
     */
    private StarTable createTable( String format, String url )
            throws IOException {
        File file = URLUtils.urlToFile( url );
        DataSource datsrc =
            file != null
                ? (DataSource) new FileDataSource( file )
                : (DataSource) new URLDataSource( new URL( url ) );
        return controlWindow_.getTableFactory().makeStarTable( datsrc, format );
    }

    /**
     * Try to find a TableWithRows object corresponding to a request.
     * The point about aggregating the table and the row map is that the
     * view of the table might have been different at the time communication
     * about it was established with another application.
     * One or both of <code>tableId</code> and <code>url</code> must be 
     * supplied.
     *
     * @param  tableId  table identifier from previous SAMP message
     * @param  url   location of file as URL
     * @return  table reference
     * @throws  RuntimeException  if no table can be found 
     */
    private TableWithRows lookupTable( String tableId, String url )
            throws MalformedURLException {

        /* Examine the tableId. */
        if ( tableId != null && idMap_.containsKey( tableId ) ) {
            return (TableWithRows) idMap_.get( tableId );
        }

        /* If that fails, examine the URL. */
        else {
            ListModel tablesList =
                ControlWindow.getInstance().getTablesListModel();
            URL u = new URL( url );
            for ( int i = 0; i < tablesList.getSize(); i++ ) {
                TopcatModel tcModel =
                    (TopcatModel) tablesList.getElementAt( i );
                if ( URLUtils.sameResource( u, tcModel.getDataModel()
                                              .getBaseTable().getURL() ) ) {
                    return new TableWithRows( tcModel, null );
                }
            }
            throw new RuntimeException( new StringBuffer()
                                       .append( "No table " )
                                       .append( "table-id=" )
                                       .append( tableId )
                                       .append( '/' )
                                       .append( "url=" )
                                       .append( url )
                                       .append( " is loaded" )
                                       .toString() );
        }
    }

    /**
     * Encapsulates a table plus its row order.
     */
    private static class TableWithRows {
        private final Reference tcModelRef_;
        private final int[] rowMap_;

        /**
         * Constructor.
         *
         * @param   rowMap   map of apparent to actual rows
         * @param   tcModel  table 
         */
        TableWithRows( TopcatModel tcModel, int[] rowMap ) {
            tcModelRef_ = new WeakReference( tcModel );
            rowMap_ = rowMap;
        }

        /**
         * Returns the TopcatModel for this object.
         *
         * @return  table
         */
        TopcatModel getTable() {
            TopcatModel tcModel = (TopcatModel) tcModelRef_.get();
            if ( tcModel != null ) {
                return tcModel;
            }
            else throw new RuntimeException( "Table no longer loaded" );
        }

        /**
         * Returns the row mapping for this object.
         *
         * @return   row map
         */
        int[] getRowMap() {
            return rowMap_;
        }
    }
}
