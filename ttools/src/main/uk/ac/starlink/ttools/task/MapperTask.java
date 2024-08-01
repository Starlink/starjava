package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
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
    private final TablesInput tablesInput_;

    /**
     * Constructor.
     *
     * @param   purpose  one-line description of the task
     * @param   outMode  processing mode which determines the destination of
     *          the processed table
     * @param   useOutFilter allow specification of filters for output table
     * @param   mapper   object which defines mapping transformation
     * @param   tablesInput  object which can acquire multiple input tables
     *          from the environment
     */
    @SuppressWarnings("this-escape")
    public MapperTask( String purpose, ProcessingMode outMode,
                       boolean useOutFilter, TableMapper mapper,
                       TablesInput tablesInput ) {
        super( purpose, outMode, useOutFilter );
        mapper_ = mapper;
        tablesInput_ = tablesInput;
        List<Parameter<?>> paramList = getParameterList();
        paramList.addAll( 0, Arrays.asList( tablesInput.getParameters() ) ); 
        paramList.addAll( Arrays.asList( mapper.getParameters() ) );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        final InputTableSpec[] inSpecs = tablesInput_.getInputSpecs( env );
        final TableMapping mapping =
            mapper_.createMapping( env, inSpecs.length );
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

    /**
     * Returns the object used for acquiring input tables from the environment.
     *
     * @return  tables input
     */
    public TablesInput getTablesInput() {
        return tablesInput_;
    }
}
