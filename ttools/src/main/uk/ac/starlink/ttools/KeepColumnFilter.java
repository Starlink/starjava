package uk.ac.starlink.ttools;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;

/**
 * Table filter for selecting a number of columns and discarding the rest.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public class KeepColumnFilter implements ProcessingFilter {

    public String getName() {
        return "keepcols";
    }

    public String getFilterUsage() {
        return "<colid-list>";
    }

    public ProcessingStep createStep( Iterator argIt ) {
        if ( argIt.hasNext() ) {
            String colIdList = (String) argIt.next();
            argIt.remove();
            return new KeepColumnStep( colIdList.split( "\\s+" ) );
        }
        else {
            return null;
        }
    }

    private static class KeepColumnStep implements ProcessingStep {
        final String[] colIds_;

        KeepColumnStep( String[] colIds ) {
            colIds_ = colIds;
        }

        public StarTable wrap( StarTable base ) throws IOException {
            ColumnIdentifier identifier = new ColumnIdentifier( base );
            int[] colMap = new int[ colIds_.length ];
            for ( int i = 0; i < colIds_.length; i++ ) {
                colMap[ i ] = identifier.getColumnIndex( colIds_[ i ] );
            }
            return new ColumnPermutedStarTable( base, colMap );
        }
    }
}
