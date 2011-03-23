package uk.ac.starlink.vo;

/**
 * Can generate example ADQL queries.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2011
 */
public abstract class AdqlExemplifier {

    private final String indent_;
    private final String newline_;
    private static final int COL_COUNT = 3;
    private static final int ROW_COUNT = 1000;

    /**
     * Constructor.
     *
     * @param   lineBreaks  whether output ADQL should include multiline
     *                      formatting
     */
    protected AdqlExemplifier( boolean lineBreaks ) {
        if ( lineBreaks ) {
            indent_ = "   ";
            newline_ = "\n";
        }
        else {
            indent_ = "";
            newline_ = "";
        }
    }

    /**
     * Simple single-table example query.
     *
     * @param  table  table to query
     */
    public abstract String createSimpleExample( TableMeta table );

    /**
     * Returns a string which can be used to separate parts of an output
     * example string.
     *
     * @param   level  notional indentation level
     * @return   whitespace string
     */
    String breakLevel( int level ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( newline_ );
        for ( int i = 0; i < level; i++ ) {
            sbuf.append( indent_ );
        }
        return sbuf.toString();
    }

    /**
     * Returns a new instance suitable for a given query language.
     *
     * @param  queryLanguage  query language specifier as per TapRegExt
     * @param  lineBreaks  true iff the output query should be formatted
     *                     with line breaks
     */
    public static AdqlExemplifier createExemplifier( String queryLanguage,
                                                     boolean lineBreaks ) {
        if ( queryLanguage.equals( "ADQL-2.0" ) ) {
            return new AdqlExemplifier2( lineBreaks );
        }
        else if ( queryLanguage.equals( "ADQL-1.0" ) ) {
            return new AdqlExemplifier1( lineBreaks );
        }
        else {
            return new AdqlExemplifier2( lineBreaks );
        }
    }

    /**
     * AdqlExemplifier subclass for ADQL-2.0.
     */
    private static class AdqlExemplifier2 extends AdqlExemplifier {

        /**
         * Constructor.
         *
         * @param  lineBreaks  true iff formatting is desired
         */
        AdqlExemplifier2( boolean lineBreaks ) {
            super( lineBreaks );
        }

        public String createSimpleExample( TableMeta table ) {
            ColumnMeta[] cols = table.getColumns();
            final String colSelection;
            if ( cols != null && cols.length > COL_COUNT ) {
                StringBuffer sbuf = new StringBuffer();
                for ( int i = 0; i < COL_COUNT; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( cols[ i ].getName() );
                }
                colSelection = sbuf.toString();
            }
            else {
                colSelection = "*";
            }
            return new StringBuffer()
                .append( "SELECT" )
                .append( breakLevel( 1 ) )
                .append( "TOP" )
                .append( ' ' )
                .append( ROW_COUNT )
                .append( breakLevel( 1 ) )
                .append( colSelection )
                .append( breakLevel( 1 ) )
                .append( "FROM" )
                .append( ' ' )
                .append( table.getName() )
                .toString();
        }
    }

    /**
     * AdqlExemplifier subclass for ADQL-1.0.
     */
    private static class AdqlExemplifier1 extends AdqlExemplifier {

        /**
         * Constructor.
         *
         * @param  lineBreaks  true iff formatting is desired
         */
        AdqlExemplifier1( boolean lineBreaks ) {
            super( lineBreaks );
        }

        /**
         * Returns a suitable ADQL alias for a given table.
         *
         * @param  table  table being referenced
         * @return  alias
         */
        private String getNickname( TableMeta table ) {
            String subname = table.getName().replaceFirst( "^[^\\.]*\\.", "" );
            char letter = '\0';
            if ( subname.length() > 0 ) {
                letter = subname.charAt( 0 );
            }
            if ( ( letter >= 'a' && letter <= 'z' ) ||
                 ( letter >= 'A' && letter <= 'Z' ) ) {
                return new String( new char[] { letter } );
            }
            else {
                return "t";
            }
        }

        public String createSimpleExample( TableMeta table ) {
            ColumnMeta[] cols = table.getColumns();
            String tname = getNickname( table );
            final String colSelection;
            if ( cols != null && cols.length > COL_COUNT ) {
                StringBuffer sbuf = new StringBuffer();
                for ( int i = 0; i < COL_COUNT; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( tname )
                        .append( '.' )
                        .append( cols[ i ].getName() );
                }
                colSelection = sbuf.toString();
            }
            else {
                colSelection = "*";
            }
            return new StringBuffer()
                .append( "SELECT" )
                .append( breakLevel( 1 ) )
                .append( "TOP " )
                .append( ROW_COUNT )
                .append( breakLevel( 1 ) )
                .append( colSelection )
                .append( breakLevel( 1 ) )
                .append( "FROM" )
                .append( ' ' )
                .append( table.getName() )
                .append( " AS " )
                .append( tname )
                .toString();
        } 
    }
}
