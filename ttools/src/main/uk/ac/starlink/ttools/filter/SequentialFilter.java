package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Processing filter that makes sure any table filtered by it does not
 * have random access.  The table will also not reveal how many rows
 * it has.
 * Only really likely to be used for debugging purposes.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mar 2005
 */
public class SequentialFilter extends BasicFilter implements ProcessingStep {

    public SequentialFilter() {
        super( "sequential", null );
    }

    public ProcessingStep createStep( Iterator argIt ) {
        return this;
    }

    public StarTable wrap( final StarTable base ) throws IOException {
        return new WrapperStarTable( base ) {
            public boolean isRandom() {
                return false;
            }
            public long getRowCount() {
                return -1L;
            }
        };
    }
}
