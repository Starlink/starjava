package uk.ac.starlink.ttools.pipe;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.ArgException;

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

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        if ( argIt.hasNext() ) {
            String colIdList = (String) argIt.next();
            argIt.remove();
            final String[] colIds = colIdList.split( "\\s+" );
            return new ProcessingStep() {
                public StarTable wrap( StarTable base ) throws IOException {
                    return keepColumnTable( base, colIds );
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
    public static StarTable keepColumnTable( StarTable table, String[] colIds )
            throws IOException {
        ColumnIdentifier identifier = new ColumnIdentifier( table );
        int[] colMap = new int[ colIds.length ];
        for ( int i = 0; i < colIds.length; i++ ) {
            colMap[ i ] = identifier.getColumnIndex( colIds[ i ] );
        }
        return new ColumnPermutedStarTable( table, colMap );
    }

}
