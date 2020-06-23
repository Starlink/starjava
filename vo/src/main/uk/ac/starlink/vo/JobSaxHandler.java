package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.auth.AuthManager;

/**
 * SAX content handler implementation which can parse one or more
 * &lt;uws:job&gt; elements.
 *
 * @author   Mark Taylor
 * @since    4 May 2011
 */
class JobSaxHandler extends DefaultHandler {

    private final StringBuffer txtbuf_;
    private List<UwsJobInfo> jobList_;
    private List<UwsJobInfo.Parameter> parameterList_;
    private List<UwsJobInfo.Result> resultList_;
    private JobImpl job_;
    private ParameterImpl parameter_;
    private ResultImpl result_;
    private ErrorImpl error_;
    private UwsJobInfo[] jobs_;

    /**
     * Constructor.
     */
    public JobSaxHandler() {
        txtbuf_ = new StringBuffer();
    }

    public void startDocument() {
        jobList_ = new ArrayList<UwsJobInfo>();
    }

    public void endDocument() {
        jobs_ = jobList_.toArray( new JobImpl[ 0 ] );
    }

    public void startElement( String uri, String localName, String qName,
                              Attributes atts ) {
        txtbuf_.setLength( 0 );
        String tname = getTagName( uri, localName, qName );
        if ( "job".equals( tname ) ) {
            String version = atts.getValue( "version" );
            job_ = new JobImpl();
            job_.version_ = version;
        }
        else if ( job_ != null ) {
            if ( "parameters".equals( tname ) ) {
                parameterList_ = new ArrayList<UwsJobInfo.Parameter>();
            }
            else if ( "results".equals( tname ) ) {
                resultList_ = new ArrayList<UwsJobInfo.Result>();
            }
            else if ( "errorSummary".equals( tname ) ) {
                error_ = new ErrorImpl();
                error_.hasDetail_ =
                    booleanValue( atts.getValue( "hasDetail" ) );
                error_.isFatal_ =
                    ! "transient".equals( atts.getValue( "type" ) );
            }
            else if ( "parameter".equals( tname ) && parameterList_ != null ) {
                parameter_ = new ParameterImpl();
                parameter_.id_ = atts.getValue( "id" );
                parameter_.isByReference_ =
                    booleanValue( atts.getValue( "byReference" ) );
                parameter_.isPost_ =
                    booleanValue( atts.getValue( "isPost" ) );
            }
            else if ( "result".equals( tname ) && resultList_ != null ) {
                result_ = new ResultImpl();
                result_.id_ = atts.getValue( "id" );
                result_.href_ = atts.getValue( "xlink:href" );
                result_.type_ = atts.getValue( "xlink:type" );
            }
        }
    }

    public void endElement( String uri, String localName, String qName ) {
        String txt = txtbuf_.toString();
        txtbuf_.setLength( 0 );
        String tname = getTagName( uri, localName, qName );
        if ( "job".equals( tname ) ) {
            jobList_.add( job_ );
            job_ = null;
        }
        else if ( job_ != null ) {
            if ( "jobId".equals( tname ) ) {
                job_.jobId_ = txt;
            }
            else if ( "runId".equals( tname ) ) {
                job_.runId_ = txt;
            }
            else if ( "ownerId".equals( tname ) ) {
                job_.ownerId_ = txt;
            }
            else if ( "phase".equals( tname ) ) {
                job_.phase_ = txt;
            }
            else if ( "quote".equals( tname ) ) {
                job_.quote_ = txt;
            }
            else if ( "startTime".equals( tname ) ) {
                job_.startTime_ = txt;
            }
            else if ( "endTime".equals( tname ) ) {
                job_.endTime_ = txt;
            }
            else if ( "executionDuration".equals( tname ) ) {
                job_.executionDuration_ = txt;
            }
            else if ( "destruction".equals( tname ) ) {
                job_.destruction_ = txt;
            }
            else if ( "parameters".equals( tname ) ) {
                job_.parameters_ =
                    parameterList_.toArray( new UwsJobInfo.Parameter[ 0 ] );
            }
            else if ( "results".equals( tname ) ) {
                job_.results_ =
                    resultList_.toArray( new UwsJobInfo.Result[ 0 ] );
            }
            else if ( "errorSummary".equals( tname ) ) {
                job_.error_ = error_;
                error_ = null;
            }
            else if ( "parameter".equals( tname ) && parameterList_ != null ) {
                parameter_.value_ = txt;
                parameterList_.add( parameter_ );
                parameter_ = null;
            }
            else if ( "result".equals( tname ) && resultList_ != null ) {
                result_.value_ = txt;
                resultList_.add( result_ );
                result_ = null;
            }
            else if ( "message".equals( tname ) && error_ != null ) {
                error_.message_ = txt;
            }
        }
    }

