package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.LongParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.cone.BlockUploader;
import uk.ac.starlink.ttools.cone.JELQuerySequenceFactory;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.ttools.cone.ServiceFindMode;
import uk.ac.starlink.ttools.cone.TapUploadMatcher;
import uk.ac.starlink.ttools.cone.UploadMatcher;
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.IOSupplier;
import uk.ac.starlink.vo.TapService;

/**
 * Upload matcher that uses an external TAP service.
 *
 * @author   Mark Taylor
 * @since    5 Nov 2014
 */
public class TapUploadSkyMatch extends SingleMapperTask {

    private final StringParameter inlonParam_;
    private final StringParameter inlatParam_;
    private final StringParameter srParam_;
    private final TapServiceParams tapserviceParams_;
    private final StringParameter taptableParam_;
    private final StringParameter taplonParam_;
    private final StringParameter taplatParam_;
    private final StringMultiParameter tapcolsParam_;
    private final ChoiceParameter<UserFindMode> findParam_;
    private final IntegerParameter chunkParam_;
    private final IntegerParameter maxrecParam_;
    private final BooleanParameter syncParam_;
    private final LongParameter tapmaxrecParam_;
    private final ContentCodingParameter codingParam_;
    private final JoinFixActionParameter fixcolsParam_;
    private final StringParameter insuffixParam_;
    private final StringParameter tapsuffixParam_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public TapUploadSkyMatch() {
        super( "Crossmatches table on sky position against TAP table",
               new ChoiceMode(), true, true );
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();

        inlonParam_ = new StringParameter( "inlon" );
        inlonParam_.setUsage( "<expr/deg>" );
        inlonParam_.setPrompt( "Longitude coordinate in input table" );
        inlonParam_.setDescription( new String[] {
            "<p>Longitude in degrees for the position of each row",
            "in the input table.",
            "This may simply be a column name, or it may be",
            "an algebraic expression as explained in <ref id='jel'/>.",
            "The coordinate system must match that used for the",
            "coordinates in the remote table.",
            "</p>",
        } );
        paramList.add( inlonParam_ );

        inlatParam_ = new StringParameter( "inlat" );
        inlatParam_.setUsage( "<expr/deg>" );
        inlatParam_.setPrompt( "Latitude coordinate in input table" );
        inlatParam_.setDescription( new String[] {
            "<p>Longitude in degrees for the position of each row",
            "in the input table.",
            "This may simply be a column name, or it may be",
            "an algebraic expression as explained in <ref id='jel'/>.",
            "The coordinate system must match that used for the",
            "coordinates in the remote table.",
            "</p>",
        } );
        paramList.add( inlatParam_ );

        tapserviceParams_ = new TapServiceParams( "tapurl", false );
        paramList.add( tapserviceParams_.getBaseParameter() );
        paramList.addAll( tapserviceParams_.getInterfaceParameters() );

        /* For now don't report the other endpoint parameters,
         * since most of them will have no effect in practice,
         * and they would confuse the documentation.
         * But they are present undocumented if necessary. */
        if ( false ) {
            paramList.addAll( tapserviceParams_.getOtherParameters() );
        }

        taptableParam_ = new StringParameter( "taptable" );
        taptableParam_.setUsage( "<name>" );
        taptableParam_.setPrompt( "Name of table in TAP service" );
        taptableParam_.setDescription( new String[] {
            "<p>Name of the table in the given TAP service",
            "against which the matching will be performed.",
            "</p>",
        } );
        paramList.add( taptableParam_ );

        taplonParam_ = new StringParameter( "taplon" );
        taplonParam_.setUsage( "<column>" );
        taplonParam_.setPrompt( "Longitude column in TAP table" );
        taplonParam_.setDescription( new String[] {
            "<p>Longitude in degrees for the position of each row",
            "in the remote table.",
            "This is an ADQL expression interpreted within the TAP service,",
            "typically just a column name.",
            "The coordinate system must match that used for the input table.",
            "</p>",
        } );
        paramList.add( taplonParam_ );

        taplatParam_ = new StringParameter( "taplat" );
        taplatParam_.setUsage( "<column>" );
        taplatParam_.setPrompt( "Latitude column in TAP table" );
        taplatParam_.setDescription( new String[] {
            "<p>Latitude in degrees for the position of each row",
            "in the remote table.",
            "This is an ADQL expression interpreted within the TAP service,",
            "typically just a column name.",
            "The coordinate system must match that used for the input table.",
            "</p>",
        } );
        paramList.add( taplatParam_ );

        tapcolsParam_ = new StringMultiParameter( "tapcols", ',' );
        tapcolsParam_.setUsage( "<colname,...>" );
        tapcolsParam_.setPrompt( "List of columns from TAP table" );
        tapcolsParam_.setDescription( new String[] {
            "<p>Comma-separated list of column names",
            "to retrieve from the remote table.",
            "If no value is supplied (the default),",
            "all columns from the remote table will be returned.",
            "</p>",
        } );
        tapcolsParam_.setNullPermitted( true );
        paramList.add( tapcolsParam_ );

        srParam_ = new StringParameter( "sr" );
        srParam_.setPrompt( "Search radius expression in degrees" );
        srParam_.setUsage( "<expr/deg>" );
        srParam_.setDescription( new String[] {
            "<p>Maximum distance in degrees",
            "from the local table (lat,lon) position",
            "at which counterparts from the remote table will be identified.",
            "This is an ADQL expression interpreted within the TAP service,",
            "so it may be a constant value or may involve columns in",
            "the remote table.",
            "</p>",
        } );
        paramList.add( srParam_ );

        chunkParam_ = new IntegerParameter( "blocksize" );
        chunkParam_.setPrompt( "Maximum number of rows per request" );
        chunkParam_.setDescription( new String[] {
            "<p>The number of rows uploaded in each TAP query.",
            "TAP services may have limits on the number of rows in",
            "a table uploaded for matching.",
            "This command can therefore break up input tables into blocks",
            "and make a number of individual TAP queries to generate the",
            "result.",
            "This parameter controls the maximum number of rows uploaded",
            "in each individual request.",
            "For an input table with fewer rows than this value,",
            "the whole thing is done as a single query.",
            "</p>",
        } );
        chunkParam_.setMinimum( 1 );
        chunkParam_.setIntDefault( 5 * 1000 );

        /* Work out what find modes are supported. */
        List<ServiceFindMode> serviceModes =
            Arrays.asList( TapUploadMatcher.getSupportedServiceModes() );
        List<UserFindMode> umodeList = new ArrayList<UserFindMode>();
        for ( UserFindMode userMode : UserFindMode.getInstances() ) {
            if ( serviceModes.contains( userMode.getServiceMode() ) ) {
                umodeList.add( userMode );
            }
        }
        UserFindMode[] userModes = umodeList.toArray( new UserFindMode[ 0 ] );
        findParam_ = new ChoiceParameter<UserFindMode>( "find", userModes );
        findParam_.setPrompt( "Which pair matches to include" );
        StringBuffer optBuf = new StringBuffer();
        for ( UserFindMode findMode : findParam_.getOptionValueList() ) {
            optBuf.append( "<li>" )
                  .append( "<code>" )
                  .append( findMode.getName() )
                  .append( "</code>: " )
                  .append( findMode.getSummary() )
                  .append( "</li>" )
                  .append( '\n' );
        }
        findParam_.setDescription( new String[] {
            "<p>Determines which pair matches are included in the result.",
            "<ul>",
            optBuf.toString(),
            "</ul>",
            "Note only the <code>" + UserFindMode.ALL + "</code> mode",
            "is symmetric between the two tables.",
            "</p>",
        } );
        findParam_.setDefaultOption( UserFindMode.ALL );

        paramList.add( findParam_ );
        paramList.add( chunkParam_ );

        maxrecParam_ = new IntegerParameter( "maxrec" );
        maxrecParam_.setPrompt( "Maximum number of output rows" );
        maxrecParam_.setDescription( new String[] {
            "<p>Limit to the number of rows resulting from this operation.",
            "If the value is negative (the default) no limit is imposed.",
            "Note however that there can be truncation of the result",
            "if the number of records returned from a single chunk",
            "exceeds limits imposed by the service.",
            "</p>",
        } );
        maxrecParam_.setIntDefault( -1 );
        paramList.add( maxrecParam_ );

        syncParam_ = new BooleanParameter( "sync" );
        syncParam_.setPrompt( "Submit queries in synchronous mode?" );
        syncParam_.setDescription( new String[] {
            "<p>Determines whether the TAP queries are submitted",
            "in synchronous or asynchronous mode.",
            "Since this command uses chunking to keep requests to a",
            "reasonable size, hopefully requests will not take too",
            "long to execute, therefore the default is synchronous (true).",
            "</p>",
        } );
        syncParam_.setBooleanDefault( true );
        paramList.add( syncParam_ );

        tapmaxrecParam_ = new LongParameter( "blockmaxrec" );
        tapmaxrecParam_.setPrompt( "MAXREC limit per block" );
        tapmaxrecParam_.setDescription( new String[] {
            "<p>Sets the MAXREC parameter passed to the TAP service",
            "for each uploaded block.",
            "This allows you to request that the service overrides its",
            "default limit for the number of rows output from a single query.",
            "The service may still impose some \"hard\" limit beyond which",
            "the output row count cannot be increased.",
            "</p>",
            "<p>Note this differs from the",
            "<code>" + maxrecParam_.getName() + "</code>",
            "parameter, which gives the maximum total number of rows",
            "to be returned from this command.",
            "</p>",
        } );
        tapmaxrecParam_.setUsage( "<nrow>" );
        tapmaxrecParam_.setMinimum( 0L );
        tapmaxrecParam_.setNullPermitted( true );
        paramList.add( tapmaxrecParam_ );

        codingParam_ = new ContentCodingParameter();
        paramList.add( codingParam_ );

        fixcolsParam_ = new JoinFixActionParameter( "fixcols" );
        insuffixParam_ =
            fixcolsParam_.createSuffixParameter( "suffixin",
                                                 "the input table", "_in" );
        tapsuffixParam_ =
            fixcolsParam_.createSuffixParameter( "suffixremote",
                                                 "the TAP result table",
                                                 "_tap" );
        paramList.add( fixcolsParam_ );
        paramList.add( insuffixParam_ );
        paramList.add( tapsuffixParam_ );

        getParameterList().addAll( paramList );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {

        /* Interrogate environment for parameter values. */
        final String inlonString = inlonParam_.stringValue( env );
        final String inlatString = inlatParam_.stringValue( env );
        final IOSupplier<TapService> tapServiceSupplier =
            tapserviceParams_.getServiceSupplier( env );
        String taptable = taptableParam_.stringValue( env );
        String taplonString = taplonParam_.stringValue( env );
        String taplatString = taplatParam_.stringValue( env );
        String[] tapcols = tapcolsParam_.stringsValue( env );
        if ( tapcols != null && tapcols.length == 0 ) {
            tapcols = null;
        }
        final String srString = srParam_.stringValue( env );
        UserFindMode userMode = findParam_.objectValue( env );
        ServiceFindMode serviceMode = userMode.getServiceMode();
        boolean oneToOne = userMode.isOneToOne();
        int blocksize = chunkParam_.intValue( env );
        final long maxrec = maxrecParam_.intValue( env );
        boolean isSync = syncParam_.booleanValue( env );
        Map<String,String> extraParams = new LinkedHashMap<String,String>();
        Long tapmaxrec = tapmaxrecParam_.objectValue( env );
        if ( tapmaxrec != null ) {
            extraParams.put( "MAXREC", tapmaxrec.toString() );
        }
        ContentCoding coding = codingParam_.codingValue( env );
        TapUploadMatcher umatcher =
            new TapUploadMatcher( tapServiceSupplier, taptable,
                                  taplonString, taplatString, srString,
                                  isSync, tapcols, serviceMode,
                                  extraParams, coding );
        final String adql = umatcher.getAdql( maxrec );
        final QuerySequenceFactory qsFact =
            new JELQuerySequenceFactory( inlonString, inlatString, "0" );
        String tableName = "tapmatch(" + taptable + ")";
        JoinFixAction inFixAct =
            fixcolsParam_.getJoinFixAction( env, insuffixParam_ );
        JoinFixAction tapFixAct =
            fixcolsParam_.getJoinFixAction( env, tapsuffixParam_ );
        final TableProducer inProd = createInputProducer( env );
        final StoragePolicy storage =
            LineTableEnvironment.getStoragePolicy( env );
        boolean uploadEmpty = true;
        final BlockUploader blocker =
            new BlockUploader( umatcher, blocksize, maxrec, tableName,
                               inFixAct, tapFixAct, serviceMode, oneToOne,
                               uploadEmpty );

        /* Create and return an object which will produce the result. */
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                StarTable inTable = Tables.randomTable( inProd.getTable() );
                logger_.info( "ADQL query:\n" + adql );
                Bi<StarTable,BlockUploader.BlockStats> result =
                    blocker.runMatch( inTable, qsFact, storage );
                StarTable outTable = result.getItem1();
                BlockUploader.BlockStats stats = result.getItem2();
                int nBlock = stats.getBlockCount();
                int nTrunc = stats.getTruncatedBlockCount();
                if ( nTrunc > 0 ) {
                    String msg =
                          "Truncations in " + nTrunc + "/" + nBlock + " blocks;"
                        + " Reduce " + chunkParam_.getName() + "? "
                        + "Increase " + tapmaxrecParam_.getName() + "?";
                    logger_.warning( msg );
                }
                return outTable;
            }
        };
    }
}
