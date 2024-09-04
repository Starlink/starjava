package uk.ac.starlink.parquet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.IOSupplier;

/**
 * Utility classes for Parquet I/O.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2021
 */
public class ParquetUtil {

    private static final IOSupplier<ParquetIO> ioSupplier_ = createIOSupplier();

    /**
     * Private constructor prevents instantiation.
     */
    private ParquetUtil() {
    }

    /**
     * Indicates whether the given buffer starts with the Parquet file
     * format magic number, "PAR1".  Note that Parquet files have to
     * both start and end with this sequence.
     *
     * @param   buffer   byte content
     * @return   true iff the first four bytes in the buffer are ASCII "PAR1"
     */
    public static boolean isMagic( byte[] buffer ) {
        return buffer.length >= 4 &&
               (char) buffer[ 0 ] == 'P' &&
               (char) buffer[ 1 ] == 'A' &&
               (char) buffer[ 2 ] == 'R' &&
               (char) buffer[ 3 ] == '1';
    }

    /**
     * Returns the ParquetIO object that handles all the
     * parquet-mr-dependent I/O.
     * If the parquet-mr libraries are not present, an IOException
     * with a suitable message will be thrown.
     *
     * @return  parquetIO object, not null
     * @throws  IOException  if libraries are not present
     */
    public static ParquetIO getIO() throws IOException {
        return ioSupplier_.get();
    }

    /**
     * Prepares a supplier for ParquetIO objects.
     * Called once at static initialisation.
     *
     * @return   ParquetIO supplier
     */
    private static IOSupplier<ParquetIO> createIOSupplier() {
        Logger logger = Logger.getLogger( "uk.ac.starlink.parquet" );
        try {
            final ParquetIO io = new ParquetIO();
            logger.info( "Parquet support available" );
            return () -> io;
        }
        catch ( final Throwable e ) {
            logger.log( Level.INFO, "No Parquet support (parquet-mr missing)",
                        e );
            return () -> {
                throw new IOException( "Parquet-mr libraries not available",
                                       e );
            };
        }
    }
}
