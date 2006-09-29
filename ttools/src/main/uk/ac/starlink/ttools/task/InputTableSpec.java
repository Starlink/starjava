package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.filter.ProcessingStep;

/**
 * Provides the specifications for a single input table.
 *
 * @author   Mark Taylor
 * @since    29 Sep 2006
 */
public class InputTableSpec {

    private final StarTable table_;
    private final ProcessingStep[] steps_;
    private final String loc_;

    /**
     * Constructor.
     *
     * @param  table  table
     * @param  steps  processing pipeline
     * @param  loc    original table location
     */
    public InputTableSpec( StarTable table, ProcessingStep[] steps,
                           String loc ) {
        table_ = table;
        steps_ = steps == null ? new ProcessingStep[ 0 ] : steps;
        loc_ = loc;
    }

    /**
     * Returns input table.
     *
     * @param  input table
     */
    public StarTable getInputTable() {
        return table_;
    }

    /**
     * Returns the input filter parameter.
     *
     * @param   input filter parameter (may be null)
     */
    public ProcessingStep[] getSteps() {
        return steps_;
    }

    /**
     * Returns input table location as specified in the parameter value.
     *
     * @param  input table location
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
    public StarTable getWrappedTable() throws IOException {
        ProcessingStep[] steps = getSteps();
        StarTable table = getInputTable();
        for ( int i = 0; i < steps.length; i++ ) {
            table = steps[ i ].wrap( table );
        }
        return table;
    }
}
