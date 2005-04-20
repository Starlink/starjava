package uk.ac.starlink.ttools.pipe;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;

/**
 * Filter for picking only the first few rows of a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class HeadFilter implements ProcessingFilter {

    public String getName() {
        return "head";
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
            return new HeadStep( count );
        }
        else {
            return null;
        }
    }

    private static class HeadStep implements ProcessingStep {
        final long count_;

        HeadStep( long count ) {
            count_ = count;
        }

        public StarTable wrap( StarTable base ) {
            return new HeadTable( base, count_ );
        }
    }
}
