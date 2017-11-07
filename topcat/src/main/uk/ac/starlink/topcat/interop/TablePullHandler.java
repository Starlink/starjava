package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.ListModel;
import org.astrogrid.samp.ErrInfo;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.httpd.ServerResource;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * Message handler for table pull messages.
 * These return a reponse giving the URL of a table held by topcat.
 *
 * @author   Mark Taylor
 * @since    28 Feb 2014
 */
public abstract class TablePullHandler extends AbstractMessageHandler {

    /**
     * Constructor.
     *
     * @param  mtype  SAMP Mtype
     */
    protected TablePullHandler( String mtype ) {
        super( mtype );
    }

    /**
     * If it's a Response, use it as is, if not wrap it up as a success.
     */
    @Override
    protected Response createResponse( Map processOutput ) {
        return processOutput instanceof Response
             ? (Response) processOutput
             : super.createResponse( processOutput );
    }

    @Override
    public Map processCall( HubConnection conn, String senderId, Message msg )
            throws IOException {
        StarTableWriter twriter = getTableWriter( msg );

        /* Parameter "index" gives the ID number of the table in topcat. */
        String indexObj = (String) msg.getParam( "index" );
        int index = indexObj == null ? -1 : SampUtils.decodeInt( indexObj );
        TopcatModel tcModel = getTableByID( index );

        /* If there's no table, return an error response. */
        if ( tcModel == null ) {
            String errtxt = index <= 0 ? "No current table"
                                       : "No table with ID " + index;
            return Response.createErrorResponse( new ErrInfo( errtxt ) );
        }

        /* Otherwise turn it into an HTTP resource and send its URL as a
         * response value. */
        else {
            StarTable table = TopcatUtils.getSaveTable( tcModel );
            String fname = "t" + index + getSuffix( twriter );
            ServerResource resource =
                TableSendActionManager.createTableResource( table, twriter );
            URL turl =
                TopcatServer.getInstance().addResource( fname, resource );
            Map result = new LinkedHashMap();
            result.put( "url", turl.toString() );
            return Response.createSuccessResponse( result );
        }
    }

    /**
     * Returns a table writer to use for the given message.
     *
     * @param  msg  message
     * @return  table serializer
     */
    protected abstract StarTableWriter getTableWriter( Message msg )
            throws IOException;

    /**
     * Returns a message handler for a given fixed table format.
     *
     * @param  mtype  SAMP Mtype
     * @param  twriter  serializer defining table format
     * @return   new table pull handler
     */
    public static TablePullHandler
            createFormatTablePullHandler( String mtype,
                                          final StarTableWriter twriter ) {
        return new TablePullHandler( mtype ) {
            protected StarTableWriter getTableWriter( Message msg ) {
                return twriter;
            }
        };
    }

    /**
     * Returns a message handler where the table format is determined by
     * a "format" parameter in the incoming message.
     *
     * @param  mtype  SAMP Mtype
     * @return   new table pull handler
     */
    public static TablePullHandler
            createGenericTablePullHandler( String mtype ) {
        final StarTableOutput sto = new StarTableOutput();
        return new TablePullHandler( mtype ) {
            protected StarTableWriter getTableWriter( Message msg )
                    throws IOException {
                String fmtName = (String) msg.getRequiredParam( "format" );
                return sto.getHandler( fmtName );
            }
        };
    }

    /**
     * Returns the topcat table with a given ID, or the default one
     * if it's non-positive.
     *
     * @param  id  topcat table ID or non-positive value
     * @return  topcat model or null if ID not used
     */
    private static TopcatModel getTableByID( int id ) throws IOException {
        ControlWindow cwin = ControlWindow.getInstance();
        if ( id <= 0 ) {
            return cwin.getCurrentModel();
        }
        else {
            ListModel listModel = cwin.getTablesListModel();
            int nt = listModel.getSize();
            for ( int it = 0; it < nt; it++ ) {
                TopcatModel tcModel =
                    (TopcatModel) listModel.getElementAt( it );
                if ( tcModel.getID() == id ) {
                    return tcModel;
                }
            }
            return null;
        }
    }

    /**
     * Returns a plausible filename suffix for a table writer.
     * This is really just intended for cosmetic purposes.
     * The restult may be the empty string.
     *
     * @param   twriter  table writer
     * @return  file suffix
     */
    private static String getSuffix( StarTableWriter twriter ) {
        String fmtname = twriter.getFormatName();
        if ( fmtname == null || fmtname.trim().length() == 0 ) {
            return "";
        }
        int dashIndex = fmtname.indexOf( '-' );
        String abbrev = dashIndex > 0 ? fmtname.substring( 0, dashIndex )
                                      : fmtname;
        return "." + abbrev.toLowerCase();
    }
}
