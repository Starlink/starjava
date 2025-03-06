package uk.ac.starlink.ttools.filter;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

public class EveryFilter extends BasicFilter {

    public EveryFilter() {
        super( "every", "[-exact|-approx] <step>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Include only every <code>&lt;step&gt;</code>'th row in the",
            "result, starting with the first row.",
            "The optional <code>-approx</code>/<code>-exact</code> argument",
            "controls whether the selection needs to be exact;",
            "in some cases an approximate calculation can take advantage",
            "of parallelism where an exact one cannot.",
            "</p>",
            "<p>The <code>&lt;step&gt;</code> argument",
            Tables.PARSE_COUNT_MAY_BE_GIVEN,
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        String countStr = null;
        boolean isExact = true;
        while ( argIt.hasNext() && countStr == null ) {
            String arg = argIt.next();
            if ( arg.equals( "-exact" ) ) {
                argIt.remove();
                isExact = true;
            }
            else if ( arg.startsWith( "-approx" ) ) {
                argIt.remove();
                isExact = false;
            }
            else if ( arg.startsWith( "-" ) ) {
                argIt.remove();
                throw new ArgException( "Unknown flag " + arg );
            }
            else {
                argIt.remove();
                countStr = arg;
            }
        }
        if ( countStr == null ) {
            throw new ArgException( "No step given" );
        }
        long count;
        try {
            count = Tables.parseCount( countStr );
        }
        catch ( NumberFormatException e ) {
            throw new ArgException( "Step value " + countStr + " not numeric" );
        }
        assert count >= 0;
        return new EveryStep( count, isExact );
    }

    private static class EveryStep implements ProcessingStep {
        final long count_;
        final boolean isExact_;
        EveryStep( long count, boolean isExact ) {
            count_ = count;
            isExact_ = isExact;
        }
        public StarTable wrap( StarTable base ) {
            return new EveryTable( base, count_, isExact_ );
        }
    }
}
