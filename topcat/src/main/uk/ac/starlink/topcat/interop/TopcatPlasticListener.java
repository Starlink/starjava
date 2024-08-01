package uk.ac.starlink.topcat.interop;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.ApplicationItem;
import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.plastic.MessageId;
import uk.ac.starlink.plastic.NoHubException;
import uk.ac.starlink.plastic.PlasticTransmitter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.LoadingToken;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.SubsetWindow;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Implements the PlasticListener interface on behalf of the TOPCAT application.
 * Will attempt to unregister with the hub on finalization or JVM shutdown.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2006
 * @see      <a href="http://plastic.sourceforge.net/">PLASTIC</a>
 */
public class TopcatPlasticListener extends HubManager {

    private final ControlWindow controlWindow_;
    private final Map<String,TableWithRows> idMap_;
    private final Map<TopcatModel,Long> highlightMap_;
    private final ListModel<ApplicationItem> appList_;

    private static final URI[] SUPPORTED_MESSAGES = new URI[] {
        MessageId.VOT_LOAD,
        MessageId.VOT_LOADURL,
        MessageId.VOT_SHOWOBJECTS,
        MessageId.VOT_HIGHLIGHTOBJECT,
        MessageId.INFO_GETDESCRIPTION,
        MessageId.INFO_GETICONURL,
    };
 
    /**
     * Constructs a new listener which will react appropriately to 
     * messages from the hub.
     *
     * @param   controlWindow   control window into which accepted tables
     *          will be loaded etc
     */
    @SuppressWarnings("this-escape")
    public TopcatPlasticListener( ControlWindow controlWindow ) {
        super( "topcat", SUPPORTED_MESSAGES );
        controlWindow_ = controlWindow;
        idMap_ = Collections
                .synchronizedMap( new HashMap<String,TableWithRows>() );
        highlightMap_ = new HashMap<TopcatModel,Long>();
        @SuppressWarnings("unchecked")
        ListModel<ApplicationItem> typedAppList =
            (ListModel<ApplicationItem>) getApplicationListModel();
        appList_ = typedAppList;
    }

