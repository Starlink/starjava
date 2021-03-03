package uk.ac.starlink.parquet;

import java.util.Collections;
import java.util.List;

/**
 * Utility classes for Parquet I/O.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2021
 */
public class ParquetUtil {

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
     * Suppress all output from Log4j.
     */
    public static void silenceLog4j() {
        @SuppressWarnings("unchecked")
        List<org.apache.log4j.Logger> loggers =
            Collections.list( org.apache.log4j.LogManager.getCurrentLoggers() );
        loggers.add( org.apache.log4j.LogManager.getRootLogger() );
        for ( org.apache.log4j.Logger logger : loggers ) {
            logger.setLevel( org.apache.log4j.Level.OFF );
        }
    }
}
