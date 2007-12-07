package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Filter for replacing magic values with blanks.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2005
 */
public class BadValueFilter extends BasicFilter {

    public BadValueFilter() {
        super( "badval", "<bad-val> <colid-list>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>For each column specified in <code>&lt;colid-list&gt;</code>",
            "any occurrence of the value <code>&lt;bad-val&gt;</code>",
            "is replaced by a blank entry.",
            "</p>",
            explainSyntax( new String[] { "colid-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        String badStr = null;
        String colIdList = null;
        while ( argIt.hasNext() ) {
            String arg = (String) argIt.next();
            if ( badStr == null ) {
                badStr = arg;
                argIt.remove();
            }
            else if ( colIdList == null ) {
                colIdList = arg;
                argIt.remove();
            }
            else {
                break;
            }
        }
        if ( badStr != null && colIdList != null ) {
            final String fBadStr = badStr;
            final String fColIdList = colIdList;
            return new ProcessingStep() {
                public StarTable wrap( StarTable base ) throws IOException {
                    return new ReplaceValueTable( base,
                                                  new ColumnIdentifier( base )
                                                 .getColumnFlags( fColIdList ),
                                                  fBadStr, null );
                }
            };
        }
        else {
            throw new ArgException( "Bad " + getName() + " specification" );
        }
    }
}
