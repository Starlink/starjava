package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;

/**
 * Table filter for selecting only certain rows using a JEL expression.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mark 2005
 */
public class SelectFilter extends BasicFilter {

    public SelectFilter() {
        super( "select", "<expr>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Include in the output table only rows for which the",
            "expression <code>&lt;expr&gt;</code> evaluates to true.",
            "<code>&lt;expr&gt;</code> must be an expression which",
            "evaluates to a boolean value (true/false).",
            "</p>",
            explainSyntax( new String[] { "expr", } ),
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        if ( argIt.hasNext() ) {
            String expr = (String) argIt.next();
            argIt.remove();
            return new SelectStep( expr );
        }
        else {
            throw new ArgException( "Missing expression" );
        }
    }

    private static class SelectStep implements ProcessingStep {
        final String expr_;
        public SelectStep( String expr ) {
            expr_ = expr;
        }
        public StarTable wrap( StarTable base ) throws IOException {
            try {
                return new JELSelectorTable( base, expr_ );
            }
            catch ( CompilationException e ) {
                String msg = "Bad expression \"" + expr_ + 
                             "\" (" + e.getMessage() + ")";
                throw (IOException) new IOException( msg ).initCause( e );
            }
        }
    }
}
