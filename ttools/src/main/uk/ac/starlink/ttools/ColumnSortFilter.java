package uk.ac.starlink.ttools;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Sort processing step which sorts on one or more column identifiers.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mark 2005
 */
public class ColumnSortFilter implements ProcessingFilter {

    public String getName() {
        return "sort";
    }

    public String getFilterUsage() {
        return "[-down] [-nullsfirst] <colid-list>";
    }

    public ProcessingStep createStep( Iterator argIt ) {
        boolean up = true;
        boolean nullsLast = true;
        String colidList = null;
        while ( argIt.hasNext() && ( colidList == null ) ) {
            String arg = (String) argIt.next();
            if ( arg.equals( "-down" ) ) {
                argIt.remove();
                up = false;
            }
            else if ( arg.equals( "-nullsfirst" ) ) {
                argIt.remove();
                nullsLast = false;
            }
            else {
                argIt.remove();
                colidList = arg;
            }
        }
        if ( colidList != null ) {
            return new ColumnSortStep( colidList.split( "\\s+" ), 
                                       up, nullsLast );
        }
        else {
            return null;
        }
    }

    private static class ColumnSortStep implements ProcessingStep {
        final String[] colIds_;
        final boolean up_;
        final boolean nullsLast_;

        ColumnSortStep( String[] colIds, boolean up, boolean nullsLast ) {
            colIds_ = colIds;
            up_ = up;
            nullsLast_ = nullsLast;
        }

        public StarTable wrap( StarTable base ) throws IOException {
            ColumnIdentifier identifier = new ColumnIdentifier( base );
            int[] colIndices = new int[ colIds_.length ];
            for ( int i = 0; i < colIds_.length; i++ ) {
                colIndices[ i ] = identifier.getColumnIndex( colIds_[ i ] );
            }
            return Tables.sortTable( base, colIndices, up_, nullsLast_ );
        }
    }
}
