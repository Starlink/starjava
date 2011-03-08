package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Query using the Table Access Protocol.
 * This class is a TAP-aware decorator for {@link UwsJob}.
 *
 * @see <a href="http://www.ivoa.net/Documents/TAP/">IVOA TAP Recommendation</a>
 */
public class TapQuery {

    private final UwsJob uwsJob_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param  uwsJob  UWS job doing the work for this TAP query
     */
    public TapQuery( UwsJob uwsJob ) {
        uwsJob_ = uwsJob;
    }

    /**
     * Returns the UwsJob used for this query.
     *
     * @return  UWS job
     */
    public UwsJob getUwsJob() {
        return uwsJob_;
    }

    /**
     * Starts the job.
     */
    public void start() throws IOException {
        uwsJob_.start();
    }

    /**
     * Blocks until the TAP query represented by this object's UWS job,
     * has completed, and returns a table based on the result.
     * In case of job failure, an exception will be thrown instead.
     *
     * @param   tfact  table factory for table creation from result document
     * @param   poll   polling interval in milliseconds 
     * @return  result table
     */
    public StarTable waitForResult( StarTableFactory tfact, long poll )
            throws IOException, InterruptedException {
        URL jobUrl = uwsJob_.getJobUrl();
        try {
            String phase = uwsJob_.waitForFinish( poll );
            assert UwsStage.forPhase( phase ) == UwsStage.FINISHED;
            if ( "COMPLETED".equals( phase ) ) {
                URL resultUrl = new URL( jobUrl + "/results/result" );
                DataSource datsrc = new URLDataSource( resultUrl );

                /* Note this does take steps to follow redirects. */
                return tfact.makeStarTable( datsrc, "votable" );
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
                URL errUrl = new URL( jobUrl + "/error" );
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
     * Returns an array of metadata items which describe this query.
     *
     * @return  some TAP query metadata items
     */
    public DescribedValue[] getQueryMetadata() {
        List<DescribedValue> metaList = new ArrayList<DescribedValue>();
        Map<String,String> jobParams = uwsJob_.getParameters();
        if ( jobParams != null ) {
            for ( Map.Entry<String,String> param : jobParams.entrySet() ) {
                String pname = param.getKey();
                String pvalue = param.getValue();
                ValueInfo pinfo =
                    new DefaultValueInfo( pname, String.class,
                                          "TAP " + pname + " parameter" );
                metaList.add( new DescribedValue( pinfo, pvalue ) );
            }
        }
        return metaList.toArray( new DescribedValue[ 0 ] );
    }

    /**
     * Returns a TapQuery given a serviceUrl and ADQL text.
     *
     * @param  serviceUrl  base service URL for TAP service (excluding "/async")
     * @param  adql   text of ADQL query
     * @param  uploadMap  name->table map of tables to be uploaded to
     *                    the service for the query
     * @param  extraParams  key->value map for optional parameters;
     *                      if any of these match the names of standard
     *                      parameters (upper case) the standard values will
     *                      be overwritten, so use with care
     * @return   new TapQuery
     */
    public static TapQuery createAdqlQuery( URL serviceUrl, String adql,
                                            Map<String,StarTable> uploadMap,
                                            Map<String,String> extraParams )
            throws IOException {

        /* Prepare basic TAP ADQL parameters. */
        Map<String,String> stringMap = new LinkedHashMap<String,String>();
        stringMap.put( "REQUEST", "doQuery" );
        stringMap.put( "LANG", "ADQL" );
        stringMap.put( "QUERY", adql );
        if ( extraParams != null ) {
            stringMap.putAll( extraParams );
        }

        /* Prepare upload parameters (UPLOAD itself and any uploaded tables)
         * if required. */
        Map<String,HttpStreamParam> streamMap =
            new LinkedHashMap<String,HttpStreamParam>();
        if ( uploadMap != null && ! uploadMap.isEmpty() ) {
            StringBuffer ubuf = new StringBuffer();
            for ( Map.Entry<String,StarTable> upload : uploadMap.entrySet() ) {
                String tname = upload.getKey();
                String tlabel = toParamLabel( tname );
                StarTable table = upload.getValue();
                if ( ubuf.length() != 0 ) {
                    ubuf.append( ';' );
                }
                ubuf.append( tname )
                    .append( ',' )
                    .append( "param:" )
                    .append( tlabel );
                streamMap.put( tlabel,
                               new UploadStreamParam( upload.getValue() ) );
            }
            stringMap.put( "UPLOAD", ubuf.toString() );
        }

        /* Attempt to create a UWS job corresponding to the query. */
        UwsJob uwsJob;
        try {
            uwsJob = UwsJob.createJob( serviceUrl + "/async",
                                       stringMap, streamMap );
        }
        catch ( UwsJob.UnexpectedResponseException e ) {
            throw asIOException( e );
        }
        uwsJob.setParameters( stringMap );
        return new TapQuery( uwsJob );
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
     * Returns a short textual summary of an ADQL query on a given TAP service.
     *
     * @param  serviceUrl  base service URL for TAP service (excluding "/async")
     * @param  adql   text of ADQL query
     * @return  query summary
     */
    static String summarizeAdqlQuery( URL serviceUrl, String adql ) {
        return "TAP query";
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
     * HttpStreamParam implementation for uploading tables.
     */
    private static class UploadStreamParam implements HttpStreamParam {
        private final StarTable table_;

        /**
         * Constructor.
         *
         * @param   table  table to upload
         */
        public UploadStreamParam( StarTable table ) {
            table_ = table;
        }

        public Map<String,String> getHttpHeaders() {
            Map<String,String> map = new LinkedHashMap<String,String>();
            map.put( "Content-Type", "application/x-votable+xml" );
            return map;
        }

        public void writeContent( OutputStream out ) throws IOException {
            VOTableWriter vowriter =
                new VOTableWriter( DataFormat.TABLEDATA, true );
            vowriter.setVotableVersion( "1.2" );
            vowriter.writeStarTable( table_, out );
        }
    }
}
