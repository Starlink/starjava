package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.ColumnIdentifier;

public class ReplaceColumnFilter extends BasicFilter {

    public ReplaceColumnFilter() {
        super( "replacecol", "<col-id> <expr>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "Replaces the content of a column with the value of an",
            "algebraic expression.",
            "The position, name and metadata of the new column are the same",
            "as those of the replaced one (though the data type may change)",
            "but the old values are discarded in favour of the result of",
            "evaluating <code>&lt;expr&gt;</code>.",
            "You can reference the replaced column in the expression,",
            "so for example",
            "\"<code>replacecol pixsize pixsize*2</code>\" just multiplies",
            "the values in column <code>pixsize</code> by 2.",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        String colId = null;
        String expr = null;
        while ( argIt.hasNext() && ( colId == null || expr == null ) ) {
            String arg = (String) argIt.next();
            if ( colId == null ) {
                argIt.remove();
                colId = arg;
            }
            else if ( expr == null ) {
                argIt.remove();
                expr = arg;
            }
        }
        if ( colId != null && expr != null ) {
            return new ReplaceColumnStep( colId, expr );
        }
        else {
            throw new ArgException( "Bad " + getName() + " specification" );
        }
    }

    private static class ReplaceColumnStep implements ProcessingStep {

        final String colId_;
        final String expr_;

        ReplaceColumnStep( String colId, String expr ) {
            colId_ = colId;
            expr_ = expr;
        }

        public StarTable wrap( StarTable base ) throws IOException {

            /* Identify the column to be replaced. */
            int icol = new ColumnIdentifier( base )
                      .getColumnIndex( colId_ );

            /* Add the new column, using the same metadata as the old one. */
            ColumnInfo cinfo = base.getColumnInfo( icol );
            StarTable added;
            try {
                added = new AddJELColumnTable( base, cinfo, expr_, icol );
            }
            catch ( CompilationException e ) {
                String msg = "Bad expression \"" + expr_;
                String errMsg = e.getMessage();
                if ( errMsg != null ) {
                    msg += ": " + errMsg;
                }
                throw (IOException) new IOException( msg )
                                   .initCause( e );
            }

            /* Delete the old column. */
            StarTable removed = ColumnPermutedStarTable
                               .deleteColumns( added, new int[] { icol + 1 } );

            /* Return the result. */
            return removed;
        }
    }
}
