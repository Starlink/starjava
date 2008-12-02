package uk.ac.starlink.topcat.interop;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
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
public class TopcatSampControl {

    private final HubConnector hubConnector_;
    private final ControlWindow controlWindow_;
    private final Map idMap_;
    private final Map highlightMap_;
    private int idCount_;

    /**
     * Constructor.
     *
     * @param   controlWindow  TOPCAT top-level window
     */
    public TopcatSampControl( HubConnector hubConnector,
                              ControlWindow controlWindow )
            throws IOException {
        hubConnector_ = hubConnector;
        controlWindow_ = controlWindow;
        idMap_ = Collections.synchronizedMap( new HashMap() );
        highlightMap_ = Collections.synchronizedMap( new WeakHashMap() );

        /* Declare metadata. */
        Metadata meta = new Metadata();
        TopcatServer tcServer = TopcatServer.getInstance();
        URL tcPkgUrl = tcServer.getTopcatPackageUrl();
        String homepage = "http://www.starlink.ac.uk/topcat/";
        meta.setName( "topcat" );
        meta.setDescriptionText( "Tool for OPerations "
                               + "on Catalogues And Tables" );
        URL docUrl = new URL( tcPkgUrl, "sun253/index.html" );
        meta.setDocumentationUrl( tcServer.isFound( docUrl )
                                  ? docUrl.toString()
                                  : homepage );
        URL logoUrl = new URL( tcPkgUrl, "images/tc3.gif" );
        meta.setIconUrl( tcServer.isFound( logoUrl )
                             ? logoUrl.toString()
                             : homepage + "images/tc3.gif" );
        meta.put( "home.page", homepage );
        meta.put( "author.name", "Mark Taylor" );
        meta.put( "author.affiliation",
                  "Astrophysics Group, Bristol University" );
        meta.put( "author.email", "m.b.taylor@bristol.ac.uk" );
        meta.put( "topcat.version", TopcatUtils.getVersion() );
        hubConnector_.declareMetadata( meta );

        /* Add MType-specific handlers and declare subscriptions. */
        MessageHandler[] handlers = createMessageHandlers();
        for ( int ih = 0; ih < handlers.length; ih++ ) {
            hubConnector_.addMessageHandler( handlers[ ih ] );
        }
        hubConnector_.declareSubscriptions( hubConnector_
                                           .computeSubscriptions() );
    }

    /**
     * Returns the control window which owns this connector.
     *
     * @return  control window
     */
    public ControlWindow getControlWindow() {
        return controlWindow_;
    }

    /**
     * Returns a public reference ID which may be used to the current state
     * of a given TOPCAT table.  For now, "state" refers to the combination
     * of the table and its row sequence, though other items may become 
     * important if SAMP messages arise which require consistency of
     * other attributes.
     *
     * @param    tcModel   table to identify
     * @return   opaque ID string
     */
    public String getTableId( TopcatModel tcModel ) {
        int[] rowMap = tcModel.getViewModel().getRowMap();

        /* If the table model and row map is the same as an existing entry
         * in the map, return the ID for that one. */
        for ( Iterator it = idMap_.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String id = (String) entry.getKey();
            TableWithRows tr = (TableWithRows) entry.getValue();
            if ( tr.getTable() == tcModel &&
                 Arrays.equals( tr.rowMap_, rowMap ) ) {
                return id;
            }
        }

        /* It's not, so we will need to make a new entry in the map as well
         * as returning an ID. */
        /* If the rowMap is null and the table has a URL, use that as the ID. */
        URL url = tcModel.getDataModel().getBaseTable().getURL();
        String id;
        if ( rowMap == null && url != null ) {
            id = url.toString();
        }
        else {
            id = createId();
        }
        idMap_.put( id, new TableWithRows( tcModel, rowMap ) );
        return id;
    }

