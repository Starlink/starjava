package uk.ac.starlink.vo;

/**
 * Represents a type of example ADQL query.
 * The query text can be generated as a function of given service metadata.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2011
 */
public abstract class AbstractAdqlExample implements AdqlExample {

    private final String name_;
    private final String description_;
    private static final int COL_COUNT = 3;
    private static final int ROW_COUNT = 1000;

    /**
     * Constructor.
     *
     * @param   name  example name
     * @param   description   example short description
     */
    protected AbstractAdqlExample( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    public String getName() {
        return name_;
    }

    public String getDescription() {
        return description_;
    }

    /**
     * Indicates if a language string represents ADQL version 1.
     * If not, at time of writing, it's a fair guess that it's ADQL version 2.
     *
     * @param  lang  language string
     * @return   true if lang looks like ADQL 1
     */
    private static boolean isAdql1( String lang ) {
        return lang != null && lang.toUpperCase().startsWith( "ADQL-1." );
    }

    /**
     * Returns a breaker instance suitable for a given line break policy.
     *
     * @param  lineBreaks  whether line breaks are required
     * @return   breaker instance
     */
    private static Breaker createBreaker( boolean lineBreaks ) {
        return lineBreaks
             ? new Breaker() {
                   public String level( int ilev ) {
                       StringBuffer sbuf = new StringBuffer();
                       sbuf.append( '\n' );
                       for ( int i = 0; i < ilev; i++ ) {
                           sbuf.append( "   " );
                       }
                       return sbuf.toString();
                   }
               }
             : new Breaker() {
                   public String level( int ilev ) {
                       return "";
                   }
               };
    }

    /**
     * Returns a table ref for a given table and a given language variant.
     *
     * @param  table  table metadata object
     * @param   lang  language string
     */
    private static TableRef createTableRef( final TableMeta table,
                                            String lang ) {
        if ( ! isAdql1( lang ) ) {
            return new TableRef() {
                public String getColumnName( String rawName ) {
                    return rawName;
                }
                public String getIntroName() {
                    return table.getName();
                }
            };
        }
        else {
            final String nick = getNickName( table );
            return new TableRef() {
                public String getColumnName( String rawName ) {
                    return nick + "." + rawName;
                }
                public String getIntroName() {
                    return table.getName() + " AS " + nick;
                }
            };
        }
    }

    /**
     * Interface for configurable line breaking.
     */
    private static abstract class Breaker {

        /**
         * Returns a string which can be used to separate parts of an output
         * example string.
         *
         * @param   ilev  notional indentation level
         * @return   whitespace string
         */
        public abstract String level( int ilev );
    }

    /**
     * Interface for providing table references in ADQL text.
     */
    private static abstract class TableRef {

        /**
         * Returns the text by which a given column in this object's table
         * should be referred to in ADQL text.
         *
         * @param  rawName  basic column name
         * @return   quoted column name
         */
        public abstract String getColumnName( String rawName );

        /**
         * Returns the text with which this object's table should be
         * introduced in ADQL text.
         *
         * @return   table introductory text
         */
        public abstract String getIntroName();
    }

    /**
     * Returns a suitable ADQL alias for a given table.
     *
     * @param  table  table being referenced
     * @return  alias
     */
    private static String getNickName( TableMeta table ) {
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

    /**
     * Returns a dummy example which never provides any text.
     *
     * @return  dummy example
     */
    public static AdqlExample createDummyExample() {
        return new AbstractAdqlExample( "Dummy", "Never enabled" ) {
            public String getText( boolean lineBreaks, String lang,
                                   TapCapability tcap, TableMeta[] tables,
                                   TableMeta table ) {
                return null;
            }
        };
    }

    /**
     * Returns a selection of examples.
     *
     * @return   example list
     */
    public static AdqlExample[] createSomeExamples() {
        return new AdqlExample[] {
            new AbstractAdqlExample( "Full table",
                                     "All columns from a single table" ) {
                public String getText( boolean lineBreaks, String lang,
                                       TapCapability tcap, TableMeta[] tables,
                                       TableMeta table ) {
                    if ( table == null ) {
                        return null;
                    }
                    return "SELECT * FROM " + table.getName();
                }
            },
            new AbstractAdqlExample( "Columns from table",
                                     "Selection of columns from "
                                   + "a single table" ) {
                public String getText( boolean lineBreaks, String lang,
                                       TapCapability tcap, TableMeta[] tables,
                                       TableMeta table ) {
                    if ( table == null ) {
                        return null;
                    }
                    Breaker breaker = createBreaker( lineBreaks );
                    TableRef tref = createTableRef( table, lang );
                    ColumnMeta[] cols = table.getColumns();
                    final String colSelection;
                    if ( cols != null && cols.length > COL_COUNT ) {
                        StringBuffer sbuf = new StringBuffer();
                        for ( int i = 0; i < COL_COUNT; i++ ) {
                            if ( i > 0 ) {
                                sbuf.append( ", " );
                            }
                            sbuf.append( tref
                                        .getColumnName( cols[ i ].getName() ) );
                        }
                        colSelection = sbuf.toString();
                    }
                    else {
                        colSelection = "*";
                    }
                    return new StringBuffer()
                        .append( "SELECT" )
                        .append( breaker.level( 1 ) )
                        .append( "TOP " )
                        .append( ROW_COUNT )
                        .append( breaker.level( 1 ) )
                        .append( colSelection )
                        .append( breaker.level( 1 ) )
                        .append( "FROM" )
                        .append( ' ' )
                        .append( tref.getIntroName() )
                        .toString();
                }
            },
        };
    }
}
