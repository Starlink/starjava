package uk.ac.starlink.ttools.filter;

import java.util.Iterator;
import uk.ac.starlink.table.StarTable;

/**
 * Filter which renames a table.
 *
 * @author   Mark Taylor
 * @since    16 Sep 2005
 */
public class NameFilter extends BasicFilter {

    public NameFilter() {
        super( "tablename", "<name>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Sets the table's name attribute to the given string.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        if ( argIt.hasNext() ) {
            final String name = (String) argIt.next();
            argIt.remove();
            return new ProcessingStep() {
                public StarTable wrap( StarTable base ) {
                    base.setName( name );
                    return base;
                }
            };
        }
        else {
            throw new ArgException( "No name given" );
        }
    }
}
