package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Mapper which maps input tables directly to output ones with no change.
 *
 * @author   Mark Taylor
 * @since    22 May 2006
 */
public class TrivialMapper implements TableMapper, TableMapping {

    private final int nTable_;

    /**
     * Constructor.
     *
     * @param   nTable  number of tables (both input and output) to map
     */
    public TrivialMapper( int nTable ) {
        nTable_ = nTable;
    }

    public Parameter[] getParameters() {
        return new Parameter[ 0 ];
    }

    public TableMapping createMapping( Environment env ) throws TaskException {
        return this;
    }

    public void mapTables( StarTable[] in, TableConsumer[] out )
            throws IOException {
        for ( int i = 0; i < nTable_; i++ ) {
            out[ i ].consume( in[ i ] );
        }
    }
}
