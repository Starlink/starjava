package uk.ac.starlink.ttools;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;

public class EveryFilter implements ProcessingFilter {

    public String getName() {
        return "every";
    }

    public String getFilterUsage() {
        return "<step>";
    }

    public ProcessingStep createStep( Iterator argIt ) {
        if ( argIt.hasNext() ) {
            long count = Long.parseLong( (String) argIt.next() );
            if ( count <= 0 ) {
                throw new IllegalArgumentException( "Step must be >= 1" );
            }
            argIt.remove();
            return new EveryStep( count );
        }
        else {
            return null;
        }
    }

    private static class EveryStep implements ProcessingStep {
        final long count_;
        EveryStep( long count ) {
            count_ = count;
        }
        public StarTable wrap( StarTable base ) {
            return new EveryTable( base, count_ );
        }
    }
}