    /**
     * Does the work for processing a hub message.
     *
     * @param  sender   sender ID
     * @param  message  message ID (determines the action required)
     * @param  args     message argument list
     * @return  return value requested by message
     */
    @SuppressWarnings("rawtypes")
    public Object doPerform( URI sender, URI message, List args )
            throws IOException {

        /* Load VOTable passed as text in an argument. */
        if ( MessageId.VOT_LOAD.equals( message ) &&
             checkArgs( args, new Class<?>[] { String.class } ) ) {
            String text = (String) args.get( 0 );
            String id = args.size() > 1 ? String.valueOf( args.get( 1 ) )
                                        : null;
            votableLoad( sender, text, id );
	        return Boolean.TRUE;
        }

        /* Load VOTable by URL. */
        else if ( MessageId.VOT_LOADURL.equals( message ) &&
                  checkArgs( args, new Class<?>[] { Object.class } ) ) {
            String url = args.get( 0 ) instanceof String
                       ? (String) args.get( 0 )
                       : args.get( 0 ).toString();
            String id = url;
            if ( args.size() > 1 ) {
                id = args.get( 1 ) instanceof String
                   ? (String) args.get( 1 )
                   : args.get( 0 ).toString();
            }
            votableLoadFromURL( sender, url, id );
            return Boolean.TRUE;
        }

        /* Select VOTable rows. */
        else if ( MessageId.VOT_SHOWOBJECTS.equals( message ) &&
                  args.size() >= 2 &&
                  args.get( 1 ) instanceof List ) {
            String tableId = args.get( 0 ).toString();
            List<?> objList = (List<?>) args.get( 1 );
            return Boolean.valueOf( showObjects( sender, tableId, objList ) );
        }

        /* Highlight a single row. */
        else if ( MessageId.VOT_HIGHLIGHTOBJECT.equals( message ) &&
                  args.size() >= 2 &&
                  args.get( 1 ) instanceof Number ) {
            String tableId = args.get( 0 ).toString();
            int irow = ((Number) args.get( 1 )).intValue();
            return Boolean.valueOf( highlightObject( sender, tableId, irow ) );
        }

        /* Get TOPCAT icon. */
        else if ( MessageId.INFO_GETICONURL.equals( message ) ) {
            return "http://www.starlink.ac.uk/topcat/images/tc_sok.png";
        }

        else if ( MessageId.INFO_GETDESCRIPTION.equals( message ) ) {
            return "TOol for Processing Catalogues And Tables";
        }

        /* Unknown message. */
        else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns a ComboBoxModel which selects applications registered with
     * this hub manager; only those which support a given message are
     * included.  This listener is excluded.
     *
     * @param   messageId  message which must be supported
     * @return   selection model
     */
    public ComboBoxModel<Object> createPlasticComboBoxModel( URI messageId ) {
        return new SelectivePlasticListModel( appList_, messageId, true, this );
    }

    /**
     * Returns a new PlasticTransmitter which will transmit tables to
     * one or more listeners.
     *
     * @return  new table transmitter
     */
    public PlasticTransmitter createTableTransmitter() {
        return new TopcatTransmitter( this, MessageId.VOT_LOADURL, "table" ) {
            protected void transmit( PlasticHubListener hub, URI clientId,
                                     ApplicationItem app )
                    throws IOException {
                TopcatModel tcModel = controlWindow_.getCurrentModel();
                if ( tcModel != null ) {
                    URI[] recipients = app == null
                                     ? null
                                     : new URI[] { app.getId() };
                    transmitTable( tcModel, hub, clientId, recipients );
                }
            }
        };
    }

    /**
     * Sends a table to a specific list of PLASTIC listeners.
     *
     * @param  tcModel   the table model to broadcast
     * @param  hub      hub object
     * @param  plasticId  registration ID for this application
     * @param  recipients  listeners to receive it; null means do a broadcast
     */
    private void transmitTable( TopcatModel tcModel,
                                final PlasticHubListener hub,
                                final URI plasticId, final URI[] recipients )
            throws IOException {

        /* Write the data as a VOTable to a temporary file preparatory to
         * broadcast. */
        final File tmpfile = File.createTempFile( "plastic", ".vot" );
        final String tmpUrl = URLUtils.makeFileURL( tmpfile ).toString();
        tmpfile.deleteOnExit();
        OutputStream ostrm =
            new BufferedOutputStream( new FileOutputStream( tmpfile ) );
        try {
            new VOTableWriter( DataFormat.TABLEDATA, true )
               .writeStarTable( TopcatUtils.getSaveTable( tcModel ), ostrm );
        }
        catch ( IOException e ) {
            tmpfile.delete();
            throw e;
        }
        finally {
            ostrm.close();
        }

        /* Store a record of the table that was broadcast with its 
         * state. */
        int[] rowMap = tcModel.getViewModel().getRowMap();
        idMap_.put( tmpUrl, new TableWithRows( tcModel, rowMap ) );

        /* Do the broadcast, synchronously so that we don't delete the 
         * temporary file too early, but in another thread so that we
         * don't block the GUI. */
        new Thread( "PLASTIC table broadcast" ) {
            public void run() {
                List<String> argList =
                    Arrays.asList( new String[] { tmpUrl, tmpUrl } );
                Map<?,?> responses = recipients == null 
                    ? hub.request( plasticId, MessageId.VOT_LOADURL, argList )
                    : hub.requestToSubset( plasticId, MessageId.VOT_LOADURL,
                                           argList,
                                           Arrays.asList( recipients ) );

                /* Delete the temp file. */
                tmpfile.delete();
            }
        }.start();
    }

    /**
     * Returns a new PlasticTransmitter which will transmit subsets to
     * one or more listeners.
     *
     * @param  subSelector  SubsetWindow which supplies the currently selected
     *         subset ({@link SubsetWindow#getSelectedSubset})
     */
    public PlasticTransmitter
           createSubsetTransmitter( final TopcatModel tcModel,
                                    final SubsetWindow subSelector ) {
        return new TopcatTransmitter( this, MessageId.VOT_SHOWOBJECTS,
                                      "subset" ) {
            protected void transmit( PlasticHubListener hub, URI clientId,
                                     ApplicationItem app )
                    throws IOException {
                RowSubset rset = subSelector.getSelectedSubset();
                if ( rset != null ) {
                    URI[] recipients = app == null
                                     ? null
                                     : new URI[] { app.getId() };
                    transmitSubset( tcModel, rset, hub, clientId, recipients );
                }
            }
        };
    }

    /**
     * Sends a row subset to a specific list of PLASTIC listeners.
     * It uses the <code>ivo://votech.org/votable/showObjects</code> message.
     *
     * @param   tcModel  topcat model
     * @param   rset   row subset within tcModel
     * @param  recipients  listeners to receive it; null means do a broadcast
     */
    public void transmitSubset( TopcatModel tcModel, RowSubset rset,
                                URI[] recipients ) throws IOException {

        /* Get the hub and ID. */
        register();
        PlasticHubListener hub = getHub();
        URI plasticId = getRegisteredId();

        /* Do the work. */
        if ( plasticId != null ) {
            transmitSubset( tcModel, rset, hub, plasticId, recipients );
        }
        else {
            throw new NoHubException( "No hub" );
        }
    }


    /**
     * Sends a row subset to a specific list of PLASTIC listeners.
     * It uses the <code>ivo://votech.org/votable/showObjects</code> message.
     *
     * @param   tcModel  topcat model
     * @param   rset   row subset within tcModel
     * @param   hub    hub object
     * @param   plasticId  registration ID for this application
     * @param  recipients  listeners to receive it; null means do a broadcast
     */
    private void transmitSubset( TopcatModel tcModel, RowSubset rset, 
                                 PlasticHubListener hub, URI plasticId,
                                 final URI[] recipients )
            throws IOException {

        /* See if the table we're broadcasting the set for is any of the
         * tables we've previously broadcast.  If so, send the rows using
         * the same ID. */
        boolean done = false;
        for ( Map.Entry<String,TableWithRows> entry : idMap_.entrySet() ) {
            String tableId = entry.getKey();
            TableWithRows tr = entry.getValue();
            TopcatModel tcm = tr.tcModelRef_.get();
            if ( tcm != null && tcm == tcModel ) {
                List<Integer> rowList = new ArrayList<Integer>();

                /* Assemble a list of rows, possibly modulated by the
                 * row order when the table was sent originally. */
                int[] rowMap = tr.rowMap_;
                if ( rowMap == null ) {
                    int nrow =
                        (int) Math.min( (long) Integer.MAX_VALUE,
                                        tcModel.getDataModel().getRowCount() );
                    for ( int i = 0; i < nrow; i++ ) {
                        if ( rset.isIncluded( i ) ) {
                            rowList.add( Integer.valueOf( i ) );
                        }
                    }
                }
                else {
                    int nrow = rowMap.length;
                    for ( int i = 0; i < nrow; i++ ) {
                        if ( rset.isIncluded( rowMap[ i ] ) ) {
                            rowList.add( Integer.valueOf( i ) );
                        }
                    }
                }

                /* Send the request. */
                List<Object> argList =
                    Arrays.asList( new Object[] { tableId, rowList } );
                if ( recipients == null ) {
                    hub.requestAsynch( plasticId, MessageId.VOT_SHOWOBJECTS,
                                       argList );
                }
                else {
                    hub.requestToSubsetAsynch( plasticId,
                                               MessageId.VOT_SHOWOBJECTS,
                                               argList,
                                               Arrays.asList( recipients ) );
                }
                done = true;
            }
        }

        /* If that didn't result in any sends, try using the basic URL of
         * the table. */
        if ( ! done ) {
            URL url = URLUtils.fixURL( tcModel.getDataModel()
                                              .getBaseTable().getURL() );
            if ( url != null ) {
                List<Integer> rowList = new ArrayList<Integer>();
                int nrow =
                    (int) Math.min( (long) Integer.MAX_VALUE,
                                    tcModel.getDataModel().getRowCount() );
                for ( int i = 0; i < nrow; i++ ) {
                    if ( rset.isIncluded( i ) ) {
                        rowList.add( Integer.valueOf( i ) );
                    }
                }
                List<Object> argList =
                    Arrays.asList( new Object[] { url.toString(), rowList } );
                if ( recipients == null ) {
                    hub.requestAsynch( plasticId, MessageId.VOT_SHOWOBJECTS,
                                       argList );
                }
                else {
                    hub.requestToSubsetAsynch( plasticId,
                                               MessageId.VOT_SHOWOBJECTS,
                                               argList, 
                                               Arrays.asList( recipients ) );
                }
            }
        }
    }

    /** 
     * Transmits a request for listening applications to highlight a given
     * table row.
     *
     * @param   tcModel   topcat model of table to broadcast
     * @param   lrow      row index within tcModel
     * @param   recipients  array of plastic IDs for target applications;
     *         if null, broadcast will be to all
     * @return  true iff message was broadcast successfully
     */
    public boolean highlightRow( TopcatModel tcModel, long lrow, 
                                 URI[] recipients ) throws IOException {

        /* Get the hub and ID. */
        register();
        PlasticHubListener hub = getHub();
        URI plasticId = getRegisteredId();
        int irow = Tables.checkedLongToInt( lrow );

        /* See if the table we're broadcasting the row for is any of the
         * tables we've previously broadcast.  If so, send the row using the
         * same ID. */
        boolean done = false;
        int sendRow = -1;
        String tableId = null;
        for ( Iterator<Map.Entry<String,TableWithRows>> it =
                  idMap_.entrySet().iterator();
              ! done && it.hasNext(); ) {
            Map.Entry<String,TableWithRows> entry = it.next();
            TableWithRows tr = entry.getValue();
            TopcatModel tcm = tr.tcModelRef_.get();
            if ( tcm != null && tcm == tcModel ) {
                int[] rowMap = tr.rowMap_;
                if ( rowMap == null ) {
                    sendRow = irow;
                }
                else {
                    for ( int j = 0; j < rowMap.length; j++ ) {
                        if ( irow == rowMap[ j ] ) {
                            sendRow = j;
                            break;
                        }
                    }
                }
                tableId = entry.getKey();
                done = true;
            }
        }

        /* If that didn't result in any sends, try using the basic URL of
         * the table. */
        if ( ! done ) {
            URL url = URLUtils.fixURL( tcModel.getDataModel()
                                              .getBaseTable().getURL() );
            if ( url != null ) {
                sendRow = irow;
                tableId = url.toString();
                done = true;
            }
        }

        /* Send the message if we've got the arguments. */
        if ( done && sendRow >= 0 ) {
            List<Object> args = Arrays.asList( new Object[] {
                tableId,
                Integer.valueOf( sendRow ),
            } );
            if ( recipients == null ) {
                hub.requestAsynch( plasticId, MessageId.VOT_HIGHLIGHTOBJECT,
                                   args );
            }
            else {
                hub.requestToSubsetAsynch( plasticId,
                                           MessageId.VOT_HIGHLIGHTOBJECT, args,
                                           Arrays.asList( recipients ) );
            }
            return true;
        }

        /* Otherwise return failure status. */
        else {
            return false;
        }
    }

    /**
     * Broadcasts a request for listening applications to point at a given
     * sky position.
     *
     * @param  ra2000  right ascension J2000.0 in degrees
     * @param  dec2000 declination J2000.0 in degrees
     * @param  recipients  array of plastic IDs for target applications;
     *         if null, broadcast will be to all
     */
    public void pointAt( double ra2000, double dec2000, URI[] recipients )
            throws IOException {
        register();
        PlasticHubListener hub = getHub();
        URI plasticId = getRegisteredId();
        List<Object> args =
            Arrays.asList( new Object[] { Double.valueOf( ra2000 ),
                                          Double.valueOf( dec2000 ) } );
        if ( recipients == null ) {
            hub.requestAsynch( plasticId, MessageId.SKY_POINT, args );
        }
        else {
            hub.requestToSubsetAsynch( plasticId, MessageId.SKY_POINT, args,
                                       Arrays.asList( recipients ) );
        }
    }

    /**
     * Does the work for the load-from-string VOTable message.
     *
     * @param  sender  sender ID
     * @param  votText   VOTable text contained in a string, assumed UTF-8
     *                   encoded
     * @param  votId   identifies the sent VOTable for later use
     */
    private void votableLoad( URI sender, String votText, String votId )
            throws IOException {
        final byte[] votBytes;
        try {
            votBytes = votText.getBytes( "UTF-8" );
        }
        catch ( UnsupportedEncodingException e ) {
            throw (AssertionError)
                  new AssertionError( "JVMs are required to support UTF-8" )
                 .initCause( e );
        }
        votText = null;
        final DataSource datsrc = new DataSource() {
            public InputStream getRawInputStream() {
                return new ByteArrayInputStream( votBytes );
            }
        };
        TableProducer tp = new TableProducer() {
            public StarTable produceTable( StarTableFactory factory )
                    throws IOException {
                return factory.makeStarTable( datsrc, "votable" );
            }
        };
        attemptLoadTable( tp, sender, votId );
    }

    /**
     * Does the work for the load-from-URL VOTable load message.
     *
     * @param   sender  sender ID
     * @param   url  location of table
     * @param   key  identifier for loaded table   
     */
    private void votableLoadFromURL( URI sender, final String url, String key )
            throws IOException {
        TableProducer tp = new TableProducer() {
            public StarTable produceTable( StarTableFactory factory )
                    throws IOException {
                return factory.makeStarTable( url, "votable" );
            }
        };
        attemptLoadTable( tp, sender, key );
    }

    /**
     * Loads a StarTable into TOPCAT.  Must be called from the event dispatch
     * thread.
     *
     * @param  table  table to load
     * @param  sender  sender ID
     * @param  key   identifier for the loaded table
     */
    private void loadTable( StarTable table, URI sender, String key ) {
        String name = table.getName();
        if ( name == null || name.trim().length() == 0 ) {
            name = sender.toString();
        }
        TopcatModel tcModel = controlWindow_.addTable( table, name, true );
        if ( key != null && key.trim().length() > 0 ) {
            idMap_.put( key, new TableWithRows( tcModel, null ) );
        }
    }

    /**
     * Does the work for the highlight-object message.
     *
     * @param   sender  sender ID
     * @param   tableId  identifier for the table
     * @param   irow     row index corresponding to the table ID
     * @return  true  iff the highlight was successful
     */
    private boolean highlightObject( URI sender, String tableId, int irow ) {

        /* See if we have the table named in the message. */
        TableWithRows tr = lookupTable( tableId );
        final TopcatModel tcModel = tr == null
                                  ? null
                                  : tr.tcModelRef_.get();
        if ( tcModel != null ) {

            /* Find out what row is named in the message. */
            final long lrow = tr.rowMap_ == null ? (long) irow
                                                 : (long) tr.rowMap_[ irow ];

            /* Call a highlight on this row.  However, if it's the same
             * as the last-highlighted row for this table, do nothing.
             * The purpose of this is to avoid the possibility of
             * eternal PLASTIC ping-pong between two (or more) 
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
            return true;
        }
        return false;
    }

    /**
     * Does the work for the show-objects message.
     *
     * @param   sender  sender ID
     * @param   tableId  identifier for the table
     * @param   objList  list of row indices (should be Numbers)
     * @return   true iff highlight was successful
     */
    private boolean showObjects( URI sender, String tableId, List<?> objList ) {
        TableWithRows tr = lookupTable( tableId );
        TopcatModel tcModel = tr == null
                            ? null
                            : tr.tcModelRef_.get();
        if ( tcModel != null ) {

            /* Turn the list of row indices into a bit vector. */
            int[] rowMap = tr.rowMap_;
            BitSet mask = new BitSet();
            for ( Object val : objList ) {
                if ( val instanceof Number ) {
                    int index = ((Number) val).intValue();
                    mask.set( rowMap == null ? index : rowMap[ index ] );
                }
            }
            String appname = getAppName( sender ).replaceAll( "\\s+", "_" );

            /* Behaviour changed here.  At rev 1.25, the response was to 
             * generate a new subset and set it current.  Subsequently,
             * it's to co-opt an existing subset if one of the right name
             * exists (otherwise generate a new one) and send a SHOW_SUBSET
             * message.  This is less drastic and easier to see what's going
             * on, and probably better.  However it may make sense to provide
             * the old (applyNewSubset) behaviour as an option?. */
            // applyNewSubset( tcModel, mask, appname );
            showSubset( tcModel, mask, appname );

            /* Success return. */
            return true;
        }
        else {

            /* Failure return. */
            return false;
        }
    }

    /**
     * Takes a bit mask representing selected rows and causes it to be 
     * highlighted for the given table.
     *
     * @param  tcModel   topcat model
     * @param  mask      row selection mask
     * @param  baseName  name of the sending application
     */
    private void showSubset( final TopcatModel tcModel, BitSet mask,
                             String appName ) {
        final RowSubset rset = new BitsRowSubset( appName, mask );
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                tcModel.addSubset( rset );
            }
        } );
    }

