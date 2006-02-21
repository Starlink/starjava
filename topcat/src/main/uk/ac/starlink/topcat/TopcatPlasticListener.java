package uk.ac.starlink.topcat;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DataSource;
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
    private final Map idMap_;

    private static final URI VOT_LOAD;
    private static final URI VOT_LOADURL;
    private static final URI VOT_SHOWOBJECTS;
    private static final URI[] SUPPORTED_MESSAGES = new URI[] {
        VOT_LOAD = createURI( "ivo://votech.org/votable/load" ),
        VOT_LOADURL = createURI( "ivo://votech.org/votable/loadFromURL" ),
        VOT_SHOWOBJECTS = createURI( "ivo://votech.org/votable/showObjects" ),
    };

    /**
     * Constructs a new listener which will react appropriately to 
     * messages from the hub.
     *
     * @param   controlWindow   control window into which accepted tables
     *          will be loaded etc
     */
    public TopcatPlasticListener( ControlWindow controlWindow ) {
        super( "topcat", SUPPORTED_MESSAGES );
        controlWindow_ = controlWindow;
        idMap_ = Collections.synchronizedMap( new HashMap() );
    }

    /**
     * Does the work for processing a hub message.
     *
     * @param  sender   sender ID
     * @param  message  message ID (determines the action required)
     * @param  args     message argument list
     * @return  return value requested by message
     */
    public Object doPerform( URI sender, URI message, List args )
            throws IOException {

        /* Load VOTable passed as text in an argument. */
        if ( VOT_LOAD.equals( message ) &&
                  checkArgs( args, new Class[] { String.class } ) ) {
            votableLoad( sender, (String) args.get( 0 ) );
	    return Boolean.TRUE;
	}

        /* Load VOTable by URL. */
        else if ( VOT_LOADURL.equals( message ) &&
                  checkArgs( args, new Class[] { Object.class } ) ) {
            String url = args.get( 0 ) instanceof String
                       ? (String) args.get( 0 )
                       : args.get( 0 ).toString();
            votableLoadFromURL( sender, url );
            return Boolean.TRUE;
        }

        /* Select VOTable rows. */
        else if ( VOT_SHOWOBJECTS.equals( message ) && args.size() >= 2 &&
                  args.get( 1 ) instanceof List ) {
            String tableId = args.get( 0 ).toString();
            List objList = (List) args.get( 1 );
            return Boolean.valueOf( showObjects( sender, tableId, objList ) );
        }

        /* Unknown message. */
        else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Broadcasts a table to other PLASTIC listeners.
     *
     * @param  tcModel   the table model to broadcast
     */
    public void broadcastTable( TopcatModel tcModel ) throws IOException {

        /* Get the hub and ID. */
        register();
        PlasticHubListener hub = getHub();
        URI plasticId = getRegisteredId();

        /* Write the data as a VOTable to a temporary file preparatory to
         * broadcast. */
        File tmpfile = File.createTempFile( "plastic", ".vot" );
        URL tmpUrl = tmpfile.toURL();
        tmpfile.deleteOnExit();
        OutputStream ostrm =
            new BufferedOutputStream( new FileOutputStream( tmpfile ) );
        try {
            new VOTableWriter( DataFormat.TABLEDATA, true )
               .writeStarTable( tcModel.getApparentStarTable(), ostrm );
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

        /* Do the broadcast, syncrhronously so that we don't delete the 
         * temporary file too early. */
        Map responses =
            hub.request( plasticId, VOT_LOADURL,
                         Arrays.asList( new Object[] { tmpUrl } ) );

        /* Delete the temp file. */
        tmpfile.delete();
    }

    /**
     * Sends a given subset to plastic listeners.  It is broadcast using
     * the {@link #VOT_SHOWOBJECTS} message.
     *
     * @param   tcModel  topcat model
     * @param   rset   row subset within tcModel
     */
    public void broadcastSubset( TopcatModel tcModel, RowSubset rset )
            throws IOException {

        /* Get the hub and ID. */
        register();
        PlasticHubListener hub = getHub();
        URI plasticId = getRegisteredId();

        /* See if the table we're broadcasting the set for is any of the
         * tables we've previously broadcast.  If so, send the rows using
         * the same ID. */
        boolean done = false;
        for ( Iterator it = idMap_.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String tableId = entry.getKey().toString();
            TableWithRows tr = (TableWithRows) entry.getValue();
            TopcatModel tcm = (TopcatModel) tr.tcModelRef_.get();
            if ( tcm != null && tcm == tcModel ) {
                List rowList = new ArrayList();

                /* Assemble a list of rows, possibly modulated by the
                 * row order when the table was sent originally. */
                int[] rowMap = tr.rowMap_;
                if ( rowMap == null ) {
                    int nrow =
                        (int) Math.min( (long) Integer.MAX_VALUE,
                                        tcModel.getDataModel().getRowCount() );
                    for ( int i = 0; i < nrow; i++ ) {
                        if ( rset.isIncluded( i ) ) {
                            rowList.add( new Integer( i ) );
                        }
                    }
                }
                else {
                    int nrow = rowMap.length;
                    for ( int i = 0; i < nrow; i++ ) {
                        if ( rset.isIncluded( rowMap[ i ] ) ) {
                            rowList.add( new Integer( i ) );
                        }
                    }
                }

                /* Send the request. */
                hub.requestAsynch( plasticId, VOT_SHOWOBJECTS,
                                   Arrays.asList( new Object[] { tableId,
                                                                 rowList } ) );
                done = true;
            }
        }

        /* If that didn't result in any sends, try using the basic URL of
         * the table. */
        if ( ! done ) {
            URL url = tcModel.getDataModel().getBaseTable().getURL();
            if ( url != null ) {
                List rowList = new ArrayList();
                int nrow =
                    (int) Math.min( (long) Integer.MAX_VALUE,
                                    tcModel.getDataModel().getRowCount() );
                for ( int i = 0; i < nrow; i++ ) {
                    if ( rset.isIncluded( i ) ) {
                        rowList.add( new Integer( i ) );
                    }
                }
                hub.requestAsynch( plasticId, VOT_SHOWOBJECTS,
                                   Arrays.asList( new Object[] { url.toString(),
                                                                 rowList } ) );
            }
        }
    }

    /**
     * Does the work for the load-from-string VOTable message.
     *
     * @param  sender  sender ID
     * @param  votText   VOTable text contained in a string, assumed UTF-8
     *                   encoded
     */
    private void votableLoad( URI sender, String votText ) throws IOException {
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
        DataSource datsrc = new DataSource() {
            public InputStream getRawInputStream() {
                return new ByteArrayInputStream( votBytes );
            }
        };
        loadTable( controlWindow_.getTableFactory()
                  .makeStarTable( datsrc, "votable" ), sender, null );
    }

    /**
     * Does the work for the load-from-URL VOTable load message.
     *
     * @param   sender  sender ID
     * @param   url  location of table
     */
    private void votableLoadFromURL( URI sender, String url )
            throws IOException {
        loadTable( controlWindow_.getTableFactory()
                  .makeStarTable( url, "votable" ), sender, url.toString() );
    }

    /**
     * Loads a StarTable into TOPCAT.
     *
     * @param  table  table to load
     * @param  sender  sender ID
     * @param  key   identifier for the loaded table
     */
    private void loadTable( final StarTable table, URI sender,
                            final String key ) {
        String name = table.getName();
        if ( name == null || name.trim().length() == 0 ) {
            name = sender.toString();
        }
        final String title = name;

        /* Best do it asynchronously since this may not be called from the
         * event dispatch thread. */
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                TopcatModel tcModel =
                    controlWindow_.addTable( table, title, true );
                if ( key != null ) {
                    idMap_.put( key, new TableWithRows( tcModel, null ) );
                }
            }
        } );
    }

    /**
     * Does the work for the show-objects message.
     *
     * @param   sender  sender ID
     * @param   tableId  identifier for the table
     * @param   objList  list of row indices (should be Numbers)
     * @return   true iff highlight was successful
     */
    private boolean showObjects( URI sender, String tableId, List objList ) {
        TableWithRows tr = (TableWithRows) lookupTable( tableId );
        final TopcatModel tcModel = tr == null
                                  ? null
                                  : (TopcatModel) tr.tcModelRef_.get();
        if ( tcModel != null ) {

            /* Turn the list of row indices into a bit vector. */
            int[] rowMap = tr.rowMap_;
            BitSet mask = new BitSet();
            for ( Iterator it = objList.iterator(); it.hasNext(); ) {
                Object val = it.next();
                if ( val instanceof Number ) {
                    int index = ((Number) val).intValue();
                    mask.set( rowMap == null ? index : rowMap[ index ] );
                }
            }

            /* Get a unique name for the new subset. */
            int ipset = 0;
            for ( Iterator it = tcModel.getSubsets().iterator();
                  it.hasNext(); ) {
                String setName = ((RowSubset) it.next()).getName();
                if ( setName.startsWith( "plastic-" ) ) {
                    ipset = Math.max( ipset,
                                      Integer.parseInt( setName
                                                       .substring( 8 ) + 1 ) );
                }
            }
            String setName = "plastic-" + ipset;

            /* Construct, add and apply the new subset. */
            final RowSubset rset =
                new BitsRowSubset( "plastic-" + ipset, mask );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    tcModel.addSubset( rset );
                    tcModel.applySubset( rset );
                }
            } );

            /* Success return. */
            return true;
        }
        else {

            /* Failure return. */
            return false;
        }
    }

    /**
     * Attempts to locate a table by its ID.  This is currently a URL;
     * either one got from a previous VOT_LOADURL message or one inherent
     * in the table.
     *
     * @param   tableId   table identifier URL string
     * @return  tableWithRows object corresponding to tableId, or null
     */
    private TableWithRows lookupTable( String tableId ) {
        TableWithRows tr = (TableWithRows) idMap_.get( tableId );
        if ( tr != null ) {
            return tr;
        }
        else {
            ListModel tablesList =
                ControlWindow.getInstance().getTablesListModel();
            for ( int i = 0; i < tablesList.getSize(); i++ ) {
                TopcatModel tcModel =
                    (TopcatModel) tablesList.getElementAt( i );
                URL url = tcModel.getDataModel().getBaseTable().getURL();
                if ( url != null && url.toString().equals( tableId ) ) {
                    return new TableWithRows( tcModel, null );
                }
            }
        }
        return null;
    }
    
    /**
     * Encapsulates a table plus its row order.
     */
    private static class TableWithRows {
        final Reference tcModelRef_;
        final int[] rowMap_;
        TableWithRows( TopcatModel tcModel, int[] rowMap ) {
            tcModelRef_ = new WeakReference( tcModel );
            rowMap_ = rowMap;
        }
    }
}
