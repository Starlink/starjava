package uk.ac.starlink.ttools.cone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.task.ChoiceMode;
import uk.ac.starlink.ttools.task.JoinFixActionParameter;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.ttools.task.SingleMapperTask;
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
    private final Parameter raParam_;
    private final Parameter decParam_;
    private final Parameter srParam_;
    private final Parameter copycolsParam_;
    private final ChoiceParameter modeParam_;
    private final BooleanParameter ostreamParam_;
    private final IntegerParameter parallelParam_;
    private final JoinFixActionParameter fixcolsParam_;
    private final Parameter insuffixParam_;
    private final Parameter conesuffixParam_;

    /**
     * Constructor.
     *
     * @param  purpose  one-line description of the purpose of the task
     * @param  coner   object which provides the sky cone search service
     * @param  allowParallel  if true, provide parameters for selecting
     *         multi-threaded operation
     */
    public SkyConeMatch2( String purpose, Coner coner, boolean allowParallel ) {
        super( purpose, new ChoiceMode(), true, true );
        coner_ = coner;
        List paramList = new ArrayList();
        String system = coner.getSkySystem();
        String sysParen;
        String inSys;
        if ( system == null || system.length() == 0 ) {
            sysParen = "";
            inSys = "";
        }
        else {
            sysParen = " (" + system + ")";
            inSys = " in the " + system + " coordinate system";
        }
    
        raParam_ = new Parameter( "ra" );
        raParam_.setUsage( "<expr>" );
        raParam_.setPrompt( "Right Ascension expression in degrees"
                          + sysParen );
        raParam_.setDescription( new String[] {
            "<p>Expression which evaluates to the right ascension in degrees"
            + inSys,
            "for the request at each row of the input table.",
            "This will usually be the name or ID of a column in the",
            "input table, or a function involving one.",
            "</p>",
        } );
        paramList.add( raParam_ );

        decParam_ = new Parameter( "dec" );
        decParam_.setUsage( "<expr>" );
        decParam_.setPrompt( "Declination expression in degrees"
                           + sysParen );
        decParam_.setDescription( new String[] {
            "<p>Expression which evaluates to the declination in degrees"
            + inSys,
            "for the request at each row of the input table.",
            "This will usually be the name or ID of a column in the",
            "input table, or a function involving one.",
            "</p>",
        } );
        paramList.add( decParam_ );

        srParam_ = new Parameter( "sr" );
        srParam_.setUsage( "<expr>" );
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

        copycolsParam_ = new Parameter( "copycols" );
        copycolsParam_.setUsage( "<colid-list>" );
        copycolsParam_.setNullPermitted( true );
        copycolsParam_.setDefault( "*" );
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

        modeParam_ = new ChoiceParameter( "find", new String[] {
            "best", "all",
        } );
        modeParam_.setDefault( "all" );
        modeParam_.setPrompt( "Type of match to perform" );
        modeParam_.setDescription( new String[] {
            "<p>Determines which matches are retained.",
            "If <code>best</code> is selected, then only the query table row",
            "which best matches the row from the input table will be output.",
            "If <code>all</code> is selected, then any rows in the query table",
            "which match the input table are output.",
            "</p>",
        } );
        paramList.add( modeParam_ );

        parallelParam_ = new IntegerParameter( "parallel" );
        parallelParam_.setDefault( "1" );
        parallelParam_.setPrompt( "Number of queries to make in parallel" );
        parallelParam_.setUsage( "<n>" );
        parallelParam_.setMinimum( 1 );
        parallelParam_.setDescription( new String[] {
            "<p>Allows multiple cone searches to be performed concurrently.",
            "If set to the default value, 1, the cone query corresponding",
            "to the first row of the input table will be dispatched,",
            "when that is completed the query corresponding to the",
            "second row will be dispatched, and so on.",
            "If set to <code>&lt;n&gt;</code>, then queries will be overlapped",
            "in such a way that up to approximately <code>&lt;n&gt;</code>",
            "may be running at any one time.",
            "Whether this is a good idea, and what might be a sensible",
            "maximum value for <code>&lt;n&gt;</code>, depends on the",
            "characteristics of the service being queried.",
            "</p>",
        } );
        if ( allowParallel ) {
            paramList.add( parallelParam_ );
        }

        ostreamParam_ = new BooleanParameter( "ostream" );
        ostreamParam_.setDefault( "false" );
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

    protected TableProducer createProducer( Environment env )
            throws TaskException {

        /* Interrogate environment for parameter values. */
        String copyColIdList = copycolsParam_.stringValue( env );
        String raString = raParam_.stringValue( env );
        String decString = decParam_.stringValue( env );
        String srString = srParam_.stringValue( env );
        boolean ostream = ostreamParam_.booleanValue( env );
        int parallelism = parallelParam_.intValue( env );
        boolean bestOnly;
        String mode = modeParam_.stringValue( env );
        if ( mode.toLowerCase().equals( "best" ) ) {
            bestOnly = true;
        }
        else if ( mode.toLowerCase().equals( "all" ) ) {
            bestOnly = false;
        }
        else {
            throw new UsageException( "Unknown value of " +
                                      modeParam_.getName() + "??" );
        }
        TableProducer inProd = createInputProducer( env );
        ConeSearcher coneSearcher = coner_.createSearcher( env, bestOnly );
        JoinFixAction inFixAct =
            fixcolsParam_.getJoinFixAction( env, insuffixParam_ );
        JoinFixAction coneFixAct =
            fixcolsParam_.getJoinFixAction( env, conesuffixParam_ );
        QuerySequenceFactory qsFact =
            new JELQuerySequenceFactory( raString, decString, srString );

        /* Return a table producer using these values. */
        SkyConeMatch2Producer producer =
            new SkyConeMatch2Producer( coneSearcher, inProd, qsFact,
                                       parallelism, bestOnly, copyColIdList,
                                       inFixAct, coneFixAct );
        producer.setStreamOutput( ostream );
        return producer;
    }
}
