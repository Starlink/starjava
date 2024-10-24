package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.formats.TextTableWriter;
import uk.ac.starlink.table.jdbc.WriteMode;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.ttools.task.OutputFormatParameter;
import uk.ac.starlink.ttools.task.OutputTableParameter;

/**
 * Processing mode which writes out a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    11 Feb 2005
 */
public class CopyMode implements ProcessingMode {

    private final OutputTableParameter locParam_;
    private final Parameter<?> formatParam_;

    /** Name of output file parameter. */
    public static final String OUT_PARAM_NAME = "out";

    public CopyMode() {
        locParam_ = new OutputTableParameter( OUT_PARAM_NAME );
        formatParam_ = locParam_.getFormatParameter();
    }

    public Parameter<?>[] getAssociatedParameters() {
        return new Parameter<?>[] {
            locParam_,
            formatParam_,
        };
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Writes a new table.",
            "</p>",
        } );
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
        if ( loc.startsWith( "jdbc:" ) ) {
            if ( ! isAuto( fmt ) && ! fmt.trim().equalsIgnoreCase( "jdbc" ) ) {
                throw new UsageException( "jdbc: output location does not "
                                        + "match output format " + fmt );
            }
            return new JdbcConsumer( loc, env, WriteMode.DROP_CREATE );
        }
        else {
            return new CopyConsumer( loc, fmt,
                                     LineTableEnvironment
                                    .getTableOutput( env ) );
        }
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
            boolean isAuto = isAuto( fmt );
            if ( loc == null && isAuto ) {
                handler_ = new TextTableWriter();
            }
            else {
                try {
                    handler_ = tout.getHandler( fmt, loc );
                }
                catch ( TableFormatException e ) {
                    String msg = isAuto
                               ? ( "Can't guess output format for " + loc )
                               : ( "No handler for output format " + fmt );
                    throw new UsageException( msg, e );
                }
            }
            loc_ = loc;
        }

        public void consume( StarTable table ) throws IOException {
            handler_.writeStarTable( table, loc_, tout_ );
        }
    }

    /**
     * Determines whether a format string has the meaning of automatic
     * format determination.
     *
     * @param  fmt   format string
     * @return   true iff format should be determined automatically
     */
    private static boolean isAuto( String fmt ) {
        return fmt == null
            || fmt.trim().length() == 0
            || fmt.equals( StarTableOutput.AUTO_HANDLER );
    }
}
