package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.ttools.cone.BlockUploader;
import uk.ac.starlink.ttools.cone.CdsUploadMatcher;
import uk.ac.starlink.ttools.cone.Coverage;
import uk.ac.starlink.ttools.cone.CoverageQuerySequenceFactory;
import uk.ac.starlink.ttools.cone.HealpixSortedQuerySequenceFactory;
import uk.ac.starlink.ttools.cone.JELQuerySequenceFactory;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.ttools.cone.ServiceFindMode;
import uk.ac.starlink.ttools.cone.UploadMatcher;
import uk.ac.starlink.ttools.cone.UrlMocCoverage;
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.ContentCoding;

/**
 * Upload matcher that uses CDS's Xmatch service.
 *
 * @author   Mark Taylor
 * @since    14 May 2014
 */
public class CdsUploadSkyMatch extends SingleMapperTask {

    private final StringParameter raParam_;
    private final StringParameter decParam_;
    private final DoubleParameter srParam_;
    private final StringParameter cdstableParam_;
    private final ChoiceParameter<UserFindMode> findParam_;
    private final IntegerParameter chunkParam_;
    private final IntegerParameter maxrecParam_;
    private final ContentCodingParameter codingParam_;
    private final URLParameter urlParam_;
    private final BooleanParameter usemocParam_;
    private final BooleanParameter presortParam_;
    private final JoinFixActionParameter fixcolsParam_;
    private final StringParameter insuffixParam_;
    private final StringParameter cdssuffixParam_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public CdsUploadSkyMatch() {
        super( "Crossmatches table on sky position against VizieR/SIMBAD table",
               new ChoiceMode(), true, true );
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
        String system = "ICRS";
        String inDescrip = "the input table";

        raParam_ =
            SkyCoordParameter.createRaParameter( "ra", system, inDescrip );
        paramList.add( raParam_ );

        decParam_ =
            SkyCoordParameter.createDecParameter( "dec", system, inDescrip );
        paramList.add( decParam_ );

        srParam_ = new DoubleParameter( "radius" );
        srParam_.setPrompt( "Search radius value in arcsec (0-180)" );
        srParam_.setUsage( "<value/arcsec>" );
        srParam_.setDescription( new String[] {
            "<p>Maximum distance from the local table (ra,dec) position",
            "at which counterparts from the remote table will be identified.",
            "This is a fixed value given in arcseconds,",
            "and must be in the range [0,180]",
            "(this limit is currently enforced by the CDS Xmatch service).",
            "</p>",
        } );
        srParam_.setMinimum( 0, true );
        srParam_.setMaximum( 180, true );
        paramList.add( srParam_ );

        cdstableParam_ = new StringParameter( "cdstable" );
        cdstableParam_.setPrompt( "Identifier for remote table" );
        cdstableParam_.setDescription( new String[] {
            "<p>Identifier of the table from the CDS crossmatch service",
            "that is to be matched against the local table.",
            "This identifier may be the standard VizieR identifier",
            "(e.g. \"<code>II/246/out</code>\"",
            "for the 2MASS Point Source Catalogue)",
            "or \"<code>simbad</code>\" to indicate SIMBAD data.",
            "</p>",
            "<p>See for instance the TAPVizieR table searching facility at" ,
            "<code>https://tapvizier.cds.unistra.fr/adql/</code>",
            "to find VizieR catalogue identifiers.",
            "</p>",
        } );
        paramList.add( cdstableParam_ );

        chunkParam_ = new IntegerParameter( "blocksize" );
        chunkParam_.setPrompt( "Maximum number of rows per request" );
        chunkParam_.setDescription( new String[] {
            "<p>The CDS Xmatch service operates limits on",
            "the maximum number of rows that can be uploaded and",
            "the maximum number of rows that is returned as a result",
            "from a single query.",
            "In the case of large input tables,",
            "they are broken down into smaller blocks,",
            "and one request is sent to the external service for each block.",
            "This parameter controls the number of rows in each block.",
            "For an input table with fewer rows than this value,",
            "the whole thing is done as a single request.",
            "</p>",
            "<p>At time of writing, the maximum upload size is 100Mb",
            "(about 3Mrow; this does not depend on the width of your table),",
            "and the maximum return size is 2Mrow.",
            "</p>",
            "<p>Large blocksizes tend to be good (up to a point) for",
            "reducing the total amount of time a large xmatch operation takes,",
            "but they can make it harder to see the job progressing.",
            "There is also the danger (for ALL-type find modes)",
            "of exceeding the return size limit, which will result in",
            "truncation of the returned result.",
            "</p>",
        } );
        chunkParam_.setMinimum( 1 );
        chunkParam_.setIntDefault( 50 * 1000 );

        findParam_ =
            new ChoiceParameter<UserFindMode>( "find",
                                               UserFindMode.getInstances() );
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
            "<p><strong>Note also that there is a bug</strong> in",
            "<code>" + UserFindMode.BEST_REMOTE + "</code>",
            "matching.",
            "If the match is done in multiple blocks,",
            "it's possible for a remote table row to appear matched against",
            "one local table row per uploaded block,",
            "rather than just once for the whole result.",
            "If you're worried about that, set",
            "<code>" + chunkParam_.getName(), "&gt;=</code>",
            "<em>rowCount</em>.",
            "This may be fixed in a future release.",
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
            "exceeds the service hard limit",
            "(2,000,000 at time of writing).",
            "</p>",
        } );
        maxrecParam_.setIntDefault( -1 );
        paramList.add( maxrecParam_ );

        codingParam_ = new ContentCodingParameter();
        paramList.add( codingParam_ );

        urlParam_ = new URLParameter( "serviceurl" );
        urlParam_.setPrompt( "URL for CDS Xmatch service" );
        urlParam_.setDescription( new String[] {
            "<p>The URL at which the CDS Xmatch service can be found.",
            "Normally this should not be altered from the default,",
            "but if other implementations of the same service are known,",
            "this parameter can be used to access them.",
            "</p>",
        } );
        urlParam_.setStringDefault( CdsUploadMatcher.XMATCH_URL );
        paramList.add( urlParam_ );

        usemocParam_ = new BooleanParameter( "usemoc" );
        usemocParam_.setPrompt( "Use VizieR MOC footprint?" );
        usemocParam_.setDescription( new String[] {
            "<p>If true, first acquire a MOC coverage map from CDS,",
            "and use that to pre-filter rows before uploading them",
            "for matching.",
            "This should improve efficiency, but have no effect on the result.",
            "</p>",
        } );
        usemocParam_.setBooleanDefault( true );
        paramList.add( usemocParam_ );

        presortParam_ = new BooleanParameter( "presort" );
        presortParam_.setPrompt( "Pre-sort rows before uploading?" );
        presortParam_.setDescription( new String[] {
            "<p>If true, the rows are sorted by HEALPix index before",
            "they are uploaded to the CDS X-Match service.",
            "If the match is done in multiple blocks,",
            "this may improve efficiency,",
            "since when matching against a large remote catalogue",
            "the X-Match service likes to process requests",
            "in which sources are grouped into a small region",
            "rather than scattered all over the sky.",
            "</p>",
            "<p>Note this will have a couple of other side effects that may",
            "be undesirable:",
            "it will read all the input rows into the task at once,",
            "which may make it harder to assess progress,",
            "and it will affect the order of the rows in the output table.",
            "</p>",
            "<p>It is <em>probably</em> only worth setting true for rather",
            "large (multi-million-row?) multi-block matches,",
            "where both local and remote catalogues are spread over",
            "a significant fraction of the sky.",
            "But feel free to experiment.",
            "</p>",
        } );
        presortParam_.setBooleanDefault( false );
        paramList.add( presortParam_ );

        fixcolsParam_ = new JoinFixActionParameter( "fixcols" );
        insuffixParam_ =
            fixcolsParam_.createSuffixParameter( "suffixin",
                                                 "the input table", "_in" );
        cdssuffixParam_ =
            fixcolsParam_.createSuffixParameter( "suffixremote",
                                                 "the CDS result table",
                                                 "_cds" );
        paramList.add( fixcolsParam_ );
        paramList.add( insuffixParam_ );
        paramList.add( cdssuffixParam_ );

        getParameterList().addAll( paramList );
    }

    /**
     * Returns the parameter for acquiring the expression for Right Ascension.
     *
     * @return RA parameter
     */
    public Parameter<String> getRaParameter() {
        return raParam_;
    }

    /**
     * Returns the parameter for acquiring the expression for Declination.
     *
     * @return  declination parameter
     */
    public Parameter<String> getDecParameter() {
        return decParam_;
    }

    /**
     * Returns the parameter for acquiring the search radius in arcseconds.
     *
     * @return  radius parameter
     */
    public Parameter<Double> getRadiusArcsecParameter() {
        return srParam_;
    }

    /**
     * Returns the parameter for acquiring the name of the remote VizieR table.
     *
     * @return   CDS table name parameter
     */
    public Parameter<String> getCdsTableParameter() {
        return cdstableParam_;
    }

    /**
     * Returns the parameter for acquiring the whether a MOC should be
     * sought and used to pre-filter rows before upload.
     *
     * @return  useMoc parameter
     */
    public Parameter<Boolean> getUseMocParameter() {
        return usemocParam_;
    }

    /**
     * Returns the parameter for acquiring the find mode.
     *
     * @return  find mode parameter
     */
    public Parameter<UserFindMode> getFindParameter() {
        return findParam_;
    }

    /**
     * Returns the parameter for acquiring upload block size.
     *
     * @return  block size parameter
     */
    public Parameter<Integer> getBlocksizeParameter() {
        return chunkParam_;
    }

    /**
     * Returns the parameter for acquiring the Fixer action.
     *
     * @return  column fixer parameter
     */
    public Parameter<JoinFixActionParameter.Fixer> getFixColsParameter() {
        return fixcolsParam_;
    }

    /**
     * Returns the parameter for acquiring the fixer suffix parameter.
     *
     * @return  fixer suffix parameter
     */
    public Parameter<String> getCdsSuffixParameter() {
        return cdssuffixParam_;
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {

        /* Interrogate environment for parameter values. */
        String raString = raParam_.stringValue( env );
        String decString = decParam_.stringValue( env );
        double sr = srParam_.doubleValue( env );
        String cdsName = cdstableParam_.stringValue( env );
        String cdsId = CdsUploadMatcher.toCdsId( cdsName );
        if ( cdsId == null ) {
            throw new ParameterValueException( cdstableParam_,
                                               "Bad value " + cdsName );
        }
        double srDeg = sr / 3600.;
        final QuerySequenceFactory qsFact0 =
            new JELQuerySequenceFactory( raString, decString,
                                         Double.toString( srDeg ) );
        UserFindMode userMode = findParam_.objectValue( env );
        ServiceFindMode serviceMode = userMode.getServiceMode();
        boolean oneToOne = userMode.isOneToOne();
        int blocksize = chunkParam_.intValue( env );
        long maxrec = maxrecParam_.intValue( env );
        ContentCoding coding = codingParam_.codingValue( env );
        URL url = urlParam_.objectValue( env );
        final Coverage coverage = usemocParam_.booleanValue( env )
                                ? UrlMocCoverage.getVizierMoc( cdsName, -1 )
                                : null;
        final boolean presort = presortParam_.booleanValue( env );
        UploadMatcher umatcher =
            new CdsUploadMatcher( url, cdsId, sr, serviceMode, coding );
        String tableName = "xmatch(" + cdsIdToTableName( cdsId ) + ")";
        JoinFixAction inFixAct =
            fixcolsParam_.getJoinFixAction( env, insuffixParam_ );
        JoinFixAction cdsFixAct =
            fixcolsParam_.getJoinFixAction( env, cdssuffixParam_ );
        final TableProducer inProd = createInputProducer( env );
        final StoragePolicy storage =
            LineTableEnvironment.getStoragePolicy( env );
        boolean uploadEmpty = CdsUploadMatcher.UPLOAD_EMPTY;
        final BlockUploader blocker =
            new BlockUploader( umatcher, blocksize, maxrec, tableName,
                               inFixAct, cdsFixAct, serviceMode, oneToOne,
                               uploadEmpty );

        /* Create and return an object which will produce the result. */
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                StarTable inTable = Tables.randomTable( inProd.getTable() );
                Coverage cov;
                if ( coverage != null ) {
                    try {
                        coverage.initCoverage();
                        cov = coverage;
                    }
                    catch ( IOException e ) {
                        logger_.log( Level.WARNING,
                                     "Failed to read coverage", e );
                        cov = null;
                    }
                }
                else {
                    cov = null;
                }
                QuerySequenceFactory qsFact1 = qsFact0;
                if ( cov != null ) {
                    qsFact1 = new CoverageQuerySequenceFactory( qsFact1, cov );
                }
                if ( presort ) {
                    qsFact1 = new HealpixSortedQuerySequenceFactory( qsFact1 );
                }
                Bi<StarTable,BlockUploader.BlockStats> result =
                    blocker.runMatch( inTable, qsFact1, storage );
                StarTable outTable = result.getItem1();
                BlockUploader.BlockStats stats = result.getItem2();
                int nBlock = stats.getBlockCount();
                int nTrunc = stats.getTruncatedBlockCount();
                if ( nTrunc > 0 ) {
                    String msg =
                          "Truncations in " + nTrunc + "/" + nBlock + " blocks;"
                        + " Reduce " + chunkParam_.getName() + "?";
                    logger_.warning( msg );
                }
                return outTable;
            }
        };
    }

    /**
     * Turns a CDS Xmatch table identifier into something suitable for
     * use in a table name.  This is a bit of a hack due to the fact
     * that topcat doesn't like table names containing "/" characters.
     *
     * @param  cdsId  identifier for a table in the CDS Xmatch service
     * @return   string suitable for use in a stilts output table
     */
    private static String cdsIdToTableName( String cdsId ) {
        return cdsId.replaceFirst( "^vizier:", "" )
                    .replaceAll( "/", "_" );
    }
}
