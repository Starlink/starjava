package uk.ac.starlink.ttools.taplint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.ByteList;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.UwsJob;
import uk.ac.starlink.vo.UwsJobInfo;
import uk.ac.starlink.vo.UwsStage;

/**
 * TapLint stage which submits and manipulates UWS jobs, mostly to check
 * that the UWS operations are performing correctly.
 *
 * @author   Mark Taylor
 * @since    24 Jun 2011
 */
public class JobStage implements Stage {

    private final MetadataHolder metaHolder_;
    private final long pollMillis_;

    // This expression pinched at random without testing from
    // "http://www.pelagodesign.com/blog/2009/05/20/" +
    // "iso-8601-date-validation-that-doesnt-suck/"
    private final static Pattern ISO8601_REGEX = Pattern.compile(
        "^([\\+-]?\\d{4}(?!\\d{2}\\b))"
      + "((-?)((0[1-9]|1[0-2])(\\3([12]\\d|0[1-9]|3[01]))?"
      + "|W([0-4]\\d|5[0-2])(-?[1-7])?"
      + "|(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))"
      + "([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)"
      + "([\\.,]\\d+(?!:))?)?(\\17[0-5]\\d([\\.,]\\d+)?)?"
      + "([zZ]|([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?$"
    );

    /**
     * Constructor.
     *
     * @param  metaHolder  supplies table metadata at run time so we know
     *                   what to query
     * @param  pollMillis  number of milliseconds between polling attempts
     *         when waiting for a normal job to complete
     */
    public JobStage( MetadataHolder metaHolder, long pollMillis ) {
        metaHolder_ = metaHolder;
        pollMillis_ = pollMillis;
    }

    public String getDescription() {
        return "Test asynchronous UWS/TAP behaviour";
    }

    public void run( Reporter reporter, URL serviceUrl ) {
        TableMeta[] tmetas = metaHolder_.getTableMetadata();
        if ( tmetas == null || tmetas.length == 0 ) {
            reporter.report( ReportType.FAILURE, "NOTM",
                             "No table metadata available "
                           + "(earlier stages failed/skipped? "
                           + " - will not attempt UWS tests" );
            return;
        }
        new UwsRunner( reporter, serviceUrl, tmetas[ 0 ], pollMillis_ ).run();
    }

    /**
     * Class which does the work for this stage.
     */
    private static class UwsRunner implements Runnable {
        private final Reporter reporter_;
        private final URL serviceUrl_;
        private final TableMeta tmeta_;
        private final long poll_;
        private final String shortAdql_;
        private final String runId1_;
        private final String runId2_;
  
        /**
         * Constructor.
         *
         * @param  reporter  destination for validation messages
         * @param  serviceUrl  base URL of TAP service
         * @param  tmeta  example table metadata
         * @param  poll  number of milliseconds between polls when waiting
         */
        UwsRunner( Reporter reporter, URL serviceUrl, TableMeta tmeta,
                   long poll ) {
            reporter_ = reporter;
            serviceUrl_ = serviceUrl;
            tmeta_ = tmeta;
            poll_ = poll;
            shortAdql_ = "SELECT TOP 100 * FROM " + tmeta.getName();
            runId1_ = "TAPLINT-001";
            runId2_ = "TAPLINT-002";
        }

        /**
         * Invokes subordinate checking tasks.
         */
        public void run() {
            checkCreateAbortDelete( shortAdql_ );
            checkCreateDelete( shortAdql_ );
            checkCreateRun( shortAdql_ );
        }

        /**
         * Runs sequence which creates, aborts and then deletes a job.
         *
         * @param  adql  adql text for query
         */
        private void checkCreateAbortDelete( String adql ) {
            UwsJob job = createJob( adql );
            if ( job == null ) {
                return;
            }
            URL jobUrl = job.getJobUrl();
            checkPhase( job, "PENDING" );
            checkParameter( job, "REQUEST", "doQuery", true );
            checkParameter( job, "RUNID", runId1_, false );
            if ( postParameter( job, "runId", runId2_ ) ) {
                checkParameter( job, "RUNID", runId2_, false );
            }
            if ( postPhase( job, "ABORT" ) ) {
                checkPhase( job, "ABORTED" );
            }
            // should check 303 response here really
            if ( postKeyValue( job, "", "ACTION", "DELETE" ) ) {
           
                checkDeleted( job );
            }
        }

