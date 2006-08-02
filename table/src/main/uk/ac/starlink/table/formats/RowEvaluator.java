package uk.ac.starlink.table.formats;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.TableFormatException;

/**
 * Examines unknown rows (arrays of strings) to work out what they contain.
 * By repeatedly calling {@link #submitRow} the evaluator can refine its
 * idea of what kind of data is represented by each column.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Sep 2004
 */
public class RowEvaluator {

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

    private static final Pattern ISO8601_REGEX = Pattern.compile(
        "([0-9]+)-([0-9]{1,2})-([0-9]{1,2})" +
        "(?:[" + 'T' + " ]([0-9]{1,2})" +
            "(?::([0-9]{1,2})" +
                "(?::([0-9]{1,2}(?:\\.[0-9]*)?))?" +
            ")?" +
        "Z?)?"
    );
    private static final Pattern HMS_REGEX = Pattern.compile(
        "[ 012]?[0-9][:h ][ 0-5][0-9][:m ][0-5][0-9](\\.[0-9]*)?"
    );
    private static final Pattern DMS_REGEX = Pattern.compile(
        "[-+][ 0-9]?[0-9][:d ][ 0-5][0-9][:m ][0-5][0-9](\\.[0-9]*)?"
    );

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
     * @param   row  <tt>ncol</tt>-element list of strings
     * @throws  TableFormatException  if the number of elements in
     *          <tt>row</tt> is not the same as on the first call
     */
    public void submitRow( List row ) throws TableFormatException {
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
            String cell = (String) row.get( icol );
            cell = cell == null ? "" : cell.trim();
            int leng = cell.length();
            if ( leng == 0 ) {
                done = true;
            }
            if ( leng > stringLength_[ icol ] ) {
                stringLength_[ icol ] = leng;
            }
            if ( ! done && maybeBoolean_[ icol ] ) {
                if ( cell.equalsIgnoreCase( "false" ) ||
                     cell.equalsIgnoreCase( "true" ) ||
                     cell.equalsIgnoreCase( "f" ) ||
                     cell.equalsIgnoreCase( "t" ) ) {
                    done = true;
                }
                else {
                    maybeBoolean_[ icol ] = false;
                }
            }

            /* We are careful to check for "-0" type cells here - it is
             * essential that they are coded as floating types (which
             * can represent negative zero) rather than integer types
             * (which can't), since a negative zero is most likely the
             * hours/degrees part of a sexegesimal angle, in which the
             * difference is very important
             * (see uk.ac.starlink.topcat.func.Angles.dmsToRadians). */
            boolean isMinus = ( ! done ) ? cell.charAt( 0 ) == '-' : false;

            if ( ! done && maybeShort_[ icol ] ) {
                try {
                    short val = Short.parseShort( cell );
                    if ( val == (short) 0 && isMinus ) {
                        throw new NumberFormatException();
                    }
                    done = true;
                }
                catch ( NumberFormatException e ) {
                    maybeShort_[ icol ] = false;
                }
            }
            if ( ! done && maybeInteger_[ icol ] ) {
                try {
                    int val = Integer.parseInt( cell );
                    if ( val == 0 && isMinus ) {
                        throw new NumberFormatException();
                    }
                    done = true;
                }
                catch ( NumberFormatException e ) {
                    maybeInteger_[ icol ] = false;
                }
            }
            if ( ! done && maybeLong_[ icol ] ) {
                try {
                    long val = Long.parseLong( cell );
                    if ( val == 0 && isMinus ) {
                        throw new NumberFormatException();
                    }
                    done = true;
                }
                catch ( NumberFormatException e ) {
                    maybeLong_[ icol ] = false;
                }
            }
            if ( ! done && ( maybeFloat_[ icol ] ||
                             maybeDouble_[ icol ] ) ) {
                try {
                    ParsedFloat pf = parseFloating( cell );
                    if ( maybeFloat_[ icol ] ) {
                        if ( pf.sigFig > 6 ) {
                            maybeFloat_[ icol ] = false;
                        }
                        else if ( ! Double.isInfinite( pf.dValue ) &&
                                  Float.isInfinite( (float) pf.dValue ) ) {
                            maybeFloat_[ icol ] = false;
                        }
                    }
                    done = true;
                }
                catch ( NumberFormatException e ) {
                    maybeFloat_[ icol ] = false;
                    maybeDouble_[ icol ] = false;
                }
            }
            if ( ! done && ( maybeDate_[ icol ] ) ) {
                if ( ISO8601_REGEX.matcher( cell ).matches() ) {
                    done = true;
                }
                else {
                    maybeDate_[ icol ] = false;
                }
            }
            if ( ! done && ( maybeHms_[ icol ] ) ) {
                if ( HMS_REGEX.matcher( cell ).matches() ) {
                    done = true;
                }
                else {
                    maybeHms_[ icol ] = false;
                }
            }
            if ( ! done && ( maybeDms_[ icol ] ) ) {
                if ( DMS_REGEX.matcher( cell ).matches() ) {
                    done = true;
                }
                else {
                    maybeDms_[ icol ] = false;
                }
            }
        }
    }

    /**
     * Returns information gleaned from previous <tt>submitRow</tt>
     * calls about the kind of data that appears to be in the columns.
     *
     * @return  metadata
     */
    public Metadata getMetadata() {
        ColumnInfo[] colInfos = new ColumnInfo[ ncol_ ];
        Decoder[] decoders = new Decoder[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            Class clazz;
            Decoder decoder;
            ColumnInfo colinfo;
            String name = "col" + ( icol + 1 );
            if ( maybeBoolean_[ icol ] ) {
                colinfo = new ColumnInfo( name, Boolean.class, null );
                decoder = new Decoder() {
                    public Object decode( String value ) {
                        char v1 = value.charAt( 0 );
                        return ( v1 == 't' || v1 == 'T' ) ? Boolean.TRUE
                                                          : Boolean.FALSE;
                    }
                };
            }
            else if ( maybeShort_[ icol ] ) {
                colinfo = new ColumnInfo( name, Short.class, null );
                decoder = new Decoder() {
                    public Object decode( String value ) {
                        return new Short( Short.parseShort( value ) );
                    }
                };
            }
            else if ( maybeInteger_[ icol ] ) {
                colinfo = new ColumnInfo( name, Integer.class, null );
                decoder = new Decoder() {
                    public Object decode( String value ) {
                        return new Integer( Integer.parseInt( value ) );
                    }
                };
            }
            else if ( maybeLong_[ icol ] ) {
                colinfo = new ColumnInfo( name, Long.class, null );
                decoder = new Decoder() {
                    public Object decode( String value ) {
                        return new Long( Long.parseLong( value ) );
                    }
                };
            }
            else if ( maybeFloat_[ icol ] ) {
                colinfo = new ColumnInfo( name, Float.class, null );
                decoder = new Decoder() {
                    public Object decode( String value ) {
                        return new Float( Float.parseFloat( value ) );
                    }
                };
            }
            else if ( maybeDouble_[ icol ] ) {
                colinfo = new ColumnInfo( name, Double.class, null );
                decoder = new Decoder() {
                    public Object decode( String value ) {
                        return new Double( Double.parseDouble( value ) );
                    }
                };
            }
            else {
                colinfo = new ColumnInfo( name, String.class, null );
                colinfo.setElementSize( stringLength_[ icol ] );
                clazz = String.class;
                decoder = new Decoder() {
                    public Object decode( String value ) {
                        return value;
                    }
                };
                if ( maybeDate_[ icol ] ) {
                    colinfo.setUnitString( "iso-8601" );
                    colinfo.setUCD( "TIME" );
                }
                else if ( maybeHms_[ icol ] ) {
                    colinfo.setUnitString( "hms" );
                }
                else if ( maybeDms_[ icol ] ) {
                    colinfo.setUnitString( "dms" );
                }
            }
            colInfos[ icol ] = colinfo;
            decoders[ icol ] = decoder;
        }
        return new Metadata( colInfos, decoders, nrow_ );
    }

    /**
     * Returns a new <tt>ncol</tt>-element boolean array.
     *
     * @param   val  initial value of all flags
     * @return  new flag array initialized to <tt>val</tt>
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
     *          value extracted from <tt>item</tt> - note it's always the
     *          same instance returned, so don't hang onto it
     * @throws  NumberFormatException  if <tt>item</tt> can't be understood
     *          as a float or double
     */
    private static ParsedFloat parseFloating( String item ) {

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
        return ParsedFloat.getInstance( sigFig, dvalue );
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
    public abstract class Decoder {
        public abstract Object decode( String value );
    }

    /**
     * Helper class to encapsulate the result of a floating point number
     * parse.
     */
    private static class ParsedFloat {

        /** Singleton instance. */
        static ParsedFloat instance = new ParsedFloat();

        /** Number of significant figures. */
        int sigFig;

        /** Value of the number. */
        double dValue;

        /**
         * Returns an instance with given values.  This is always the
         * same instance - cheap, and possible, because we happen to know
         * only one instance is ever considered at once.
         */
        static ParsedFloat getInstance( int sigFig, double dValue ) {
            instance.sigFig = sigFig;
            instance.dValue = dValue;
            return instance;
        }
    }
}
