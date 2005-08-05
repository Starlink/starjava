package uk.ac.starlink.ttools.pipe;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ExplodedStarTable;
import uk.ac.starlink.ttools.ArgException;
import uk.ac.starlink.ttools.ColumnIdentifier;

/**
 * Table filter for replacing selected N-element array-valued columns
 * with N scalar-valued columns.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mark 2005
 */
public class ExplodeColsFilter implements ProcessingFilter {

    public String getName() {
        return "explodecols";
    }

    public String getFilterUsage() {
        return "<colid-list>";
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
