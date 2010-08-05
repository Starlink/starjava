package uk.ac.starlink.ttools.filter;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;

/**
 * Filter for repeating a table's rows multiple times.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2010
 */
public class RepeatFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public RepeatFilter() {
        super( "repeat", "<count>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Repeats the rows of a table multiple times to produce",
            "a longer table.",
            "The output table will have <code>&lt;count&gt;</code> times",
            "as many rows as the input table.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        if ( argIt.hasNext() ) {
            String countStr = (String) argIt.next();
            argIt.remove();
            final long count;
            try {
                count = Long.parseLong( countStr );
            }
            catch ( NumberFormatException e ) {
                throw new ArgException( "Count " + countStr + " not numeric" );
            }
            if ( count < 0 ) {
                throw new ArgException( "count must be >= 0" );
            }
            return new ProcessingStep() {
                public StarTable wrap( StarTable base ) {
                    return new RepeatTable( base, count );
                }
            };
        }
        else {
            throw new ArgException( "No count argument given" );
        }
    }
}
