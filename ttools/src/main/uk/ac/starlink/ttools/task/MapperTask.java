package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.Arrays;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.mode.ProcessingMode;

/**
 * Task which maps one or more input tables to an output table.
 * This class provides methods to acquire the table sources and sink;
 * any actual transformation work is done by a separate 
 * {@link TableMapper} object.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public abstract class MapperTask extends ConsumerTask {

    private final TableMapper mapper_;

    /**
     * Constructor.
     *
     * @param   purpose  one-line description of the task
     * @param   outMode  processing mode which determines the destination of
     *          the processed table
     * @param   useOutFilter allow specification of filters for output table
     * @param   mapper   object which defines mapping transformation
     */
    public MapperTask( String purpose, ProcessingMode outMode,
                       boolean useOutFilter, TableMapper mapper ) {
        super( purpose, outMode, useOutFilter );
        mapper_ = mapper;
        getParameterList().addAll( Arrays.asList( mapper.getParameters() ) );
    }

    /**
     * Returns an array of InputTableSpec objects describing the input tables
     * used by this task.
     *
     * @param    env  execution environment
     * @return   input table specifiers
     */
    protected abstract InputTableSpec[] getInputSpecs( Environment env )
            throws TaskException;

    protected TableProducer createProducer( Environment env )
            throws TaskException {
        final InputTableSpec[] inSpecs = getInputSpecs( env );
        final TableMapping mapping = mapper_.createMapping( env );
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                return mapping.mapTables( inSpecs );
            }
        };
    }

    /**
     * Returns this task's Mapper object.
     *
     * @return  mapper
     */
    public TableMapper getMapper() {
        return mapper_;
    }
}
