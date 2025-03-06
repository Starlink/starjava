package uk.ac.starlink.ttools.filter;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Filter for picking only the first few rows of a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class HeadFilter extends BasicFilter {

    public HeadFilter() {
        super( "head", "<nrows>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Include only the first <code>&lt;nrows&gt;</code> rows of",
            "the table.",
            "If the table has fewer than <code>&lt;nrows&gt;</code> rows",
            "then it will be unchanged.",
            "</p>",
            "<p>The <code>&lt;nrows&gt;</code> argument",
            Tables.PARSE_COUNT_MAY_BE_GIVEN,
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        if ( argIt.hasNext() ) {
            String countStr = argIt.next();
            argIt.remove();
            long count;
            try {
                count = Tables.parseCount( countStr );
            }
            catch ( NumberFormatException e ) {
                throw new ArgException( "Row count " + countStr + 
                                        " not numeric" );
            }
            assert count >= 0;
            return new HeadStep( count );
        }
        else {
            throw new ArgException( "No row count given" );
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
