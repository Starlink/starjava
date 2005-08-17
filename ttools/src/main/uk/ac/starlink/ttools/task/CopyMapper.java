package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Mapper which just copies the input table to the output.
 *
 * @author   Mark Taylor
 * 
 */
public class CopyMapper implements TableMapper, TableMapping {

    public int getInCount() {
        return 1;
    }

    public Parameter[] getParameters() {
        return new Parameter[ 0 ];
    }

    public TableMapping createMapping( Environment env ) {
        return this;
    }

    public void mapTables( StarTable[] in, TableConsumer[] out )
            throws IOException {
        if ( in.length != 1 || out.length != 1 ) {
            throw new IllegalArgumentException();
        }
        out[ 0 ].consume( in[ 0 ] );
    }
}
