package uk.ac.starlink.ttools.task;

import java.util.Arrays;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;

/**
 * General purpose task for copying multiple input tables to an output
 * table container.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2010
 */
public class TableMultiCopy extends MultiOutputTask {

    private final TablesInput tablesInput_;

    /**
     * Constructor.
     *
     * @param   purpose  task purpose
     * @param   tablesInput  input parameter object for tables
     */
    @SuppressWarnings("this-escape")
    public TableMultiCopy( String purpose, TablesInput tablesInput ) {
        super( purpose );
        tablesInput_ = tablesInput;
        getParameterList().addAll( 0, Arrays.asList( tablesInput
                                                    .getParameters() ) );
    }

    protected TableSequence createTableSequence( Environment env )
            throws TaskException {
        return createTableSequence( tablesInput_.getInputSpecs( env ) );
    }
}
