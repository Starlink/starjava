package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Table filter for adding a single synthetic column.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2005
 */
public class AddColumnFilter extends BasicFilter {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.filter" );

    public AddColumnFilter() {
        super( "addcol", 
               "[-after <col-id> | -before <col-id>]\n" +
               "[-units <units>] [-ucd <ucd>] [-utype <utype>] " +
               "[-desc <descrip>]\n" +
               "[-shape <n>[,<n>...][,*]] [-elsize <n>]\n" +
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
            "</p>",
            "<p>The <code>-units</code>, <code>-ucd</code>,",
            "<code>-utype</code> and <code>-desc</code> flags can be used",
            "to define textual metadata values for the new column.",
            "</p>",
            "<p>The <code>-shape</code> flag can also be used,",
            "but is intended only for array-valued columns,",
            "e.g. <code>-shape 3,3</code> to declare a 3x3 array.",
            "The final entry only in the shape list",
            "may be a \"<code>*</code>\" character",
            "to indicate unknown extent.",
            "Array values with no specified shape effectively have a",
            "shape of \"<code>*</code>\".",
            "The <code>-elsize</code> flag may be used to specify the length",
            "of fixed length strings; use with non-string columns",
            "is not recommended.",
            "</p>",
            explainSyntax( new String[] { "expr", "col-id", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        String posId = null;
        String colName = null;
        String expr = null;
        String units = null;
        String ucd = null;
        String utype = null;
        String description = null;
        int[] shape = null;
        int elsize = -1;
        
        boolean after = false;
        while ( argIt.hasNext() && ( colName == null || expr == null ) ) {
            String arg = argIt.next();
            if ( arg.equals( "-after" ) && posId == null && 
                 argIt.hasNext() ) {
                argIt.remove();
                after = true;
                posId = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-before" ) && posId == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                after = false;
                posId = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-units" ) && argIt.hasNext() ) {
                argIt.remove();
                units = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-ucd" ) && argIt.hasNext() ) {
                argIt.remove();
                ucd = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-utype" ) && argIt.hasNext() ) {
                argIt.remove();
                utype = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-desc" ) && argIt.hasNext() ) {
                argIt.remove();
                description = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-shape" ) && argIt.hasNext() ) {
                argIt.remove();
                String shapeTxt = argIt.next();
                argIt.remove();
                try {
                    shape = DefaultValueInfo.unformatShape( shapeTxt );
                }
                catch ( Exception e ) {
                    throw new ArgException( "Bad -shape specification \""
                                          + shapeTxt + "\"" );
                }
            }
            else if ( arg.equals( "-elsize" ) && argIt.hasNext() ) {
                argIt.remove();
                String elsizeTxt = argIt.next();
                argIt.remove();
                try {
                    elsize = Integer.parseInt( elsizeTxt );
                }
                catch ( NumberFormatException e ) {
                    throw new ArgException( "Bad -elsize specification \""
                                          + elsizeTxt + "\"" );
                }
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
            if ( utype != null ) {
                colinfo.setUtype( utype );
            }
            if ( description != null ) {
                colinfo.setDescription( description );
            }
            if ( shape != null ) {
                colinfo.setShape( shape );
            }
            if ( elsize >= 0 ) {
                colinfo.setElementSize( elsize );
            }
            return new AddColumnStep( expr, colinfo, posId, after );
        }
        else {
            throw new ArgException( "Bad " + getName() + " specification" );
        }
    }

    /**
     * Checks that a given column in a table does not have the same name
     * as any of the other columns in the table.
     * If it does (even case-insensitively), a warning is written
     * through the logging system.
     *
     * @param  table  table to check
     * @param  icol0  column index to check
     */
    public static void checkDuplicatedName( StarTable table, int icol0 ) {
        int ncol = table.getColumnCount();
        String name0 = table.getColumnInfo( icol0 ).getName();
        if ( name0 != null ) {
            for ( int ic = 0; ic < ncol; ic++ ) {
                String name = table.getColumnInfo( ic ).getName();
                if ( ic != icol0 && name0.equalsIgnoreCase( name ) ) {
                    boolean isExact = name0.equals( name );
                    String msg = new StringBuffer()
                       .append( "Column #" )
                       .append( icol0 + 1 )
                       .append( " " )
                       .append( name0 )
                       .append( " has same name " )
                       .append( isExact ? "" : "(modulo case) " )
                       .append( "as existing column #" )
                       .append( ic + 1 )
                       .append( " " )
                       .append( name )
                       .toString();
                    logger_.warning( msg );
                }
            }
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
            StarTable out = new AddColumnsTable( base, jelSup, ipos );
            checkDuplicatedName( out, ipos );
            return out;
        }
    }
}
