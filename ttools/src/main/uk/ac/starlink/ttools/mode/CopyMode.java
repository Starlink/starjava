package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.formats.TextTableWriter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.task.OutputFormatParameter;
import uk.ac.starlink.ttools.task.OutputTableParameter;
import uk.ac.starlink.ttools.task.TableEnvironment;

/**
 * Processing mode which writes out a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    11 Feb 2005
 */
public class CopyMode implements ProcessingMode {

    private final OutputTableParameter locParam_;
    private final Parameter formatParam_;

    public CopyMode() {
        locParam_ = new OutputTableParameter( "out" );
        formatParam_ = locParam_.getFormatParameter();
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            locParam_,
            formatParam_,
        };
    }

    public String getDescription() {
        return "Writes a new table.";
    }

    public TableConsumer createConsumer( Environment env ) 
            throws TaskException {
        String loc = locParam_.stringValue( env );
        String fmt = formatParam_.stringValue( env );
        return createConsumer( env, loc, fmt );
    }

    /**
     * Creates a TableConsumer which writes to a given location.
     *
     * @param   env  execution environment
     * @param   loc  location of output
     * @param   fmt  output format name
     */
    public static TableConsumer createConsumer( Environment env,
                                                String loc, String fmt )
            throws UsageException {
        return new CopyConsumer( loc, fmt, 
                                 TableEnvironment.getTableOutput( env ) );
    }

    /**
     * Consumer implementation which writes out a table.
     */
    private static class CopyConsumer implements TableConsumer {

        final String loc_;
        final StarTableOutput tout_;
        final StarTableWriter handler_;

        /**
         * Constructor.
         *
         * @param   loc  table location
         * @param   fmt  output format
         */
        CopyConsumer( String loc, String fmt, StarTableOutput tout )
                throws UsageException {
            tout_ = tout;
            if ( loc == null && fmt == null ) {
                handler_ = new TextTableWriter();
            }
            else {
                try {
                    handler_ = tout.getHandler( fmt, loc );
                }
                catch ( TableFormatException e ) {
                    String msg = fmt == null 
                               ? ( "Can't guess output format for " + loc )
                               : ( "No handler for output format " + fmt );
                    throw new UsageException( msg );
                }
            }
            loc_ = loc;
        }

        public void consume( StarTable table ) throws IOException {
            handler_.writeStarTable( table, loc_, tout_ );
        }
    }
}
