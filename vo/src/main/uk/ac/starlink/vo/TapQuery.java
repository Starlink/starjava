package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.storage.LimitByteStore;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Represents a query to be made to a Table Access Protocol service.
 * This class aggregates the information which needs to be sent to
 * make such a query and provides methods to submit the query
 * synchronously or asynchronously.
 * It also contains some static methods to perform other TAP-related operations.
 *
 * @author   Mark Taylor
 * @since    8 Apr 2011
 * @see <a href="http://www.ivoa.net/Documents/TAP/">IVOA TAP Recommendation</a>
 */
public class TapQuery {

    private final URL serviceUrl_;
    private final String adql_;
    private final Map<String,String> stringMap_;
    private final Map<String,HttpStreamParam> streamMap_;
    private final long uploadLimit_;
    private final StarTableFactory tfact_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.  May throw an IOException if the tables specified for
     * upload exceed the stated upload limit.
     *
     * @param  serviceUrl  base service URL for TAP service
     *                     (excluding "/[a]sync")
     * @param  adql   text of ADQL query
     * @param  extraParams  key->value map for optional parameters;
     *                      if any of these match the names of standard
     *                      parameters (upper case) the standard values will
     *                      be overwritten, so use with care
     * @param  uploadMap  name->table map of tables to be uploaded to
     *                    the service for the query
     * @param  uploadLimit  maximum number of bytes that may be uploaded;
     *                      if negative, no limit is applied
     * @param  tfact   table factory
     */
    public TapQuery( URL serviceUrl, String adql,
                     Map<String,String> extraParams,
                     Map<String,StarTable> uploadMap,
                     long uploadLimit, StarTableFactory tfact )
            throws IOException {
        serviceUrl_ = serviceUrl;
        adql_ = adql;
        uploadLimit_ = uploadLimit;
        tfact_ = tfact;

        /* Prepare the map of string parameters. */
        stringMap_ = new LinkedHashMap<String,String>();
        stringMap_.put( "REQUEST", "doQuery" );
        stringMap_.put( "LANG", "ADQL" );
        stringMap_.put( "QUERY", adql );
        if ( extraParams != null ) {
            stringMap_.putAll( extraParams );
        }

        /* Prepare the map of streamed parameters, required for table uploads.
         * This also affects the string parameter map. */
        streamMap_ = new LinkedHashMap<String,HttpStreamParam>();
        StringBuffer ubuf = new StringBuffer();
        if ( uploadMap != null ) {
            for ( Map.Entry<String,StarTable> upload : uploadMap.entrySet() ) {
                String tname = upload.getKey();
                String tlabel = toParamLabel( tname );
                StarTable table = upload.getValue();
                if ( ubuf.length() != 0 ) {
                    ubuf.append( ';' );
                }
                ubuf.append( tname )
                    .append( ',' )
                    .append( tlabel );
                HttpStreamParam streamParam =
                    createUploadStreamParam( table, uploadLimit,
                                             tfact_.getStoragePolicy() );
                streamMap_.put( tlabel, streamParam );
            }
        }
        if ( ubuf.length() > 0 ) {
            stringMap_.put( "UPLOAD", ubuf.toString() );
        }
    }

    /**
     * Executes this query synchronously and returns the resulting table.
     *
     * @return   result table
     */
    public StarTable executeSync() throws IOException {
        HttpURLConnection hconn =
            UwsJob.postForm( new URL( serviceUrl_ + "/sync" ),
                             stringMap_, streamMap_ );
        InputStream in = hconn.getInputStream();
        TableBuilder votBuilder = tfact_.getTableBuilder( "votable" );
        if ( votBuilder == null ) {
            votBuilder = new VOTableBuilder();
        }
        try {
            return tfact_.makeStarTable( in, votBuilder );
        }
        finally {
            in.close();
        }
    }

    /**
     * Submits this query asynchronously and returns the corresponding UWS job.
     * The job is not started.
     *
     * @return   new UWS job for this query
     */
    public UwsJob submitAsync() throws IOException {
        try {
            return UwsJob.createJob( serviceUrl_ + "/async",
                                     stringMap_, streamMap_ );
        }
        catch ( UwsJob.UnexpectedResponseException e ) {
            throw asIOException( e );
        }
    }

