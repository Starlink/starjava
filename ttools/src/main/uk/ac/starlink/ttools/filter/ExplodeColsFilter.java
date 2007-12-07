package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ExplodedStarTable;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Table filter for replacing selected N-element array-valued columns
 * with N scalar-valued columns.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mark 2005
 */
public class ExplodeColsFilter extends BasicFilter {

    public ExplodeColsFilter(){ 
        super( "explodecols", "<colid-list>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Takes a list of specified columns which represent N-element",
            "arrays and replaces each one with N scalar columns.",
            "Each of the columns specified by <code>&lt;colid-list&gt;</code>",
            "must have a fixed-length array type,",
            "though not all the arrays need to have the same number",
            "of elements.",
            "</p>",
            explainSyntax( new String[] { "colid-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        if ( argIt.hasNext() ) {
            final String colIdList = (String) argIt.next();
            argIt.remove();
            return new ProcessingStep() {
                public StarTable wrap( StarTable base ) throws IOException {
                    boolean[] colFlags = new ColumnIdentifier( base )
                                        .getColumnFlags( colIdList );
                    try {
                        return new ExplodedStarTable( base, colFlags );
                    }
                    catch ( IllegalArgumentException e ) {
                        throw (IOException) new IOException( e.getMessage() )
                                           .initCause( e );
                    }
                }
            };
        }
        else {
            throw new ArgException( "Missing column list" );
        }
    }
}
