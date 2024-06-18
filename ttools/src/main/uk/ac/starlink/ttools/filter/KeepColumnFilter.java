package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

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

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Select the columns from the input table which will be",
            "included in the output table.",
            "The output table will include only those columns listed in",
            "<code>&lt;colid-list&gt;</code>, in that order.",
            "The same column may be listed more than once,",
            "in which case it will appear in the output table more than once.",
            "</p>",
            explainSyntax( new String[] { "colid-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        if ( argIt.hasNext() ) {
            final String colIdList = argIt.next();
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
     * @param  colIdList  list of column IDs, one for each column 
     *         in the output table
     * @return  new table using columns selected from <code>table</code>
     */
    public static StarTable keepColumnTable( StarTable table, String colIdList )
            throws IOException {
        int[] colMap = new ColumnIdentifier( table )
                      .getColumnIndices( colIdList );
        return new ColumnPermutedStarTable( table, colMap );
    }

}