    /**
     * Creates a message suitable for sending a row list selection SAMP
     * message to other clients.  It is sensibly done here because this
     * class keeps track of which tables have been labelled with which
     * IDs in communications with other SAMP clients.
     *
     * @param  tcModel  table
     * @param  rset    row subset of tcModel to send
     * @return   table.select.rowList message
     */
    public Map createSubsetMessage( TopcatModel tcModel, RowSubset rset ) {

        /* Try to identify a table we have already talked about via SAMP 
         * which relates to the supplied table. 
         * Note this can't be done perfectly: there may be more than one
         * possible entry in the stored ID map, if the same table has been
         * broadcast multiple times using different row maps.  But since we
         * are only being asked to send one message, we have to plump 
         * for one of them.  Just pick the first (i.e. a random) one. */
        TableWithRows tr = new TableWithRows( tcModel, null );
        String tableId = null;
        for ( Iterator it = idMap_.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String id = (String) entry.getKey();
            TableWithRows twr = (TableWithRows) entry.getValue();
            if ( twr.getTable() == tcModel ) {
                tableId = id;
                tr = twr;
                break;
            }
        }

        /* Try to get a URL for it, if it does not represent a table with
         * permuted rows (if it did, messages about row indexes would
         * use the wrong values). */
        String url = null;
        if ( tr.getRowMap() == null ) {
            URL uurl = tcModel.getDataModel().getBaseTable().getURL();
            if ( uurl != null ) {
                url = uurl.toString();
            }
        }

        /* Assemble a list of row indices in SAMP-friendly format which 
         * represents the subset we have been asked to represent,
         * but in terms of the row numbers of the publicly identified table. */
        int[] rowMap = tr.getRowMap();
        List rowList = new ArrayList();
        if ( rowMap == null ) {
            int nrow = (int) tcModel.getDataModel().getRowCount();
            for ( int ir = 0; ir < nrow; ir++ ) {
                if ( rset.isIncluded( ir ) ) {
                    rowList.add( SampUtils.encodeInt( ir ) );
                }
            }
        }
        else {
            int nrow = rowMap.length;
            for ( int ir = 0; ir < nrow; ir++ ) {
                if ( rset.isIncluded( rowMap[ ir ] ) ) {
                    rowList.add( SampUtils.encodeInt( ir ) );
                }
            }
        }

        /* Assemble and return the SAMP message. */
        Message msg = new Message( "table.select.rowList" );
        if ( tableId != null ) {
            msg.addParam( "table-id", tableId );
        }
        if ( url != null ) {
            msg.addParam( "url", url );
        }
        if ( url == null && tableId == null ) {
            msg.addParam( "table-id", createId() );  // pretty pointless
        }
        msg.addParam( "row-list", rowList );
        msg.check();
        return msg;
    }

    /**
     * Creates a message suitable for sending a row highlight SAMP message
     * to other clients.
     *
     * @param   tcModel  table
     * @param   lrow  index of row in tcModel to highlight
     * @return   table.highlight.row message, or null if no suitable message
     *           can be constructed
     */
    public Map createRowMessage( TopcatModel tcModel, long lrow ) {

        /* Get a table id. */
        TableWithRows tr = new TableWithRows( tcModel, null );
        String tableId = null;
        for ( Iterator it = idMap_.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String id = (String) entry.getKey();
            TableWithRows twr = (TableWithRows) entry.getValue();
            if ( twr.getTable() == tcModel ) {
                tableId = id;
                tr = twr;
                break;
            }
        }

        /* Get a URL. */
        String url = null;
        if ( tr.getRowMap() == null ) {
            URL uurl = tcModel.getDataModel().getBaseTable().getURL();
            if ( uurl != null ) {
                url = uurl.toString();
            }
        }

        /* If neither URL nor table-id can be found, return null since the
         * message would be useless. */
        if ( tableId == null && url == null ) {
            return null;
        }

        /* Get the (possibly transformed) row index. */
        int[] rowMap = tr.getRowMap();
        long row;
        if ( rowMap == null ) {
            row = lrow;
        }
        else {
            long r = -1;
            int nrow = rowMap.length;
            for ( int ir = 0; ir < nrow && r < 0; ir++ ) {
                if ( rowMap[ ir ] == lrow ) {
                    r = ir;
                }
            }
            row = r;
        }

        /* If the row isn't present in the referenced copy of the table,
         * return null, since the message would be useless. */
        if ( row < 0 ) {
            return null;
        }

        /* Assemble and return the SAMP message. */
        Message msg = new Message( "table.highlight.row" );
        if ( tableId != null ) {
            msg.addParam( "table-id", tableId );
        }
        if ( url != null ) {
            msg.addParam( "url", url );
        }
        msg.addParam( "row", SampUtils.encodeLong( row ) );
        msg.check();
        return msg;
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
        Client client = (Client) hubConnector_.getClientMap().get( clientId );
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
     * Returns a unique ID each time it is called.
     *
     * @return   opaque ID string
     */
    private synchronized String createId() {
        return "topcat"
             + Integer.toString( System.identityHashCode( this ) & 0xffff, 16 )
             + "-"
             + ++idCount_;
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
