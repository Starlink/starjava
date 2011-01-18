package uk.ac.starlink.vo;

import java.io.IOException;
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
 * UWS communications are handled by a supplied UwsJob, and the
 * TAP-specific aspects of the protocol are provided by this class.
 * The supplied UwsJob should not in general be invoked by the user of
 * this class.
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
     * @param  uwsJob  UWS job representing the TAP query;
     *         
     */
    public TapQuery( UwsJob uwsJob ) {
        uwsJob_ = uwsJob;
    }

    /**
     * Returns the UWS job on which this query is based.
     *
     * @return   UWS job
     */
    public UwsJob getJob() {
        return uwsJob_;
    }

    /**
     * Runs the TAP query represented by this object's UWS job, and
     * returns a table based on the result.  In case of job failure,
     * an exception will be thrown instead.
     *
     * @param   tfact  table factory for table creation from result document
     * @param   poll   polling interval in milliseconds 
     */
    public StarTable execute( StarTableFactory tfact, long poll )
            throws IOException, InterruptedException {
        try {
            String phase = uwsJob_.runToCompletion( poll );
            if ( "COMPLETED".equals( phase ) ) {
                URL resultUrl =
                    new URL( uwsJob_.getJobUrl() + "/results/result" );
                DataSource datsrc = new URLDataSource( resultUrl );

                /* note this does take steps to follow redirects. */
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
                try {
                    errText = readErrorInfo( new URL( uwsJob_.getJobUrl()
                                                    + "/error" ) );
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
        finally {
            final String jobId = String.valueOf( uwsJob_.getJobUrl() );
            new Thread( "UWS Job deletion" ) {
                public void run() {
                    try {
                        HttpURLConnection hconn = uwsJob_.postDelete();
                        int tapDeleteCode =
                            HttpURLConnection.HTTP_SEE_OTHER; // 303
                        int code = hconn.getResponseCode();
                        if ( code == tapDeleteCode ) {
                            logger_.info( "UWS job " + jobId + " deleted" );
                        }
                        else {
                            logger_.warning( "UWS job deletion error"
                                           + " - response " + code
                                           + " not " + tapDeleteCode
                                           + " for job " + jobId );
                        }
                    }
                    catch ( IOException e ) {
                        logger_.warning( "UWS job deletion failed for "
                                       + jobId );
                    }
                }
            }.start();
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
              uwsJob_.getParameterMap().entrySet() ) {
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
     * Returns a short textual summary of this query.
     *
     * @return  query summary
     */
    public String getSummary() {
        return "TAP query";
    }

    /**
     * Returns the job list corresponding to a TAP service URL.
     *
     * @return  serviceUrl + "/async"
     */
    public static String jobListUrl( String serviceUrl ) {
        return serviceUrl + "/async";
    }

    /**
     * Returns a TapQuery given a serviceUrl and ADQL text.
     *
     * @param  serviceUrl  base service URL for TAP service (excluding "/async")
     * @param  adql   text of ADQL query
     */
    public static TapQuery createAdqlQuery( String serviceUrl, String adql ) {
        Map<String,String> tapMap = new LinkedHashMap<String,String>();
        tapMap.put( "REQUEST", "doQuery" );
        tapMap.put( "LANG", "ADQL" );
        tapMap.put( "QUERY", adql );
        return new TapQuery( new UwsJob( jobListUrl( serviceUrl ), tapMap ) );
    }

    /**
     * Reads the error text as encoded in a TAP VOTable error document.
     *
     * @return   plain text error message
     */
    private static String readErrorInfo( URL voturl ) throws IOException {
        try {
            Document errDoc = DocumentBuilderFactory.newInstance()
                             .newDocumentBuilder().parse( voturl.toString() );
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
