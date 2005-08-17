package uk.ac.starlink.ttools.filter;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ExplodedStarTable;

/**
 * Table filter for replacing every N-element array valued column in 
 * a table with N scalar-valued columns.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2005
 */
public class ExplodeAllFilter extends BasicFilter {


    public ExplodeAllFilter() {
        super( "explodeall", null );
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) {
                return new ExplodedStarTable( base );
            }
        };
    }
}
