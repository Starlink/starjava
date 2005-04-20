package uk.ac.starlink.ttools.pipe;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;

/**
 * Filter for picking only the last few rows of a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mar 2005
 */
public class TailFilter implements ProcessingFilter {

    public String getName() {
        return "tail";
    }

    public String getFilterUsage() {
        return "<nrows>";
    }

    public ProcessingStep createStep( Iterator argIt ) {
        if ( argIt.hasNext() ) {
            long count = Long.parseLong( (String) argIt.next() );
            if ( count < 0 ) {
                throw new IllegalArgumentException( "Nrows must be >= 0" );
            }
            argIt.remove();
            return new TailStep( count );
        }
        else {
            return null;
        }
    }

    private static class TailStep implements ProcessingStep {
        final long count_;

        TailStep( long count ) {
            count_ = count;
        }

        public StarTable wrap( StarTable base ) {
            return new TailTable( base, count_ );
        }
    }
}
