package uk.ac.starlink.table.formats;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ReaderRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.DataSource;

/**
 * Abstract superclass for tables which reads a stream of characters to
 * obtain the row data and metadata.
 * Since metadata is typically scarce in such tables, the strategy is
 * to make one pass through the data attempting to work out 
 * column types etc at table initialisation time, and to make 
 * a further pass through for each required RowSequence, using the
 * metadata obtained earlier.
 * This superclass contains a rough framework for such behaviour and
 * a number of useful protected classes and methods which may be used
 * to achieve it.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Sep 2004
 */
public abstract class StreamStarTable extends AbstractStarTable {

    private final DataSource datsrc_;
    private final int ncol_;
    private final long nrow_;
    private final Decoder[] decoders_;
    private final ColumnInfo[] colInfos_;

    /** Char representation of -1 (as returned end-of-stream read) */
    protected final static char END = (char) -1;

    /**
     * Constructor.
     *
     * @param  datsrc  data source from which the stream can be obtained
     */
    protected StreamStarTable( DataSource datsrc )
            throws TableFormatException, IOException {
        datsrc_ = datsrc;

        /* Work out the table metadata, probably by reading through
         * the rows once. */
        Metadata meta = obtainMetadata();
        decoders_ = meta.decoders_;
        colInfos_ = meta.colInfos_;
        nrow_ = meta.nrow_;
        ncol_ = meta.ncol_;

        /* Configure some table characteristics from the data source. */
        setName( datsrc_.getName() );
        setURL( datsrc.getURL() );
    }

    public int getColumnCount() {
        return ncol_;
    }

    public long getRowCount() {
        return nrow_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public RowSequence getRowSequence() throws IOException {
        final PushbackInputStream in = getInputStream();
        final int ncol = getColumnCount();
        return new ReaderRowSequence() {
            protected Object[] readRow() throws IOException {
                List cellList = StreamStarTable.this.readRow( in );
                if ( cellList == null ) {
                    in.close();
                    return null;
                }
                else {
                    Object[] row = new Object[ ncol ];
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        String sval = (String) cellList.get( icol );
                        if ( sval != null && sval.length() > 0 ) {
                            row[ icol ] = decoders_[ icol ].decode( sval );
                        }
                    }
                    return row;
                }
            }
            public void close() throws IOException {
                in.close();
            }
        };
    }

    /**
     * Convenience method which returns a buffered pushback stream based
     * on this table's data source.
     *
     * @return  input stream containing source data
     */
    protected PushbackInputStream getInputStream() throws IOException {
        return new PushbackInputStream( 
                   new BufferedInputStream( datsrc_.getInputStream() ) );
    }

    /**
     * Obtains column metadata for this table, probably by reading through
     * the rows once and using a RowEvaluator.  Note, this method is
     * called in the StreamStarTable constructor.
     *
     * @return   information about the table represented by the character
     *           stream
     * @throws   TableFormatException  if the data doesn't represent this
     *           kind of table
     * @throws   IOException   if I/O error is encountered
     */
    protected abstract Metadata obtainMetadata()
            throws TableFormatException, IOException;

    /**
     * Reads the next row of data from a given stream.
     * Ignorable rows are skipped; comments may be stashed away.
     *
     * @param  in  input stream
     * @return  list of Strings one for each cell in the row, or
     *          <tt>null</tt> for end of stream
     * @throws   TableFormatException  if the data doesn't represent this
     *           kind of table
     * @throws   IOException   if I/O error is encountered
     */
    protected abstract List readRow( PushbackInputStream in )
            throws TableFormatException, IOException;

    /**
     * Helper class used to group quantities which describe what the 
     * data types found in the columns of a table are.
     */
    protected static class Metadata {
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
    protected static abstract class Decoder {
        abstract Object decode( String value );
    }

    /**
     * Helper class to examine unknown rows (arrays of strings) and
     * work out what they contain.  By repeatedly calling <tt>submitRow</tt>
     * the evaluator can refine its idea of what kind of data is represented
     * by each column.
     */
    protected static class RowEvaluator {

        private boolean[] maybeBoolean_;
        private boolean[] maybeShort_;
        private boolean[] maybeInteger_;
        private boolean[] maybeLong_;
        private boolean[] maybeFloat_;
        private boolean[] maybeDouble_;
        private int[] stringLength_;
        private long nrow_;
        private int ncol_;

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
            if ( nrow_++ == 0 ) {
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
                int leng = cell.length(); 
                if ( cell == null || leng == 0 ) {
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
            }
        }

        /**
         * Returns information gleaned from previous <tt>submitRow</tt>
         * calls about the kind of data that appears to be in the columns.
         *
         * @return  metadata
         */
        protected Metadata getMetadata() {
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
                        Object decode( String value ) {
                            char v1 = value.charAt( 0 );
                            return ( v1 == 't' || v1 == 'T' ) ? Boolean.TRUE
                                                              : Boolean.FALSE;
                        }
                    };
                }
                else if ( maybeShort_[ icol ] ) {
                    colinfo = new ColumnInfo( name, Short.class, null );
                    decoder = new Decoder() {
                        Object decode( String value ) {
                            return new Short( Short.parseShort( value ) );
                        } 
                    };
                }
                else if ( maybeInteger_[ icol ] ) {
                    colinfo = new ColumnInfo( name, Integer.class, null );
                    decoder = new Decoder() {
                        Object decode( String value ) {
                            return new Integer( Integer.parseInt( value ) );
                        }
                    };
                }
                else if ( maybeLong_[ icol ] ) {
                    colinfo = new ColumnInfo( name, Long.class, null );
                    decoder = new Decoder() {
                        Object decode( String value ) {
                            return new Long( Long.parseLong( value ) );
                        }
                    };
                }
                else if ( maybeFloat_[ icol ] ) {
                    colinfo = new ColumnInfo( name, Float.class, null );
                    decoder = new Decoder() {
                        Object decode( String value ) {
                            return new Float( Float.parseFloat( value ) );
                        }
                    };
                }
                else if ( maybeDouble_[ icol ] ) {
                    colinfo = new ColumnInfo( name, Double.class, null );
                    decoder = new Decoder() {
                        Object decode( String value ) {
                            return new Double( Double.parseDouble( value ) );
                        }
                    };
                }
                else {
                    colinfo = new ColumnInfo( name, String.class, null );
                    colinfo.setElementSize( stringLength_[ icol ] );
                    clazz = String.class;
                    decoder = new Decoder() {
                        Object decode( String value ) {
                            return value;
                        }
                    };
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
