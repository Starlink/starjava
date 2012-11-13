package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Table filter for adding a single synthetic column.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2005
 */
public class AddColumnFilter extends BasicFilter {

    public AddColumnFilter() {
        super( "addcol", 
               "[-after <col-id> | -before <col-id>]\n" +
               "[-units <units>] [-ucd <ucd>] [-desc <description>]\n" +
               "<col-name> <expr>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Add a new column called <code>&lt;col-name&gt;</code> defined",
            "by the algebraic expression <code>&lt;expr&gt;</code>.",
            "By default the new column appears after the last column",
            "of the table, but you can position it either before or",
            "after a specified column using the <code>-before</code>",
            "or <code>-after</code> flags respectively.",
            "The <code>-units</code>, <code>-ucd</code> and",
            "<code>-desc</code> flags can be used to define",
            "metadata values for the new column.",
            "</p>",
            explainSyntax( new String[] { "expr", "col-id", } ),
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        String posId = null;
        String colName = null;
        String expr = null;
        String units = null;
        String ucd = null;
        String description = null;
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
            else if ( arg.equals( "-units" ) && argIt.hasNext() ) {
                argIt.remove();
                units = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-ucd" ) && argIt.hasNext() ) {
                argIt.remove();
                ucd = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-desc" ) && argIt.hasNext() ) {
                argIt.remove();
                description = (String) argIt.next();
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
            if ( units != null ) {
                colinfo.setUnitString( units );
            }
            if ( ucd != null ) {
                colinfo.setUCD( ucd );
            }
            if ( description != null ) {
                colinfo.setDescription( description );
            }
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
            ColumnSupplement jelSup =
                new JELColumnSupplement( base, expr_, cinfo_ );
            return new AddColumnsTable( base, jelSup, ipos );
        }
    }
}
