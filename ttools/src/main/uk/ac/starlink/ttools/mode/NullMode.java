package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.filter.AssertFilter;

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
        return DocUtils.join( new String[] {
           "<p>Reads all the data in the table in sequential mode",
           "and discards it.",
           "May be useful in conjunction with",
           "the " + DocUtils.filterRef( new AssertFilter() ) + " filter.",
           "</p>",
        } );
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
