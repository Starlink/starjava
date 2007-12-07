package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Filter which removes parameters from a table.
 *
 * @author   Mark Taylor
 * @since    3 Aug 2006
 */
public class ClearParameterFilter extends BasicFilter {

    public ClearParameterFilter() {
        super( "clearparams", "<pname> ..." );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Clears the value of one or more named parameters.",
            "Each of the <code>&lt;pname&gt;</code> values supplied may be",
            "either a parameter name or a simple wildcard expression",
            "matching parameter names.  Currently the only wildcarding",
            "is a \"<code>*</code>\" to match any sequence of characters.",
            "<code>clearparams *</code> will clear all the parameters",
            "in the table.",
            "</p>",
            "<p>It is not an error to supply <code>&lt;pname&gt;</code>s",
            "which do not exist in the table - these have no effect.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        List pnameList = new ArrayList();
        while ( argIt.hasNext() ) {
            String arg = (String) argIt.next();
            pnameList.add( arg );
            argIt.remove();
        }
        if ( pnameList.isEmpty() ) {
            throw new ArgException( "No parameter names supplied" );
        }
        final String[] pnames = (String[]) pnameList.toArray( new String[ 0 ] );
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) {
                removeParameters( base.getParameters(), pnames );
                return base;
            }
        };
    }

    /**
     * Takes a list of {@link uk.ac.starlink.table.DescribedValue}
     * objects representing table parameters, and removes any 
     * whose name match one of the expressions in the <code>pnames</code>
     * array.
     * Elements of <code>pnames</code> may be exact matches or globs.
     *
     * @param  paramList  input list of parameters (DescribedValues)
     * @param  pnames   list of globs/names to remove
     * @see   uk.ac.starlink.ttools.ColumnIdentifier#globToRegex
     */
    private static void removeParameters( List paramList, String[] pnames ) {
        int nname = pnames.length;
        Pattern[] patterns = new Pattern[ nname ];
        for ( int iname = 0; iname < nname; iname++ ) {
            patterns[ iname ] =
                ColumnIdentifier.globToRegex( pnames[ iname ], false );
        }
        for ( Iterator it = paramList.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if ( obj instanceof DescribedValue ) {
                DescribedValue dval = (DescribedValue) obj;
                String pname = dval.getInfo().getName();
                for ( int iname = 0; iname < nname; iname++ ) {
                    if ( patterns[ iname ] == null ) {
                        if ( pnames[ iname ].equalsIgnoreCase( pname ) ) {
                            it.remove();
                        }
                    }
                    else {
                        if ( patterns[ iname ].matcher( pname ).matches() ) {
                            it.remove();
                        }
                    }
                }
            }
            else {
                it.remove();  // shouldn't be there in the first place
            }
        }
    }
}
