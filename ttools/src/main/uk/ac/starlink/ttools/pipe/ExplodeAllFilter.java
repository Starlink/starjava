package uk.ac.starlink.ttools.pipe;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ExplodedStarTable;
import uk.ac.starlink.ttools.ArgException;

/**
 * Table filter for replacing every N-element array valued column in 
 * a table with N scalar-valued columns.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2005
 */
public class ExplodeAllFilter implements ProcessingFilter {

    public String getName() {
        return "explodeall";
    }

    public String getFilterUsage() {
        return null;
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) {
                return new ExplodedStarTable( base );
            }
        };
    }
}
