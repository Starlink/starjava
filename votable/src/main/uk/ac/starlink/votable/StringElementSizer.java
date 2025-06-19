package uk.ac.starlink.votable;

import java.io.IOException;
import java.util.Arrays;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.IOConsumer;

/**
 * Object which is able to provide element sizes (string lengths)
 * for table columns containing String[] arrays.
 *
 * <p>This functionality is required when writing VOTable output;
 * the FIELD headers need to have the common length of the strings,
 * since VOTable output may contain variable-length arrays of fixed-length
 * strings, but not fixed-length arrays of variable-length strings.
 *
 * <p>A number of implementations are provided.
 * The basic approaches are to read all the elements and find the maximum
 * string length, which in general requires an extra pass through the data,
 * or just provide some sort of guess.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2025
 */
public abstract class StringElementSizer {

    /** The existing string length values, which may not be legal, are copied.*/
    public static final StringElementSizer NOCALC = createNocalcSizer();
   
    /** All elements are read and the maximum length used. */
    public static final StringElementSizer READ = createSampleReadSizer( -1 );

    /** The first few elements are read and the maximum length used. */
    public static final StringElementSizer SAMPLE =
        createSampleReadSizer( 1000 );

    /** A fixed element length of 2 is used. */
    public static final StringElementSizer FIXED2 = createFixedSizer( 2 );

    /** If called upon to provide length values, an error will be raised. */
    public static final StringElementSizer ERROR_IF_USED =
        createNoUseSizer( "errorIfUsed",
                          msg -> { throw new IOException( msg ); } );

    /** If called upon to provide length values, a false assertion is made. */
    public static final StringElementSizer ASSERT_UNUSED =
        createNoUseSizer( "assertUnused",
                          msg -> { assert false : msg; } );

    /**
     * Fills in element length values for string array-valued columns
     * in a supplied table.
     *
     * <p>The return value is an array the same size as the input
     * <code>icols</code> array whose elements are the calculated
     * string lengths ({@link uk.ac.starlink.table.ValueInfo#getElementSize})
     * for the corresponding column indices.
     *
     * @param  table  input table
     * @param  icols  index array of String[]-valued columns whose
     *                element lengths need to be determined
     */
    public abstract int[] calculateStringArrayElementSizes( StarTable table,
                                                            int[] icols )
            throws IOException;

    /**
     * Creates an instance that just copies string lengths from the input
     * (which likely includes negative=unknown values).
     *
     * @return  new sizer
     */
    private static StringElementSizer createNocalcSizer() {
        return new StringElementSizer() {
            public int[] calculateStringArrayElementSizes( StarTable table,
                                                           int[] icols ) {
                int[] elSizes = new int[ icols.length ];
                for ( int i = 0; i < icols.length; i++ ) {
                    elSizes[ i ] =
                        table.getColumnInfo( icols[ i ] ).getElementSize();
                }
                return elSizes;
            }
            @Override
            public String toString() {
                return "noCalc";
            }
        };
    }

    /**
     * Creates an instance that reports a fixed length for all strings.
     *
     * @param   elSize  fixed string length
     * @return  new sizer
     */
    public static StringElementSizer createFixedSizer( int elSize ) {
        return new StringElementSizer() {
            public int[] calculateStringArrayElementSizes( StarTable table,
                                                           int[] icols ) {
                int[] elSizes = new int[ icols.length ];
                Arrays.fill( elSizes, elSize );
                return elSizes;
            }
            @Override
            public String toString() {
                return "fixed" + elSize;
            }
        };
    }

    /**
     * Creates an instance which is not expecting to be called upon to
     * calculate sting lengths.
     *
     * @param  name  short name
     * @param  usedReport  callback that will be invoked with a supplied
     *                     error message if the
     *                     calculateStringArrayElementSizes method
     *                     is actually called upon to make evaluations
     */
    private static StringElementSizer
            createNoUseSizer( String name, IOConsumer<String> usedReport ) {
        return new StringElementSizer() {
            public int[] calculateStringArrayElementSizes( StarTable table,
                                                           int[] icols )
                    throws IOException {
                if ( icols.length > 0 ) {
                    usedReport.accept( "Unsized String array columns detected");
                }
                return new int[ icols.length ];
            }
            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Creates an instance which reads some or all of the data to determine
     * the maximum string length.
     *
     * @param  maxRow  maximum number of rows to read;
     *                 if not positive, all rows are read
     * @return   new instance
     */
    public static StringElementSizer createSampleReadSizer( int maxRow ) {
        final long laxRow;
        final String name;
        if ( maxRow > 0 ) {
            laxRow = maxRow;
            name = "limitedRead" + maxRow;
        }
        else {
            laxRow = Long.MAX_VALUE;
            name = "read";
        }
        return new StringElementSizer() {
            public int[] calculateStringArrayElementSizes( StarTable table,
                                                           int[] icols )
                    throws IOException {
                int nc = icols.length;
                int[] elSizes = new int[ nc ];
                try ( RowSequence rseq = table.getRowSequence() ) {
                    for ( int irow = 0; rseq.next() && irow < laxRow; irow++ ) {
                        for ( int i = 0; i < nc; i++ ) {
                            int icol = icols[ i ];
                            Object value = rseq.getCell( icol );
                            int siz = 0;
                            if ( value instanceof String[] ) {
                                for ( String s : (String[]) value ) {
                                    if ( s != null ) {
                                        siz = Math.max( siz, s.length() );
                                    }
                                }
                            }
                            if ( siz > elSizes[ i ] ) {
                                elSizes[ i ] = siz;
                            }
                        }
                    }
                }
                return elSizes;
            }
            @Override
            public String toString() {
                return name;
            }
        };
    }
}
