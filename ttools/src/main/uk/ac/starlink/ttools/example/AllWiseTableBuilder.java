package uk.ac.starlink.ttools.example;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

/**
 * TableBuilder implementation for the ASCII files comprising the AllWise
 * data release.
 * At time of writing these files are available from
 * <a href="http://irsadist.ipac.caltech.edu/wise-allwise/"
 *         >http://irsadist.ipac.caltech.edu/wise-allwise/</a>.
 * The files are formatted as pipe-separated ASCII, and optionally compressed.
 * Note that reading seems to be considerably faster for the gzip than
 * for the bzip2 form of the input files (though the gzip ones are a bit
 * bigger).
 *
 * <p>For FITS output (though not colfits) two passes through the input
 * ASCII file are required, the first one (much faster) just to count the rows.
 * There are therefore two variants of this input handler, one which does a
 * row count and one which does not.
 * If the row count is known, it would be possible to adapt this handler
 * so you tell it the row count up front and only one pass is required.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2014
 */
public class AllWiseTableBuilder implements TableBuilder {

    private final String name_;
    private final boolean preCount_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.examples" );

    /**
     * Default mode constructor.
     */
    public AllWiseTableBuilder() {
        this( "allwise", true );
    }

    /**
     * Configurable constructor.
     *
     * @param  name  handler name
     * @param  preCount  true to return a table that knows its row count
     */
    public AllWiseTableBuilder( String name, boolean preCount ) {
        name_ = name;
        preCount_ = preCount;
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public String getFormatName() {
        return name_;
    }

    public boolean looksLikeFile( String location ) {
        return false;
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        URL schema = getClass().getResource( "allwise-meta-full.txt" );
        if ( schema == null ) {
            throw new IOException( "No schema found" );
        }
        else {
            final long nrow;
            if ( preCount_ ) {
                logger_.info( "Counting allwise lines" );
                nrow = countLines( datsrc );
                logger_.info( "Got " + nrow + " allwise rows" );
            }
            else {
                nrow = -1;
            }
            return new AllWiseAsciiStarTable( datsrc, schema, nrow );
        }
    }

    public void streamStarTable( InputStream in, TableSink sink,
                                 String pos ) {

        /* It would be possible to implement this. */
        throw new UnsupportedOperationException();
    }

    /**
     * Counts the lines in the ASCII file at the given location.
     *
     * @param  datsrc  data source
     * @return   number of '\n'-terminated lines in the file
     */
    public static long countLines( DataSource datsrc ) throws IOException {
        int buflen = 1024 * 1024;
        InputStream in = datsrc.getInputStream();
        byte[] buf = new byte[ buflen ];
        long nrow = 0;
        for ( int n; ( n = in.read( buf, 0, buflen ) ) >= 0; ) {
            for ( int i = 0; i < n; i++ ) {
                if ( buf[ i ] == '\n' ) {
                    nrow++;
                }
            }
        }
        in.close();
        return nrow;
    }

    /**
     * Input handler which reads AllWise ASCII files and does not count
     * the rows first.  Suitable for conversions to most non-FITS formats.
     */
    public static class NoCount extends AllWiseTableBuilder {

        /**
         * No-arg constructor.
         */
        public NoCount() {
           super( "allwise-nocount", false );
        }
    }

    /**
     * Input handler which reads AllWise ASCII files and does count the
     * rows first.  Suitable for conversions to FITS format.
     */
    public static class Count extends AllWiseTableBuilder {

        /**
         * No-arg constructor.
         */
        public Count() {
            super( "allwise-count", true );
        }
    }
}
