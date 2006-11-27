package uk.ac.starlink.ttools.mode;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.MessageId;
import uk.ac.starlink.plastic.PlasticUtils;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Processing mode for broadcasting the table for loading using the
 * PLASTIC tool interop protocol.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2006
 * @see  <a href="http://plastic.sourceforge.net/">PLASTIC</a>
 */
public class PlasticMode implements ProcessingMode {

    private final Parameter transportParam_;
    private final Parameter clientParam_;

    final static String TRANSPORT_STRING;
    final static String TRANSPORT_FILE;
    private final static String[] TRANSPORTS = new String[] {
        TRANSPORT_STRING = "string",
        TRANSPORT_FILE = "file",
    };

    /** Message ID for load by passing VOTable text as a string argument. */
    public final static URI MSG_BYTEXT = MessageId.VOT_LOAD;

    /** Message ID for load by passing VOTable URL (temp file) as argument.*/
    public final static URI MSG_BYURL = MessageId.VOT_LOADURL;

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Broadcasts the table to any registered Plastic-aware",
            "applications.",
            "<webref url='http://plastic.sourceforge.net/'>PLASTIC</webref>,",
            "the PLatform for AStronomical Tool InterConnection,",
            "is a tool interoperability protocol.",
            "A <em>Plastic hub</em> must be running in order for this to work.",
            "</p>",
        } );
    }

    public PlasticMode() {
        transportParam_ = new ChoiceParameter( "transport", TRANSPORTS );
        transportParam_.setDescription( new String[] {
            "<p>Determines the method (PLASTIC <em>message</em>) used",
            "to perform the PLASTIC communication.  The choices are",
            "<ul>",
            "<li><code>string</code>:",
            "VOTable serialized as a string and passed as a call parameter",
            "(<code>" + MSG_BYTEXT + "</code>).",
            "Not suitable for very large files.</li>",
            "<li><code>file</code>:",
            "VOTable written to a temporary file and the filename passed as",
            "a call parameter",
            "(<code>" + MSG_BYURL + "</code>).",
            "The file ought to be deleted once it has been loaded.",
            "Not suitable for inter-machine communication.</li>",
            "</ul>",
            "If no value is set (<code>null</code>) then a decision will",
            "be taken based on the apparent size of the table.",
            "</p>",
        } );
        transportParam_.setNullPermitted( true );
        transportParam_.setPrompt( "PLASTIC transport mechanism" );

        clientParam_ = new Parameter( "client" );
        clientParam_.setDescription( new String[] {
            "<p>Gives the name of a PLASTIC listener application which is to",
            "receive the broadcast table.",
            "If a non-null value is given, then only the first registered",
            "application which reports its application name as that value",
            "will receive the message.  If no value is supplied, the",
            "broadcast will be to all listening applications.",
            "</p>",
        } );
        clientParam_.setNullPermitted( true );
        clientParam_.setPrompt( "Recipient application" );
        clientParam_.setUsage( "<app-name>" );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            transportParam_,
            clientParam_,
        };
    }

    public TableConsumer createConsumer( Environment env )
            throws TaskException {
        String transport = transportParam_.stringValue( env );
        final URI msg;
        if ( transport == null ) {
            msg = null;
        }
        else if ( transport.equals( TRANSPORT_STRING ) ) {
            msg = MSG_BYTEXT;
        }
        else if ( transport.equals( TRANSPORT_FILE ) ) {
            msg = MSG_BYURL;
        }
        else {
            throw new ParameterValueException( transportParam_,
                                               "Unknown transport type" );
        }

        final String client = clientParam_.stringValue( env );

        final PlasticHubListener hub;
        final URI plasticId;
        try {
            hub = PlasticUtils.getLocalHub();
            plasticId = hub.registerNoCallBack( "stilts" );
        }
        catch ( Throwable e ) {
            throw new TaskException( "Can't connect to PLASTIC hub", e );
        }
        final StoragePolicy policy =
            LineTableEnvironment.getStoragePolicy( env );
        final PrintStream out = env.getOutputStream();
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                try {
                    broadcast( table, msg, hub, plasticId, policy,
                               client, out );
                }
                finally {
                    hub.unregister( plasticId );
                }
            }
        };
    }

    /**
     * Broadcasts a table to PLASTIC listeners by sending it to the hub
     * as a VOTable.
     *
     * @param   table  table to broadcast
     * @param   msg    PLASTIC message key which defines how the transport
     *                 will take place (one of MSG_BYTEXT, MSG_BYURL or null)
     * @param   hub    plastic hub object
     * @param   plasticId  plastic identifier for this client
     * @param   policy  storage policy
     * @param   client  application name of sole target for broadcast,
     *                  or null for all
     * @param   out    output stream to the environment (may be null)
     */
    public static void broadcast( StarTable table, URI msg,
                                  PlasticHubListener hub, URI plasticId,
                                  StoragePolicy policy, String client,
                                  PrintStream out )
            throws IOException {
        long nrow = table.getRowCount();

        /* Determine the list of clients for receipt of the message. */
        URI clientId = null;
        if ( client != null && client.trim().length() > 0 ) {
            Map nameResponses = hub.request( plasticId, MessageId.INFO_GETNAME,
                                             new ArrayList() );
            for ( Iterator it = nameResponses.entrySet().iterator();
                  clientId == null && it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                URI id = (URI) entry.getKey();
                Object value = entry.getValue();
                if ( value instanceof String &&
                     ((String) value).equalsIgnoreCase( client ) ) {
                    clientId = id;
                }
            }
        }
        if ( client != null && clientId == null ) {
            throw new IOException( "No PLASTIC listener by the name of "
                                 + client );
        }
        List clientList = clientId == null
                        ? null
                        : Collections.singletonList( clientId );

        /* If we've been instructed to use the inline votable load, or
         * if the table looks short, then write the VOTable into a string
         * and pass it as one of the parameters to the hub call. */
        if ( ( MSG_BYTEXT.equals( msg ) ) ||
             ( msg == null &&
               policy == StoragePolicy.PREFER_MEMORY &&
               nrow >= 0 && nrow < 1000 ) ) {
            try {
                ByteArrayOutputStream bstrm = new ByteArrayOutputStream();
                new VOTableWriter( DataFormat.TABLEDATA, true )
                    .writeStarTable( table, bstrm );
                bstrm.close();
                String vot = bstrm.toString();
                bstrm = null;
                List args = Arrays.asList( new String[] { vot, "" } );
                if ( clientList == null ) {
                    hub.requestAsynch( plasticId, MSG_BYTEXT, args );
                }
                else {
                    hub.requestToSubsetAsynch( plasticId, MSG_BYTEXT, args,
                                               clientList );
                }
                return;
            }
            catch ( OutOfMemoryError e ) {
                if ( MSG_BYTEXT.equals( msg ) ) {
                    throw e;
                }

                /* If we ran out of memory but weren't explicitly instructed
                 * to use the inline method, fall through here and use the
                 * URL message. */
            }
        }

        /* Either by default or by explicit instruction do it by URL.
         * We write the table to a temporary file and pass its URL 
         * to the hub.  Ensure it's deleted afterwards. */
        if ( msg == null || msg.equals( MSG_BYURL ) ) {
            File tmpfile = File.createTempFile( "plastic", ".vot" );
            tmpfile.deleteOnExit();
            OutputStream ostrm =
                new BufferedOutputStream( new FileOutputStream( tmpfile ) );
            new VOTableWriter( DataFormat.TABLEDATA, true )
               .writeStarTable( table, ostrm );
            ostrm.close();

            /* Hub communication is synchronous; we want to wait until all
             * clients have responded so we can delete the temporary file
             * afterwards. */
            List args = Collections
                       .singletonList( URLUtils.makeFileURL( tmpfile )
                                               .toString() );
            Map loadResponses = clientList == null
                              ? hub.request( plasticId, MSG_BYURL, args )
                              : hub.requestToSubset( plasticId, MSG_BYURL, args,
                                                     clientList );
            tmpfile.delete();

            /* If requested, get and output the names of the listeners which
             * responded. */
            if ( out != null ) {
                if ( loadResponses.size() > 0 ) {
                    out.print( "Broadcast to listeners: " );
                    Map nameResponses =
                        hub.requestToSubset( plasticId, MessageId.INFO_GETNAME,
                                             new ArrayList(),
                                             new ArrayList( loadResponses
                                                           .keySet() ) );
                    for ( Iterator it = loadResponses.keySet().iterator();
                          it.hasNext(); ) {
                        URI id = (URI) it.next();
                        String name = (String) nameResponses.get( id );
                        if ( name == null || name.trim().length() == 0 ) {
                            name = id.toString();
                        }
                        out.print( name );
                        if ( it.hasNext() ) {
                            out.print( ", " );
                        }
                    }
                    out.println();
                }
                else {
                    out.println( "Nobody listening" );
                }
            }
            return;
        }

        /* Can't get here? */
        throw new AssertionError( "Unknown transport: " + msg );
    }
}