    /**
     * Blocks until the TAP query represented by a given UWS job has completed,
     * then returns a table based on the result.
     * In case of job failure, an exception will be thrown instead.
     *
     * @param  uwsJob  started UWS job representing an async TAP query
     * @param  tfact  table factory
     * @param  pollMillis  polling interval in milliseconds
     * @return  result table
     */
    public static StarTable waitForResult( UwsJob uwsJob,
                                           StarTableFactory tfact,
                                           long pollMillis )
            throws IOException, InterruptedException {
        try {
            String phase = uwsJob.waitForFinish( pollMillis );
            assert UwsStage.forPhase( phase ) == UwsStage.FINISHED;
            if ( "COMPLETED".equals( phase ) ) {
                return getResult( uwsJob, tfact );
            }
            else if ( "ABORTED".equals( phase ) ) {
                throw new IOException( "TAP query did not complete ("
                                     + phase + ")" );
            }
            else if ( "ERROR".equals( phase ) ) {
                String errText = null;

                /* We read the error text from the /error job resource,
                 * which TAP says has to be a VOTable.  This is always
                 * present.  In some cases it might be better to read
                 * from the errorSummary element of the job resource
                 * (e.g. xpath /job/errorSummary/message/text()), since
                 * that might contain a shorter and more human-friendly
                 * message, but (a) many/most TAP servers do not supply
                 * this at time of writing and (b) it's difficult to
                 * navigate the UWS document in the presence of namespaces
                 * which often refer to different/older versions of the
                 * UWS protocol. */
                URL errUrl = new URL( uwsJob.getJobUrl() + "/error" );
                logger_.info( "Read error VOTable from " + errUrl );
                try {
                    errText = readErrorInfo( errUrl.openStream() );
                }
                catch ( Throwable e ) {
                    throw (IOException)
                          new IOException( "TAP Execution error"
                                         + " (can't get detail)" )
                         .initCause( e );
                }
                throw new IOException( "TAP execution error: " + errText );
            }
            else {
                throw new IOException( "Unknown UWS execution phase " + phase );
            }
        }
        catch ( UwsJob.UnexpectedResponseException e ) {
            throw asIOException( e );
        }
    }

    /**
     * Reads and returns the table that resulted from a successful TAP query,
     * represented by a given UWS job.
     * If the job has not reached COMPLETED phase, an IOException will result.
     *
     * @param  uwsJob  successfully completed UWS job representing 
     *                 an async TAP query
     * @param  tfact  table factory
     * @return   the result of reading the TAP result as a table
     */
    public static StarTable getResult( UwsJob uwsJob, StarTableFactory tfact )
            throws IOException {
        URL resultUrl = new URL( uwsJob.getJobUrl() + "/results/result" );
        DataSource datsrc = new URLDataSource( resultUrl );

        /* Note this does take steps to follow redirects. */
        return tfact.makeStarTable( datsrc, "votable" );
    }

    /**
     * Reads table metadata from a TAP service.
     *
     * @param  serviceUrl  base TAP service URL
     * @return   table metadata
     */
    public static TableMeta[] readTableMetadata( URL serviceUrl )
            throws IOException, SAXException {
        URL turl = new URL( serviceUrl + "/tables" );
        logger_.info( "Reading table metadata from " + turl );
        return TableSetSaxHandler.readTableSet( turl );
    }

    /**
     * Reads capability information from a TAP service.
     *
     * @param  serviceUrl  base TAP service URL
     * @return   capability information
     */
    public static TapCapability readTapCapability( URL serviceUrl )
            throws IOException, SAXException {
        URL curl = new URL( serviceUrl + "/capabilities" );
        logger_.info( "Reading capability metadata from " + curl );
        return TapCapability.readTapCapability( curl );
    }

