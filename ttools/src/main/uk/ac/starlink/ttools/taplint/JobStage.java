package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.xml.sax.SAXException;
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
            reporter.report( Reporter.Type.FAILURE, "NOTM",
                             "No table metadata available"
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
            checkParameter( job, "REQUEST", "doQuery" );
            checkParameter( job, "RUNID", runId1_ );
            if ( postParameter( job, "runId", runId2_ ) ) {
                checkParameter( job, "RUNID", runId2_ );
            }
            if ( postPhase( job, "ABORT" ) ) {
                checkPhase( job, "ABORTED" );
            }
            if ( postPhase( job, "DELETE" ) ) {
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
                reporter_.report( Reporter.Type.ERROR, "HTOF",
                                  "Failed to contact " + jobUrl, e );
                return;
            }
            if ( ! ( conn instanceof HttpURLConnection ) ) {
                reporter_.report( Reporter.Type.ERROR, "NOHT",
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
                reporter_.report( Reporter.Type.ERROR, "HTDE",
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
                reporter_.report( Reporter.Type.ERROR, "RDPH",
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
                reporter_.report( Reporter.Type.ERROR, "BAPH", msg );
            }
            if ( UwsStage.FINISHED == UwsStage.forPhase( phase ) ) {
                reporter_.report( Reporter.Type.INFO, "JOFI",
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
            boolean gotPhase;
            try {
                job.readPhase();
                gotPhase = true;
            }
            catch ( IOException e ) {
                reporter_.report( Reporter.Type.ERROR, "PHER",
                                  "Error reading job phase ", e );
                gotPhase = false;
            }
            if ( gotPhase ) {
                String lastPhase = job.getLastPhase();
                if ( ! mustPhase.equals( lastPhase ) ) {
                    String msg = new StringBuilder()
                        .append( "Phase from /phase URL " )
                        .append( lastPhase )
                        .append( " != " )
                        .append( mustPhase )
                        .toString();
                    reporter_.report( Reporter.Type.ERROR, "PHUR", msg );
                }
            }
            UwsJobInfo jobInfo = readJobInfo( job );
            if ( jobInfo != null ) {
                String infoPhase = jobInfo.getPhase();
                if ( ! mustPhase.equals( infoPhase ) ) {
                    String msg = new StringBuilder()
                        .append( "Phase from <uws:job> element " ) 
                        .append( infoPhase )
                        .append( " != " )
                        .append( mustPhase )
                        .toString();
                    reporter_.report( Reporter.Type.ERROR, "PHUR", msg );
                }
            }
        }

        /**
         * Checks that a job parameter has a given value.
         *
         * @param  job  job to check
         * @param  name job parameter name
         * @param  value  asserted parameter value
         */
        private void checkParameter( UwsJob job, final String name,
                                     final String mustValue ) {
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
                    reporter_.report( Reporter.Type.ERROR, "PANZ", msg );
                }
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
                reporter_.report( Reporter.Type.ERROR, "PAMM", msg );
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
                reporter_.report( Reporter.Type.ERROR, "DEOP",
                                  "Can't open connection to " + jobUrl, e );
                return;
            }
            if ( conn instanceof HttpURLConnection ) {
                int code;
                try {
                    code = ((HttpURLConnection) conn).getResponseCode();
                }
                catch ( IOException e ) {
                    reporter_.report( Reporter.Type.ERROR, "DEHT",
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
                    reporter_.report( Reporter.Type.ERROR, "DENO", msg );
                }
            }
            else {
                reporter_.report( Reporter.Type.ERROR, "NOHT",
                                  "Job " + jobUrl + " not HTTP?" );
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
                reporter_.report( Reporter.Type.ERROR, "POER", msg, e );
                return false;
            }
            if ( code >= 400 ) {
                String msg = new StringBuilder()
                   .append( "Error response to POSTed parameter " )
                   .append( key )
                   .append( "=" )
                   .append( value )
                   .append( ": " )
                   .append( code )
                   .append( " " )
                   .append( responseMsg )
                   .toString();
                reporter_.report( Reporter.Type.ERROR, "PORE", msg );
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
            reporter_.report( Reporter.Type.INFO, "POPA", msg );
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
                reporter_.report( Reporter.Type.ERROR, "DENO",
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
                        reporter_.report( Reporter.Type.ERROR, "RUPH",
                                          "Incorrect phase " + phase
                                        + " for started job " + jobUrl );
                        return;
                    case ILLEGAL:
                    case UNKNOWN:
                        reporter_.report( Reporter.Type.ERROR, "BAPH",
                                          "Bad phase " + phase
                                        + " for job " + jobUrl );
                        return;
                    case RUNNING:
                        try {
                            Thread.sleep( poll_ );
                        }
                        catch ( InterruptedException e ) {
                            reporter_.report( Reporter.Type.FAILURE, "INTR",
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
                    reporter_.report( Reporter.Type.ERROR, "RDPH",
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
                        reporter_.report( Reporter.Type.ERROR, "PANO",
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
                            reporter_.report( Reporter.Type.ERROR, "PADU",
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
                reporter_.report( Reporter.Type.ERROR, "JBIO",
                                  "Error reading job info", e );
                return null;
            }
            catch ( SAXException e ) {
                reporter_.report( Reporter.Type.ERROR, "JBSP",
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
                reporter_.report( Reporter.Type.ERROR, "QFAA",
                                  "Failed to submit TAP query "
                                + shortAdql_, e );
                return null;
            }
            reporter_.report( Reporter.Type.INFO, "CJOB",
                              "Created new job " + job.getJobUrl() );
            return job;
        }
    }
}
