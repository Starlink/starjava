package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.CallableClient;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.httpd.HttpServer;
import org.astrogrid.samp.httpd.ResourceHandler;
import org.astrogrid.samp.httpd.ServerResource;
import org.astrogrid.samp.httpd.URLMapperHandler;
import org.astrogrid.samp.xmlrpc.StandardClientProfile;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.task.DefaultMultiParameter;
import uk.ac.starlink.ttools.task.LineTableEnvironment;

/**
 * Processing mode for sending the table to other subscribed clients
 * using the SAMP tool interop protocol.
 *
 * @author   Mark Taylor
 * @since    7 Jan 2008
 */
public class SampMode implements ProcessingMode {

    private final DefaultMultiParameter formatsParam_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.mode" );

    /**
     * Constructor.
     */
    public SampMode() {
        formatsParam_ = new DefaultMultiParameter( "format", ' ' );
        formatsParam_.setPrompt( "Format(s) for SAMP transmission of table" );
        formatsParam_.setDescription( new String[] {
            "<p>Gives one or more table format types for attempting the",
            "table transmission over SAMP.",
            "If multiple values are supplied, they should be separated",
            "by spaces.",
            "Each value supplied for this parameter corresponds to a different",
            "MType which may be used for the transmission.",
            "If a single value is used, a SAMP broadcast will be used.",
            "If multiple values are used, each registered client will be",
            "interrogated to see whether it subscribes to the corresponding",
            "MTypes in order; the first one to which it is subscribed will be",
            "used to send the table.",
            "The standard options are",
            "<ul>",
            "<li><code>votable</code>:",
            "use MType <code>table.load.votable</code></li>",
            "<li><code>fits</code>:",
            "use MType <code>table.load.fits</code></li>",
            "</ul>",
            "If any other string is used which corresponds to one of",
            "STILTS's known table output formats,",
            "an attempt will be made to use an ad-hoc MType of the form",
            "<code>table.load.format</code>.",
            "</p>",
        } );
        formatsParam_.setDefault( "votable fits" );
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Sends the table to registered SAMP-aware applications",
            "subscribed to a suitable table load MType.",
            "SAMP, the Simple Application Messaging Protocol,",
            "is a tool interoperability protocol.",
            "A <em>SAMP Hub</em> must be running for this to work.",
            "</p>",
        } );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            formatsParam_,
        };
    }

    public TableConsumer createConsumer( Environment env )
            throws TaskException {
        final String[] formats = formatsParam_.stringsValue( env );
        final StarTableWriter[] writers = new StarTableWriter[ formats.length ];
        StarTableOutput sto = LineTableEnvironment.getTableOutput( env );
        for ( int i = 0; i < formats.length; i++ ) {
            try {
                writers[ i ] = sto.getHandler( formats[ i ] );
            }
            catch ( TableFormatException e ) {
            }
            if ( writers[ i ] == null ) {
                throw new ParameterValueException( formatsParam_,
                                                   "Unknown table format " +
                                                   formats[ i ] );
            }
        }
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                TableTransmitter transmitter =
                    new TableTransmitter( formats, writers, table );
                transmitter.run();
                transmitter.close();
            }
        };
    }

    /**
     * Object which can transmit a table to subscribed clients as appropriate
     * using SAMP.
     */
    private static class TableTransmitter {
        private final String[] formats_;
        private final StarTableWriter[] writers_;
        private final StarTable table_;
        private final HubConnection connection_;
        private final HttpServer httpd_;
        private final ResponseCollector responseCollector_;
        private final Map nameMap_;

        /**
         * Constructor.
         *
         * @param   formats   array of permitted table format names
         *                    (last atom of "table.load.*" MType)
         * @param   writers   array of table output handlers corresponding
         *                    to <code>formats</code> (must be same length)
         */
        TableTransmitter( String[] formats, StarTableWriter[] writers,
                          StarTable table )
                throws IOException {
            if ( formats.length != writers.length ) {
                throw new IllegalArgumentException();
            }
            formats_ = formats;
            writers_ = writers;
            table_ = table;
            nameMap_ = new HashMap();

            /* Bail out early if no hub is running. */
            if ( ! SampUtils.getLockFile().exists() ) {
                throw new IOException( "No SAMP hub is running" );
            }

            /* Register with hub. */
            connection_ = StandardClientProfile.getInstance().register();
            if ( connection_ == null ) {
                throw new IOException( "No SAMP hub running" );
            }

            /* Set up an HTTP server which will serve the table etc.
             * It would be more efficient to use the same server as the SAMP
             * classes, if those are using an HTTP server, but that is
             * contingent on the SAMP XML-RPC implementation being used,
             * which is pluggable - could be done conditionally, but 
             * leave it for now. */
            httpd_ = new HttpServer();
            httpd_.setDaemon( true );
            httpd_.start();

            /* Inform hub of metadata. */
            Metadata meta = new Metadata();
            meta.setName( "STILTS" );
            meta.setDescriptionText( "STIL Tool Set"
                                   + " - table manipulation suite" );
            meta.setDocumentationUrl( "http://www.starlink.ac.uk/stilts/" );
            URL iconUrl = Stilts.class.getResource( "images/stilts_icon.gif" );
            if ( iconUrl != null ) {
                URLMapperHandler iconHandler =
                    new URLMapperHandler( httpd_, "stilts_icon.gif", iconUrl,
                                          false );
                httpd_.addHandler( iconHandler );
                meta.setIconUrl( iconHandler.getBaseUrl().toString() );
            }
            meta.put( "author.name", "Mark Taylor" );
            meta.put( "author.affiliation",
                      "Astrophysics Group, Bristol University, UK" );
            meta.put( "author.email", "m.b.taylor@bristol.ac.uk" );
            connection_.declareMetadata( meta );

            /* Get ready to receive responses from asynchronous calls. */
            responseCollector_ = new ResponseCollector( this );
            connection_.setCallable( responseCollector_ );
        }

        /**
         * Performs the actual table send.  As currently written, this
         * method should only be used once during this object's lifetime.
         */
        public void run() throws IOException {

            /* Prepare to host dynamic table resources on our internal
             * HTTP server. */
            ResourceHandler resourceHandler =
                new ResourceHandler( httpd_, "stilts-table" );
            httpd_.addHandler( resourceHandler );

            /* Do the sends. */
            String msgTag = responseCollector_.getTag();
            Collection recipientList;

            /* If there's only one format, a broadcast (send to all) is the
             * cleanest way to do it. */
            if ( formats_.length == 1 ) {
                Message msg =
                    createSendMessage( table_, formats_[ 0 ], writers_[ 0 ],
                                       resourceHandler );
                Map recipientMap = connection_.callAll( msgTag, msg );
                recipientList = recipientMap.keySet();
                if ( logger_.isLoggable( Level.INFO ) ) {
                    StringBuffer sbuf = new StringBuffer()
                        .append( "Broadcast " )
                        .append( msg.getMType() )
                        .append( " to " );
                    for ( Iterator it = recipientList.iterator();
                          it.hasNext(); ) {
                        String clientId = (String) it.next();
                        sbuf.append( formatId( clientId ) );
                        if ( it.hasNext() ) {
                            sbuf.append( ", " );
                        }
                    }
                    logger_.info( sbuf.toString() );
                }
            }

            /* If there are multiple formats however, we need to be more
             * careful - can't broadcast each one because it risks sending
             * the same table multiple times to some of the clients. */
            else {
                recipientList = new HashSet();
                for ( int i = 0; i < formats_.length; i++ ) {
                    Message msg =
                        createSendMessage( table_, formats_[ i ], writers_[ i ],
                                           resourceHandler );
                    Set clientIdSet =
                        connection_.getSubscribedClients( msg.getMType() )
                                   .keySet();
                    for ( Iterator it = clientIdSet.iterator();
                          it.hasNext(); ) {
                        String clientId = (String) it.next();
                        if ( ! recipientList.contains( clientId ) ) {
                            connection_.call( clientId, msgTag, msg );
                            recipientList.add( clientId );
                            if ( logger_.isLoggable( Level.INFO ) ) {
                                logger_.info( "Send " + msg.getMType() + " to "
                                            + formatId( clientId ) );
                            }
                        }
                    }
                }
            }

            /* Work out who we have sent the message to, and wait until all
             * the expected responses have been received.  It is necessary
             * to wait for this, because the internal HTTP server needs to
             * keep serving requests until the recipients have finished
             * reading them.  */
            try {
                responseCollector_
                    .waitForResponses( (String[]) recipientList
                                                 .toArray( new String[ 0 ] ) );
            }
            catch ( InterruptedException e ) {
                logger_.warning( "Interrupted" );
            }
        }

        /**
         * Releases resources used by this object.
         * If this is not called, things will be tidied up at JVM shutdown.
         */
        public void close() throws IOException {
            httpd_.stop();
            connection_.unregister();
        }

        /**
         * Returns the human-readable name of a client application, if one
         * is available.
         *
         * @param  clientId   client public ID
         * @return  client name, or null
         */
        private String getClientName( String clientId ) {
            if ( ! nameMap_.containsKey( clientId ) ) {
                String name = null;
                try {
                    Metadata meta = connection_.getMetadata( clientId );
                    if ( meta != null ) {
                        name = meta.getName();
                    }
                }
                catch ( IOException e ) {
                }
                nameMap_.put( clientId, name );
            }
            return (String) nameMap_.get( clientId );
        }

        /**
         * Turns a client's public ID into a string suitable for presenting
         * to the user.
         *
         * @param  clientId  client public ID
         * @return  formatted id/name
         */
        private String formatId( String clientId ) {
            StringBuffer sbuf = new StringBuffer()
                .append( clientId );
            String name = getClientName( clientId );
            if ( name != null && name.length() > 0 ) {
                sbuf.append( " (" )
                    .append( name )
                    .append( ')' );
            }
            return sbuf.toString();
        }
    }

    /**
     * Constructs a SAMP message which can be used to send this object's
     * table in a given table format.
     *
     * @param  table  table to send
     * @param  format   table format name (last atom of "table.load.*" MType)
     * @param  writer   table output handler to generate table serialization
     * @param  resHandler  resource handler which can host dynamic resources
     * @return  message suitable for transmission
     */
    private static Message createSendMessage( final StarTable table,
                                              String format,
                                              final StarTableWriter writer,
                                              ResourceHandler resHandler ) {
        Message msg = new Message( "table.load." + format );
        ServerResource tableResource = new ServerResource() {
            public long getContentLength() {
                return -1L;
            }
            public String getContentType() {
                return writer.getMimeType();
            }
            public void writeBody( OutputStream out ) throws IOException {
                writer.writeStarTable( table, out );
            }
        };
        msg.addParam( "url", resHandler.addResource( "table." + format,
                                                     tableResource )
                                       .toString() );
        String name = table.getName();
        if ( name != null && name.length() > 0 ) {
            msg.addParam( "name", name );
        }
        return msg;
    }

    /**
     * CallableClient implementation used to receive responses from 
     * asynchronous calls.
     */
    private static class ResponseCollector implements CallableClient {

        private final TableTransmitter transmitter_;
        private String[] recipientIds_;
        private final Map responseMap_;
        private boolean interrupted_;

        /**
         * Constructor.
         */
        ResponseCollector( TableTransmitter transmitter ) {
            transmitter_ = transmitter;
            responseMap_ = new HashMap();
        }

        /**
         * Returns the message tag used to identify messages which will be
         * intercepted by this collector.
         */
        public String getTag() {
            return "table";
        }

        /**
         * Blocks until responses have been received by all of the given
         * recipients, then returns.
         *
         * @param   recipientIds  public IDs of SAMP clients from which
         *          responses are expected
         * @throws  InterruptedException  if {@link #interrupt} is called 
         *          before all responses are in
         */
        public synchronized void waitForResponses( String[] recipientIds )
                throws InterruptedException {
            Collection recipientIdList =
                new HashSet( Arrays.asList( recipientIds ) );
            while ( ! responseMap_.keySet().containsAll( recipientIdList ) &&
                    ! interrupted_ ) {
                if ( interrupted_ ) {
                    throw new InterruptedException();
                }
                wait();
            }
        }

        /**
         * Interrupts the blocking {@link #waitForResponses()} method.
         */
        synchronized void interrupt() {
            interrupted_ = true;
            notifyAll();
        }

        public void receiveCall( String senderId, String msgId, Message msg ) {
            throw new UnsupportedOperationException();
        }

        public void receiveNotification( String senderId, Message msg ) {
            throw new UnsupportedOperationException();
        }

        public synchronized void receiveResponse( String responderId,
                                                  String msgTag,
                                                  Response response ) {
            if ( msgTag.equals( getTag() ) ) {
                responseMap_.put( responderId, response );
                if ( logger_.isLoggable( Level.INFO ) ) {
                    logger_.info( "Response " + response.getStatus()
                                + " from "
                                + transmitter_.formatId( responderId ) );
                }
                notifyAll();
            }
            else {
                throw new IllegalArgumentException( "Unknown tag "
                                                  + msgTag + "??" );
            }
        }
    }
}
