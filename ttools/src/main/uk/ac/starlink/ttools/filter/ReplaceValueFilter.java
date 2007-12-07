package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Filter for replacing a given value with another one in a list of columns.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2005
 */
public class ReplaceValueFilter extends BasicFilter {

    public ReplaceValueFilter() {
        super( "replaceval", "<old-val> <new-val> <colid-list>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>For each column specified in <code>&lt;colid-list&gt;</code>",
            "any instance of <code>&lt;old-val&gt;</code> is replaced by",
            "<code>&lt;new-val&gt;</code>.",
            "The value string '<code>null</code>' can be used for either",
            "<code>&lt;old-value&gt;</code> or <code>&lt;new-value&gt;</code>",
            "to indicate a blank value",
            "(but see also the " + DocUtils.filterRef( new BadValueFilter() ),
            "filter).",
            "</p>",
            explainSyntax( new String[] { "colid-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        String oldStr = null;
        String newStr = null;
        String colIdList = null;
        while ( argIt.hasNext() ) {
            String arg = (String) argIt.next();
            if ( oldStr == null ) {
                oldStr = arg;
                argIt.remove();
            }
            else if ( newStr == null ) {
                newStr = arg;
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
        if ( oldStr != null && newStr != null && colIdList != null ) {
            final String fOldStr = oldStr.equals( "null" ) ? null : oldStr;
            final String fNewStr = newStr.equals( "null" ) ? null : newStr;
            final String fColIdList = colIdList;
            return new ProcessingStep() {
                public StarTable wrap( StarTable base ) throws IOException {
                    return new ReplaceValueTable( base,
                                                  new ColumnIdentifier( base )
                                                 .getColumnFlags( fColIdList ),
                                                  fOldStr, fNewStr );
                }
            };
        }
        else {
            throw new ArgException( "Bad " + getName() + " specification" );
        }
    }
}