        /**
         * Runs sequence which creates and then deletes a job.
         *
         * @param  adql  adql text for query
         */
        private void checkCreateDelete( String adql ) {
            UwsJob job = createJob( adql );
            if ( job == null ) {
                return;
            }
            URL jobUrl = job.getJobUrl();
            checkPhase( job, "PENDING" );
            URLConnection conn;
            try {
                conn = jobUrl.openConnection();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "HTOF",
                                  "Failed to contact " + jobUrl, e );
                return;
            }
            if ( ! ( conn instanceof HttpURLConnection ) ) {
                reporter_.report( ReportType.ERROR, "NOHT",
                                  "Job url " + jobUrl + " not HTTP?" );
                return;
            }
            HttpURLConnection hconn = (HttpURLConnection) conn;
            int response;
            try {
                hconn.setRequestMethod( "DELETE" );
                hconn.setInstanceFollowRedirects( false );
                hconn.connect();
                response = hconn.getResponseCode();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "HTDE",
                                  "Failed to perform HTTP DELETE to " + jobUrl,
                                  e );
                return;
            }
            checkDeleted( job ); 
        }

        /**
         * Runs sequence which creates and then runs a job.
         *
         * @param  adql  adql text for query
         */
        private void checkCreateRun( String adql ) {
            UwsJob job = createJob( adql );
            if ( job == null ) {
                return;
            }
            URL jobUrl = job.getJobUrl();
            checkEndpoints( job );
            checkPhase( job, "PENDING" );
            if ( ! postPhase( job, "RUN" ) ) {
                return;
            }
            String phase;
            try {
                job.readPhase();
                phase = job.getLastPhase();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "RDPH",
                                  "Can't read phase for job " + jobUrl, e );
                return;
            }
            if ( ! new HashSet( Arrays.asList( new String[] {
                                    "QUEUED", "EXECUTING", "SUSPENDED",
                                    "ERROR", "COMPLETED",
                                } ) ).contains( phase ) ) {
                String msg = new StringBuilder()
                   .append( "Incorrect phase " )
                   .append( phase )
                   .append( " for started job " )
                   .append( jobUrl )
                   .toString();
                reporter_.report( ReportType.ERROR, "BAPH", msg );
            }
            if ( UwsStage.FINISHED == UwsStage.forPhase( phase ) ) {
                reporter_.report( ReportType.INFO, "JOFI",
                                  "Job completed immediately - "
                                + "can't test phase progression" );
                delete( job );
                return;
            }
            waitForFinish( job );
        }

        /**
         * Checks that a job has a given phase.
         *
         * @param  job  job to check
         * @param  mustPhase  asserted phase string
         */
        private void checkPhase( UwsJob job, String mustPhase ) {
            URL phaseUrl = resourceUrl( job, "/phase" );
            String resourcePhase = readTextContent( phaseUrl, true );
            UwsJobInfo jobInfo = readJobInfo( job );
            String infoPhase = jobInfo == null ? null : jobInfo.getPhase();
            String phase = resourcePhase != null ? resourcePhase : infoPhase;
            if ( phase != null ) {
                if ( ! mustPhase.equals( phase ) ) {
                    String msg = new StringBuilder()
                        .append( "Phase " )
                        .append( phase )
                        .append( " != " )
                        .append( mustPhase )
                        .toString();
                    reporter_.report( ReportType.ERROR, "PHUR", msg );
                }
            }
            if ( infoPhase != null && resourcePhase != null &&
                 ! ( infoPhase.equals( resourcePhase ) ) ) {
                String msg = new StringBuilder()
                            .append( "Phase mismatch between job info " )
                            .append( "and /phase URL " )
                            .append( '(' )
                            .append( infoPhase )
                            .append( " != " )
                            .append( resourcePhase )
                            .append( ')' )
                            .toString();
                reporter_.report( ReportType.ERROR, "JDPH", msg );
            }
        }

        /**
         * Checks that a job parameter has a given value.
         *
         * @param  job  job to check
         * @param  name job parameter name
         * @param  value  asserted parameter value
         * @param  mandatory  true iff parameter must be supported by TAP
         *                    implementation
         */
        private void checkParameter( UwsJob job, final String name,
                                     final String mustValue,
                                     boolean mandatory ) {
            UwsJobInfo jobInfo = readJobInfo( job );
            if ( jobInfo == null ) {
                return;
            }
            UwsJobInfo.Parameter param =
                getParamMap( jobInfo ).get( name.toUpperCase() );
            String actualValue = param == null ? null : param.getValue();
            if ( mustValue == null ) { 
                if ( actualValue == null ) {
                    // ok 
                } 
                else {
                    String msg = new StringBuilder()
                       .append( "Parameter " )
                       .append( name )
                       .append( " has value " )
                       .append( actualValue )
                       .append( " not blank in job document" ) 
                       .toString();
                    reporter_.report( ReportType.ERROR, "PANZ", msg );
                }
            }
            else if ( actualValue == null && ! mandatory ) {
                // ok
            }
            else if ( ! mustValue.equals( actualValue ) ) {
                String msg = new StringBuilder()
                    .append( "Parameter " )
                    .append( name )
                    .append( " has value " )
                    .append( actualValue )
                    .append( " not " )
                    .append( mustValue )
                    .append( " in job document" )
                    .toString();
                reporter_.report( ReportType.ERROR, "PAMM", msg );
            }
        }

        /**
         * Perform checks of resource declared types and contents for various
         * job sub-resources.
         *
         * @param  job  job to check
         */
        private void checkEndpoints( UwsJob job ) {

            /* Check and read the job document. */
            URL jobUrl = job.getJobUrl();
            readContent( jobUrl, "text/xml", true );
            UwsJobInfo jobInfo;
            try {
                jobInfo = job.readJob();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "JDIO",
                                  "Error reading job document " + jobUrl, e );
                return;
            }
            catch ( SAXException e ) {
                reporter_.report( ReportType.ERROR, "JDSX",
                                  "Error parsing job document " + jobUrl, e );
                return;
            }
            if ( jobInfo == null ) {
                reporter_.report( ReportType.ERROR, "JDNO",
                                  "No job document found " + jobUrl );
                return;
            }

            /* Check the job ID is consistent between the job URL and
             * job info content. */
            if ( ! jobUrl.toString().endsWith( "/" + jobInfo.getJobId() ) ) {
                String msg = new StringBuilder()
                   .append( "Job ID mismatch; " )
                   .append( jobInfo.getJobId() )
                   .append( " is not final path element of " )
                   .append( jobUrl )
                   .toString();
                reporter_.report( ReportType.ERROR, "JDID", msg );
            }

            /* Check the type of the quote resource. */
            URL quoteUrl = resourceUrl( job, "/quote" );
            String quote = readTextContent( quoteUrl, true );
          
            /* Check the type and content of the executionduration, and
             * whether it matches that in the job document. */
            URL durationUrl = resourceUrl( job, "/executionduration" );
            String duration = readTextContent( durationUrl, true );
            checkInt( durationUrl, duration );
            if ( ! equals( duration, jobInfo.getExecutionDuration() ) ) {
                String msg = new StringBuilder()
                   .append( "Execution duration mismatch between job info " )
                   .append( "and /executionduration URL " )
                   .append( '(' )
                   .append( jobInfo.getExecutionDuration() )
                   .append( " != " )
                   .append( duration )
                   .append( ')' )
                   .toString();
                reporter_.report( ReportType.ERROR, "JDED", msg );
            }

            /* Check the type and content of the destruction time, and
             * whether it matches that in the job document. */
            URL destructUrl = resourceUrl( job, "/destruction" );
            String destruct = readTextContent( destructUrl, true );
            checkDateTime( destructUrl, destruct );
            if ( ! equals( destruct, jobInfo.getDestruction() ) ) {
                String msg = new StringBuilder()
                   .append( "Destruction time mismatch between job info " )
                   .append( "and /destruction URL " )
                   .append( '(' )
                   .append( jobInfo.getDestruction() )
                   .append( " != " )
                   .append( destruct )
                   .append( ')' )
                   .toString();
                reporter_.report( ReportType.ERROR, "JDDE", msg );
            }
        }

        /**
         * Checks that a job has been deleted, and no longer exists.
         *
         * @param  job  job to check
         */
        private void checkDeleted( UwsJob job ) {
            URL jobUrl = job.getJobUrl();
            URLConnection conn;
            try {
                conn = jobUrl.openConnection();
                conn.connect();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "DEOP",
                                  "Can't open connection to " + jobUrl, e );
                return;
            }
            if ( conn instanceof HttpURLConnection ) {
                int code;
                try {
                    code = ((HttpURLConnection) conn).getResponseCode();
                }
                catch ( IOException e ) {
                    reporter_.report( ReportType.ERROR, "DEHT",
                                      "Bad HTTP connection to " + jobUrl, e );
                    return;
                }
                if ( code != 404 ) {
                    String msg = new StringBuilder()
                        .append( "Deleted job " )
                        .append( "gives HTTP response " )
                        .append( code )
                        .append( " not 404" )
                        .append( " for " )
                        .append( jobUrl )
                        .toString();
                    reporter_.report( ReportType.ERROR, "DENO", msg );
                }
            }
            else {
                reporter_.report( ReportType.ERROR, "NOHT",
                                  "Job " + jobUrl + " not HTTP?" );
            }
        }

        /**
         * Returns the URL of a job subresource, supressing exceptions.
         *
         * @param  job   job object
         * @param  subResource   resource subpath, starting "/"
         * @return   resource URL, or null in the unlikely event of failure
         */
        private URL resourceUrl( UwsJob job, String subResource ) {
            String urlStr = job.getJobUrl() + subResource;
            try {
                return new URL( urlStr );
            }
            catch ( MalformedURLException e ) {
                reporter_.report( ReportType.FAILURE, "MURL",
                                  "Bad URL " + urlStr + "??", e );
                return null;
            }
        }

        /**
         * Sets a parameter value for a UWS job.
         *
         * @param   job   UWS job
         * @param   name  parameter name
         * @param   value  parameter value
         * @return   true iff parameter was set successfully
         */
        private boolean postParameter( UwsJob job, String name, String value ) {
            return postKeyValue( job, "/parameters", name, value );
        }

        /**
         * Posts the phase for a UWS job.
         *
         * @param  job  UWS job
         * @param  phase  UWS phase string
         * @return   true iff phase was posted successfully
         */
        private boolean postPhase( UwsJob job, String phase ) {
            return postKeyValue( job, "/phase", "PHASE", phase );
        }

        /**
         * POSTs a key=value pair to a resource URL relating to a UWS job.
         *
         * @param  job  UWS job
         * @param  subResource  relative path of resource within job
         *                      (include leading "/")
         * @param  key    key string
         * @param  value  value string
         * @return   true iff POST completed successfully
         */
        private boolean postKeyValue( UwsJob job, String subResource,
                                      String key, String value ) {
            URL url;
            try {
                url = new URL( job.getJobUrl() + subResource );
            }
            catch ( MalformedURLException e ) {
                throw (AssertionError) new AssertionError().initCause( e );
            }
            Map<String,String> map = new HashMap<String,String>();
            map.put( key, value );
            int code;
            String responseMsg;
            try {
                HttpURLConnection conn = UwsJob.postUnipartForm( url, map );
                code = conn.getResponseCode();
                responseMsg = conn.getResponseMessage();
            }
            catch ( IOException e ) {
                String msg = new StringBuilder()
                   .append( "Failed to POST parameter " )
                   .append( key )
                   .append( "=" )
                   .append( value )
                   .append( " to " )
                   .append( url )
                   .toString();
                reporter_.report( ReportType.ERROR, "POER", msg, e );
                return false;
            }
            if ( code >= 400 ) {
                String msg = new StringBuilder()
                   .append( "Error response " )
                   .append( code )
                   .append( " " )
                   .append( responseMsg )
                   .append( " for POST " )
                   .append( key )
                   .append( "=" )
                   .append( value )
                   .append( " to " )
                   .append( url )
                   .toString();
                reporter_.report( ReportType.ERROR, "PORE", msg );
                return false;
            }
            String msg = new StringBuilder()
               .append( "POSTed " )
               .append( key )
               .append( "=" )
               .append( value )
               .append( " to " )
               .append( url )
               .toString();
            reporter_.report( ReportType.INFO, "POPA", msg );
            return true;
        }

        /**
         * Deletes a job.
         *
         * @param  job  UWS job
         */
        private void delete( UwsJob job ) {
            try {
                job.postDelete();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "DENO",
                                  "Failed to delete job " + job.getJobUrl(),
                                  e );
                return;
            }
            checkDeleted( job );
        }

        /**
         * Waits for a running job to complete.
         *
         * @param  job  UWS job
         */
        private void waitForFinish( UwsJob job ) {
            URL jobUrl = job.getJobUrl();
            while ( UwsStage.forPhase( job.getLastPhase() )
                    != UwsStage.FINISHED ) {
                String phase = job.getLastPhase();
                UwsStage stage = UwsStage.forPhase( phase );
                switch ( stage ) {
                    case UNSTARTED:
                        reporter_.report( ReportType.ERROR, "RUPH",
                                          "Incorrect phase " + phase
                                        + " for started job " + jobUrl );
                        return;
                    case ILLEGAL:
                    case UNKNOWN:
                        reporter_.report( ReportType.ERROR, "BAPH",
                                          "Bad phase " + phase
                                        + " for job " + jobUrl );
                        return;
                    case RUNNING:
                        try {
                            Thread.sleep( poll_ );
                        }
                        catch ( InterruptedException e ) {
                            reporter_.report( ReportType.FAILURE, "INTR",
                                              "Interrupted??" );
                            return;
                        }
                        break;
                    case FINISHED:
                        break;
                    default:
                        throw new AssertionError();
                }
                try {
                    job.readPhase();
                }
                catch ( IOException e ) {
                    reporter_.report( ReportType.ERROR, "RDPH",
                                      "Can't read phase for job " + jobUrl );
                    return;
                }
            }
        }

        /**
         * Constructs a name->param map from a UwsJobInfo object.
         *
         * @param   jobInfo  job metadata structure describing a UWS job
         * @return  name->param  map
         */
        private Map<String,UwsJobInfo.Parameter>
                getParamMap( UwsJobInfo jobInfo ) {
            Map<String,UwsJobInfo.Parameter> paramMap =
                new LinkedHashMap<String,UwsJobInfo.Parameter>();
            if ( jobInfo != null && jobInfo.getParameters() != null ) {
                UwsJobInfo.Parameter[] params = jobInfo.getParameters();
                for ( int ip = 0; ip < params.length; ip++ ) {
                    UwsJobInfo.Parameter param = params[ ip ];
                    String name = param.getId();
                    if ( name == null || name.length() == 0 ) {
                        reporter_.report( ReportType.ERROR, "PANO",
                                          "Parameter with no name" );
                    }
                    else {
                        String upName = param.getId().toUpperCase();
                        if ( paramMap.containsKey( upName ) ) {
                            String msg = new StringBuilder()
                               .append( "Duplicate parameter " )
                               .append( upName )
                               .append( " in job parameters list" )
                               .toString();
                            reporter_.report( ReportType.ERROR, "PADU",
                                              msg );
                        }
                        else {
                            paramMap.put( upName, param );
                        }
                    }
                }
            }
            return paramMap;
        }

        /**
         * Reads the metadata object for a job.
         *
         * @param  job  UWS job
         * @return  job info, or null if it couldn't be read
         */
        private UwsJobInfo readJobInfo( UwsJob job ) {
            try {
                return job.readJob();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "JBIO",
                                  "Error reading job info", e );
                return null;
            }
            catch ( SAXException e ) {
                reporter_.report( ReportType.ERROR, "JBSP",
                                  "Error parsing job info", e );
                return null;
            }
        }

        /**
         * Creates a UWS job based on a given adql query string.
         *
         * @param  adql  query text
         * @return   new job, or null if there was an error
         */
        private UwsJob createJob( String adql ) {
            TapQuery tq;
            Map<String,String> paramMap = new LinkedHashMap<String,String>();
            paramMap.put( "RUNID", runId1_ );
            try {
                tq = new TapQuery( serviceUrl_, adql, paramMap, null, 0 );
            }
            catch ( IOException e ) {
                throw new AssertionError( "no upload!" );
            }
            UwsJob job;
            try {
                job = UwsJob.createJob( tq.getServiceUrl() + "/async",
                                        tq.getStringParams(),
                                        tq.getStreamParams() );
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "QFAA",
                                  "Failed to submit TAP query "
                                + shortAdql_, e );
                return null;
            }
            reporter_.report( ReportType.INFO, "CJOB",
                              "Created new job " + job.getJobUrl() );
            return job;
        }

        /**
         * Equality utility for two strings.
         *
         * @param  s1  string 1, may be null
         * @param  s2  string 2, may be null
         * @return  true iff they are equal
         */
        private boolean equals( String s1, String s2 ) {
            return s1 == null || s1.trim().length() == 0 
                 ? ( s2 == null || s2.trim().length() == 0 )
                 : s1.equals( s2 );
        }

        /**
         * Checks that the content of a given URL is an integer.
         *
         * @param  url  source of text, for reporting
         * @param  txt  text content
         */
        private void checkInt( URL url, String txt ) {
            try {
                Long.parseLong( txt );
            }
            catch ( NumberFormatException e ) {
                String msg = new StringBuilder()
                   .append( "Not integer content " )
                   .append( '"' )
                   .append( txt )
                   .append( '"' )
                   .append( " from " )
                   .append( url )
                   .toString();
                reporter_.report( ReportType.ERROR, "IFMT", msg );
            }
        }

        /**
         * Checks that the content of a given URL is a ISO-8601 date.
         *
         * @param  url  source of text, for reporting
         * @param  txt  text content
         */
        private void checkDateTime( URL url, String txt ) {
            if ( txt != null ) {
                if ( ! ISO8601_REGEX.matcher( txt ).matches() ) {
                    String msg = new StringBuilder()
                       .append( "Not ISO-8601 content " )
                       .append( '"' )
                       .append( txt )
                       .append( '"' )
                       .append( " from " )
                       .append( url )
                       .toString();
                    reporter_.report( ReportType.WARNING, "TFMT", msg );
                }
            }
        }

        /**
         * Returns the content of a given URL, checking that it has text/plain
         * declared MIME type.
         *
         * @param  url   URL to read
         * @param  mustExist   true if non-existence should trigger error report
         * @return   content string (assumed UTF-8), or null
         */
        private String readTextContent( URL url, boolean mustExist ) {
            byte[] buf = readContent( url, "text/plain", mustExist );
            try {
                return buf == null ? null : new String( buf, "UTF-8" );
            }
            catch ( UnsupportedEncodingException e ) {
                reporter_.report( ReportType.FAILURE, "UTF8",
                                  "Unknown encoding UTF-8??", e );
                return null;
            }
        }

        /**
         * Reads the content of a URL and checks that it has a given declared
         * MIME type.
         *
         * @param  url   URL to read
         * @param  mimeType  required declared Content-Type
         * @param  mustExist   true if non-existence should trigger error report
         * @return  content bytes, or null
         */
        private byte[] readContent( URL url, String mimeType,
                                    boolean mustExist ) {
            if ( url == null ) {
                return null;
            }
            HttpURLConnection hconn;
            int responseCode;
            String responseMsg;
            try {
                URLConnection conn = url.openConnection();
                conn = TapQuery.followRedirects( conn );
                if ( ! ( conn instanceof HttpURLConnection ) ) {
                    reporter_.report( ReportType.WARNING, "HURL",
                                      "Redirect to non-HTTP URL? "
                                    + conn.getURL() );
                    return null;
                }
                hconn = (HttpURLConnection) conn;
                hconn.connect();
                responseCode = hconn.getResponseCode();
                responseMsg = hconn.getResponseMessage();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "EURL",
                                  "Error contacting URL " + url );
                return null;
            }
            if ( responseCode != 200 ) {
                if ( mustExist ) {
                    String msg = new StringBuilder()
                       .append( "Non-OK response " )
                       .append( responseCode )
                       .append( " " )
                       .append( responseMsg )
                       .append( " from " )
                       .append( url )
                       .toString();
                    reporter_.report( ReportType.ERROR, "NFND", msg );
                }
                return null;
            }
            InputStream in = null;
            byte[] buf;
            try {
                in = new BufferedInputStream( hconn.getInputStream() );
                ByteList blist = new ByteList();
                for ( int b; ( b = in.read() ) >= 0; ) {
                    blist.add( (byte) b );
                }
                buf = blist.toByteArray();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.WARNING, "RDIO",
                                  "Error reading resource " + url );
                buf = null;
            }
            finally {
                if ( in != null ) {
                    try {
                        in.close();
                    }
                    catch ( IOException e ) {
                    }
                }
            }
            String ctype = hconn.getContentType();
            if ( ctype == null || ctype.trim().length() == 0 ) {
                reporter_.report( ReportType.WARNING, "NOCT",
                                  "No Content-Type header for " + url );
            }
            else if ( ! ctype.startsWith( mimeType ) ) {
                String msg = new StringBuilder()
                   .append( "Incorrect Content-Type " )
                   .append( ctype )
                   .append( " != " )
                   .append( mimeType )
                   .append( " for " )
                   .append( url )
                   .toString();
                reporter_.report( ReportType.ERROR, "GMIM", msg );
                return buf;
            }
            return buf;
        }
    }
}
