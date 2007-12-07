package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Table filter for deleting a single column.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public class DeleteColumnFilter extends BasicFilter {

    public DeleteColumnFilter() {
        super( "delcols", "<colid-list>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Delete the specified columns.",
            "The same column may harmlessly be specified more than once.",
            "</p>",
            explainSyntax( new String[] { "colid-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        if ( argIt.hasNext() ) {
            String colIdList = (String) argIt.next();
            argIt.remove();
            return new DeleteColumnStep( colIdList );
        }
        else {
            throw new ArgException( "Missing column list" );
        }
    }

    private static class DeleteColumnStep implements ProcessingStep {
        final String colidList_;

        DeleteColumnStep( String colidList ) {
            colidList_ = colidList;
        }

        public StarTable wrap( StarTable base ) throws IOException {
            int[] idels = new ColumnIdentifier( base )
                         .getColumnIndices( colidList_ );
            return ColumnPermutedStarTable.deleteColumns( base, idels );
        }
    }
}
