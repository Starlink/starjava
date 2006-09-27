package uk.ac.starlink.ttools.filter;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;

public class EveryFilter extends BasicFilter {

    public EveryFilter() {
        super( "every", "<step>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Include only every <code>&lt;step&gt;</code>'th row in the",
            "result, starting with the first row.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        if ( argIt.hasNext() ) {
            String countStr = (String) argIt.next();
            argIt.remove();
            long count;
            try {
                count = Long.parseLong( countStr );
            }
            catch ( NumberFormatException e ) {
                throw new ArgException( "Step value " + countStr + 
                                        " not numeric" );
            }
            if ( count <= 0 ) {
                throw new ArgException( "Step must be >= 1" );
            }
            return new EveryStep( count );
        }
        else {
            throw new ArgException( "No step given" );
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
