package uk.ac.starlink.ttools;

import java.util.Iterator;
import uk.ac.starlink.table.ExplodedStarTable;
import uk.ac.starlink.table.StarTable;

/**
 * Table filter for replacing every N-element array-valued columns
 * with N scalar-valued columns.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mark 2005
 */
public class ExplodeFilter implements ProcessingFilter {

    public String getName() {
        return "explode";
    }

    public String getFilterUsage() {
        return null;
    }

    public ProcessingStep createStep( Iterator argIt ) {
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) {
                return new ExplodedStarTable( base );
            }
        };
    }
}
