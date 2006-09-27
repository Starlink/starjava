package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Processing step which runs the 
 * {@link uk.ac.starlink.table.Tables#checkTable} diagnostic.
 *
 * @author   Mark Taylor
 * @since    26 Jan 2006
 */
public class CheckFilter extends BasicFilter implements ProcessingStep {

    public CheckFilter() {
        super( "check", null );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Runs checks on the table at the indicated point in the",
            "processing pipeline.  This is strictly a debugging measure,",
            "and may be time-consuming for large tables.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) {
        return this;
    }

    public StarTable wrap( StarTable in ) throws IOException {
        try {
            Tables.checkTable( in );
        }
        catch ( AssertionError e ) {
            throw (IOException)
                  new IOException( e.getMessage() ).initCause( e );
        }
        return in;
    }
}