    public void characters( char[] ch, int start, int length ) {
        txtbuf_.append( ch, start, length );
    }

    /**
     * Utility method to get the unadorned tag name of an element
     * without worrying about namespaces.
     *
     * @param   uri  namespace URI
     * @param   localName  local name
     * @param   qName  qualified name, if available
     */ 
    private String getTagName( String uri, String localName, String qName ) {
        return localName != null && localName.length() > 0
             ? localName
             : qName.replaceFirst( ".*:", "" );
    }

    /**
     * Reads the document at the given URL and returns an array of
     * UwsJobInfo objects for all the uws:job elements found in it.
     *
     * @param  url  location of job(s) document
     * @return   job infos for jobs found
     */
    public static UwsJobInfo[] readJobInfos( URL url )
            throws IOException, SAXException {
        SAXParserFactory spfact = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            spfact.setNamespaceAware( false );
            spfact.setValidating( false );
            parser = spfact.newSAXParser();
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "SAX trouble" ).initCause( e );
        }
        JobSaxHandler jHandler = new JobSaxHandler();
        URLConnection conn = AuthManager.getInstance().connect( url );
        if ( conn instanceof HttpURLConnection ) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            int code = hconn.getResponseCode();
            if ( code != HttpURLConnection.HTTP_OK ) {
                throw new IOException( "Job info access failure (" + code + " "
                                     + hconn.getResponseMessage() + ")" );
            }
        }
        InputStream in = new BufferedInputStream( conn.getInputStream() );
        try {
            parser.parse( in, jHandler );
            return jHandler.jobs_;
        }
        finally {
            in.close();
        }
    }

    /**
     * Utility function for parsing an xsi:boolean value.
     *
     * @param  value declared using the XML Schema data type xsi:boolean
     * @return   boolean value
     */
    private static boolean booleanValue( String txt ) {
        return "1".equals( txt ) || "true".equals( txt );
    }

    /**
     * Trivial UwsJobInfo implementation.
     */
    private static class JobImpl implements UwsJobInfo {
        String version_;
        String jobId_;
        String runId_;
        String ownerId_;
        String phase_;
        String quote_;
        String startTime_;
        String endTime_;
        String executionDuration_;
        String destruction_;
        UwsJobInfo.Parameter[] parameters_;
        UwsJobInfo.Result[] results_;
        UwsJobInfo.Error error_;

        public String getUwsVersion() {
            return version_;
        }
        public String getJobId() {
            return jobId_;
        }
        public String getRunId() {
            return runId_;
        }
        public String getOwnerId() {
            return ownerId_;
        }
        public String getPhase() {
            return phase_;
        }
        public String getQuote() {
            return quote_;
        }
        public String getStartTime() {
            return startTime_;
        }
        public String getEndTime() {
            return endTime_;
        }
        public String getExecutionDuration() {
            return executionDuration_;
        }
        public String getDestruction() {
            return destruction_;
        }
        public Parameter[] getParameters() {
            return parameters_;
        }
        public Result[] getResults() {
            return results_;
        }
        public Error getError() {
            return error_;
        }
    }

    /**
     * Trivial UwsJobInfo.Parameter implementation.
     */
    private static class ParameterImpl implements UwsJobInfo.Parameter {
        String id_;
        String value_;
        boolean isByReference_;
        boolean isPost_;

        public String getId() {
            return id_;
        }
        public String getValue() {
            return value_;
        }
        public boolean isByReference() {
            return isByReference_;
        }
        public boolean isPost() {
            return isPost_;
        }
    }

    /**
     * Trivial UwsJobInfo.Result implementation.
     */
    private static class ResultImpl implements UwsJobInfo.Result {
        String id_;
        String value_;
        String href_;
        String type_;

        public String getId() {
            return id_;
        }
        public String getValue() {
            return value_;
        }
        public String getHref() {
            return href_;
        }
        public String getType() {
            return type_;
        }
    }

    /**
     * Trivial UwsJobInfo.Error implementation.
     */
    private static class ErrorImpl implements UwsJobInfo.Error {
        boolean isFatal_;
        boolean hasDetail_;
        String message_;

        public boolean isFatal() {
            return isFatal_;
        }
        public boolean hasDetail() {
            return hasDetail_;
        }
        public String getMessage() {
            return message_;
        }
    }

    public static void main( String[] args ) throws IOException, SAXException {
        UwsJobInfo[] infos = readJobInfos( new URL( args[ 0 ] ) );
        UwsJobInfo info = infos[ 0 ];
        System.out.println( "job id: " + info.getJobId() );
        System.out.println( "start time: " + info.getStartTime() );
    }
}
