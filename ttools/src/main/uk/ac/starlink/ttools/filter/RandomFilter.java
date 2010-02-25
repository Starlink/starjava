package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Processing step which calls the
 * {@link uk.ac.starlink.table.Tables#randomTable} utility.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2010
 */
public class RandomFilter extends BasicFilter implements ProcessingStep {

    public RandomFilter() {
        super( "random", null );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Ensures that random access is available on this table.",
            "If the table currently has random access, it has no effect.",
            "If only sequential access is available, the table is",
            "<ref id='cache'>cached</ref>",
            "so that downstream steps will see the cached,",
            "hence random-access, copy.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) {
        return this;
    }

    public StarTable wrap( StarTable in ) throws IOException {
        return Tables.randomTable( in );
    }
}
