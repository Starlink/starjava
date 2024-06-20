package uk.ac.starlink.table.formats;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TimeMapper;

/**
 * Examines unknown rows (arrays of strings) to work out what they contain.
 * By repeatedly calling {@link #submitRow} the evaluator can refine its
 * idea of what kind of data is represented by each column.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Sep 2004
 */
public class RowEvaluator {

    private boolean[] maybeBlank_;
    private boolean[] maybeBoolean_;
    private boolean[] maybeShort_;
    private boolean[] maybeInteger_;
    private boolean[] maybeLong_;
    private boolean[] maybeFloat_;
    private boolean[] maybeDouble_;
    private boolean[] maybeDate_;
    private boolean[] maybeHms_;
    private boolean[] maybeDms_;
    private int[] stringLength_;
    private long nrow_;
    private int ncol_ = -1;

    /** Regular expression for ISO 8601 dates. */
    public static final Pattern ISO8601_REGEX = Pattern.compile(
        "([0-9]+)-([0-9]{1,2})-([0-9]{1,2})" +
        "(?:[" + 'T' + " ]([0-9]{1,2})" +
            "(?::([0-9]{1,2})" +
                "(?::([0-9]{1,2}(?:\\.[0-9]*)?))?" +
            ")?" +
        "Z?)?"
    );
    private static final Pattern HMS_REGEX = Pattern.compile(
        "[ 012]?[0-9][:h ][ 0-6][0-9][:m ][0-6][0-9](\\.[0-9]*)?"
    );
    private static final Pattern DMS_REGEX = Pattern.compile(
        "[-+][ 0-9]?[0-9][:d ][ 0-6][0-9][:m ][0-6][0-9](\\.[0-9]*)?"
    );
    private static final Pattern NAN_REGEX = Pattern.compile(
        "NaN", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INFINITY_REGEX = Pattern.compile(
        "([+-]?)(Infinity|inf)", Pattern.CASE_INSENSITIVE
    );

    /** Decoder for values that are all blank. */
    private static Decoder BLANK_DECODER = new StringDecoder() {
        private Pattern blankRegex_ = Pattern.compile( " *" );
        public Object decode( String value ) {
            return null;
        }
        public boolean isValid( String value ) {
            return value == null
                || blankRegex_.matcher( value ).matches();
        }
    };

    /** Decoder for booleans. */
    private static Decoder BOOLEAN_DECODER = new Decoder( Boolean.class ) {
        public Object decode( String value ) {
            char v1 = value.trim().charAt( 0 );
            return ( v1 == 't' || v1 == 'T' ) ? Boolean.TRUE
                                              : Boolean.FALSE;
        }
        public boolean isValid( String value ) {
            return value.equalsIgnoreCase( "false" )
                || value.equalsIgnoreCase( "true" )
                || value.equalsIgnoreCase( "f" )
                || value.equalsIgnoreCase( "t" );
        }
    };

    /* We are careful to check for "-0" type cells in the integer type
     * decoders - it is essential that they are coded as floating types
     * (which can represent negative zero) rather than integer types
     * (which can't), since a negative zero is most likely the
     * hours/degrees part of a sexegesimal angle, in which the
     * difference is very important
     * (see uk.ac.starlink.topcat.func.Angles.dmsToRadians). */

    /** Decoder for shorts. */
    private static Decoder SHORT_DECODER = new Decoder( Short.class ) {
        public Object decode( String value ) {
            return Short.valueOf( value.trim() );
        }
        public boolean isValid( String value ) {
            try {
                return Short.parseShort( value ) != 0
                    || value.charAt( 0 ) != '-';
            }
            catch ( NumberFormatException e ) {
                return false;
            }
        }
    };

    /** Decoder for integers. */
    private static Decoder INTEGER_DECODER = new Decoder( Integer.class ) {
        public Object decode( String value ) {
            return Integer.valueOf( value.trim() );

        }
        public boolean isValid( String value ) {
            try {
                return Integer.parseInt( value ) != 0
                    || value.charAt( 0 ) != '-';
            }
            catch ( NumberFormatException e ) {
                return false;
            }
        }
    };

    /** Decoder for longs. */
    private static Decoder LONG_DECODER = new Decoder( Long.class ) {
        public Object decode( String value ) {
            return Long.valueOf( value.trim() );
        }
        public boolean isValid( String value ) {
            try {
                return Long.parseLong( value ) != 0L
                    || value.charAt( 0 ) != '-';
            }
            catch ( NumberFormatException e ) {
                return false;
            }
        }
    };

    /** Decoder for floats. */
    private static Decoder FLOAT_DECODER = new Decoder( Float.class ) {
        public Object decode( String value ) {
            return Float.valueOf( (float) parseFloating( value.trim() ).dValue);
        }
        public boolean isValid( String value ) {
            try {
                ParsedFloat pf = parseFloating( value );
                double dval = pf.dValue;
                return dval == 0
                    || Double.isNaN( dval )
                    || Double.isInfinite( dval )
                    || ( pf.sigFig <= 6 && isSinglePrecision( dval ) );
            }
            catch ( NumberFormatException e ) {
                return false;
            }
        }
        private boolean isSinglePrecision( double dval ) {
            double absVal = Math.abs( dval );
            return absVal > Float.MIN_NORMAL && absVal < Float.MAX_VALUE;
        }
    };

    /** Decoder for doubles. */
    private static Decoder DOUBLE_DECODER = new Decoder( Double.class ) {
        public Object decode( String value ) {
            return Double.valueOf( parseFloating( value.trim() ).dValue );
        }
        public boolean isValid( String value ) {
            try {
                parseFloating( value );
                return true;
            }
            catch ( NumberFormatException e ) {
                return false;
            }
        }
    };

    /** Decoder for ISO-8601 dates. */
    private static Decoder DATE_DECODER = new StringDecoder() {
        public ColumnInfo createColumnInfo( String name ) {
            ColumnInfo info = super.createColumnInfo( name );
            info.setUnitString( "iso-8601" );
            info.setUCD( "TIME" );
            info.setDomainMappers( new DomainMapper[] { TimeMapper.ISO_8601 } );
            return info;
        }
        public boolean isValid( String value ) {
            return ISO8601_REGEX.matcher( value ).matches();
        }
    };

    /** Decoder for HMS sexagesimal strings. */
    private static Decoder HMS_DECODER = new StringDecoder() {
        public ColumnInfo createColumnInfo( String name ) {
            ColumnInfo info = super.createColumnInfo( name );
            info.setUnitString( "hms" );
            return info;
        }
        public boolean isValid( String value ) {
            return HMS_REGEX.matcher( value ).matches();
        }
    };

    /** Decoder for DMS sexagesimal strings. */
    private static Decoder DMS_DECODER = new StringDecoder() {
        public ColumnInfo createColumnInfo( String name ) {
            ColumnInfo info = super.createColumnInfo( name );
            info.setUnitString( "dms" );
            return info;
        }
        public boolean isValid( String value ) {
            return DMS_REGEX.matcher( value ).matches();
        }
    };

    /** Decoder for any old string. */
    private static Decoder STRING_DECODER = new StringDecoder() {
        public boolean isValid( String value ) {
            return true;
        }
    };

    /**
     * Constructs a new RowEvaluator which will work out the number of
     * columns from the data.
     */
    public RowEvaluator() {
    }

    /**
     * Constructs a new RowEvaluator which will examine rows with a
     * fixed number of columns.
     *
     * @param  ncol  column count
     */
    public RowEvaluator( int ncol ) {
        init( ncol );
    }

    /**
     * Initializes to deal with rows of a given number of elements.
     */
    private void init( int ncol ) {
        ncol_ = ncol; 

        /* This data could be set up more compactly, indexing via type-specific
         * decoders rather than having a named array for each possible type. */
        maybeBlank_ = makeFlagArray( true );
        maybeBoolean_ = makeFlagArray( true );
        maybeShort_ = makeFlagArray( true );
        maybeInteger_ = makeFlagArray( true );
        maybeLong_ = makeFlagArray( true );
        maybeFloat_ = makeFlagArray( true );
        maybeDouble_ = makeFlagArray( true );
        maybeDate_ = makeFlagArray( true );
        maybeHms_ = makeFlagArray( true );
        maybeDms_ = makeFlagArray( true );
        stringLength_ = new int[ ncol ];
    }

    /**
     * Looks at a given row (list of strings) and records information about
     * what sort of things it looks like it contains.
     *
     * @param   row  <code>ncol</code>-element list of strings
     * @throws  TableFormatException  if the number of elements in
     *          <code>row</code> is not the same as on the first call
     */
    public void submitRow( List<String> row ) throws TableFormatException {
        nrow_++;
        if ( ncol_ < 0 ) {
            init( row.size() );
        }
        if ( row.size() != ncol_ ) {
            throw new TableFormatException(
                "Wrong number of columns at row " + nrow_ +
                " (expecting " + ncol_ + ", found " + row.size() +  ")" );
        }
        for ( int icol = 0; icol < ncol_; icol++ ) {
            boolean done = false;
            String cell0 = row.get( icol );
            int leng0 = cell0 == null ? 0 : cell0.length();
            String cell = cell0 == null ? "" : cell0.trim();
            int leng = cell.length();
            if ( leng0 > stringLength_[ icol ] ) {
                stringLength_[ icol ] = leng0;
            }
            if ( leng > 0 ) {
                updateColFlag( icol, cell, maybeBlank_, BLANK_DECODER );
                updateColFlag( icol, cell, maybeBoolean_, BOOLEAN_DECODER );
                updateColFlag( icol, cell, maybeShort_, SHORT_DECODER );
                updateColFlag( icol, cell, maybeInteger_, INTEGER_DECODER );
                updateColFlag( icol, cell, maybeLong_, LONG_DECODER );
                updateColFlag( icol, cell, maybeFloat_, FLOAT_DECODER );
                updateColFlag( icol, cell, maybeDouble_, DOUBLE_DECODER );
                updateColFlag( icol, cell, maybeDate_, DATE_DECODER );
                updateColFlag( icol, cell, maybeHms_, HMS_DECODER );
                updateColFlag( icol, cell, maybeDms_, DMS_DECODER );
            }
        }
    }

    /**
     * Updates an element of a flags array based on compatibility of
     * a cell value with a given decoder.
     *
     * @param  icol  index into colFlags array
     * @param  cell  test cell value
     * @param  colFlags   flags array
     * @param  decoder   if cell is marked as invalid by decoder,
     *                   then <code>colFlags[icol]</code> will be set false
     */
    private static void updateColFlag( int icol, String cell,
                                       boolean[] colFlags, Decoder decoder ) {

        /* Get the short circuiting right for efficiency; a failed validity
         * test can throw an exception and so be expensive, so it's important
         * that it's not done over and over again for a column. */
        if ( colFlags[ icol ] && ! decoder.isValid( cell ) ) {
            colFlags[ icol ] = false;
        }
    }

    /**
     * Returns information gleaned from previous <code>submitRow</code>
     * calls about the kind of data that appears to be in the columns.
     *
     * @return  metadata
     */
    public Metadata getMetadata() {
        ColumnInfo[] colInfos = new ColumnInfo[ ncol_ ];
        Decoder[] decoders = new Decoder[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            final Decoder decoder;
            String name = "col" + ( icol + 1 );
            if ( maybeBlank_[ icol ] ) {
                decoder = BLANK_DECODER;
            }
            else if ( maybeBoolean_[ icol ] ) {
                decoder = BOOLEAN_DECODER;
            }
            else if ( maybeShort_[ icol ] ) {
                decoder = SHORT_DECODER;
            }
            else if ( maybeInteger_[ icol ] ) {
                decoder = INTEGER_DECODER;
            }
            else if ( maybeLong_[ icol ] ) {
                decoder = LONG_DECODER;
            }
            else if ( maybeFloat_[ icol ] ) {
                decoder = FLOAT_DECODER;
            }
            else if ( maybeDouble_[ icol ] ) {
                decoder = DOUBLE_DECODER;
            }
            else if ( maybeDate_[ icol ] ) {
                decoder = DATE_DECODER;
            }
            else if ( maybeHms_[ icol ] ) {
                decoder = HMS_DECODER;
            }
            else if ( maybeDms_[ icol ] ) {
                decoder = DMS_DECODER;
            }
            else {
                decoder = STRING_DECODER;
            }
            decoders[ icol ] = decoder;
            ColumnInfo info = decoder.createColumnInfo( name );
            if ( decoder instanceof StringDecoder ) {
                info.setElementSize( stringLength_[ icol ] );
            }
            colInfos[ icol ] = info;
        }
        return new Metadata( colInfos, decoders, nrow_ );
    }

    /**
     * Returns a new <code>ncol</code>-element boolean array.
     *
     * @param   val  initial value of all flags
     * @return  new flag array initialized to <code>val</code>
     */
    private boolean[] makeFlagArray( boolean val ) {
        boolean[] flags = new boolean[ ncol_ ];
        Arrays.fill( flags, val );
        return flags;
    }

    /**
     * Parses a floating point value.  This does a couple of extra things
     * than Double.parseDouble - it understands 'd' or 'D' as the exponent
     * signifier as well as 'e' or 'E', and it counts the number of
     * significant figures.
     *
     * @param   item  string representing a floating point number
     * @return  object encapsulating information about the floating pont
     *          value extracted from <code>item</code> - note it's always the
     *          same instance returned, so don't hang onto it
     * @throws  NumberFormatException  if <code>item</code> can't be understood
     *                                 as a float or double
     */
    private static ParsedFloat parseFloating( String item ) {

        /* Check for special values.  Although parseDouble picks up 
         * some of these, it only works with java-friendly forms like
         * "NaN" and not (e.g.) python-friendly ones like "nan". */
        if ( NAN_REGEX.matcher( item ).matches() ) {
            return ParsedFloat.NaN;
        }
        Matcher infMatcher = INFINITY_REGEX.matcher( item );
        if ( infMatcher.matches() ) {
            String sign = infMatcher.group( 1 );
            return sign.length() > 0 && sign.charAt( 0 ) == '-'
                 ? ParsedFloat.NEGATIVE_INFINITY
                 : ParsedFloat.POSITIVE_INFINITY;
        }

        /* Do a couple of jobs by looking at the string directly:
         * Substitute 'd' or 'D' which may indicate an exponent in
         * FORTRAN77-style output for an 'e', and count the number of
         * significant figures.  With some more work it would be possible
         * to do the actual parse here, but since this probably isn't
         * a huge bottleneck we leave it to Double.parseDouble. */
        int nc = item.length();
        boolean foundExp = false;
        int sigFig = 0;
        for ( int i = 0; i < nc; i++ ) {
            char c = item.charAt( i );
            switch ( c ) {
                case 'd':
                case 'D':
                    if ( ! foundExp ) {
                        StringBuffer sbuf = new StringBuffer( item );
                        sbuf.setCharAt( i, 'e' );
                        item = sbuf.toString();
                    }
                    foundExp = true;
                    break;
                case 'e':
                case 'E':
                    foundExp = true;
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if ( ! foundExp ) {
                        sigFig++;
                    }
                    break;
                default:
            }
        }

        /* Parse the number. */
        double dvalue = Double.parseDouble( item );
        return new ParsedFloat( sigFig, dvalue );
    }

    /**
     * Helper class used to group quantities which describe what the
     * data types found in the columns of a table are.
     */
    public static class Metadata {
        public final ColumnInfo[] colInfos_;
        public final Decoder[] decoders_;
        public final long nrow_;
        public final int ncol_;
        public Metadata( ColumnInfo[] colInfos, Decoder[] decoders,
                         long nrow ) {
            colInfos_ = colInfos;
            decoders_ = decoders;
            nrow_ = nrow;
            if ( colInfos_.length != decoders_.length ) {
                throw new IllegalArgumentException();
            }
            ncol_ = colInfos_.length;
        }
    }

    /**
     * Interface for an object that can turn a string into a cell content
     * object.
     */
    public static abstract class Decoder {
        private final Class<?> clazz_;

        /**
         * Constructor.
         *
         * @param   clazz  class of object to be returned by decode method
         */
        public Decoder( Class<?> clazz ) {
            clazz_ = clazz;
        }

        /**
         * Returns a new ColumnInfo suitable for the decoded values.
         *
         * @param  name  column name
         * @return  new metadata object
         */
        public ColumnInfo createColumnInfo( String name ) {
            return new ColumnInfo( name, clazz_, null );
        }

        /**
         * Decodes a value.
         * Will complete without exception if {@link #isValid} returns true
         * for the presented <code>value</code>; otherwise may throw an
         * unchecked exception.
         *
         * @param  value  string to decode
         * @return   typed object corresponding to <code>value</code>
         */
        public abstract Object decode( String value );

        /**
         * Indicates whether this decoder is capable of decoding a 
         * given string.
         *
         * @param  value  string to decode
         * @return  true iff this decoder can make sense of the string
         */
        public abstract boolean isValid( String value );
    }

    /**
     * Partial Decoder implementation for strings..
     */
    private static abstract class StringDecoder extends Decoder {
        StringDecoder() {
            super( String.class );
        }

        /**
         * Returns the value unchanged.
         */
        public Object decode( String value ) {
            return value;
        }
    }

    /**
     * Helper class to encapsulate the result of a floating point number
     * parse.
     */
    private static class ParsedFloat {

        /** Number of significant figures. */
        final int sigFig;

        /** Value of the number. */
        final double dValue;

        static final ParsedFloat NaN = new ParsedFloat( 0, Double.NaN );
        static final ParsedFloat POSITIVE_INFINITY =
            new ParsedFloat( 0, Double.POSITIVE_INFINITY );
        static final ParsedFloat NEGATIVE_INFINITY =
            new ParsedFloat( 0, Double.NEGATIVE_INFINITY );

        /**
         * Constructor.
         *
         * @param  sigFig  number of significant figures
         * @param  dValue  floating point value
         */
        ParsedFloat( int sigFig, double dValue ) {
            this.sigFig = sigFig;
            this.dValue = dValue;
        }
    }
}
