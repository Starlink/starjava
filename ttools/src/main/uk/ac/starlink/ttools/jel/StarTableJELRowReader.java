package uk.ac.starlink.ttools.jel;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * JELRowReader subclass for access to StarTables.
 *
 * <p>In addition to the syntax provided by the superclass, the following
 * symbols are understood:
 * <dl>
 * 
 * <dt>"$0" or "Index" (case insensitive):
 * <dd>the 1-based index of the current row
 *
 * <dt>Parameter names:
 * <dd>The string {@link #PARAM_PREFIX} followed by the name of a table
 *     parameter (case-insensitive) is a constant for the table
 *     (as a primitive, if applicable).  This can only work
 *     if the parameter name is a legal java identifier.
 *
 * <dt>UCD specifiers:
 * <dd>The string {@link #UCD_PREFIX} followed by the text of a UCD
 *     giving the required value.  Any punctuation (such as ".", ";", "-")
 *     in the UCD should be replaced with a "_" (since these symbols cannot
 *     appear in identifiers).  If the identifier has a trailing "_",
 *     then any UCD which starts as specified is considered to match.
 *     The first matching column, or if there is none the first matching
 *     parameter value is returned.  UCD matching is case-insensitive.
 *
 * <dt>"RANDOM":
 * <dd>The special token "RANDOM" evaluates to a double-precision random
 *     number <code>0<=x<1</code> which is constant for a given row
 *     within this reader.  The quality of the random numbers may not
 *     be particularly good.
 *
 * </dl>
 *
 * @author   Mark Taylor
 * @since    7 Dec 2007
 */
public abstract class StarTableJELRowReader extends JELRowReader {

    private final StarTable table_;
    private final long HASH_LONG = System.identityHashCode( this );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.jel" );

    /**
     * The string which should be prefixed to a table parameter (constant)
     * name to result in substituting its value.
     */
    public static final String PARAM_PREFIX = "param$";

    /**
     * A string to prefix to a UCD string to indicate the column/parameter
     * with that UCD.  The first matching column, else the first matching
     * parameter, is used.  Punctuation in the UCD name is all mapped to "_".
     * A trailing "_" corresponds to a wildcard.
     */
    public static final String UCD_PREFIX = "ucd$";

    /**
     * Constructs a new row reader for a given StarTable.
     * Note that this reader cannot become aware of changes to the
     * columns of the table; in the event of
     * such changes this object should be dicarded and and a new one
     * used for any new expressions.
     *
     * @param  table  the StarTable this reader will read from
     */
    public StarTableJELRowReader( StarTable table ) {
        table_ = table;
    }

    /**
     * Returns the value for a given column in this reader's table at
     * the current row.
     *
     * @param  icol  column index
     * @return  contents of column <tt>icol</tt> at the current row
     */
    protected abstract Object getCell( int icol ) throws IOException;

    /**
     * Returns the index of the row on which evaluations are currently
     * taking place.
     *
     * @return   row index (first row is 0)
     */
    public abstract long getCurrentRow();

    protected boolean isBlank( int icol ) {
        try {
            return Tables.isBlank( getCell( icol ) );
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return true;
        }
    }

    protected Class getColumnClass( int icol ) {
        return table_.getColumnInfo( icol ).getContentClass();
    }

    protected int getColumnIndexByName( String name ) {

        /* Try it as a UCD specification. */
        String ucdSpec = stripPrefix( name, UCD_PREFIX );
        if ( ucdSpec != null ) {
            Pattern ucdRegex = getUcdRegex( ucdSpec );
            for ( int icol = 0; icol < table_.getColumnCount(); icol++ ) {
                String ucd = table_.getColumnInfo( icol ).getUCD();
                if ( ucd != null && ucdRegex.matcher( ucd ).matches() ) {
                    return icol;
                }
            }
        }

        /* Try the column name. */
        for ( int icol = 0; icol < table_.getColumnCount(); icol++ ) {
            if ( table_.getColumnInfo( icol ).getName()
                       .equalsIgnoreCase( name ) ) {
                return icol;
            }
        }

        /* It's not a column. */
        return -1;
    }

    /**
     * Understands table parameters identified case-insensitively
     * by name (using the {@link #PARAM_PREFIX} prefix) or
     * by UCD (using the {@link #UCD_PREFIX} prefix).
     */
    protected Constant getConstantByName( String name ) {

        /* Try it as a UCD specification. */
        String ucdSpec = stripPrefix( name, UCD_PREFIX );
        if ( ucdSpec != null ) {
            Pattern ucdRegex = getUcdRegex( ucdSpec );
            for ( Iterator it = table_.getParameters().iterator();
                  it.hasNext(); ) {
                DescribedValue dval = (DescribedValue) it.next();
                String ucd = dval.getInfo().getUCD();
                if ( ucd != null && ucdRegex.matcher( ucd ).matches() ) {
                    return new DescribedValueConstant( dval );
                }
            }
            return null;
        }

        /* Try it as a named parameter. */
        String pname = stripPrefix( name, PARAM_PREFIX );
        if ( pname != null ) {
            for ( Iterator it = table_.getParameters().iterator();
                  it.hasNext(); ) {
                DescribedValue dval = (DescribedValue) it.next();
                if ( pname.equalsIgnoreCase( dval.getInfo().getName() ) ) {
                    return new DescribedValueConstant( dval );
                }
            }
            return null;
        }

        /* Not a parameter. */
        return null;
    }

    /**
     * Adds to the superclass implementation the following:
     * <ul>
     * <li>"$0" or "index" returns INDEX_ID, which refers to the
     *     (1-based) row number
     * <li>"RANDOM" returns a double random number, always the same for a
     *     given row
     * </ul>
     */
    protected Constant getSpecialByName( String name ) {
        if ( name.equals( COLUMN_ID_CHAR + "0" ) ||
             name.equalsIgnoreCase( "Index" ) ) {
            return new Constant() {
                public Class getContentClass() {
                    return Long.class;
                }
                public Object getValue() {
                    return new Long( getCurrentRow() + 1 );
                }
            };
        }
        else if ( name.equals( "RANDOM" ) ) {
            return new Constant() {
                public Class getContentClass() {
                    return Double.class;
                }
                public Object getValue() {
                    long seed = HASH_LONG + ( getCurrentRow() * 2000000011L );
                    return new Double( new Random( seed ).nextDouble() );
                }
            };
        }
        else {
            return super.getSpecialByName( name );
        }
    }

    protected boolean getBooleanColumnValue( int icol ) {
        return getBooleanValue( (Boolean) getCellValue( icol ) );
    }
    protected byte getByteColumnValue( int icol ) {
        return getByteValue( (Byte) getCellValue( icol ) );
    }
    protected char getCharColumnValue( int icol ) {
        return getCharValue( (Character) getCellValue( icol ) );
    }
    protected short getShortColumnValue( int icol ) {
        return getShortValue( (Short) getCellValue( icol ) );
    }
    protected int getIntColumnValue( int icol ) {
        return getIntValue( (Integer) getCellValue( icol ) );
    }
    protected long getLongColumnValue( int icol ) {
        return getLongValue( (Long) getCellValue( icol ) );
    }
    protected float getFloatColumnValue( int icol ) {
        return getFloatValue( (Float) getCellValue( icol ) );
    }
    protected double getDoubleColumnValue( int icol ) {
        return getDoubleValue( (Double) getCellValue( icol ) );
    }
    protected Object getObjectColumnValue( int icol ) {
        return getCellValue( icol );
    }

    /**
     * Returns the value of a cell in the current row without throwing checked
     * errors.
     *
     * @param   icol  column index
     * @return  cell value
     */
    private Object getCellValue( int icol ) {
        try {
            return getCell( icol );
        }
        catch ( IOException e ) {
            logger_.warning( "Expression evaluation error: " + e );
            return null;
        }
    }

    /**
     * Takes a (non-prefixed) UCD specification and returns a Pattern
     * actual UCDs should match if they represent the same thing.
     * Punctuation is mapped to underscores, and the pattern is
     * case-insensitive, which means that the same
     * syntax can work for UCD1s and UCD1+s.  If a trailing underscore
     * (or other punctuation mark) is present in the input <code>ucd</code>
     * it is considered as a trailing match-all wildcard.
     *
     * @param   ucd  UCD1 or UCD1+ specification/pattern
     * @return   regular expression pattern which matches actual UCD1s or UCD1+s
     */
    public static Pattern getUcdRegex( String ucd ) {
        String regex = ucd.replaceAll( "[_\\W]", "\\[_\\\\W\\]" );
        if ( regex.endsWith( "[_\\W]" ) ) {
            regex = regex.substring( 0, regex.length() - 5 ) + ".*";
        }
        return Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
    }

    /**
     * Constant implementation based on a DescribedValue.
     */
    private static class DescribedValueConstant implements Constant {
        private final Class clazz_;
        private final Object value_;

        /**
         * Constructor.
         *
         * @param  dval  described value
         */
        DescribedValueConstant( DescribedValue dval ) {
            clazz_ = dval.getInfo().getContentClass();
            Object val = dval.getValue();
            value_ = Tables.isBlank( val ) ? null : val;
        }

        public Class getContentClass() {
            return clazz_;
        }

        public Object getValue() {
            return value_;
        }
    }
}
