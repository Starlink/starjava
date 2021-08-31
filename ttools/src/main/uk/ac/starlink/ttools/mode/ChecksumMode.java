package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Supplier;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Processing mode for calculating a checksum of table data.
 *
 * @author   Mark Taylor
 * @since    31 Aug 2021
 */
public class ChecksumMode implements ProcessingMode {

    private final Supplier<Checksum> checksumFactory_;

    public ChecksumMode() {
        this( Adler32::new );
    }

    public ChecksumMode( Supplier<Checksum> checksumFactory ) {
        checksumFactory_ = checksumFactory;
    }

    public String getDescription() {
        return String.join( "\n",
            "<p>Calculates a checksum from all the data in the table.",
            "The checksum is written to standard output in hexadecimal;",
            "row and column counts are also written.",
            "</p>",
            "<p>If two tables have the same checksum",
            "it is extremely likely that they contain the same cell data.",
            "If they have a different checksum, their cell data differs.",
            "By default, the checksum implementation uses Adler32,",
            "which is fast but not cryptographically secure.",
            "</p>"
        );
    }

    public Parameter<?>[] getAssociatedParameters() {
        return new Parameter<?>[ 0 ];
    }

    public TableConsumer createConsumer( Environment env ) {
        final PrintStream out = env.getOutputStream();
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                Checksum checksum = checksumFactory_.get();
                int ncol = table.getColumnCount();
                long nrow =
                    Tables.checksumData( table.getRowSequence(), checksum );
                long checkValue = checksum.getValue();
                out.println( "Checksum: " + Long.toHexString( checkValue )
                           + " \tNcol: " + ncol + " \tNrow: " + nrow );
            }
        };
    }
}
