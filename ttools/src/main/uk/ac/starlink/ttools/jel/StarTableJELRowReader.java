package uk.ac.starlink.ttools.jel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
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
 * <dt>"$0" or "$index" or (deprecated) "index" (case insensitive):
 * <dd>the 1-based index of the current row.
 *
 * <dt>"$ncol"</dt>
 * <dd>Number of columns in the table.
 *
 * <dt>"$nrow"</dt>
 * <dd>Number of rows in the table.  If this is not known, a null value
 *     is returned.
 *
 * <dt>Parameter names:
 * <dd>The string {@value #PARAM_PREFIX} followed by the name of a table
 *     parameter (case-insensitive) is a constant for the table
 *     (as a primitive, if applicable).  This can only work
 *     if the parameter name is a legal java identifier.
 *
 * <dt>UCD specifiers:
 * <dd>The string {@value #UCD_PREFIX} followed by the text of a UCD
 *     giving the required value.  Any punctuation (such as ".", ";", "-")
 *     in the UCD should be replaced with a "_" (since these symbols cannot
 *     appear in identifiers).  If the identifier has a trailing "_",
 *     then any UCD which starts as specified is considered to match.
 *     The first matching column, or if there is none the first matching
 *     parameter value is returned.  UCD matching is case-insensitive.
 *
 * <dt>Utype specifiers:
 * <dd>The string {@value #UTYPE_PREFIX} followed by the text of a Utype
 *     identifying the required value.  Any punctuation (such as ".", ":", "-")
 *     in the Utype should be replaced with a "_" (since these symbols cannot
 *     appear in identifiers).
 *     The first matching column, or if there is none the first matching
 *     parameter value is returned.  UType matching is case-insensitive.
 *
 * <dt>"$random" (case insensitive) or (deprecated) "RANDOM":
 * <dd>The special token "$random" evaluates to a double-precision random
 *     number <code>0&lt;=x&lt;1</code> which is constant for a given row
 *     within this reader.  The quality of the random numbers may not
 *     be particularly good.
 *
 * <dt>"value*()" functions:
 * <dd>The methods {@link #valueDouble valueDouble},
 *     {@link #valueInt valueInt}, {@link #valueLong valueLong},
 *     {@link #valueString valueString} and {@link #valueObject valueObject}
 *     are provided.
 *     Each takes as an argument an exact column name, and provides the
 *     typed value of the column at the current row.
 *     These methods are <em>not</em> the generally recommended way to
 *     access column values; they are slower and less tidy than simply
 *     using column names or $IDs in expressions, and do not admit of
 *     static analysis, so their use can <em>not</em> be reflected in
 *     the results of {@link #getTranslatedColumns getTranslatedColumns}.
 *     However, it does allow access by column name to columns with names
 *     that are not legal java identifiers.
 *
 * </dl>
 *
 * @author   Mark Taylor
 * @since    7 Dec 2007
 */
public abstract class StarTableJELRowReader extends JELRowReader {

    private final StarTable table_;
    private Map<String,ColMeta> colMetaMap_;
    private static final AtomicInteger seeder_ = new AtomicInteger();
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
     * A string to prefix to a Utype string to indicate the column/parameter
     * with that Utype.  The first matching column, else the first matching
     * parameter, is used.  Punctuation in the Utype name is all mapped to "_".
     */
    public static final String UTYPE_PREFIX = "utype$";

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
     * Returns the table associated with this reader.
     *
     * @return  table
     */
    public StarTable getTable() {
        return table_;
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

    /**
     * Indicates whether this RowReader has been asked to reference any
     * constants for which the row index is required.
     * In practice this means it will return true if any of the JEL expressions
     * which this RowReader has been asked to compile may need to call
     * the {@link #getCurrentRow} method.
     * Since not all row reader implementations are able to return a value
     * for that method, this is useful information.
     *
     * @return    true if the current row index may be required during
     *            evaluation
     */
    public boolean requiresRowIndex() {
        for ( Constant<?> konst : getTranslatedConstants() ) {
            if ( konst.requiresRowIndex() ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value of a named column in this reader's table
     * at the current row as a double.
     * This is not generally the recommended way to access column values,
     * but it will work for column names without any syntactical restrictions.
     *
     * @param  colName  column name, matched exactly
     * @return   value of named column as a double,
     *           or NaN on failure (no such column or value not numeric)
     */
    public double valueDouble( String colName ) {
        ColMeta colMeta = getColMetaByExactName( colName );
        if ( colMeta != null && colMeta.isNumber_ ) {
            Object value = getCellValue( colMeta.icol_ );
            if ( value instanceof Number ) {
                return ((Number) value).doubleValue();
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the value of a named column in this reader's table
     * at the current row as an int.
     * This is not generally the recommended way to access column values,
     * but it will work for column names without any syntactical restrictions.
     *
     * @param  colName  column name, matched exactly
     * @return   value of column as an int;
     *           foundNull is called on failure
     *           (no such column or value not numeric or not finite)
     */
    public int valueInt( String colName ) { 
        ColMeta colMeta = getColMetaByExactName( colName );
        if ( colMeta != null && colMeta.isNumber_ ) {
            Object value = getCellValue( colMeta.icol_ );
            if ( value instanceof Number ) {
                Number num = (Number) value;
                if ( Double.isFinite( num.doubleValue() ) ) {
                    return num.intValue();
                }
            }
        }
        foundNull();
        return 0;
    }

    /**
     * Returns the value of a named column in this reader's table
     * at the current row as a long int.
     * This is not generally the recommended way to access column values,
     * but it will work for column names without any syntactical restrictions.
     *
     * @param  colName  column name, matched exactly
     * @return   value of column as a long;
     *           foundNull is called on failure
     *           (no such column or value not numeric or not finite)
     */
    public long valueLong( String colName ) {
        ColMeta colMeta = getColMetaByExactName( colName );
        if ( colMeta != null && colMeta.isNumber_ ) {
            Object value = getCellValue( colMeta.icol_ );
            if ( value instanceof Number ) {
                Number num = (Number) value;
                if ( Double.isFinite( num.doubleValue() ) ) {
                    return num.longValue();
                }
            }
        }
        foundNull();
        return 0L;
    }

    /**
     * Returns the value of a named column in this reader's table
     * at the current row as a String.
     * This is not generally the recommended way to access column values,
     * but it will work for column names without any syntactical restrictions.
     *
     * @param  colName  column name, matched exactly
     * @return   value of column as a String, or null if no such column
     */
    public String valueString( String colName ) {
        ColMeta colMeta = getColMetaByExactName( colName );
        if ( colMeta != null ) {
            Object value = getCellValue( colMeta.icol_ );
            if ( value != null ) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * Returns the value of a named column in this reader's table
     * at the current row as an Object;
     * This is not generally the recommended way to access column values,
     * but it will work for column names without any syntactical restrictions.
     *
     * @param  colName  column name, matched exactly
     * @return   value of column as an Object, or null if no such column
     */
    public Object valueObject( String colName ) {
        ColMeta colMeta = getColMetaByExactName( colName );
        if ( colMeta != null ) {
            return getCellValue( colMeta.icol_ );
        }
        return null;
    }

    protected boolean isBlank( int icol ) {
        try {
            return Tables.isBlank( getCell( icol ) );
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return true;
        }
    }

    protected Class<?> getColumnClass( int icol ) {
        return icol < table_.getColumnCount()
             ? table_.getColumnInfo( icol ).getContentClass()
             : null;
    }

    protected int getColumnIndexByName( String name ) {
        ColumnInfo[] colInfos = Tables.getColumnInfos( table_ );
        int ncol = colInfos.length;

        /* Try it as a UCD specification. */
        String ucdSpec = stripPrefix( name, UCD_PREFIX );
        if ( ucdSpec != null ) {
            Pattern ucdRegex = getUcdRegex( ucdSpec );
            for ( int icol = 0; icol < ncol; icol++ ) {
                String ucd = colInfos[ icol ].getUCD();
                if ( ucd != null && ucdRegex.matcher( ucd ).matches() ) {
                    return icol;
                }
            }
        }

        /* Try it as a Utype specification. */
        String utypeSpec = stripPrefix( name, UTYPE_PREFIX );
        if ( utypeSpec != null ) {
            Pattern utypeRegex = getUtypeRegex( utypeSpec );
            for ( int icol = 0; icol < ncol; icol++ ) {
                String utype = colInfos[ icol ].getUtype();
                if ( utype != null && utypeRegex.matcher( utype ).matches() ) {
                    return icol;
                }
            }
        }

        /* Try the column name.  Try case-sensitive first,
         * then case-insensitive. */
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( colInfos[ icol ].getName().equals( name ) ) {
                return icol;
            }
        }
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( colInfos[ icol ].getName().equalsIgnoreCase( name ) ) {
                return icol;
            }
        }

        /* It's not a column. */
        return -1;
    }

    /**
     * Returns a table parameter that can be identified by the given
     * designation.  This will typically have a prefix such as
     * {@value #PARAM_PREFIX}, {@value #UCD_PREFIX} etc.
     * Called during expression evaluation, may be overridden.
     *
     * @param  name   designation in JEL expression
     * @return  fixed value for <code>name</code>, or null
     */
    public DescribedValue getDescribedValueByName( String name ) {
        List<DescribedValue> paramList = table_.getParameters();

        /* Try it as a UCD specification. */
        String ucdSpec = stripPrefix( name, UCD_PREFIX );
        if ( ucdSpec != null ) {
            Pattern ucdRegex = getUcdRegex( ucdSpec );
            for ( DescribedValue dval : paramList ) {
                String ucd = dval.getInfo().getUCD();
                if ( ucd != null && ucdRegex.matcher( ucd ).matches() ) {
                    return dval;
                }
            }
            return null;
        }

        /* Try it as a Utype specification. */
        String utypeSpec = stripPrefix( name, UTYPE_PREFIX );
        if ( utypeSpec != null ) {
            Pattern utypeRegex = getUtypeRegex( utypeSpec );
            for ( DescribedValue dval : paramList ) {
                String utype = dval.getInfo().getUtype();
                if ( utype != null && utypeRegex.matcher( utype ).matches() ) {
                    return dval;
                }
            }
            return null;
        }

        /* Try it as a named parameter.  Try case-sensitive first, then
         * case-insensitive. */
        String pname = stripPrefix( name, PARAM_PREFIX );
        if ( pname != null ) {
            for ( DescribedValue dval : paramList ) {
                if ( pname.equals( dval.getInfo().getName() ) ) {
                    return dval;
                }
            }
            for ( DescribedValue dval : paramList ) {
                if ( pname.equalsIgnoreCase( dval.getInfo().getName() ) ) {
                    return dval;
                }
            }
            return null;
        }
        return null;
    }

    /**
     * Understands table parameters identified case-insensitively
     * by name (using the {@link #PARAM_PREFIX} prefix) or
     * by UCD (using the {@link #UCD_PREFIX} prefix) or
     * by Utype (using the {@link #UTYPE_PREFIX} prefix).
     */
    protected Constant<?> getConstantByName( String name ) {

        /* Try a table parameter. */
        DescribedValue dval = getDescribedValueByName( name );
        if ( dval != null ) {
            return createDescribedValueConstant( dval );
        }

        /* Try special values for row and column count. */
        if ( name.equalsIgnoreCase( "$nrow" ) ) {
            return new Constant<Long>() {
                public Class<Long> getContentClass() {
                    return Long.class;
                }
                public Long getValue() {
                    return new Long( table_.getRowCount() );
                }
                public boolean requiresRowIndex() {
                    return false;
                }
            };
        }
        if ( name.equalsIgnoreCase( "$ncol" ) ) {
            return new Constant<Integer>() {
                public Class<Integer> getContentClass() {
                    return Integer.class;
                }
                public Integer getValue() {
                    int ncol = table_.getColumnCount();
                    return ncol >= 0 ? Integer.valueOf( ncol ) : null;
                }
                public boolean requiresRowIndex() {
                    return false;
                }
            };
        }

        /* Not a parameter. */
        return null;
    }

    /**
     * Adds to the superclass implementation the following:
     * <ul>
     * <li>"$0", "index" or "$index" gives the (1-based) row number
     * <li>"$ncol" gives the number of columns in the table
     * <li>"$nrow" gives the number of rows in the table (null if unknown)
     * <li>"$random" or "RANDOM" returns a double random number,
     *      always the same for a given row
     * </ul>
     */
    protected Constant<?> getSpecialByName( String name ) {
        if ( name.equals( COLUMN_ID_CHAR + "0" ) ||
             name.equalsIgnoreCase( "Index" ) ||
             name.equalsIgnoreCase( "$index" ) ) {
            return new Constant<Long>() {
                public Class<Long> getContentClass() {
                    return Long.class;
                }
                public Long getValue() {
                    return new Long( getCurrentRow() + 1 );
                }
                public boolean requiresRowIndex() {
                    return true;
                }
            };
        }

        /* Use of this token is DEPRECATED.
         * The implementation is fatally flawed, in that it won't always
         * return the same value from the same table cell.
         * It gets the row index (or at least tries to), but it needs also
         * to obtain another seed component in case the $random token is
         * used multiple times in the same row, since it doesn't want to
         * return the same random value for e.g. different columns.
         * It does that using the seeder_, which updates every time an
         * expression is compiled by this JELRowReader, but the trouble is
         * that the same expression may be compiled multiple times,
         * especially in a multi-threaded context.
         * But this functionality has been here for a long time,
         * so leave it in place at least for now.
         */
        else if ( name.equalsIgnoreCase( "$random" ) ||
                  name.equals( "RANDOM" ) ) {
            final long seed0 = seeder_.incrementAndGet() * -2323;
            return new Constant<Double>() {
                public Class<Double> getContentClass() {
                    return Double.class;
                }
                public Double getValue() {
                    long seed = seed0 + ( getCurrentRow() * 2000000011L );
                    return new Double( new Random( seed ).nextDouble() );
                }
                public boolean requiresRowIndex() {
                    return true;
                }
            };
        }

        else {
            return super.getSpecialByName( name );
        }
    }

    /**
     * Returns a Constant object based on a DescribedValue.
     * The supplied implementation evaluates the constant's class and value
     * once when this method is called.
     *
     * @param   dval  described value object
     * @return   constant which evaluates to dval's value
     */
    protected Constant<?> createDescribedValueConstant( DescribedValue dval ) {
        Object val = dval.getValue();
        return createConstant( dval.getInfo().getContentClass(),
                               Tables.isBlank( val ) ? null : val );
    }

    /**
     * Returns a Constant object given its class and its typed fixed value.
     *
     * @param  clazz  content class
     * @param  objValue  value which must be consistent with clazz
     * @return   fixed-value constant
     */
    private static <T> Constant<T> createConstant( Class<T> clazz,
                                                   Object objValue ) {
        final T value = clazz.cast( objValue );
        return new Constant<T>() {
            public Class<T> getContentClass() {
                return clazz;
            }
            public T getValue() {
                return value;
            }
            public boolean requiresRowIndex() {
                return false;
            }
        };
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
     * Returns a ColMeta object corresponding to the supplied column name.
     * There are no lexical restrictions on the form of the name,
     * and matching is exact.
     *
     * @param  name  column name
     * @return  metadata for column,
     *          or null if no column with the given name exists
     */
    private ColMeta getColMetaByExactName( String name ) {

        /* Construct the required map lazily, since for most instances
         * of this class it will never be used.
         * This code is effectively thread-safe; the worst that can happen
         * is that N identical instances will be constructed concurrently
         * and N-1 will be discarded. */
        if ( colMetaMap_ == null ) {
            Map<String,ColMeta> map = new HashMap<>();
            int ncol = table_.getColumnCount();
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnInfo info = table_.getColumnInfo( icol );
                map.put( info.getName(),
                         new ColMeta( icol, info.getContentClass() ) );
            }
            colMetaMap_ = map;
        }
        return colMetaMap_.get( name );
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
     * Takes a (non-prefixed) Utype specification and returns a Pattern
     * actual Utypes should match if they represent the same thing.
     * Punctuation is mapped to underscores, and the pattern is
     * case-insensitive.
     *
     * @param  utype  utype specification
     * @return  regular expression pattern which matches actual Utypes
     */
    public static Pattern getUtypeRegex( String utype ) {
        String regex = utype.replaceAll( "_", "\\\\W" );
        return Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
    }

    /**
     * Aggregates a column index and class information for a table column.
     */
    private static class ColMeta {
        final int icol_;
        final boolean isNumber_;
        final boolean isString_;

        /**
         * Constructor.
         *
         * @param   icol   column index in table
         * @param   clazz  content class of column
         */
        ColMeta( int icol, Class<?> clazz ) {
            icol_ = icol;
            isNumber_ = Number.class.isAssignableFrom( clazz );
            isString_ = String.class.equals( clazz );
        }
    }
}
