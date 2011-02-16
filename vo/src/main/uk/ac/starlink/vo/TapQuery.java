package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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

/**
 * Query using the Table Access Protocol.
 * The work is done by a {@link UwsJob} instance.
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
     * @param  serviceUrl  base URL for TAP service (without "/async")
     * @param  paramMap   parameters to pass for TAP request
     */
    public TapQuery( URL serviceUrl, Map<String,String> paramMap )
            throws IOException {
        try {
            uwsJob_ = UwsJob.createJob( serviceUrl + "/async", paramMap );
        }
        catch ( UwsJob.UnexpectedResponseException e ) {
            String errMsg = null;
            try {
                errMsg = readErrorInfo( e.getConnection().getInputStream() );
            }
            catch ( IOException e2 ) {
            }
            if ( errMsg == null || errMsg.length() == 0 ) {
                errMsg = e.getMessage();
            }
            throw (IOException) new IOException( errMsg ).initCause( e );
        }
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
     * Runs the TAP query represented by this object's UWS job, and
     * returns a table based on the result.  In case of job failure,
     * an exception will be thrown instead.
     *
     * @param   tfact  table factory for table creation from result document
     * @param   poll   polling interval in milliseconds 
     * @param   delete  true iff job is to be deleted after table is obtained
     * @return  result table
     */
    public StarTable execute( StarTableFactory tfact, long poll,
                              boolean delete )
            throws IOException, InterruptedException {
        final URL jobUrl = uwsJob_.getJobUrl();
        try {
            String phase = uwsJob_.runToCompletion( poll );
            if ( "COMPLETED".equals( phase ) ) {
                URL resultUrl = new URL( jobUrl + "/results/result" );
                DataSource datsrc = new URLDataSource( resultUrl );

                /* Note this does take steps to follow redirects. */
                return tfact.makeStarTable( datsrc, "votable" );
            }
            else if ( "ABORTED".equals( phase ) ||
                      "HELD".equals( phase ) ) {
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
            String errMsg = null;
            try {
                errMsg = readErrorInfo( e.getConnection().getInputStream() );
            }
            catch ( IOException e2 ) {
            }
            if ( errMsg == null || errMsg.length() == 0 ) {
                errMsg = e.getMessage();
            }
            throw (IOException) new IOException( errMsg ).initCause( e );
        }
        finally {
            if ( delete ) {
                new Thread( "UWS Job deletion" ) {
                    public void run() {
                        try {
                            HttpURLConnection hconn = uwsJob_.postDelete();
                            int tapDeleteCode =
                                HttpURLConnection.HTTP_SEE_OTHER; // 303
                            int code = hconn.getResponseCode();
                            if ( code == tapDeleteCode ) {
                                logger_.info( "UWS job " + jobUrl
                                            + " deleted" );
                            }
                            else {
                                logger_.warning( "UWS job deletion error"
                                               + " - response " + code
                                               + " not " + tapDeleteCode
                                               + " for job " + jobUrl );
                            }
                        }
                        catch ( IOException e ) {
                            logger_.warning( "UWS job deletion failed for "
                                           + jobUrl );
                        }
                    }
                }.start();
            }
        }
    }

    /**
     * Returns an array of metadata items which describe this query.
     *
     * @return  some TAP query metadata items
     */
    public DescribedValue[] getQueryMetadata() {
        List<DescribedValue> metaList = new ArrayList<DescribedValue>();
        for ( Map.Entry<String,String> param :
              uwsJob_.getParameters().entrySet() ) {
            String pname = param.getKey();
            String pvalue = param.getValue();
            ValueInfo pinfo =
                new DefaultValueInfo( pname, String.class,
                                      "TAP " + pname + " parameter" );
            metaList.add( new DescribedValue( pinfo, pvalue ) );
        }
        return metaList.toArray( new DescribedValue[ 0 ] );
    }

    /**
     * Returns a TapQuery given a serviceUrl and ADQL text.
     *
     * @param  serviceUrl  base service URL for TAP service (excluding "/async")
     * @param  adql   text of ADQL query
     * @return   new TapQuery
     */
    public static TapQuery createAdqlQuery( URL serviceUrl, String adql )
            throws IOException {
        Map<String,String> tapMap = new LinkedHashMap<String,String>();
        tapMap.put( "REQUEST", "doQuery" );
        tapMap.put( "LANG", "ADQL" );
        tapMap.put( "QUERY", adql );
        return new TapQuery( serviceUrl, tapMap );
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
}
