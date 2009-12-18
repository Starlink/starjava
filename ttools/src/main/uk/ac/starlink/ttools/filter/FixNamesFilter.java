package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Filter to normalise syntax of column and parameter names so they
 * are legal java identifiers.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2009
 */
public class FixNamesFilter extends BasicFilter implements ProcessingStep {

    /**
     * Constructor.
     */
    public FixNamesFilter() {
        super( "fixcolnames", "" );
    }

    public String[] getDescriptionLines() {
        return new String[] {
            "<p>Renames all columns and parameters in the input table",
            "so that they have names which have convenient syntax for STILTS.",
            "For the most part this means replacing spaces and other",
            "non-alphanumeric characters with underscores.",
            "This is a convenience which lets you use column names in",
            "algebraic expressions and other STILTS syntax.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        return this;
    }

    public StarTable wrap( StarTable base ) {
        return new FixNamesTable( base );
    }

    /**
     * Performs the name unmunging.
     *
     * @param   name   input name
     * @return   string like name which is a valid java identifier
     */
    public String fixName( String name ) {
        if ( name == null || name.trim().length() == 0 ) {
            return "_unnamed_";
        }
        name = name.trim();
        StringBuffer sbuf = new StringBuffer( name.length() );
        for ( int ic = 0; ic < name.length(); ic++ ) {
            char ch = name.charAt( ic );
            sbuf.append( ( ic == 0 ? Character.isJavaIdentifierStart( ch )
                                   : Character.isJavaIdentifierPart( ch ) )
                               ? ch
                               : '_' );
        }
        return sbuf.toString();
    }

    /**
     * StarTable implementation which rewrites column and parameter names
     * as necessary.
     */
    private class FixNamesTable extends WrapperStarTable {

        private final ColumnInfo[] colInfos_;
        private final List paramList_;

        /**
         * Constructor.
         *
         * @param   base  base table.
         */
        FixNamesTable( StarTable base ) {
            super( base );

            /* Doctor column names. */
            int ncol = base.getColumnCount();
            colInfos_ = new ColumnInfo[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnInfo info = new ColumnInfo( base.getColumnInfo( icol ) );
                info.setName( fixName( info.getName() ) );
                colInfos_[ icol ] = info;
            }

            /* Doctor parameter names. */
            DescribedValue[] params =
                (DescribedValue[])
                base.getParameters().toArray( new DescribedValue[ 0 ] );
            paramList_ = new ArrayList( params.length );
            for ( int ip = 0; ip < params.length; ip++ ) {
                DefaultValueInfo info =
                    new DefaultValueInfo( params[ ip ].getInfo() );
                info.setName( fixName( info.getName() ) );
                Object value = params[ ip ].getValue();
                paramList_.add( new DescribedValue( info, value ) );
            }
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return colInfos_[ icol ];
        }

        public List getParameters() {
            return paramList_;
        }

        public DescribedValue getParameterByName( String parName ) {
            for ( Iterator it = paramList_.iterator(); it.hasNext(); ) {
                DescribedValue param = (DescribedValue) it.next();
                if ( param.getInfo().getName().equals( parName ) ) {
                    return param;
                }
            }
            return null;
        }

        public void setParameter( DescribedValue dval ) {
            paramList_.remove( getParameterByName( dval.getInfo().getName() ) );
            paramList_.add( dval );
        }
    }
}
