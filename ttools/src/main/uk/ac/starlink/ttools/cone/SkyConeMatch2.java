package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperRowAccess;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.table.join.PairMode;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.task.ChoiceMode;
import uk.ac.starlink.ttools.task.JoinFixActionParameter;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.ttools.task.SingleMapperTask;
import uk.ac.starlink.ttools.task.SkyCoordParameter;
import uk.ac.starlink.ttools.task.TableProducer;

/**
 * Crossmatcher which works by performing one cone-search type query 
 * for each row of an input table on an external service of some kind.
 * This is not <i>prima facie</i> a very efficient way of doing a
 * cross match, but if the external service represents a table which 
 * is too large or otherwise unfeasible to access as one term in a
 * normal cone search it's about the only way to do it.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2007
 */
public abstract class SkyConeMatch2 extends SingleMapperTask {

    private final Coner coner_;
    private final StringParameter raParam_;
    private final StringParameter decParam_;
    private final StringParameter srParam_;
    private final StringParameter copycolsParam_;
    private final ChoiceParameter<ConeFindMode> modeParam_;
    private final StringParameter distcolParam_;
    private final BooleanParameter ostreamParam_;
    private final IntegerParameter parallelParam_;
    private final ConeErrorPolicyParameter erractParam_;
    private final JoinFixActionParameter fixcolsParam_;
    private final StringParameter insuffixParam_;
    private final StringParameter conesuffixParam_;
    private final BooleanParameter usefootParam_;
    private final IntegerParameter nsideParam_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param  purpose  one-line description of the purpose of the task
     * @param  coner   object which provides the sky cone search service
     * @param  maxParallel  the largest number of parallel threads which
     *         will be permitted for multi-threaded operation;
     *         1 means single-threaded only, and &lt;=0 means no limit -
     *         use with care!
     */
    @SuppressWarnings("this-escape")
    public SkyConeMatch2( String purpose, Coner coner, int maxParallel ) {
        super( purpose, new ChoiceMode(), true, true );
        coner_ = coner;
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
        String system = coner.getSkySystem();
        String inDescrip = "the input table";
    
        raParam_ =
            SkyCoordParameter.createRaParameter( "ra", system, inDescrip );
        paramList.add( raParam_ );

        decParam_ =
            SkyCoordParameter.createDecParameter( "dec", system, inDescrip );
        paramList.add( decParam_ );

        srParam_ = new StringParameter( "sr" );
        srParam_.setUsage( "<expr/deg>" );
        srParam_.setPrompt( "Search radius in degrees" );
        srParam_.setDescription( new String[] {
            "<p>Expression which evaluates to the search radius in degrees",
            "for the request at each row of the input table.",
            "This will often be a constant numerical value, but may be",
            "the name or ID of a column in the input table,",
            "or a function involving one.",
            "</p>",
        } );
        paramList.add( srParam_ );

        /* Permit "best1" as an undocumented alternative to "best", since
         * it has the meaning of best1 in the pair match tasks. */
        modeParam_ =
            new ChoiceParameter<ConeFindMode>( "find", ConeFindMode.values() ) {
            @Override
            public ConeFindMode stringToObject( Environment env, String sval )
                    throws TaskException {
                return "best1".equalsIgnoreCase( sval )
                     ? ConeFindMode.BEST
                     : super.stringToObject( env, sval );
            }
        };
        modeParam_.setDefaultOption( ConeFindMode.ALL );
        modeParam_.setPrompt( "Type of match to perform" );
        modeParam_.setDescription( new String[] {
            "<p>Determines which matches are retained.",
            "<ul>",
            Arrays.stream( ConeFindMode.values() )
                  .map( mode -> new StringBuffer()
                       .append( "<li><code>" )
                       .append( mode.toString() )
                       .append( "</code>:\n" )
                       .append( mode.getXmlDescription() )
                       .append( "</li>" )
                       .toString() )
                  .collect( Collectors.joining( "\n" ) ),
            "</ul>",
            "</p>",
        } );
        paramList.add( modeParam_ );

        usefootParam_ = new BooleanParameter( "usefoot" );
        usefootParam_.setPrompt( "Use service footprint if available?" );
        usefootParam_.setDescription( new String[] {
            "<p>Determines whether an attempt will be made to restrict",
            "searches in accordance with available footprint information.",
            "If this is set true, then before any of the per-row queries",
            "are performed, an attempt may be made to acquire footprint",
            "information about the servce.",
            "If such information can be obtained, then queries which",
            "fall outside the footprint, and hence which are known to",
            "yield no results, are skipped.  This can speed up the search",
            "considerably.",
            "</p>",
            "<p>Currently, the only footprints available are those",
            "provided by the CDS MOC (Multi-Order Coverage map) service,",
            "which covers VizieR and a few other cone search services.",
            "</p>",
        } );
        usefootParam_.setBooleanDefault( true );
        paramList.add( usefootParam_ );

        nsideParam_ = new IntegerParameter( "footnside" );
        nsideParam_.setPrompt( "HEALPix Nside for footprints" );
        nsideParam_.setDescription( new String[] {
            "<p>Determines the HEALPix Nside parameter for use with the MOC",
            "footprint service.",
            "This tuning parameter determines the resolution of the footprint",
            "if available.",
            "Larger values give better resolution, hence a better chance of",
            "avoiding unnecessary queries, but processing them takes longer",
            "and retrieving and storing them is more expensive.",
            "</p>",
            "<p>The value must be a power of 2,",
            "and at the time of writing, the MOC service will not supply",
            "footprints at resolutions greater than nside=512,",
            "so it should be &lt;=512.",
            "</p>",
            "<p>Only used if <code>" + usefootParam_.getName()
                                     + "=true</code>.",
            "</p>",
        } );
        nsideParam_.setMinimum( 1 );
        nsideParam_.setNullPermitted( true );
        paramList.add( nsideParam_ );

        copycolsParam_ = new StringParameter( "copycols" );
        copycolsParam_.setUsage( "<colid-list>" );
        copycolsParam_.setNullPermitted( true );
        copycolsParam_.setStringDefault( "*" );
        copycolsParam_.setPrompt( "Columns to be copied from input table" );
        copycolsParam_.setDescription( new String[] {
            "<p>List of columns from the input table which are to be copied",
            "to the output table.",
            "Each column identified here will be prepended to the",
            "columns of the combined output table,",
            "and its value for each row taken from the input table row",
            "which provided the parameters of the query which produced it.",
            "See <ref id='colid-list'/> for list syntax.",
            "The default setting is \"<code>*</code>\", which means that",
            "all columns from the input table are included in the output.",
            "</p>",
        } );
        paramList.add( copycolsParam_ );

        distcolParam_ = new StringParameter( "scorecol" );
        distcolParam_.setNullPermitted( true );
        distcolParam_.setStringDefault( "Separation" );
        distcolParam_.setPrompt( "Angular distance output column name" );
        distcolParam_.setUsage( "<col-name>" );
        distcolParam_.setDescription( new String[] {
            "<p>Gives the name of a column in the output table to contain",
            "the distance between the requested central position and the",
            "actual position of the returned row.",
            "The distance returned is an angular distance in degrees.",
            "If a null value is chosen, no distance column will appear",
            "in the output table.",
            "</p>",
        } );
        paramList.add( distcolParam_ );

        parallelParam_ = new IntegerParameter( "parallel" );
        parallelParam_.setIntDefault( 1 );
        parallelParam_.setPrompt( "Number of queries to make in parallel" );
        parallelParam_.setUsage( "<n>" );
        parallelParam_.setMinimum( 1 );
        if ( maxParallel > 0 ) {
            parallelParam_.setMaximum( maxParallel );
        }
        parallelParam_.setDescription( new String[] {
            "<p>Allows multiple cone searches to be performed concurrently.",
            "If set to the default value, 1, the cone query corresponding",
            "to the first row of the input table will be dispatched,",
            "when that is completed the query corresponding to the",
            "second row will be dispatched, and so on.",
            "If set to <code>&lt;n&gt;</code>, then queries will be overlapped",
            "in such a way that up to approximately <code>&lt;n&gt;</code>",
            "may be running at any one time.",
            "</p>",
            "<p>Whether increasing <code>&lt;n&gt;</code> is a good idea,",
            "and what might be a sensible maximum value, depends on the",
            "characteristics of the service being queried.",
            "In particular, setting it to too large a number may overload",
            "the service resulting in some combination of failed queries,",
            "ultimately slower runtimes, and unpopularity with server admins.",
            "</p>",
            "<p>The maximum value permitted for this parameter by default is",
            ParallelResultRowSequence.DEFAULT_MAXPAR + ".",
            "This limit may be raised by use of the",
            ParallelResultRowSequence.MAXPAR_PROP + " system property",
            "but use that option with great care since you may overload",
            "services and make yourself unpopular with data centre admins.",
            "As a rule, you should only increase this value if you have",
            "obtained permission from the data centres whose services",
            "on which you will be using the increased parallelism.",
            "</p>",
        } );
        if ( maxParallel > 1 ) {
            paramList.add( parallelParam_ );
        }

        erractParam_ = new ConeErrorPolicyParameter( "erract" );
        paramList.add( erractParam_ );

        ostreamParam_ = new BooleanParameter( "ostream" );
        ostreamParam_.setBooleanDefault( false );
        ostreamParam_.setPrompt( "Whether output will be strictly streamed" );
        ostreamParam_.setDescription( new String[] {
            "<p>If set true, this will cause the operation to stream on",
            "output, so that the output table is built up as the results",
            "are obtained from the cone search service.",
            "The disadvantage of this is that some output modes and formats",
            "need multiple passes through the data to work, so depending",
            "on the output destination, the operation may fail if this is set.",
            "Use with care (or be prepared for the operation to fail).",
            "</p>",
        } );
        paramList.add( ostreamParam_ );

        fixcolsParam_ = new JoinFixActionParameter( "fixcols" );
        insuffixParam_ =
            fixcolsParam_.createSuffixParameter( "suffix0",
                                                 "the input table", "_0" );
        conesuffixParam_ =
            fixcolsParam_.createSuffixParameter( "suffix1",
                                                 "the cone result table",
                                                 "_1" );
        paramList.add( fixcolsParam_ );
        paramList.add( insuffixParam_ );
        paramList.add( conesuffixParam_ );

        getParameterList().addAll( paramList );
        getParameterList().addAll( Arrays.asList( coner.getParameters() ) );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {

        /* Interrogate environment for parameter values. */
        coner_.configureRadiusParam( env, srParam_ );
        boolean distFilter = coner_.useDistanceFilter( env );
        String copyColIdList = copycolsParam_.stringValue( env );
        String raString = raParam_.stringValue( env );
        String decString = decParam_.stringValue( env );
        String srString = srParam_.stringValue( env );
        final boolean ostream = ostreamParam_.booleanValue( env );
        int parallelism = parallelParam_.intValue( env );
        ConeErrorPolicy erract = erractParam_.policyValue( env );
        if ( erract == ConeErrorPolicy.ABORT ) {
            String advice = "Cone search failed - try other values of "
                          + erractParam_.getName() + " parameter?";
            erract = ConeErrorPolicy
                    .createAdviceAbortPolicy( erract.toString(), advice );
        }
        String distanceCol = distcolParam_.stringValue( env );
        ConeFindMode mode = modeParam_.objectValue( env );
        boolean bestOnly = mode.isBestOnly();
        boolean includeBlanks = mode.isIncludeBlanks();
        TableProducer inProd = createInputProducer( env );
        ConeSearcher coneSearcher = coner_.createSearcher( env, bestOnly );
        final Coverage footprint;
        if ( usefootParam_.booleanValue( env ) &&
             coner_ instanceof ConeSearchConer ) {
            Integer nSide = nsideParam_.objectValue( env );
            if ( nSide != null ) {
                ((ConeSearchConer) coner_).setNside( nSide.intValue() );
            }
            footprint = coner_.getCoverage( env );
        }
        else {
            footprint = null;
        }
        JoinFixAction inFixAct =
            fixcolsParam_.getJoinFixAction( env, insuffixParam_ );
        JoinFixAction coneFixAct =
            fixcolsParam_.getJoinFixAction( env, conesuffixParam_ );
        QuerySequenceFactory qsFact =
            new JELQuerySequenceFactory( raString, decString, srString );

        /* Return a table producer using these values. */
        final ConeMatcher coneMatcher =
            new ConeMatcher( coneSearcher, erract, inProd, qsFact, bestOnly,
                             footprint, includeBlanks, distFilter, parallelism,
                             copyColIdList, distanceCol, inFixAct, coneFixAct );
        coneMatcher.setStreamOutput( true );
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                ConeMatcher.ConeWorker worker = coneMatcher.createConeWorker();
                final Thread thread = new Thread( worker, "Cone Matcher" );
                thread.setDaemon( true );
                thread.start();
                StarTable result = new WrapperStarTable( worker.getTable() ) {
                    public RowSequence getRowSequence() throws IOException {
                        return new WrapperRowSequence( baseTable
                                                      .getRowSequence() ) {
                            public void close() throws IOException {
                                super.close();
                                thread.interrupt();
                            }
                        };
                    }
                    public RowAccess getRowAccess() throws IOException {
                        return new WrapperRowAccess( baseTable
                                                    .getRowAccess() ) {
                            public void close() throws IOException {
                                super.close();
                                thread.interrupt();
                            }
                        };
                    }
                    public RowSplittable getRowSplittable() throws IOException {
                        return Tables.getDefaultRowSplittable( this );
                    }
                };
                return ostream ? result : Tables.randomTable( result );
            }
        };
    }
}
