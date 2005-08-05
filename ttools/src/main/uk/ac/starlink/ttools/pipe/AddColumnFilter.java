package uk.ac.starlink.ttools.pipe;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.ArgException;
import uk.ac.starlink.ttools.ColumnIdentifier;

/**
 * Table filter for adding a single synthetic column.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2005
 */
public class AddColumnFilter implements ProcessingFilter {

    public String getName() {
        return "addcol";
    }

    public String getFilterUsage() {
        return "[-after <col-id> | -before <col-id>] <col-name> <expr>";
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        String posId = null;
        String colName = null;
        String expr = null;
        boolean after = false;
        while ( argIt.hasNext() && ( colName == null || expr == null ) ) {
            String arg = (String) argIt.next();
            if ( arg.equals( "-after" ) && posId == null && 
                 argIt.hasNext() ) {
                argIt.remove();
                after = true;
                posId = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-before" ) && posId == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                after = false;
                posId = (String) argIt.next();
                argIt.remove();
            }
            else if ( colName == null ) {
                argIt.remove();
                colName = arg;
            }
            else if ( expr == null ) {
                argIt.remove();
                expr = arg;
            }
        }
        if ( colName != null && expr != null ) {
            ColumnInfo colinfo = new ColumnInfo( colName );
            return new AddColumnStep( expr, colinfo, posId, after );
        }
        else {
            throw new ArgException( "Bad " + getName() + " specification" );
        }
    }

    private static class AddColumnStep implements ProcessingStep {

        final String expr_;
        final ColumnInfo cinfo_;
        final String placeColid_;
        final boolean after_;

        AddColumnStep( String expr, ColumnInfo cinfo, String placeColid,
                       boolean after ) {
            cinfo_ = cinfo;
            expr_ = expr;
            placeColid_ = placeColid;
            after_ = after;
        }

        public StarTable wrap( StarTable base ) throws IOException {
            int ipos;
            if ( placeColid_ != null ) {
                int iplace = new ColumnIdentifier( base )
                            .getColumnIndex( placeColid_ );
                ipos = after_ ? iplace + 1
                              : iplace;
            }
            else {
                ipos = base.getColumnCount();
            }
            try {
                return new AddJELColumnTable( base, cinfo_, expr_, ipos );
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
        }
    }
}
