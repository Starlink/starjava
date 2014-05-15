package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.ttools.cone.BlockUploader;
import uk.ac.starlink.ttools.cone.CdsFindMode;
import uk.ac.starlink.ttools.cone.CdsUploadMatcher;
import uk.ac.starlink.ttools.cone.JELQuerySequenceFactory;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.ttools.cone.UploadMatcher;

/**
 * Upload matcher that uses CDS's Xmatch service.
 *
 * @author   Mark Taylor
 * @since    14 May 2014
 */
public class CdsUploadSkyMatch extends SingleMapperTask {

    private final Parameter raParam_;
    private final Parameter decParam_;
    private final DoubleParameter srParam_;
    private final Parameter cdstableParam_;
    private final ChoiceParameter<CdsFindMode> findParam_;
    private final IntegerParameter chunkParam_;
    private final IntegerParameter maxrecParam_;
    private final URLParameter urlParam_;
    private final JoinFixActionParameter fixcolsParam_;
    private final Parameter insuffixParam_;
    private final Parameter cdssuffixParam_;

    /**
     * Constructor.
     */
    public CdsUploadSkyMatch() {
        super( "Crossmatches table on sky position against VizieR/SIMBAD table",
               new ChoiceMode(), true, true );
        List<Parameter> paramList = new ArrayList<Parameter>();
        String system = "ICRS";
        String inDescrip = "the input table";

        raParam_ =
            SkyCoordParameter.createRaParameter( "ra", system, inDescrip );
        paramList.add( raParam_ );

        decParam_ =
            SkyCoordParameter.createDecParameter( "dec", system, inDescrip );
        paramList.add( decParam_ );

        srParam_ = new DoubleParameter( "sr" );
        srParam_.setPrompt( "Search radius value in arcsec (0-180)" );
        srParam_.setDescription( new String[] {
            "<p>Maximum distance from the local table (ra,dec) position",
            "at which counterparts from the remote table will be identified.",
            "This is a fixed value is given in arcseconds,",
            "and must be in the range 0&lt;=<code>sr</code>&gt;=180",
            "(this limit is currently enforced by the CDS Xmatch service).",
            "</p>",
        } );
        srParam_.setMinimum( 0, true );
        srParam_.setMaximum( 180, true );
        paramList.add( srParam_ );

        cdstableParam_ = new Parameter( "cdstable" );
        cdstableParam_.setPrompt( "Identifier for remote table" );
        cdstableParam_.setDescription( new String[] {
            "<p>Identifier of the table from the CDS crossmatch service",
            "that is to be matched against the local table.",
            "This identifier may be the standard VizieR identifier",
            "(e.g. \"<code>II/246/out</code>\"",
            "for the 2MASS Point Source Catalogue)",
            "or \"<code>simbad</code>\" to indicate SIMBAD data.",
            "</p>",
            "<p>See for instance",
            "<code>http://vizier.u-strasbg.fr/viz-bin/VizieR</code>",
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
        } );
        chunkParam_.setMinimum( 1 );
        chunkParam_.setDefault( Integer.toString( 10 * 1000 ) );

        findParam_ =
            new ChoiceParameter<CdsFindMode>( "find", CdsFindMode.class,
                                              CdsFindMode.values() );
        findParam_.setPrompt( "Which pair matches to include" );
        StringBuffer optBuf = new StringBuffer();
        for ( CdsFindMode mode : Arrays.asList( CdsFindMode.values() ) ) {
            optBuf.append( "<li>" )
                  .append( "<code>" )
                  .append( mode.getName() )
                  .append( "</code>: " )
                  .append( mode.getSummary() )
                  .append( "</li>" )
                  .append( '\n' );
        }
        findParam_.setDescription( new String[] {
            "<p>Determines which pair matches are included in the result.",
            "<ul>",
            optBuf.toString(),
            "</ul>",
            "Note only the <code>" + CdsFindMode.ALL + "</code> mode",
            "is symmetric between the two tables.",
            "</p>",
            "<p><strong>Note also that there is a bug</strong> in",
            "<code>" + CdsFindMode.BEST1 + "</code>",
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
        findParam_.setDefaultOption( CdsFindMode.ALL );

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
        maxrecParam_.setDefault( "-1" );
        paramList.add( maxrecParam_ );

        urlParam_ = new URLParameter( "serviceurl" );
        urlParam_.setPrompt( "URL for CDS Xmatch service" );
        urlParam_.setDescription( new String[] {
            "<p>The URL at which the CDS Xmatch service can be found.",
            "Normally this should not be altered from the default,",
            "but if other implementations of the same service are known,",
            "this parameter can be used to access them.",
            "</p>",
        } );
        urlParam_.setDefault( CdsUploadMatcher.XMATCH_URL );
        paramList.add( urlParam_ );

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
        final QuerySequenceFactory qsFact =
            new JELQuerySequenceFactory( raString, decString, "-1" );
        CdsFindMode findMode = findParam_.objectValue( env );
        boolean remoteUnique = findMode.isRemoteUnique();
        int blocksize = chunkParam_.intValue( env );
        long maxrec = maxrecParam_.intValue( env );
        URL url = urlParam_.urlValue( env );
        UploadMatcher umatcher =
            new CdsUploadMatcher( url, cdsId, sr, findMode );
        String tableName = "xmatch(" + cdsIdToTableName( cdsId ) + ")";
        JoinFixAction inFixAct =
            fixcolsParam_.getJoinFixAction( env, insuffixParam_ );
        JoinFixAction cdsFixAct =
            fixcolsParam_.getJoinFixAction( env, cdssuffixParam_ );
        final TableProducer inProd = createInputProducer( env );
        final StoragePolicy storage =
            LineTableEnvironment.getStoragePolicy( env );
        final BlockUploader blocker =
            new BlockUploader( umatcher, blocksize, maxrec, tableName,
                               inFixAct, cdsFixAct, remoteUnique );

        /* Create and return an object which will produce the result. */
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                StarTable inTable = Tables.randomTable( inProd.getTable() );
                return blocker.runMatch( inTable, qsFact, storage );
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
