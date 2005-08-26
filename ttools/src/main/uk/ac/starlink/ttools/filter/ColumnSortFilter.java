package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.ColumnIdentifier;

/**
 * Sort processing step which sorts on one or more column identifiers.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mark 2005
 */
public class ColumnSortFilter extends BasicFilter {

    public ColumnSortFilter() {
        super( "sort",
               "[-down] [-nullsfirst] <colid-list>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "Sorts the table according to the columns named in",
            "<code>&lt;colid-list&gt;</code>.",
            "One or more columns may be specified; sorting is done on",
            "the values in the first-specified field., but if they are",
            "equal the tie is resolved by looking at the second-specified",
            "field, and so on.",
            "If the <code>-down</code> flag is used, the sort order is",
            "descending rather than ascending.",
            "Blank entries are usually considered to come at the end",
            "of the collation sequence, but if the <code>-nullsfirst</code>",
            "flag is given then they are considered to come at the start",
            "instead.",
        };
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
