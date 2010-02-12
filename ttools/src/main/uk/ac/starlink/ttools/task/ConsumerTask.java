package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.LineEnvironment;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.filter.ProcessingFilter;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.mode.ProcessingMode;

/**
 * Abstract task which takes an input table and disposes of it.
 * Concrete subclasses must supply the table.
 *
 * @author   Mark Taylor
 * @since    29 Aug 2005
 */
public abstract class ConsumerTask implements Task {

    private final FilterParameter outFilterParam_;
    private final ProcessingMode outMode_;
    private final String purpose_;
    private List paramList_;

    /**
     * Constructor.
     *
     * @param   purpose  one-line description of the task
     * @param   outMode  processing mode which determines the destination of
     *          the processed table
     * @param   useOutFilter allow specification of filters for output table
     */
    public ConsumerTask( String purpose, ProcessingMode outMode,
                         boolean useOutFilter ) {
        purpose_ = purpose;
        outMode_ = outMode;
        paramList_ = new ArrayList();

        /* Output filter. */
        if ( useOutFilter ) {
            outFilterParam_ = new FilterParameter( "ocmd" );
            outFilterParam_.setPrompt( "Processing command(s) " 
                                     + "for output table" );
            outFilterParam_.setDescription( new String[] {
                "<p>Commands to operate on the output table,",
                "after all other processing has taken place.",
                "</p>",
                outFilterParam_.getDescription(),
            } );
            paramList_.add( outFilterParam_ );
        }
        else {
            outFilterParam_ = null;
        }

        /* Set output parameter list. */
        paramList_.addAll( Arrays.asList( outMode.getAssociatedParameters() ) );
    }

    public String getPurpose() {
        return purpose_;
    }

    public Parameter[] getParameters() {
        return (Parameter[]) paramList_.toArray( new Parameter[ 0 ] );
    }

    /**
     * Returns the parameter list for this task; it may be modified.
     *
     * @return  parameter list
     */
    protected List getParameterList() {
        return paramList_;
    }

    /**
     * Returns an object which can produce the effective output table which
     * will be consumed by this task.
     *
     * @param   env  execution environment
     * @return  table producer
     */
    public abstract TableProducer createProducer( Environment env )
            throws TaskException;

    public Executable createExecutable( Environment env ) throws TaskException {

        /* Get the object which will provide the effective input table. */
        final TableProducer producer = createProducer( env );

        /* Get a sequence of post-processing steps for the output table. */
        final ProcessingStep[] outSteps = outFilterParam_ != null
                                        ? outFilterParam_.stepsValue( env )
                                        : new ProcessingStep[ 0 ];

        /* Get the table consumer, which defines the output table's final
         * destination. */
        final TableConsumer baseConsumer = outMode_.createConsumer( env );

        /* Construct a consumer which will combine the post-processing
         * and the final disposal. */
        final TableConsumer consumer = new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                for ( int i = 0; i < outSteps.length; i++ ) {
                    table = outSteps[ i ].wrap( table );
                }
                baseConsumer.consume( table );
            }
        };

        /* Check unused arguments for things we can write helpful messages
         * about. */
        checkUnused( env );

        /* Construct and return an executable which will do all the work. */
        return new Executable() {
            public void execute() throws IOException, TaskException {
                consumer.consume( producer.getTable() );
            }
        };
    }

    /**
     * Returns this task's output mode.
     *
     * @return  output mode
     */
    public ProcessingMode getOutputMode() {
        return outMode_;
    }

    /**
     * Checks the unused words in the environment in case we can write any
     * useful messages.
     *
     * @param  env  execution environment
     * @throws  TaskException   if there's trouble
     */
    private void checkUnused( Environment env ) throws TaskException {
        if ( env instanceof LineEnvironment ) {
            String[] unused = ((LineEnvironment) env).getUnused();
            for ( int i = 0; i < unused.length; i++ ) {
                String word = unused[ i ];
                if ( word.startsWith( "out=" ) || word.startsWith( "ofmt=" ) ) {
                    throw new UsageException(
                        word + ": out and ofmt parameters can only be used " +
                        "when omode=out" );
                }
                if ( word.startsWith( "script=" ) ) {
                    throw new UsageException(
                        word + ": script parameter withdrawn (use cmd=@file)" );
                }
            }
        }
    }

    /**
     * Constructs a table producer given an input parameter and an
     * input filter parameter.
     *
     * @param   env  execution environment
     * @param   filterParam  parameter giving filter steps (or null)
     * @param   inParam  parameter giving input table
     * @return  table producer
     */
    public static TableProducer createProducer( Environment env, 
                                                FilterParameter filterParam,
                                                InputTableParameter inParam )
            throws TaskException {
        final StarTable inTable = inParam.tableValue( env );
        final ProcessingStep[] steps = filterParam == null
                                     ? new ProcessingStep[ 0 ]
                                     : filterParam.stepsValue( env );
        return new TableProducer() {
            public StarTable getTable() throws IOException {
                StarTable table = inTable;
                for ( int i = 0; i < steps.length; i++ ) {
                    table = steps[ i ].wrap( table );
                }
                return table;
            }
        };
    }
}
