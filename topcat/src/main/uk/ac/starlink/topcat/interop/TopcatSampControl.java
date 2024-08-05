package uk.ac.starlink.topcat.interop;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.swing.AbstractListModel;
import javax.swing.Icon;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import org.astrogrid.samp.ErrInfo;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.Subscriptions;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.MessageHandler;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.gui.IconStore;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.TableLoadClient;
import uk.ac.starlink.table.gui.TableLoadDialog;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatLoadClient;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatSender;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.topcat.join.ConeMultiWindow;
import uk.ac.starlink.topcat.join.DalMultiWindow;
import uk.ac.starlink.topcat.join.SiaMultiWindow;
import uk.ac.starlink.topcat.join.SsaMultiWindow;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.vo.ConeSearchDialog;
import uk.ac.starlink.vo.DalLoader;
import uk.ac.starlink.vo.DalTableLoadDialog;
import uk.ac.starlink.vo.SiapTableLoadDialog;
import uk.ac.starlink.vo.SsapTableLoadDialog;
import uk.ac.starlink.vo.TapTableLoadDialog;

/**
 * Provides TOPCAT's SAMP functionality.
 *
 * @author   Mark Taylor
 * @since    29 Aug 2008
 */
public class TopcatSampControl {

    private final HubConnector hubConnector_;
    private final ControlWindow controlWindow_;
    private final Map<String,TableWithRows> idMap_;
    private final Map<TopcatModel,Long> highlightMap_;
    private final TableIdListModel idListModel_;
    private int idCount_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.interop" );

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
        idMap_ = new ConcurrentHashMap<String,TableWithRows>();
        highlightMap_ = Collections
                       .synchronizedMap( new WeakHashMap<TopcatModel,Long>() );
        idListModel_ = new TableIdListModel();

