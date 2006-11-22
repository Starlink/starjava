package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.filter.ProcessingStep;

/**
 * Provides the specifications for a single input table.
 *
 * @author   Mark Taylor
 * @since    29 Sep 2006
 */
public abstract class InputTableSpec {

    private final ProcessingStep[] steps_;
    private final String loc_;

    /**
     * Constructor.
     *
     * @param  loc    original table location
     * @param  steps  processing pipeline
     */
    public InputTableSpec( String loc, ProcessingStep[] steps ) {
        loc_ = loc;
        steps_ = steps == null ? new ProcessingStep[ 0 ] : steps;
    }

    /**
     * Returns input table.
     *
     * @return  input table
     */
    public abstract StarTable getInputTable() throws TaskException;

    /**
     * Returns the array of processing steps which constitutes the 
     * processing pipeline.
     *
     * @return   processing pipeline steps
     */
    public ProcessingStep[] getSteps() {
        return steps_;
    }

    /**
     * Returns input table location as specified in the parameter value.
     *
     * @return  input table location
     */
    public String getLocation() {
        return loc_;
    }

    /**
     * Returns the input table processed by all of the accumulated 
     * processing steps associated with this spec.
     *
     * @return   pre-processed table
     */
    public StarTable getWrappedTable() throws IOException, TaskException {
        ProcessingStep[] steps = getSteps();
        StarTable table = getInputTable();
        for ( int i = 0; i < steps.length; i++ ) {
            table = steps[ i ].wrap( table );
        }
        return table;
    }

    /**
     * Returns an InputTableSpec with a fixed table value.
     *
     * @param  loc    original table location
     * @param  steps  processing pipeline
     * @param  table  input table
     * @return  new table spec 
     */
    public static InputTableSpec createSpec( String loc, ProcessingStep[] steps,
                                             final StarTable table ) {
        return new InputTableSpec( loc, steps ) {
            public StarTable getInputTable() {
                return table;
            }
        };
    }
}
