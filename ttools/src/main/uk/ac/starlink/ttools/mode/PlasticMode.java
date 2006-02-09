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
import java.util.Iterator;
import java.util.Map;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.astrogrid.Plastic;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;
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

    private final static String TRANSPORT_STRING;
    private final static String TRANSPORT_FILE;
    private final static String[] TRANSPORTS = new String[] {
        TRANSPORT_STRING = "string",
        TRANSPORT_FILE = "file",
    };
    private final static URI MSG_BYTEXT =
        getURI( "ivo://votech.org/votable/load" );
    private final static URI MSG_BYURL = 
        getURI( "ivo://votech.org/votable/loadFromURL" );
    private final static URI MSG_NAME =
        getURI( "ivo://votech.org/info/getName" );

    public String getDescription() {
        return new StringBuffer()
       .append( "Broadcasts the table to any registered Plastic-aware\n" )
       .append( "applications.  PLASTIC, the PLatform for AStronomical\n" )
       .append( "Tool InterConnection, is a tool interoperability protocol.\n" )
       .append( "A Plastic hub must be running in order for this to work.\n" )
       .toString();
    }

    public PlasticMode() {
        transportParam_ = new ChoiceParameter( "transport", TRANSPORTS );
        transportParam_.setDescription( new String[] {
            "Determines the method (PLASTIC <em>message</em>) used",
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
        } );
        transportParam_.setNullPermitted( true );
        transportParam_.setPrompt( "PLASTIC transport mechanism" );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            transportParam_,
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
        final PlasticHubListener hub;
        final URI plasticId;
        try {
            hub = Plastic.getLocalHub();
            plasticId = hub.registerNoCallBack( "stilts" );
        }
        catch ( Throwable e ) {
            throw new TaskException( "Can't connect to PLASTIC hub", e );
        }
        final PrintStream out = env.getOutputStream();
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                try {
                    broadcast( table, msg, hub, plasticId, out );
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
     * @param   out    output stream to the environment
     */
    private static void broadcast( StarTable table, URI msg,
                                   PlasticHubListener hub, URI plasticId,
                                   PrintStream out )
            throws IOException {
        long nrow = table.getRowCount();

        /* If we've been instructed to use the inline votable load, or
         * if the table looks short, then write the VOTable into a string
         * and pass it as one of the parameters to the hub call. */
        if ( ( MSG_BYTEXT.equals( msg ) ) ||
             ( msg == null && nrow >= 0 && nrow < 1000 ) ) {
            try {
                ByteArrayOutputStream bstrm = new ByteArrayOutputStream();
                new VOTableWriter( DataFormat.TABLEDATA, true )
                    .writeStarTable( table, bstrm );
                bstrm.close();
                String vot = bstrm.toString();
                bstrm = null;
                hub.requestAsynch( plasticId, MSG_BYTEXT,
                                   Arrays.asList( new Object[] { vot } ) );
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
            Map loadResponses = 
                hub.request( plasticId, MSG_BYURL,
                             Arrays.asList( new Object[]{ tmpfile.toURL() } ) );
            tmpfile.delete();

            /* Get the names of the listeners which responded. */
            if ( loadResponses.size() > 0 ) {
                out.print( "Broadcast to listeners: " );
                Map nameResponses =
                    hub.requestToSubset( plasticId, MSG_NAME, new ArrayList(),
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
            return;
        }

        /* Can't get here? */
        throw new AssertionError( "Unknown transport: " + msg );
    }

    /**
     * Convenience method to turn a String into a URI without throwing
     * any pesky checked exceptions.
     *
     * @param  uri  URI text
     * @return  URI
     * @throws  IllegalArgumentException   if uri doesn't look like a URI
     */
    private static URI getURI( String uri ) {
        try {
            return new URI( uri );
        }
        catch ( URISyntaxException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad URI: " + uri )
                 .initCause( e );
        }
    }
}
