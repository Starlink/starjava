package uk.ac.starlink.table;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * TableScheme implementation for test data.
 *
 * @author   Mark Taylor
 * @since    26 Apr 2021
 */
public class TestTableScheme implements TableScheme, Documented {

    private static final int DFLT_NROW = 10;
    private static final Map<Character,ContentOpt> OPTS = createOpts();
    private static final String OPT_CHRS =
        OPTS.keySet().stream().map( Object::toString )
            .collect( Collectors.joining() );
    private static final int ZP = 10;

    public TestTableScheme() {
    }

    public String getSchemeName() {
        return "test";
    }

    public String getSchemeUsage() {
        return "[<nrow>[,<opts-" + OPT_CHRS + "*>]]";
    }

    public String getExampleSpecification() {
        return "10,is";
    }

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>Generates a table containing test data.",
            "The idea is to include columns of different data types,",
            "for instance to provide an example for testing",
            "I/O handler implementations.",
            "The columns will contain some variety of more or less meaningless",
            "values, but the content is reproducible between runs,",
            "so the same specification will produce the same output each time.",
            "Updates of the implementation might change the output however,",
            "so the output is not guaranteed to be the same for all time.",
            "</p>",
            "<p>The table specification has two comma-separated parameters:",
            "<ul>",
            "<li><code>&lt;nrow&gt;</code>: row count</li>",
            "<li><code>&lt;opts&gt;</code>:",
                "a string of letter options specifying what types of data",
                "will be included; options are:",
                "<ul>",
                OPTS.values().stream().map( opt -> new StringBuffer()
                    .append( "<li><strong>" )
                    .append( opt.idChr_ )
                    .append( "</strong>: " )
                    .append( opt.description_ )
                    .append( "</li>\n" )
                    .toString()
                ).collect( Collectors.joining( "\n" ) ),
                "<li><strong>*</strong>: equivalent to all of the above</li>",
                "</ul>",
                "</li>",
            "</ul>",
            "If <code>&lt;opts&gt;</code> and/or <code>&lt;nrow&gt;</code>",
            "are omitted, some default values are used.",
            "</p>",
            "<p>The <code>&lt;nrow&gt;</code> argument",
            Tables.PARSE_COUNT_MAY_BE_GIVEN,
            "</p>",
        "" );
    }

    public StarTable createTable( String spec ) throws TableFormatException {
        String[] args = spec.split( ",", -1 );
        if ( args.length > 2 ) {
            throw new TableFormatException( "Too many args" );
        }
        final long nrow;
        try {
            nrow = args.length > 0 && args[ 0 ].trim().length() > 0
                 ? Tables.parseCount( args[ 0 ] )
                 : DFLT_NROW;
        }
        catch ( NumberFormatException e ) {
            throw new TableFormatException( "Bad row count \""
                                          + args[ 0 ] + "\"" );
        }
        String optsTxt = args.length > 1
                       ? args[ 1 ]
                       : "b";
        List<ContentOpt> optList = new ArrayList<>();
        for ( int ic = 0; ic < optsTxt.length(); ic++ ) {
            Character chr = Character.valueOf( optsTxt.charAt( ic ) );
            if ( chr == '*' ) {
                optList.addAll( OPTS.values() );
            }
            else {
                ContentOpt opt = OPTS.get( chr );
                if ( opt == null ) {
                    throw new TableFormatException( "Unknown content option '"
                                                  + chr + "' - must be one of ["
                                                  + OPT_CHRS + "*]" );
                }
                optList.add( opt );
            }
        }
        ColumnStarTable table = new ColumnStarTable() {
            public long getRowCount() {
                return nrow;
            }
        };
        for ( ContentOpt opt : optList ) {
            opt.addContent_.accept( table );
        }
        return table;
    }

    /**
     * Creates the list of run-time options for table content.
     *
     * @param   map from identifier character to option
     */
    private static Map<Character,ContentOpt> createOpts() {
        ContentOpt[] opts = new ContentOpt[] {
            new ContentOpt( 'i', "index", "an integer index column", t -> {
                t.addColumn( new ColumnData( new ColumnInfo( "i_index",
                                                             Integer.class,
                                                             "Row index" ) ) {
                    public Object readValue( long irow ) {
                        return Integer.valueOf( (int) irow );
                    }
                } );
            } ),
            new ContentOpt( 'b', "basic", "a few basic columns", t -> {
                int nPhase = 1;
                addColumn( t, "b_int", Integer.class, nPhase++,
                           i -> Integer.valueOf( i ) );
                addColumn( t, "b_double", Double.class, nPhase++,
                           i -> Double.valueOf( i ) );
                addColumn( t, "b_string", String.class, nPhase++,
                           i -> valString( i, false ) );
            } ),
            new ContentOpt( 's', "scalars",
                            "a selection of typed scalar columns", t -> {
                int nPhase = 1;
                addColumn( t, "s_byte", Byte.class, nPhase++,
                           i -> Byte.valueOf( (byte) valInt( i ) ) );
                addColumn( t, "s_short", Short.class, nPhase++,
                           i -> Short.valueOf( (short) valInt( i ) ) );
                addColumn( t, "s_int", Integer.class, nPhase++,
                           i -> Integer.valueOf( valInt( i ) ) );
                addColumn( t, "s_long", Long.class, nPhase++,
                           i -> Long.valueOf( valInt( i ) ) );
                addColumn( t, "s_float", Float.class, nPhase++,
                           i -> Float.valueOf( (float) valDouble( i ) ) );
                addColumn( t, "s_double", Double.class, nPhase++,
                           i -> Double.valueOf( valDouble( i ) ) );
                addColumn( t, "s_string", String.class, nPhase++,
                           i -> valString( i, true ) );
                addColumn( t, "s_boolean", Boolean.class, nPhase++,
                           i -> Boolean.valueOf( i % 2 == 1 ) );
            } ),
            new ContentOpt( 'f', "fixed-vectors",
                            "a selection of fixed-length 1-d array columns",
                            t -> {
                int nPhase = 1;
                int[] s3 = new int[] { 3 };
                addShapeColumn( t, "f_byte", byte[].class, s3, nPhase++,
                                i -> new byte[] { (byte) ( i + 0 ),
                                                  (byte) ( i + 1 ),
                                                  (byte) ( i + 2 ) } );
                addShapeColumn( t, "f_short", short[].class, s3, nPhase++,
                                i -> new short[] { (short) ( i + 0 ),
                                                   (short) ( i + 1 ),
                                                   (short) ( i + 2 ) } );
                addShapeColumn( t, "f_int", int[].class, s3, nPhase++,
                                i -> new int[] { i + 0, i + 1, i + 2 } );
                addShapeColumn( t, "f_long", long[].class, s3, nPhase++,
                                i -> new long[] { i + 0, i + 1, i + 2 } );
                addShapeColumn( t, "f_float", float[].class, s3, nPhase++,
                                i -> new float[] { i, Float.NaN, i + 2.5f } );
                addShapeColumn( t, "f_double", double[].class, s3, nPhase++,
                                i -> new double[] { i, Double.NaN, i + 2.5 } );
                addShapeColumn( t, "f_string", String[].class, s3, nPhase++,
                                i -> new String[] { "foo", null,
                                                    valString( i, true ) } );
                addShapeColumn( t, "f_boolean", boolean[].class, s3, nPhase++,
                                i -> new boolean[] { (i/1)%2==1,
                                                     (i/2)%2==1,
                                                     (i/4)%2==1 } );
            } ),
            new ContentOpt( 'g', "fixed-vectors-nostr",
                            "a selection of fixed-length 1-d array columns "
                          + "excluding strings",
                            t -> {
                int nPhase = 1;
                int[] s3 = new int[] { 3 };
                addShapeColumn( t, "g_byte", byte[].class, s3, nPhase++,
                                i -> new byte[] { (byte) ( i + 0 ),
                                                  (byte) ( i + 1 ),
                                                  (byte) ( i + 2 ) } );
                addShapeColumn( t, "g_short", short[].class, s3, nPhase++,
                                i -> new short[] { (short) ( i + 0 ),
                                                   (short) ( i + 1 ),
                                                   (short) ( i + 2 ) } ); 
                addShapeColumn( t, "g_int", int[].class, s3, nPhase++,
                                i -> new int[] { i + 0, i + 1, i + 2 } );
                addShapeColumn( t, "g_long", long[].class, s3, nPhase++,
                                i -> new long[] { i + 0, i + 1, i + 2 } );
                addShapeColumn( t, "g_float", float[].class, s3, nPhase++,
                                i -> new float[] { i, Float.NaN, i + 2.5f } );
                addShapeColumn( t, "g_double", double[].class, s3, nPhase++,
                                i -> new double[] { i, Double.NaN, i + 2.5 } );
                addShapeColumn( t, "g_boolean", boolean[].class, s3, nPhase++,
                                i -> new boolean[] { (i/1)%2==1,
                                                     (i/2)%2==1,
                                                     (i/4)%2==1 } );
            } ),
            new ContentOpt( 'v', "var-vectors",
                            "a selection of variable-length 1-d array columns",
                            t -> {
                int nPhase = 1;
                addVarColumn( t, "v_byte", byte[].class, nPhase++,
                              i -> new byte[] { (byte) ( i + 0 ),
                                                (byte) ( i + 1 ),
                                                (byte) ( i + 2 ) } );
                addVarColumn( t, "v_short", short[].class, nPhase++,
                              i -> new short[] { (short) ( i + 0 ),
                                                 (short) ( i + 1 ),
                                                 (short) ( i + 2 ) } ); 
                addVarColumn( t, "v_int", int[].class, nPhase++,
                              i -> new int[] { i + 0, i + 1, i + 2 } );
                addVarColumn( t, "v_long", long[].class, nPhase++,
                              i -> new long[] { i + 0, i + 1, i + 2 } );
                addVarColumn( t, "v_float", float[].class, nPhase++,
                              i -> new float[] { i + 0, Float.NaN, i + 2.5f } );
                addVarColumn( t, "v_double", double[].class, nPhase++,
                              i -> new double[] { i + 0, Double.NaN, i + 2.5 });
                addVarColumn( t, "v_string", String[].class, nPhase++,
                              i -> new String[] { "foo", null,
                                                  valString( i, true ) } );
                addVarColumn( t, "v_boolean", boolean[].class, nPhase++,
                              i -> new boolean[] { (i/1)%2==1,
                                                   (i/2)%2==1,
                                                   (i/4)%2==1 } );
            } ),
            new ContentOpt( 'w', "var-vectors-nostr",
                            "a selection of variable-length 1-d array columns "
                          + "excluding strings",
                            t -> {
                int nPhase = 1;
                addVarColumn( t, "w_byte", byte[].class, nPhase++,
                              i -> new byte[] { (byte) ( i + 0 ),
                                                (byte) ( i + 1 ),
                                                (byte) ( i + 2 ) } );
                addVarColumn( t, "w_short", short[].class, nPhase++,
                              i -> new short[] { (short) ( i + 0 ),
                                                 (short) ( i + 1 ),
                                                 (short) ( i + 2 ) } ); 
                addVarColumn( t, "w_int", int[].class, nPhase++,
                              i -> new int[] { i + 0, i + 1, i + 2 } );
                addVarColumn( t, "w_long", long[].class, nPhase++,
                              i -> new long[] { i + 0, i + 1, i + 2 } );
                addVarColumn( t, "w_float", float[].class, nPhase++,
                              i -> new float[] { i + 0, Float.NaN, i + 2.5f } );
                addVarColumn( t, "w_double", double[].class, nPhase++,
                              i -> new double[] { i + 0, Double.NaN, i + 2.5 });
                addVarColumn( t, "w_boolean", boolean[].class, nPhase++,
                              i -> new boolean[] { (i/1)%2==1,
                                                   (i/2)%2==1,
                                                   (i/4)%2==1 } );
            } ),
            new ContentOpt( 'm', "multi-d arrays",
                            "a selection of multi-dimensional array columns",
                            t -> {
                int nPhase = 1;
                addShapeColumn( t, "m_int", int[].class, new int[] { 2, 4 },
                                nPhase++,
                                i -> new int[] { 1000 + i, 1001 + i,
                                                 2000 + i, 2001 + i,
                                                 3000 + i, 3001 + i,
                                                 4000 + i, 4001 + i, } );
                addShapeColumn( t, "m_double", double[].class,
                                new int[] { 3, 2 }, nPhase++,
                                i -> new double[] {
                                    i + .25,   i + .50,   i + .75,
                                  - i - .25, - i - .50, - i - .75,
                                } );
            } ),
            new ContentOpt( 'k', "kilo-column", "almost a thousand columns",
                            t -> {
                for ( int j = 0; j < 995; j++ ) {
                    addColumn( t, "k_" + (j + 1), Integer.class, 0,
                               i -> Integer.valueOf( valInt( i ) ) );
                }
            } ),
        };
        Map<Character,ContentOpt> map = new LinkedHashMap<>();
        for ( ContentOpt opt : opts ) {
            char chr = Character.valueOf( opt.idChr_ );
            assert ! map.containsKey( chr );
            map.put( chr, opt );
        }
        return Collections.unmodifiableMap( map );
    }

    /**
     * Gives a nominal numeric integer value for a given row index.
     *
     * @param  ix  row index
     * @return  integer numeric value
     */
    private static int valInt( int ix ) {
        return ix * ( ( ( ix / 10 ) % 2 == 0 ) ? +1 : -1 );
    }

    /**
     * Gives a nominal floating point value for a given row index.
     *
     * @param  ix  row index
     * @return  floating point numeric value
     */
    private static double valDouble( int ix ) {
        return isBlank( ix ) ? Double.NaN : valInt( ix );
    }

    /**
     * Gives a nominal string value for a given row index.
     *
     * @param  ix  row index
     * @param  isTricky  true to include some specially weird strings
     * @return   string value
     */
    private static String valString( int ix, boolean isTricky ) {
        if ( isBlank( ix ) ) {
            return "";
        }
        else if ( isTricky && isFunny( ix ) ) {
            return "' \"\\\"\"' ; '&<>";
        }
        else if ( ix == 0 ) {
            return "zero";
        }
        int u = ix % 10;
        int t = ( ix / 10 ) * 10;
        int v = ix / 10;
        StringBuffer sbuf = new StringBuffer();
        String[] qpairs = { null, "\"\"", "''", "<>", "()", "{}", "[]", ",;" };
        String qpair = isTricky ? qpairs[ v % qpairs.length ] : null;
        final String preQuote = qpair == null ? "" : qpair.substring( 0, 1 );
        final String postQuote = qpair == null ? "" : qpair.substring( 1, 2 );
        if ( t != 0 ) {
            sbuf.append( Integer.toString( t ) );
            if ( u != 0 ) {
                sbuf.append( isTricky ? " + " : "+" );
            }
        }
        if ( u != 0 ) {
            sbuf.append( preQuote )
                .append( (new String[] {
                    "zero", "one", "two", "three", "four",
                    "five", "six", "seven", "eight", "nine",
                 })[ u ] )
                .append( postQuote );
        }
        return sbuf.toString();
    }

    /**
     * Utility function that indicates whether a value at a given index
     * should be set to a non-null blank value.
     *
     * @param  ix  row index
     * @return   true for blank
     */
    private static boolean isBlank( int ix ) {
        return ix % 10 == 9;
    }

    /**
     * Utility function that indicates whether a value at a given index
     * should be set to a test value out of the normal sequence.
     *
     * @param  ix  row index
     * @return   true for funny value
     */
    private static boolean isFunny( int ix ) {
        return ix % 10 == 8;
    }

    /**
     * Adds a column to a table given a value mapping from row index.
     *
     * @param  table  table to add column to
     * @param  name  column name
     * @param  clazz  cell content class
     * @param  nullPhase  row phase at which nulls should be substituted
     * @param  cellData  produces basic value for given row index
     */
    private static <T> void addColumn( ColumnStarTable table,
                                       String name, Class<T> clazz,
                                       int nullPhase,
                                       IntFunction<T> cellData ) {
        table.addColumn( new ColumnData( new ColumnInfo( name, clazz, null ) ) {
            public T readValue( long irow ) {
                return irow % ZP == nullPhase ? null
                                              : cellData.apply( (int) irow );
            }
        } );
    }

    /**
     * Adds a column to a table with a supplied array shape.
     * No checking is done to ensure that the shape makes sense.
     *
     * @param  table  table to add column to
     * @param  name  column name
     * @param  clazz  cell content class
     * @param  shape   array shape 
     * @param  nullPhase  row phase at which nulls should be substituted
     * @param  cellData  produces basic value for given row index
     */
    private static <T> void addShapeColumn( ColumnStarTable table,
                                            String name, Class<T> clazz,
                                            int[] shape, int nullPhase,
                                            IntFunction<T> cellData ) {
        addColumn( table, name, clazz, nullPhase, cellData );
        table.getColumnInfo( table.getColumnCount() - 1 )
             .setShape( shape );
    }

    /**
     * Adds a variable-length column to a table given a value mapping
     * from row index to a fixed-length array value.
     *
     * @param  table  table to add column to
     * @param  name  column name
     * @param  clazz  cell content class, an array type
     * @param  nullPhase  row phase at which nulls should be substituted
     * @param  cellData  produces basic array value for given row index
     */
    private static <T> void addVarColumn( ColumnStarTable table,
                                          String name, Class<T> clazz,
                                          int nullPhase,
                                          IntFunction<T> cellData ) {
        final Class<?> elClazz = clazz.getComponentType();
        table.addColumn( new ColumnData( new ColumnInfo( name, clazz, null ) ) {
            public T readValue( long irow ) {
                if ( irow % ZP == nullPhase ) {
                    return null;
                }
                else {
                    T value0 = cellData.apply( (int) irow );
                    if ( value0 != null ) {
                        int leng0 = Array.getLength( value0 );
                        int leng1 = leng0 - ( ((int) irow) % (leng0 + 1) );
                        if ( leng1 == leng0 ) {
                            return value0;
                        }
                        else {
                            T value1 =
                                clazz.cast( Array.newInstance( elClazz,
                                                               leng1 ) );
                            for ( int i = 0; i < leng1; i++ ) {
                                Array.set( value1, i, Array.get( value0, i ) );
                            }
                            return value1;
                        }
                    }
                    else {
                        return value0;
                    }
                }
            }
        } );
    }

    /**
     * Defines an output table content option.
     */
    private static class ContentOpt {
        final char idChr_;
        final String name_;
        final String description_;
        final Consumer<ColumnStarTable> addContent_;

        /**
         * @param  idChr  identifier character
         * @param  name  option name
         * @param  description   user-oriented short description
         * @param  addContent   adds content to table
         */
        ContentOpt( char idChr, String name, String description,
                    Consumer<ColumnStarTable> addContent ) {
            idChr_ = idChr;
            name_ = name;
            description_ = description;
            addContent_ = addContent;
        }
    }
}
