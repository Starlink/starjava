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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.auth.Redirector;
import uk.ac.starlink.auth.UrlConnector;
import uk.ac.starlink.util.ByteList;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.ContentType;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapService;
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

    private static final ContentTypeOptions CTYPE_PLAIN =
        new ContentTypeOptions( new ContentType[] {
            new ContentType( "text", "plain" ),
        } );
    private static final ContentTypeOptions CTYPE_XML =
        new ContentTypeOptions( new ContentType[] {
            new ContentType( "text", "xml" ),
            new ContentType( "application", "xml" ),
        } );

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

    public void run( Reporter reporter, TapService tapService ) {
        SchemaMeta[] smetas = metaHolder_.getTableMetadata();
        TableMeta tmeta = getFirstTable( metaHolder_.getTableMetadata() );
        if ( tmeta == null ) {
            reporter.report( FixedCode.F_NOTM,
                             "No table metadata available "
                           + "(earlier stages failed/skipped? "
                           + " - will not attempt UWS tests" );
            return;
        }
        new UwsRunner( reporter, tapService, tmeta, pollMillis_ ).run();
    }

    /**
     * Returns the first available table from a given list of schemas.
     *
     * @param   smetas  table set metadata
     * @return  metadata for first available table
     */
    private TableMeta getFirstTable( SchemaMeta[] smetas ) {
        if ( smetas != null ) {
            for ( SchemaMeta smeta : smetas ) {
                for ( TableMeta tmeta : smeta.getTables() ) {
                    return tmeta;
                }
            }
        }
        return null;
    }

    /**
     * Class which does the work for this stage.
     */
    private static class UwsRunner implements Runnable {
        private final Reporter reporter_;
        private final TapService tapService_;
        private final TableMeta tmeta_;
        private final long poll_;
        private final String shortAdql_;
        private final String runId1_;
        private final String runId2_;
  
        /**
         * Constructor.
         *
         * @param  reporter  destination for validation messages
         * @param  tapService   TAP service description
         * @param  tmeta  example table metadata
         * @param  poll  number of milliseconds between polls when waiting
         */
        UwsRunner( Reporter reporter, TapService tapService, TableMeta tmeta,
                   long poll ) {
            reporter_ = reporter;
            tapService_ = tapService;
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
            if ( ! tapService_.getTapVersion().is11() ) {
                checkParameter( job, "REQUEST", "doQuery", true );
            }
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
            UrlConnector delConnector = hconn -> {
                hconn.setRequestMethod( "DELETE" );
                hconn.setInstanceFollowRedirects( false );
                hconn.connect();
            };
            URLConnection conn;
            try {
                conn = AuthManager.getInstance()
                      .connect( jobUrl, delConnector, Redirector.NO_REDIRECT );
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_HTDE,
                                  "Failed to perform HTTP DELETE to " + jobUrl,
                                  e );
                return;
            }
            if ( ! ( conn instanceof HttpURLConnection ) ) {
                reporter_.report( FixedCode.E_NOHT,
                                  "Job url " + jobUrl + " not HTTP?" );
                return;
            }
            HttpURLConnection hconn = (HttpURLConnection) conn;
            int response;
            try {
                response = hconn.getResponseCode();
                hconn.getInputStream().close();
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_HTDE,
                                  "Failed to perform HTTP DELETE to " + jobUrl,
                                  e );
                return;
            }
            if ( response != 303 ) {
                reporter_.report( FixedCode.E_DECO,
                                  "HTTP DELETE response was " + response
                                + " not 303" );
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
            validateJobDocument( job );
            if ( ! postPhase( job, "RUN" ) ) {
                return;
            }
            String phase = null;
            try {
                boolean knownPhase = false;
                while ( ! knownPhase ) {
                    UwsJobInfo info = job.readInfo();
                    phase = info.getPhase();
                    knownPhase = ! "UNKNOWN".equals( phase );
                    if ( ! knownPhase ) {
                        waitUnknown( jobUrl );
                    }
                }
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_RDPH,
                                  "Can't read phase for job " + jobUrl, e );
                return;
            }
            assert ! "UNKNOWN".equals( phase );
            if ( ! Arrays.asList( new String[] {
                                      "QUEUED", "EXECUTING", "SUSPENDED",
                                      "ERROR", "COMPLETED",
                                  } ).contains( phase ) ) {
                String msg = new StringBuilder()
                   .append( "Incorrect phase " )
                   .append( phase )
                   .append( " for started job " )
                   .append( jobUrl )
                   .toString();
                reporter_.report( FixedCode.E_BAPH, msg );
            }
            if ( UwsStage.FINISHED == UwsStage.forPhase( phase ) ) {
                reporter_.report( FixedCode.I_JOFI,
                                  "Job completed immediately - "
                                + "can't test phase progression" );
            }
            else {
                waitForFinish( job );
            }
            validateJobDocument( job );
            delete( job );
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
                    reporter_.report( FixedCode.E_PHUR, msg );
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
                reporter_.report( FixedCode.E_JDPH, msg );
            }
        }

        /**
         * Checks that a job parameter has a given value.
         *
         * @param  job  job to check
         * @param  name job parameter name
         * @param  mustValue  asserted parameter value
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
                    reporter_.report( FixedCode.E_PANZ, msg );
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
                reporter_.report( FixedCode.E_PAMM, msg );
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
            readContent( jobUrl, CTYPE_XML, true );
            UwsJobInfo jobInfo;
            try {
                jobInfo = job.readInfo();
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_JDIO,
                                  "Error reading job document " + jobUrl, e );
                return;
            }
            if ( jobInfo == null ) {
                reporter_.report( FixedCode.E_JDNO,
                                  "No job document found " + jobUrl );
                return;
            }
            UwsVersion version = getVersion( jobInfo );

            /* Check the job ID is consistent between the job URL and
             * job info content. */
            if ( ! jobUrl.toString().endsWith( "/" + jobInfo.getJobId() ) ) {
                String msg = new StringBuilder()
                   .append( "Job ID mismatch; " )
                   .append( jobInfo.getJobId() )
                   .append( " is not final path element of " )
                   .append( jobUrl )
                   .toString();
                reporter_.report( FixedCode.E_JDID, msg );
            }

            /* Check the type of the quote resource. */
            URL quoteUrl = resourceUrl( job, "/quote" );
            String quote = readTextContent( quoteUrl, true );
            if ( version.quoteIsIso8601_ ) {
                checkDateTime( quoteUrl, quote, version );
            }
          
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
                reporter_.report( FixedCode.E_JDED, msg );
            }

            /* Check the type and content of the destruction time, and
             * whether it matches that in the job document. */
            URL destructUrl = resourceUrl( job, "/destruction" );
            String destruct = readTextContent( destructUrl, true );
            checkDateTime( destructUrl, destruct, version );
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
                reporter_.report( FixedCode.E_JDDE, msg );
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
                conn = AuthManager.getInstance().connect( jobUrl );
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_DEOP,
                                  "Can't open connection to " + jobUrl, e );
                return;
            }
            if ( conn instanceof HttpURLConnection ) {
                int code;
                try {
                    code = ((HttpURLConnection) conn).getResponseCode();
                }
                catch ( IOException e ) {
                    reporter_.report( FixedCode.E_DEHT,
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
                    reporter_.report( FixedCode.E_DENO, msg );
                }
            }
            else {
                reporter_.report( FixedCode.E_NOHT,
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
                reporter_.report( FixedCode.F_MURL,
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
                HttpURLConnection conn =
                    UwsJob.postUnipartForm( url, ContentCoding.NONE, map );
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
                reporter_.report( FixedCode.E_POER, msg, e );
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
                reporter_.report( FixedCode.E_PORE, msg );
                return false;
            }
            String msg = new StringBuilder()
               .append( "POSTed " )
               .append( key )
               .append( "=" )
               .append( value )
               .append( " to " )
               .append( url )
               .append( " (" )
               .append( code )
               .append( ")" )
               .toString();
            reporter_.report( FixedCode.I_POPA, msg );
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
                reporter_.report( FixedCode.E_DENO,
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
            UwsJobInfo info = job.getLastInfo();
            while ( UwsStage.forPhase( info.getPhase() )
                    != UwsStage.FINISHED ) {
                String phase = info.getPhase();
                UwsStage stage = UwsStage.forPhase( phase );
                switch ( stage ) {
                    case UNSTARTED:
                        reporter_.report( FixedCode.E_RUPH,
                                          "Incorrect phase " + phase
                                        + " for started job " + jobUrl );
                        return;
                    case ILLEGAL:
                        reporter_.report( FixedCode.E_ILPH,
                                          "Bad phase " + phase
                                        + " for job " + jobUrl );
                        return;
                    case UNKNOWN:
                        waitUnknown( jobUrl );
                        break;
                    case RUNNING:
                        waitPoll();
                        break;
                    case FINISHED:
                        break;
                    default:
                        throw new AssertionError();
                }
                try {
                    info = job.readInfo();
                }
                catch ( IOException e ) {
                    reporter_.report( FixedCode.E_RDPH,
                                      "Can't read phase for job " + jobUrl );
                    return;
                }
            }
        }

        /**
         * Called when an UNKNOWN UWS phase is encountered.
         * This routine reports that the status has been encountered and
         * then waits for a short while.
         *
         * @param  jobUrl  job URL for reporting purposes only
         */
        private void waitUnknown( URL jobUrl ) {
            reporter_.report( FixedCode.W_UNPH,
                              "Phase UNKNOWN reported for job " + jobUrl
                            + "; wait and poll" );
            waitPoll();
        }

        /**
         * Waits for a short while, suitable for a delay before polling.
         */
        private void waitPoll() {
            try {
                Thread.sleep( poll_ );
            }
            catch ( InterruptedException e ) {
                reporter_.report( FixedCode.F_INTR, "Interrupted??" );
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
                        reporter_.report( FixedCode.E_PANO,
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
                            reporter_.report( FixedCode.E_PADU, msg );
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
                return job.readInfo();
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_JBIO,
                                  "Error reading job info", e );
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
            Map<String,String> paramMap = new LinkedHashMap<String,String>();
            paramMap.put( "RUNID", runId1_ );
            TapQuery tq = new TapQuery( tapService_, adql, paramMap );
            UwsJob job;
            try {
                job = UwsJob.createJob( tapService_.getAsyncEndpoint()
                                                   .toString(),
                                        tq.getStringParams(),
                                        tq.getStreamParams() );
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_QFAA,
                                  "Failed to submit TAP query "
                                + shortAdql_, e );
                return null;
            }
            reporter_.report( FixedCode.I_CJOB,
                              "Created new job " + job.getJobUrl() );
            return job;
        }

        /**
         * Runs schema validation on the UWS job document,
         * reporting any validation messages as appropriate.
         *
         * @param  job   job object
         */
        private void validateJobDocument( UwsJob job ) {
            URL url = job.getJobUrl();
            if ( url != null ) {
                boolean includeSummary = false;
                XsdValidation.validateDoc( reporter_, url, "job",
                                           IvoaSchemaResolver.UWS_URI,
                                           includeSummary );
            }
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
                reporter_.report( FixedCode.E_IFMT, msg );
            }
        }

        /**
         * Checks that the content of a given URL is a ISO-8601 date.
         *
         * @param  url  source of text, for reporting
         * @param  txt  text content
         * @param  version   UWS version, affects date time required format
         */
        private void checkDateTime( URL url, String txt, UwsVersion version ) {
            if ( txt != null && txt.length() > 0 &&
                 ! version.iso8601Regex_.matcher( txt ).matches() ) {
                StringBuffer sbuf = new StringBuffer()
                      .append( "Not recommended UWS " )
                      .append( version )
                      .append( " ISO-8601 form" )
                      .append( " or empty string" );
                if ( version.requireZ_ && ! txt.endsWith( "Z" ) ) {
                    sbuf.append( " (missing trailing Z)" );
                }
                sbuf.append( " \"" )
                    .append( txt )
                    .append( '"' )
                    .append( " from " )
                    .append( url );
                reporter_.report( FixedCode.W_TFMT, sbuf.toString() );
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
            byte[] buf = readContent( url, CTYPE_PLAIN, mustExist );
            if ( buf == null ) {
                return null;
            }
            final String txt;
            try {
                txt = new String( buf, "UTF-8" );
            }
            catch ( UnsupportedEncodingException e ) {
                reporter_.report( FixedCode.F_UTF8,
                                  "Unknown encoding UTF-8??", e );
                return null;
            }
            return UwsJob.TRIM_TEXT ? txt.trim() : txt;
        }

        /**
         * Indicates UWS version declared by UWS job document.
         * 
         * @param   reporter  reporter
         * @param   jobInfo   job to test
         * @return  best guess at version
         */
        private UwsVersion getVersion( UwsJobInfo jobInfo ) {
            String version = jobInfo.getUwsVersion();
            if ( version == null ) {
                reporter_.report( FixedCode.I_VUWS,
                                  "UWS job document implicitly V1.0" );
                return UwsVersion.V10;
            }
            else if ( "1.0".equals( version ) ) {
                reporter_.report( FixedCode.I_VUWS,
                                  "UWS job document explicitly V1.0" );
                return UwsVersion.V10;
            }
            else if ( "1.1".equals( version ) ) {
                reporter_.report( FixedCode.I_VUWS,
                                  "UWS job document explicitly V1.1" );
                return UwsVersion.V11;
            }
            else {
                reporter_.report( FixedCode.W_VUWS,
                                  "Unknown UWS version \"" + version + "\"" );
                UwsVersion vers;
                try {
                    vers = Double.parseDouble( version ) >= 1.1
                         ? UwsVersion.V11
                         : UwsVersion.V10;
                }
                catch ( NumberFormatException e ) {
                    vers = UwsVersion.V10;
                }
                reporter_.report( FixedCode.I_VUWS,
                                  "Treat UWS version as " + vers );
                return vers;
            }
        }

        /**
         * Reads the content of a URL and checks that it has a given declared
         * MIME type.
         *
         * @param  url   URL to read
         * @param  reqType  required declared Content-Type
         * @param  mustExist   true if non-existence should trigger error report
         * @return  content bytes, or null
         */
        private byte[] readContent( URL url, ContentTypeOptions reqType,
                                    boolean mustExist ) {
            if ( url == null ) {
                return null;
            }
            HttpURLConnection hconn;
            int responseCode;
            String responseMsg;
            try {
                URLConnection conn = AuthManager.getInstance().connect( url );
                if ( ! ( conn instanceof HttpURLConnection ) ) {
                    reporter_.report( FixedCode.W_HURL,
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
                reporter_.report( FixedCode.E_EURL,
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
                    reporter_.report( FixedCode.E_NFND, msg );
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
                reporter_.report( FixedCode.W_RDIO,
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
            if ( reqType != null ) {
                reqType.checkType( reporter_, hconn.getContentType(), url );
            }
            return buf;
        }
    }

    /**
     * Enumerates known UWS versions.
     */
    private enum UwsVersion {

        /* UWS Version 1.0. */
        V10( "V1.0", false, "[T ]", false ),

        /* UWS Version 1.1. */
        V11( "V1.1", true, "T", true );
           
        final String name_;
        final boolean quoteIsIso8601_;
        final boolean requireZ_;
        final Pattern iso8601Regex_;

        /**
         * Constructor.
         *
         * @param  name  user-visible name
         * @param  quoteIsIso8601  whether quote endpoint is supposed to be
         *                         an ISO-8601 string; this was modified
         *                         (corrected) from v1.0 to v1.1
         * @param  dateSep   ISO8601 date-time separator regex
         * @param  requireZ  true iff trailing Z is required on ISO-8601 dates
         * @param  dateTrail ISO8601 trailing time zone indicated regex
         */
        UwsVersion( String name, boolean quoteIsIso8601,
                    String dateSep, boolean requireZ ) {
            name_ = name;
            quoteIsIso8601_ = quoteIsIso8601;
            requireZ_ = requireZ;
            iso8601Regex_ = Pattern.compile(
                "([0-9]{4})-"
              + "(0[1-9]|1[0-2])-"
              + "(0[1-9]|[12][0-9]|3[01])"
              + dateSep
              + "(([01][0-9]|2[0-3])"
              + "(:[0-5][0-9]"
              + "(:[0-5][0-9]"
              + "([.][0-9]*)?)?)?)?"
              + ( requireZ ? "Z" : "Z?" ) );
        }

        @Override
        public String toString() {
            return name_;
        }
    }
}