        /* Declare metadata. */
        Metadata meta = new Metadata();
        TopcatServer tcServer = TopcatServer.getInstance();
        URL tcPkgUrl = tcServer.getTopcatPackageUrl();
        String homepage = "http://www.starlink.ac.uk/topcat/";
        meta.setName( "topcat" );
        meta.setDescriptionText( "Tool for OPerations "
                               + "on Catalogues And Tables" );
        URL docUrl;
        URL logoUrl;
        try {
            docUrl = tcPkgUrl.toURI().resolve( "sun253/index.html" ).toURL();
            logoUrl = tcPkgUrl.toURI().resolve( "images/tc_sok.png" ).toURL();
        }
        catch ( URISyntaxException e ) {
            docUrl = null;
            logoUrl = null;
        }
        meta.setDocumentationUrl( tcServer.isFound( docUrl )
                                  ? docUrl.toString()
                                  : homepage );
        meta.setIconUrl( tcServer.isFound( logoUrl )
                             ? logoUrl.toString()
                             : homepage + "images/tc_sok.png" );
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
     * Returns a public reference ID indicating the current state
     * of a given TOPCAT table which will be used to send it in a
     * SAMP message.  For now, "state" refers to the combination
     * of the table and its row sequence, though other items may become 
     * important if SAMP messages arise which require consistency of
     * other attributes.
     *
     * <strong>Note:</strong> this method may update the list of tables
     * known to have been sent via SAMP, which can be used to determine
     * what tables are potentially referencable by other SAMP messages.
     * For this reason, this method should not be invoked speculatively,
     * but only if the intention is to actually send a message using
     * the returned identifier.
     *
     * @param    tcModel   table to identify
     * @return   opaque ID string
     */
    public String getTableIdForSending( TopcatModel tcModel ) {
        int[] rowMap = tcModel.getViewModel().getRowMap();

        /* If the table model and row map is the same as an existing entry
         * in the map, return the ID for that one. */
        for ( Map.Entry<String,TableWithRows> entry : idMap_.entrySet() ) {
            String id = entry.getKey();
            TableWithRows tr = entry.getValue();
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
        idListModel_.update();
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
    public Map<?,?> createSubsetMessage( TopcatModel tcModel, RowSubset rset ) {

        /* Try to identify a table we have already talked about via SAMP 
         * which relates to the supplied table. 
         * Note this can't be done perfectly: there may be more than one
         * possible entry in the stored ID map, if the same table has been
         * broadcast multiple times using different row maps.  But since we
         * are only being asked to send one message, we have to plump 
         * for one of them.  Just pick the first (i.e. a random) one. */
        TableWithRows tr = new TableWithRows( tcModel, null );
        String tableId = null;
        for ( Map.Entry<String,TableWithRows> entry : idMap_.entrySet() ) {
            String id = entry.getKey();
            TableWithRows twr = entry.getValue();
            if ( twr.getTable() == tcModel ) {
                tableId = id;
                tr = twr;
                break;
            }
        }
        int[] rowMap = tr.getRowMap();

        /* Try to get a URL for it, if it does not represent a table with
         * permuted rows (if it did, messages about row indexes would
         * use the wrong values). */
        String url = null;
        if ( rowMap == null ) {
            URL uurl = tcModel.getDataModel().getBaseTable().getURL();
            if ( uurl != null ) {
                url = uurl.toString();
            }
        }

        /* Assemble a list of row indices in SAMP-friendly format which 
         * represents the subset we have been asked to represent,
         * but in terms of the row numbers of the publicly identified table. */
        List<String> rowList = new ArrayList<String>();
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
     * Returns a ListModel listing the TopcatModels that can reasonably
     * be used in SAMP messages that reference a table using the
     * <code>table-id</code>/<code>url</code> message parameter
     * (<code>table.highlight.row</code>, <code>table.select.rowList</code>).
     *
     * TopcatModels may be added to this list when they have been involved
     * in a relevant SAMP message (usually <code>table.load.*</code>).
     * Code can register a listener on this list to be notified when
     * the identifiability status of tables change.
     *
     * @return  listmodel of identifiable tables 
     */
    public ListModel<TopcatModel> getIdentifiableTableListModel() {
        return idListModel_;
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
    public Message createRowMessage( TopcatModel tcModel, long lrow ) {

        /* Get a table id. */
        TableWithRows tr = new TableWithRows( tcModel, null );
        String tableId = null;
        for ( Map.Entry<String,TableWithRows> entry : idMap_.entrySet() ) {
            String id = entry.getKey();
            TableWithRows twr = entry.getValue();
            if ( twr.getTable() == tcModel ) {
                tableId = id;
                tr = twr;
                break;
            }
        }
        int[] rowMap = tr.getRowMap();

        /* Get a URL. */
        String url = null;
        if ( rowMap == null ) {
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
    final MessageHandler[] createMessageHandlers() {
        return new MessageHandler[] {

            /* Load VOTable by reference. */
            new TableLoadHandler( "table.load.votable", "votable" ),

            /* Load FITS table by reference. */
            new TableLoadHandler( "table.load.fits", "fits" ),

            /* Load CDF table by reference. */
            new TableLoadHandler( "table.load.cdf", "cdf" ),

            /* Load PDS4 table by reference. */
            new TableLoadHandler( "table.load.pds4", "pds4" ),

            /* Load table with supplied format by reference. */
            new TableLoadHandler( TopcatSender.TOPCAT_LOAD_MTYPE, null ),

            /* Highlight a single row. */
            new AbstractMessageHandler( "table.highlight.row" ) {
                public Response processCall( HubConnection conn,
                                             String senderId, Message msg )
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
                public Response processCall( HubConnection conn,
                                             String senderId, Message msg )
                        throws MalformedURLException {
                    TableWithRows tr =
                        lookupTable( (String) msg.getParam( "table-id" ),
                                     (String) msg.getParam( "url" ) );
                    List<?> rowList =
                        (List<?>) msg.getRequiredParam( "row-list" );
                    int[] irows = new int[ rowList.size() ];
                    for ( int i = 0; i < irows.length; i++ ) {
                        irows[ i ] =
                            SampUtils.decodeInt( (String) rowList.get( i ) );
                    }
                    selectRows( tr, irows, senderId );
                    return null;
                }
            },

            /* Accept sky position. */
            new ResponseMessageHandler( "coord.pointAt.sky" ) {
                public Response processCall( HubConnection conn,
                                             String senderId, Message msg ) {
                    double ra =
                        SampUtils
                       .decodeFloat( (String) msg.getRequiredParam( "ra" ) );
                    double dec =
                        SampUtils
                       .decodeFloat( (String) msg.getRequiredParam( "dec" ) );
                    boolean accepted =
                        controlWindow_.acceptSkyPosition( ra, dec );
                    return createAcceptanceResponse( null, accepted,
                                                     "coordinate pair" );
                }
            },

            /* Accept list of VOResources. */
            new ResourceListHandler( "voresource.loadlist",
                                     DalLoader.class,
                                     DalMultiWindow.class ),
            new ResourceListHandler( "voresource.loadlist.cone",
                                     ConeSearchDialog.class,
                                     ConeMultiWindow.class ),
            new ResourceListHandler( "voresource.loadlist.siap",
                                     SiapTableLoadDialog.class,
                                     SiaMultiWindow.class ),
            new ResourceListHandler( "voresource.loadlist.ssap",
                                     SsapTableLoadDialog.class,
                                     SsaMultiWindow.class ),
            new ResourceListHandler( "voresource.loadlist.tap",
                                     TapTableLoadDialog.class, null ),

            /* Supply URL for a currently loaded table. */
            TablePullHandler.createGenericTablePullHandler( "table.get.stil" ),
        };
    }

    /**
     * Highlights a single row in a table as the result of a SAMP message.
     *
     * @param   tr   table annotated with row sequence
     * @param   irow   index of row to highlight
     */
    private void highlightRow( TableWithRows tr, int irow ) {
        final TopcatModel tcModel = tr.getLoadedTable();
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
        Long lastHigh = highlightMap_.get( tcModel );
        if ( lastHigh == null || lastHigh.longValue() != lrow ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    tcModel.highlightRow( lrow, false );
                }
            } );
        }
        highlightMap_.put( tcModel, Long.valueOf( lrow ) );
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
        final TopcatModel tcModel = tr.getLoadedTable();
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

        /* Check we have some identification. */
        if ( ( tableId == null || tableId.trim().length() == 0 ) &&
             ( url == null || url.trim().length() == 0 ) ) {
            throw new RuntimeException( "No table-id/url parameter supplied" );
        }

        /* Examine the tableId. */
        if ( tableId != null && idMap_.containsKey( tableId ) ) {
            return idMap_.get( tableId );
        }

        /* If that fails, examine the URL. */
        else if ( url != null ) {
            ListModel<TopcatModel> tablesList =
                ControlWindow.getInstance().getTablesListModel();
            URL u = URLUtils.newURL( url );
            for ( int i = 0; i < tablesList.getSize(); i++ ) {
                TopcatModel tcModel = tablesList.getElementAt( i );
                if ( URLUtils.sameResource( u, tcModel.getDataModel()
                                              .getBaseTable().getURL() ) ) {
                    return new TableWithRows( tcModel, null );
                }
            }
        }

        /* No luck. */
        throw new RuntimeException( new StringBuffer()
                                   .append( "No table with " )
                                   .append( "table-id=" )
                                   .append( tableId )
                                   .append( " or " )
                                   .append( "url=" )
                                   .append( url )
                                   .append( " is loaded" )
                                   .toString() );
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
     * MessageHandler implementation for loading a table into TOPCAT.
     */
    private class TableLoadHandler implements MessageHandler {

        private final String mtype_;
        private final String format_;
        private final Subscriptions subs_;
        private final IconStore iconStore_;

        /**
         * Constructor.
         *
         * @param   mtype  table load MType string
         * @param   format  STIL name for format specific table input handler,
         *                  or null if format is supplied in message
         */
        TableLoadHandler( String mtype, String format ) {
            mtype_ = mtype;
            format_ = format;
            subs_ = new Subscriptions();
            subs_.addMType( mtype );
            iconStore_ = new IconStore( ResourceIcon.SAMP );
        }

        public Subscriptions getSubscriptions() {
            return subs_;
        }

        public void receiveNotification( HubConnection connection,
                                         String senderId, Message message )
                throws IOException {
            loadTable( connection, senderId, message, null );
        }

        public void receiveCall( HubConnection connection, String senderId,
                                 String msgId, Message message )
                throws IOException {
            loadTable( connection, senderId, message, msgId );
        }

        /**
         * Does the work of loading a table for this message handler.
         *
         * @param   connection  hub connection
         * @param   senderId  sender ID
         * @param   message   message
         * @param   msgId  msgID for a Call, or null for a Notification
         */
        private void loadTable( HubConnection connection, String senderId,
                                Message message, String msgId )
                throws IOException {

            /* Check we do not already have a table with the given table-id. */
            String tableId = (String) message.getParam( "table-id" );
            if ( tableId != null && idMap_.containsKey( tableId ) ) {
                String errTxt = new StringBuffer()
                    .append( "Duplicate table-id: " )
                    .append( "table '" )
                    .append( tableId )
                    .append( "' has already been received" )
                    .toString();
                throw new IllegalArgumentException( errTxt );
            }

            /* Get sender information. */
            final String senderName = getClientName( senderId );
            Client sender =
                (Client) hubConnector_.getClientMap().get( senderId );
            Icon senderIcon = sender == null
                ? ResourceIcon.SAMP
                : IconStore.sizeIcon( iconStore_.getIcon( sender ), 24 );

            /* Prepare a loader which can load the table. */
            String url = (String) message.getRequiredParam( "url" );
            final String format = format_ != null
                                ? format_
                                : (String) message.getParam( "format" );
            File file = URLUtils.urlToFile( url );
            final DataSource datsrc =
                file != null ? new FileDataSource( file )
                             : new URLDataSource( URLUtils.newURL( url ) );
            String tableName = (String) message.getParam( "name" );
            String label = "SAMP" + ":" + senderName;
            if ( tableName != null ) {
                label += ":" + tableName;
            }
            final String label1 = label;
            final String tname1 = tableName;
            TableLoader loader = new TableLoader() {
                public String getLabel() {
                    return label1;
                }
                public TableSequence loadTables( StarTableFactory tfact )
                        throws IOException {
                    StarTable table = tfact.makeStarTable( datsrc, format );
                    String srcName = senderName;
                    if ( tname1 != null ) {
                        table.setName( tname1 );
                    }
                    String tname = table.getName();
                    if ( tname != null && tname.trim().length() > 0 ) {
                        srcName += ":" + tname;
                    }
                    table.setParameter( new DescribedValue( SOURCE_INFO,
                                                            srcName ) );
                    return Tables.singleTableSequence( table );
                }
            };

            /* Prepare a load client which can consume the table. */
            TableLoadClient loadClient =
                new SampLoadClient( connection, senderName, message, msgId );

            /* Pass control to the standard load method. */
            controlWindow_.runLoading( loader, loadClient, senderIcon );
        }
    }

    /**
     * TableLoadClient used with the table load message handler.
     * As well as inserting received tables into the application,
     * it makes asynchronous responses to the hub if in Call mode.
     */
    private class SampLoadClient extends TopcatLoadClient {

        private final HubConnection connection_;
        private final String senderName_;
        private final Message message_;
        private final String msgId_;
        private boolean responded_;

        /** 
         * Constructor.
         *
         * @param   connection  hub connection
         * @param   senderName  client name of sender
         * @param   message   message
         * @param   msgId  msgID for a Call, or null for a Notification
         */
        SampLoadClient( HubConnection connection, String senderName,
                        Message message, String msgId ) {
            super( controlWindow_, controlWindow_, false );
            connection_ = connection;
            senderName_ = senderName;
            message_ = message;
            msgId_ = msgId;
        }

        public boolean loadSuccess( StarTable table ) {
            respond( Response.createSuccessResponse( Collections.EMPTY_MAP ) );
            TopcatModel tcModel = super.addTable( table );
            String tableId = (String) message_.getParam( "table-id" );
            if ( tableId != null && tableId.trim().length() > 0 ) {
                idMap_.put( tableId, new TableWithRows( tcModel, null ) );
                idListModel_.update();
            }
            return false;
        }

        public boolean loadFailure( Throwable error ) {
            respond( Response.createErrorResponse( new ErrInfo( error ) ) );
            return false;
        }

        public void endSequence( boolean cancelled ) {
            super.endSequence( cancelled );
            if ( cancelled ) {
                respond( new Response( Response.ERROR_STATUS,
                                       Collections.EMPTY_MAP,
                                       new ErrInfo( "User cancelled load" ) ) );
            }
            else {
                if ( ! responded_ ) {
                    logger_.warning( "Neither success nor failure?" );
                    respond( new Response( Response.ERROR_STATUS,
                                           Collections.EMPTY_MAP,
                                           new ErrInfo( "No table found" ) ) );
                }
            }
        }

        /**
         * Passes a given SAMP response back to the hub if required.
         * This should be called exactly once; but invocations after the
         * first one are checked for, and just generate a warning.
         */
        private void respond( final Response response ) {

            /* Check we have not already responded. */
            if ( responded_ ) {
                logger_.warning( "Multiple responses attempted" );
                return;
            }
            responded_ = true;

            /* If this was a Call (not a Notification), then pass the 
             * response back to the hub. */
            if ( msgId_ != null ) {
                new Thread() {
                    public void run() {
                        try {
                            connection_.reply( msgId_, response );
                        }
                        catch ( SampException e ) {
                            logger_.warning( "SAMP response failed: " + e );
                        }
                    }
                }.start();
            }
        }
    }

    /**
     * Message handler for receipt of voresource.loadlist type MTypes.
     */
    class ResourceListHandler extends ResponseMessageHandler {

        final Class<? extends DalLoader> dalLoaderClass_;
        final Class<? extends DalMultiWindow> dalMultiWindowClass_;

        /**
         * Constructor.
         *
         * @param  mtype  mtype this responds to
         * @param  dalLoaderClass   DalLoader subclass for
         *         dialogues specific to this mtype
         * @param  dalMultiWindowClass  DalMultiWindow subclass for
         *         dialogues specific to this mtype
         */
        ResourceListHandler(
                String mtype,
                Class<? extends DalLoader> dalLoaderClass,
                Class<? extends DalMultiWindow> dalMultiWindowClass ) {
            super( mtype );
            dalLoaderClass_ = dalLoaderClass;
            dalMultiWindowClass_ = dalMultiWindowClass;
        }

        public Response processCall( HubConnection conn, String senderId,
                                     Message msg ) {
            @SuppressWarnings("unchecked") // could fail with ClassCastException
            Map<String,?> idMap = (Map<String,?>) msg.getRequiredParam( "ids" );
            String[] ids = idMap.keySet().toArray( new String[ 0 ] );
            if ( ids.length > 0 ) {
                StringBuffer mbuf = new StringBuffer();
                mbuf.append( "Loading resource set" );
                String name = (String) msg.getParam( "name" );
                if ( name != null && name.trim().length() > 0 ) {
                    mbuf.append( ' ' ).append( name );
                }
                mbuf.append( " sent by " )
                    .append( getClientName( senderId ) );
                boolean accepted =
                    controlWindow_
                   .acceptResourceIdList( ids, mbuf.toString(),
                                          dalLoaderClass_,
                                          dalMultiWindowClass_ );
                return createAcceptanceResponse( null, accepted,
                                                 "resource list" );
            }
            else {
                throw new RuntimeException( "No resources supplied" );
            }
        }
    }

    /**
     * Slightly adjusted AbstractMessageHandler class used for some of the
     * message handlers here.  Semantics of processCall are different from
     * AbstractMessageHandler - use with care.  JSAMP versions after 1.1
     * wouldn't need this hacked in quite this way (but it would still work).
     */
    private static abstract class ResponseMessageHandler
            extends AbstractMessageHandler {

        /**
         * Constructor.
         *
         * @param  mtype  sole mtype
         */
        ResponseMessageHandler( String mtype ) {
            super( mtype );
        }

        /**
         * Does the message processing.
         *
         * @param connection  hub connection
         * @param senderId  public ID of sender client
         * @param message  message with MType this handler is subscribed to
         * @return   message response
         */
        public abstract Response processCall( HubConnection connection,
                                              String senderId,
                                              Message message );


        /**
         * Overridden to use different semantics of processCall.
         */
        public void receiveCall( HubConnection connection, String senderId,
                                 String msgId, Message message )
                throws SampException {
            Response response;
            try {
                response = processCall( connection, senderId, message );
            }
            catch ( Throwable e ) {
                response = Response.createErrorResponse( new ErrInfo( e ) );
            }
            connection.reply( msgId, response );
        }

        /**
         * Creates a response based on an acceptance flag.
         *
         * @param  result  result of call
         * @param  accepted  whether the call achieved anything
         * @param  dataType  type of data transferred by the call
         *                   (used in error message)
         */
        Response createAcceptanceResponse( Map<?,?> result, boolean accepted,
                                           String dataType ) {
            if ( result == null ) {
                result = Response.EMPTY;
            }           
            if ( accepted ) {
                return Response.createSuccessResponse( result );
            }           
            else {
                String appName = TopcatUtils.getApplicationName();
                ErrInfo errInfo =
                    new ErrInfo( dataType + " not used by " + appName );
                errInfo.setUsertxt( new StringBuffer()
                    .append( "The " + dataType
                                    + " was received successfully,\n" )
                    .append( "but " + appName 
                                    + " was not in a suitable state " )
                    .append( "to make use of it\n" )
                    .append( "(perhaps suitable windows were not open).\n" )
                    .toString() );
                return new Response( Response.WARNING_STATUS, result, errInfo );
            }
        }
    }

    /**
     * Encapsulates a table plus its row order.
     */
    private static class TableWithRows {
        private final Reference<TopcatModel> tcModelRef_;
        private final String tcId_;
        private int[] rowMap_;

        /**
         * Constructor.
         *
         * @param   rowMap   map of apparent to actual rows
         * @param   tcModel  table 
         */
        TableWithRows( TopcatModel tcModel, int[] rowMap ) {
            assert tcModel != null;
            tcId_ = tcModel.toString();
            tcModelRef_ = new WeakReference<TopcatModel>( tcModel );
            rowMap_ = rowMap;
        }

        /**
         * Returns the TopcatModel for this object; may be null if the
         * table is no longer loaded.
         *
         * @return  table, or null
         */
        TopcatModel getTable() {
            TopcatModel tcModel = tcModelRef_.get();

            /* If the table has become unloaded take the opportunity
             * to clear out the row map array too, since it won't be needed.
             * This could be done in a more systematic way (using a reference
             * queue) but as currently implemented TopcatModel reference
             * chains are not tightly coded enough to make it worth while
             * (references left where they don't need to be, mostly Swing). */
            if ( tcModel == null ) {
                rowMap_ = null;
            }
            return tcModel;
        }

        /**
         * Returns the loaded TopcatModel for this object.
         * If the table is no longer loaded an exception will be thrown.
         *
         * @return   non-null table
         */
        TopcatModel getLoadedTable() {
            TopcatModel tcModel = getTable();
            if ( tcModel != null ) {
                return tcModel;
            }
            else {
                throw new IllegalStateException( "Table " + tcId_
                                               + " no longer loaded" );
            }
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

    /**
     * ListModel listing tables that can be identified for use in a SAMP
     * table message.  That means they have a table-id or URL.
     * Listeners to this list will be notified when the contents
     * may have changed.
     */
    private class TableIdListModel extends AbstractListModel<TopcatModel> {
        volatile TopcatModel[] list_;

        TableIdListModel() {
            list_ = createList();
        }

        /**
         * Must be called when the contents of this list may have changed.
         */
        void update() {
            list_ = createList();
            final int n = list_.length;
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    fireContentsChanged( this, 0, n - 1 );
                }
            } );
        }
        public int getSize() {
            return list_.length;
        }
        public TopcatModel getElementAt( int i ) {
            return list_[ i ];
        }

        /**
         * Assembles list contents from current state.
         *
         * @return  list contents
         */
        private TopcatModel[] createList() {
            Set<TopcatModel> set = new LinkedHashSet<TopcatModel>();
            for ( TableWithRows twr : idMap_.values() ) {
                set.add( twr.getTable() );
            }
            ListModel<TopcatModel> tlist = controlWindow_.getTablesListModel();
            for ( int i = 0; i < tlist.getSize(); i++ ) {
                TopcatModel tcm = tlist.getElementAt( i );
                if ( ! set.contains( tcm ) &&
                     tcm.getViewModel().getRowMap() == null &&
                     tcm.getDataModel().getBaseTable().getURL() != null ) {
                    set.add( tcm );
                }
            }
            return set.toArray( new TopcatModel[ 0 ] );
        }
    }
}
