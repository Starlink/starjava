package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Processing mode which reads all data and disposes of it.
 *
 * @author   Mark Taylor
 * @since    2 May 2006
 */
public class NullMode implements ProcessingMode {

    public Parameter[] getAssociatedParameters() {
        return new Parameter[ 0 ];
    }

    public String getDescription() {
        return new StringBuffer()
       .append( "Reads all the data in the table in sequential mode\n" )
       .append( "and discards it.\n" )
       .append( "May be useful in conjunction with the <code>assert</code>\n" )
       .append( "filter." )
       .toString();
    }

    public TableConsumer createConsumer( Environment env ) {
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                RowSequence rseq = table.getRowSequence();
                try {
                    while ( rseq.next() ) {
                        rseq.getRow();
                    }
                }
                finally {
                    rseq.close();
                }
            }
        };
    }
}