    /**
     * Takes a bit mask representing selected rows and causes it to become
     * the Current Row Subset for the given table.  Usually this means
     * creating a new Row Subset corresponding to that mask prior to
     * applying it.  However, in the special case that the mask is
     * identical to an existing subset, that one will be used instead.
     *
     * @param  tcModel   topcat model
     * @param  mask      row selection mask
     * @param  baseName  name of the sending application
     */
    private void applyNewSubset( final TopcatModel tcModel, BitSet mask,
                                 String appName ) {

        /* See if this is identical to an existing subset.  If so, don't
         * create a new one.  It's arguable whether this is the behaviour
         * that you want, but at least until we have some way to delete
         * subsets it's probably best to do it like this to cut down on
         * subset proliferation. */
        RowSubset matching = null;
        for ( Iterator<RowSubset> it = tcModel.getSubsets().iterator();
              matching == null && it.hasNext(); ) {
            RowSubset rset = it.next();
            int nrow = Tables.checkedLongToInt( tcModel.getDataModel()
                                                       .getRowCount() );
            if ( matches( mask, rset, nrow ) ) {
                matching = rset;
            }
        }

        /* If we've found an existing set with the same content, 
         * apply that one. */
        if ( matching != null ) {
            final RowSubset rset = matching;
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    tcModel.applySubset( rset );
                }
            } );
        }

        /* Otherwise make sure we have a unique name for the new subset. */
        else {
            int ipset = 0;
            for ( RowSubset rset : tcModel.getSubsets() ) {
                String setName = rset.getName();
                if ( setName.matches( appName + "-[0-9]+" ) ) {
                    String digits =
                        setName.substring( appName.length() + 1 );
                    ipset = Math.max( ipset, Integer.parseInt( digits ) );
                }
            }
            String setName = appName + '-' + ( ipset + 1 );

            /* Then construct, add and apply the new subset. */
            final RowSubset rset = new BitsRowSubset( setName, mask );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    tcModel.addSubset( rset );
                    tcModel.applySubset( rset );
                }
            } );
        }
    }

    /**
     * Attempts to locate a table by its ID.  This is currently a URL string;
     * either one got from a previous VOT_LOADURL message or one inherent
     * in the table.
     *
     * @param   tableId   table identifier URL string
     * @return  tableWithRows object corresponding to tableId, or null
     */
    private TableWithRows lookupTable( String tableId ) {
        TableWithRows tr = idMap_.get( tableId );
        if ( tr != null ) {
            return tr;
        }
        else {
            ListModel<TopcatModel> tablesList =
                ControlWindow.getInstance().getTablesListModel();
            for ( int i = 0; i < tablesList.getSize(); i++ ) {
                TopcatModel tcModel = tablesList.getElementAt( i );
                URL url = tcModel.getDataModel().getBaseTable().getURL();
                if ( URLUtils.sameResource( url,
                                            URLUtils.makeURL( tableId ) ) ) {
                    return new TableWithRows( tcModel, null );
                }
            }
        }
        return null;
    }


    /**
     * Returns the name of a registered application which has a given ID.
     * If no such application is registered, some general string like
     * "plastic" is returned.
     *
     * @param   id   application ID
     * @return  application name
     */
    private String getAppName( URI id ) {
        String name = null;
        for ( int i = 0; i < appList_.getSize(); i++ ) {
            ApplicationItem app = appList_.getElementAt( i );
            if ( app.getId().equals( id ) ) {
                return app.getName();
            }
        }
        return "plastic";
    }
    
    /**
     * Encapsulates a table plus its row order.
     */
    private static class TableWithRows {
        final Reference<TopcatModel> tcModelRef_;
        final int[] rowMap_;
        TableWithRows( TopcatModel tcModel, int[] rowMap ) {
            tcModelRef_ = new WeakReference<TopcatModel>( tcModel );
            rowMap_ = rowMap;
        }
    }

    /**
     * Determines whether a BitSet contains the same information as a
     * RowSubset.
     *
     * @param  mask  bit set
     * @param  rset  row subset
     * @param  nrow  number of rows over which they are required to match
     * @return  true iff they represent the same data
     */
    private static boolean matches( BitSet mask, RowSubset rset, int nrow ) {
        for ( int i = 0; i < nrow; i++ ) {
            if ( mask.get( i ) != rset.isIncluded( (long) i ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to generate a table from a supplied factory object,
     * and if successful loads it into TOPCAT.
     */
    private void attemptLoadTable( TableProducer producer, final URI sender,
                                   final String tableId ) {
        final LoadingToken token = new LoadingToken( "PLASTIC table" );
        controlWindow_.addLoadingToken( token );

        /* Attempt to create a table from the message received. */
        Throwable error;
        StarTable table;
        boolean success;
        try {
            table = producer.produceTable( controlWindow_.getTableFactory() );
            error = null;
            success = true;
        }
        catch ( Throwable e ) {
            error = e;
            table = null;
            success = false;
        }

        /* Do something on the event dispatch thread with the loaded table
         * or error. */
        final boolean success0 = success;
        final StarTable table0 = table;
        final Throwable error0 = error;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                if ( success0 ) {
                    loadTable( table0, sender, tableId );
                }
                else {
                    ErrorDialog.showError( controlWindow_,
                                           "PLASTIC Load Error", error0,
                                           "PLASTIC load failed" );
                }
                controlWindow_.removeLoadingToken( token );
            }
        } );
    }

    /**
     * Interface for an object which can produce a table.
     */
    private abstract static class TableProducer {

        /**
         * Generates a table.
         *
         * @param  factory  factory
         * @return  new table
         * @throws  IOException  on failure
         */
        abstract StarTable produceTable( StarTableFactory factory )
                throws IOException;
    }
}
