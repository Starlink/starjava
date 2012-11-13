package uk.ac.starlink.ttools.task;

import adql.parser.ParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.vo.AdqlValidator;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.UwsJob;

/**
 * Mapper that does the work for {@link TapQuerier}.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2011
 */
public class TapMapper implements TableMapper {

    private final URLParameter urlParam_;
    private final Parameter adqlParam_;
    private final BooleanParameter parseParam_;
    private final BooleanParameter syncParam_;
    private final Parameter langParam_;
    private final Parameter maxrecParam_;
    private final TapResultReader resultReader_;
    private final Parameter[] params_;

    public TapMapper() {
        List<Parameter> paramList = new ArrayList<Parameter>();

        paramList.add( createUploadNameParameter( VariableTablesInput
                                                 .NUM_SUFFIX ) );

        urlParam_ = new URLParameter( "tapurl" );
        urlParam_.setPrompt( "Base URL of TAP service" );
        urlParam_.setDescription( new String[] {
            "<p>The base URL of a Table Access Protocol service.",
            "This is the bare URL without a trailing \"/[a]sync\".",
            "</p>",
        } );
        paramList.add( urlParam_ );

        adqlParam_ = new Parameter( "adql" );
        adqlParam_.setPrompt( "ADQL query text" );
        adqlParam_.setDescription( new String[] {
            "<p>Astronomical Data Query Language string specifying the",
            "TAP query to execute.",
            "ADQL/S resembles SQL, so this string will likely start with",
            "\"SELECT\".",
            "</p>",
        } );
        paramList.add( adqlParam_ );

        parseParam_ = new BooleanParameter( "parse" );
        parseParam_.setPrompt( "Perform syntax checking on ADQL?" );
        parseParam_.setDescription( new String[] {
            "<p>Determines whether an attempt will be made to check",
            "the syntax of the ADQL prior to submitting the query.",
            "If this is set true, and if a syntax error is found,",
            "the task will fail with an error before any attempt is made",
            "to submit the query.",
            "</p>",
        } );
        parseParam_.setDefault( Boolean.FALSE.toString() );
        paramList.add( parseParam_ );

        syncParam_ = new BooleanParameter( "sync" );
        syncParam_.setPrompt( "Submit query in synchronous mode?" );
        syncParam_.setDescription( new String[] {
            "<p>Determines whether the TAP query is submitted in synchronous",
            "or asynchronous mode.",
            "Synchronous (<code>true</code>)",
            "means that the result is retrieved over the same HTTP connection",
            "that the query is submitted from.",
            "This is uncomplicated, but means if the query takes a long time",
            "it may time out and the results will be lost.",
            "Asynchronous (<code>false</code>)",
            "means that the job is queued and results may be retrieved later.",
            "Normally this command does the necessary waiting around and",
            "recovery of the result, though with appropriate settings",
            "you can get",
            "<ref id='tapresume'><code>tapresume</code></ref>",
            "to pick it up for you later instead.",
            "In most cases <code>false</code> (the default) is preferred.",
            "</p>",
        } );
        syncParam_.setDefault( Boolean.FALSE.toString() );
        paramList.add( syncParam_ );

        maxrecParam_ = new Parameter( "maxrec" );
        maxrecParam_.setPrompt( "Maximum number of records in output table" );
        maxrecParam_.setDescription( new String[] {
            "<p>Sets the requested maximum row count for the result of",
            "the query.",
            "The service is not obliged to respect this, but in the case",
            "that it has a default maximum record count, setting this value",
            "may raise the limit.",
            "If no value is set, the service's default policy will be used.",
            "</p>",
        } );
        maxrecParam_.setNullPermitted( true );
        paramList.add( maxrecParam_ );

        langParam_ = new Parameter( "language" );
        langParam_.setPrompt( "TAP query language" );
        langParam_.setDescription( new String[] {
            "<p>Language to use for the ADQL-like query.",
            "This will usually be \"ADQL\" (the default),",
            "but may be set to some other value supported by the service,",
            "for instance a variant indicating a different ADQL version.",
            "Note that at present, setting it to \"PQL\" is not sufficient",
            "to submit a PQL query.",
            "</p>",
        } );
        langParam_.setDefault( "ADQL" );
        paramList.add( langParam_ );

        resultReader_ = new TapResultReader();
        paramList.addAll( Arrays.asList( resultReader_.getParameters() ) );

        params_ = paramList.toArray( new Parameter[ 0 ] );
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public TableMapping createMapping( Environment env, final int nup )
            throws TaskException {
        final URL serviceUrl = urlParam_.urlValue( env );
        final String adql = adqlParam_.stringValue( env );
        if ( parseParam_.booleanValue( env ) ) {
            AdqlValidator validator = new AdqlValidator( null );
            try {
                validator.validate( adql );
            }
            catch ( Throwable e ) {
                throw new ExecutionException( "ADQL Parse error: "
                                            + e.getMessage(), e );
            }
        }
        boolean sync = syncParam_.booleanValue( env );
        final Map<String,String> extraParams =
            new LinkedHashMap<String,String>();
        String lang = langParam_.stringValue( env );
        if ( ! "ADQL".equals( lang ) ) {
            extraParams.put( "LANG", lang );
        }
        String sMaxrec = maxrecParam_.stringValue( env );
        if ( sMaxrec != null && sMaxrec.trim().length() > 0 ) {
            try {
                long maxrec = Long.parseLong( sMaxrec.trim() );
                extraParams.put( "MAXREC", Long.toString( maxrec ) );
            }
            catch ( NumberFormatException e ) {
                throw new ParameterValueException( maxrecParam_,
                                                   "Not an integer", e );
            }
        }
        final String[] upNames = new String[ nup ];
        for ( int iu = 0; iu < nup; iu++ ) {
            upNames[ iu ] =
                createUploadNameParameter( Integer.toString( iu + 1 ) )
               .stringValue( env );
        }
        final StarTableFactory tfact =
            LineTableEnvironment.getTableFactory( env );
        final long uploadLimit = -1;
        if ( sync ) {
            return new TableMapping() {
                public StarTable mapTables( InputTableSpec[] inSpecs )
                        throws TaskException, IOException {
                    TapQuery tq =
                        createTapQuery( serviceUrl, adql, extraParams, upNames,
                                        inSpecs, uploadLimit );
                    return tq.executeSync( tfact.getStoragePolicy() );
                }
            };
        }
        else {
            final TapResultProducer resultProducer =
                resultReader_.createResultProducer( env );
            final boolean progress =
                resultReader_.getProgressParameter().booleanValue( env );
            final PrintStream errStream = env.getErrorStream();
            return new TableMapping() {
                public StarTable mapTables( InputTableSpec[] inSpecs )
                        throws TaskException, IOException {
                    TapQuery tq =
                        createTapQuery( serviceUrl, adql, extraParams, upNames,
                                        inSpecs, uploadLimit );
                    UwsJob tapJob = tq.submitAsync();
                    if ( progress ) {
                        errStream.println( "SUBMITTED ..." );
                        errStream.println( tapJob.getJobUrl() );
                    }
                    tapJob.start();
                    return resultProducer.waitForResult( tapJob );
                }
            };
        }
    }

    /**
     * Returns a new TapQuery object from values available at execution time.
     *
     * @param  serviceUrl  base service URL for TAP service (excluding "/async")
     * @param  adql   text of ADQL query
     * @param  extraParams  key->value map for optional parameters;
     *                      if any of these match the names of standard
     *                      parameters (upper case) the standard values will
     *                      be overwritten, so use with care
     * @param  upNames  name array for tables to be uploaded
     * @param  inSpecs   upload input table specifier array
     *                   (must be same size as <code>upNames</code>)
     * @param  uploadLimit  maximum number of bytes that may be uploaded;
     *                      if negative, no limit is applied
     * @return   new TAP query object
     */
    private static TapQuery createTapQuery( URL serviceUrl, String adql,
                                            Map<String,String> extraParams,
                                            String[] upNames,
                                            InputTableSpec[] inSpecs,
                                            long uploadLimit ) 
            throws IOException, TaskException {
        int nup = upNames.length; 
        Map<String,StarTable> uploadMap = new LinkedHashMap<String,StarTable>();
        for ( int iu = 0; iu < nup; iu++ ) {
            uploadMap.put( upNames[ iu ], inSpecs[ iu ].getWrappedTable() );
        }
        return new TapQuery( serviceUrl, adql, extraParams, uploadMap, 
                             uploadLimit );
    }

    /**
     * Returns a parameter for acquiring the label under which one of the
     * uploaded tables should be presented to the TAP server.
     *
     * @param   label  parameter suffix 
     * @return   upload name parameter
     */
    private static Parameter createUploadNameParameter( String label ) {
        Parameter upnameParam = new Parameter( "upname" + label );
        upnameParam.setPrompt( "Label for uploaded table #" + label );
        upnameParam.setUsage( "<label>" );
        upnameParam.setDescription( new String[] {
            "<p>Identifier to use in server-side expressions for uploaded",
            "table #" + label + ".",
            "In ADQL expressions, the table should be referred to as",
            "\"<code>TAP_UPLOAD.&lt;label&gt;</code>\".",
            "</p>",
        } );
        upnameParam.setDefault( "up" + label );
        return upnameParam;
    }
}
