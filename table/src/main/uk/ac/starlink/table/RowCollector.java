package uk.ac.starlink.table;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.SplitCollector;

/**
 * Convenience implementation of <code>SplitCollector</code>
 * for use with table row processing.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2020
 */
public abstract class RowCollector<A>
        implements SplitCollector<RowSplittable,A> {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table" );

    /**
     * Processes rows as required.
     * This method is invoked by {@link #accumulate},
     * with the necessary IOException handling.
     * Implementations do not need to close the supplied row sequence,
     * which will be taken care of elsewhere.
     *
     * @param  rseq  row sequence
     * @param  acc   accumulator
     */
    public abstract void accumulateRows( RowSplittable rseq, A acc )
            throws IOException;

    public final void accumulate( RowSplittable rseq, A acc ) {
        try {
            accumulateRows( rseq, acc );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Data error in parallel processing",
                                        e );
        }
        finally {
            try {
                rseq.close();
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING,
                             "IOException in RowSplittable close: " + e, e );
            }
        }
    }
}
