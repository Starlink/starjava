package uk.ac.starlink.ttools.pipe;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.ArgException;
import uk.ac.starlink.ttools.ColumnIdentifier;

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

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
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
            return new ColumnSortStep( colidList, up, nullsLast );
        }
        else {
            throw new ArgException( "Missing column list" );
        }
    }

    private static class ColumnSortStep implements ProcessingStep {
        final String colidList_;
        final boolean up_;
        final boolean nullsLast_;

        ColumnSortStep( String colidList, boolean up, boolean nullsLast ) {
            colidList_ = colidList;
            up_ = up;
            nullsLast_ = nullsLast;
        }

        public StarTable wrap( StarTable base ) throws IOException {
            base = Tables.randomTable( base );
            int[] colIndices = new ColumnIdentifier( base )
                              .getColumnIndices( colidList_ );
            return Tables.sortTable( base, colIndices, up_, nullsLast_ );
        }
    }
}
