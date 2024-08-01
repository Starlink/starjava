package uk.ac.starlink.ttools.task;

import java.util.Arrays;
import uk.ac.starlink.table.LoopStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;

/**
 * Creates a single-column table with values dispensed from a for loop.
 *
 * @author   Mark Taylor
 * @since    4 Nov 2013
 */
public class TableLoop extends ConsumerTask {

    private final StringParameter varParam_;
    private final DoubleParameter startParam_;
    private final DoubleParameter endParam_;
    private final DoubleParameter stepParam_;
    private final BooleanParameter forceFloatParam_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public TableLoop() {
        super( "Generates a single-column table from a loop variable",
               new ChoiceMode(), true );

        varParam_ = new StringParameter( "colname" );
        varParam_.setPrompt( "Column name" );
        varParam_.setDescription( new String[] {
            "<p>Gives the name of the single column produced by this command.",
            "</p>",
        } );
        varParam_.setStringDefault( "i" );

        stepParam_ = new DoubleParameter( "step" );
        stepParam_.setPrompt( "Loop step value" );
        stepParam_.setDescription( new String[] {
            "<p>Amount by which the loop variable will be incremented",
            "at each iteration, i.e. each table row.",
            "</p>",
        } );
        stepParam_.setDoubleDefault( 1. );

        startParam_ = new DoubleParameter( "start" );
        startParam_.setPrompt( "Loop initial value" );
        startParam_.setDescription( new String[] {
            "<p>Gives the starting value of the loop variable.",
            "This will the the value in the first row of the table.",
            "</p>",
        } );
        startParam_.setDoubleDefault( 0. );

        endParam_ = new DoubleParameter( "end" );
        endParam_.setPrompt( "Loop final value" );
        endParam_.setDescription( new String[] {
            "<p>Gives the value which the loop variable will not exceed.",
            "Exceeding is in the positive or negative sense according to",
            "the sense of the <code>" + stepParam_.getName() + "</code>",
            "parameter, as usual for a <code>for</code>-type loop.",
            "</p>",
        } );

        forceFloatParam_ = new BooleanParameter( "forcefloat" );
        forceFloatParam_.setPrompt( "Force loop column to be floating point?" );
        forceFloatParam_.setDescription( new String[] {
            "<p>Affects the data type of the loop variable column.",
            "If true, the column is always floating point.",
            "If false, and if the other parameters are all of integer type,",
            "the column will be an integer column.",
            "</p>",
        } );
        forceFloatParam_.setBooleanDefault( false );

        int ipos = 0;
        varParam_.setPosition( ++ipos );
        startParam_.setPosition( ++ipos );
        endParam_.setPosition( ++ipos );
        stepParam_.setPosition( ++ipos );

        getParameterList().addAll( Arrays.asList( new Parameter<?>[] {
            varParam_,
            startParam_,
            endParam_,
            stepParam_,
            forceFloatParam_,
        } ) );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        String colname = varParam_.stringValue( env );
        double start = startParam_.doubleValue( env );
        double end = endParam_.doubleValue( env );
        double step = stepParam_.doubleValue( env );
        Boolean isInteger = forceFloatParam_.booleanValue( env )
                          ? Boolean.FALSE : null;
        final StarTable table =
            new LoopStarTable( colname, start, end, step, isInteger );
        return new TableProducer() {
            public StarTable getTable() {
                return table;
            }
        };
    }
}
