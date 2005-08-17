package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.ColumnIdentifier;

/**
 * Table filter for selecting a number of columns and discarding the rest.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public class KeepColumnFilter extends BasicFilter {

    public KeepColumnFilter() {
        super( "keepcols", "<colid-list>" );
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        if ( argIt.hasNext() ) {
            final String colIdList = (String) argIt.next();
            argIt.remove();
            return new ProcessingStep() {
                public StarTable wrap( StarTable base ) throws IOException {
                    return keepColumnTable( base, colIdList );
                }
            };
        }
        else {
            throw new ArgException( "Missing column list" );
        }
    }

    /**
     * Returns a table which selects a number of columns from its base 
     * table by column ID.
     *
     * @param  table  base table
     * @param  colIds  array of column IDs, one for each row in the output
     *         table
     * @return  new table using columns selected from <tt>table</tt>
     */
    public static StarTable keepColumnTable( StarTable table, String colIdList )
            throws IOException {
        int[] colMap = new ColumnIdentifier( table )
                      .getColumnIndices( colIdList );
        return new ColumnPermutedStarTable( table, colMap );
    }

}
