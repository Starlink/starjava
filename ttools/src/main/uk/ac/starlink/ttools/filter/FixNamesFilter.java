package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
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

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.filter" );

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
            "<p>Additionally, column names are adjusted if necessary to ensure",
            "that they are all unique when compared case-insensitively.",
            "If the names are all unique to start with",
            "then no changes are made,",
            "but if for instance two columns exist with names",
            "<code>gMag</code> and <code>GMag</code>,",
            "one of them will be altered",
            "(for instance to <code>GMag_1</code>).",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt ) {
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
     * Returns a name based on the supplied one, but which is guaranteed
     * different (case-insensitively) from all the names in a supplied set.
     * If the supplied name is already distinct from the members of the set,
     * it will be returned unchanged.
     *
     * @param  name  base name
     * @param  lcNames   set of lower-case values from which the output
     *                   must differ case-insensitively; not to be modified
     * @return  distinct string based on name
     */
    public String uniqueName( String name, Set<String> lcNames ) {
        if ( ! lcNames.contains( name.toLowerCase() ) ) {
            return name;
        }
        for ( int i = 1; i > 0; i++ ) {
            String name1 = name + "_" + i;
            if ( ! lcNames.contains( name1.toLowerCase() ) ) {
                return name1;
            }
        }
        assert false;
        return "???";
    }

    /**
     * StarTable implementation which rewrites column and parameter names
     * as necessary.
     */
    private class FixNamesTable extends WrapperStarTable {

        private final ColumnInfo[] colInfos_;
        private final List<DescribedValue> paramList_;

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
            Set<String> lcNames = new HashSet<>();
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnInfo info = new ColumnInfo( base.getColumnInfo( icol ) );
                String name = info.getName();
                String fixName = uniqueName( fixName( name ), lcNames );
                if ( ! fixName.equals( name ) ) {
                    info.setName( fixName );
                    String msg = new StringBuffer()
                       .append( "Rename column #" )
                       .append( icol + 1 )
                       .append( " " )
                       .append( name )
                       .append( " -> " )
                       .append( fixName )
                       .toString();
                    logger_.info( msg );
                }
                lcNames.add( fixName.toLowerCase() );
                colInfos_[ icol ] = info;
            }
            assert lcNames.size() == ncol;

            /* Doctor parameter names. */
            paramList_ = new ArrayList<DescribedValue>();
            for ( DescribedValue param : base.getParameters() ) {
                DefaultValueInfo info = new DefaultValueInfo( param.getInfo() );
                info.setName( fixName( info.getName() ) );
                paramList_.add( new DescribedValue( info, param.getValue() ) );
            }
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return colInfos_[ icol ];
        }

        public List<DescribedValue> getParameters() {
            return paramList_;
        }

        public DescribedValue getParameterByName( String parName ) {
            for ( DescribedValue param : paramList_ ) {
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