    /**
     * Reads the error text as encoded in a TAP VOTable error document.
     *
     * @return   plain text error message
     */
    private static String readErrorInfo( InputStream in ) throws IOException {
        try {
            Document errDoc = DocumentBuilderFactory.newInstance()
                             .newDocumentBuilder()
                             .parse( new BufferedInputStream( in ) );
            XPath xpath = XPathFactory.newInstance().newXPath();
            return xpath.evaluate( "VOTABLE/RESOURCE[@type='results']"
                                 + "/INFO[@name='QUERY_STATUS']/text()",
                                   errDoc );
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "Error doc parse failure" )
                               .initCause( e );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( "Error doc parse failure" )
                               .initCause( e );
        }
        catch ( XPathException e ) {
            throw (IOException) new IOException( "Error doc parse failure" )
                               .initCause( e );
        }
    }

    /**
     * Takes an UnexpectedResponseException generated in response to a TAP
     * query and turns it into an IOException with a helpful error message.
     * The main thing to do is to take the body content, which ought(?) to
     * be an error-bearing VOTable, and extract the error text.
     *
     * @param  error  input error
     * @return   better error
     */
    private static IOException
            asIOException( UwsJob.UnexpectedResponseException error ) {
        HttpURLConnection hconn = error.getConnection();

        /* Get an input stream for the response body.  Depending on the
         * response code HttpURLConnection may make this avaiable as
         * the input or error stream. */
        InputStream bodyIn;
        try {
            bodyIn = hconn.getInputStream();
        }
        catch ( IOException e ) {
            bodyIn = hconn.getErrorStream();
        }

        /* Try to turn the response body into an intelligible error
         * message */
        String errMsg = null;
        if ( bodyIn != null ) {
            try {
                errMsg = readErrorInfo( bodyIn );
            }
            catch ( IOException e ) {
            }
        }

        /* Fall back to the cause's message if necessary. */
        if ( errMsg == null || errMsg.length() == 0 ) {
            errMsg = error.getMessage();
        }

        /* Return an exception with the correct type, message and cause. */
        return (IOException) new IOException( errMsg ).initCause( error );
    }

    /**
     * Returns a label to be used for identifying a table which has been
     * assigned a given name.  This function could return the input string,
     * but doesn't just to make it clearer what's going on with the POSTed
     * parameters.
     *
     * @param   assigned table name
     * @return  label for identifying table in posted query
     */
    private static String toParamLabel( String tname ) {
        return "upload_" + tname.replaceAll( "[^a-zA-Z0-9]", "" );
    }

    /**
     * Creates a new stream parameter based on a given table.
     *
     * @param   table  table to upload
     * @param   uploadLimit  maximum number of bytes permitted; -1 if no limit
     * @param   storage  storage policy for buffering bytes
     * @return  stream parameter
     */
    private static HttpStreamParam
                   createUploadStreamParam( final StarTable table,
                                            long uploadLimit,
                                            StoragePolicy storage )
            throws IOException {
        final Map<String,String> headerMap = new LinkedHashMap<String,String>();
        final VOTableWriter vowriter =
            new VOTableWriter( DataFormat.BINARY, true );
        vowriter.setVotableVersion( "1.2" );
        headerMap.put( "Content-Type", "application/x-votable+xml" );
        if ( uploadLimit < 0 ) {
            return new HttpStreamParam() {
                public Map<String,String> getHttpHeaders() {
                    return headerMap;
                }
                public void writeContent( OutputStream out )
                        throws IOException {
                    vowriter.writeStarTable( table, out );
                }
                public long getContentLength() {
                    return -1;
                }
            };
        }
        else {
            final ByteStore hbuf =
                new LimitByteStore( storage.makeByteStore(), uploadLimit );
            OutputStream tout = hbuf.getOutputStream();
            vowriter.writeStarTable( table, tout );
            tout.close();
            return new HttpStreamParam() {
                public Map<String,String> getHttpHeaders() {
                    return headerMap;
                }
                public void writeContent( OutputStream out )
                        throws IOException {
                    hbuf.copy( out );
                }
                public long getContentLength() {
                    return hbuf.getLength();
                }
            };
        }
    }
}
