package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.List;

/**
 * A matching engine which provides matching facilities by combining the
 * characteristics of a number of other matching engines.
 * Because of the way it calculates bins (effectively multiplying one
 * bin array by another), it is a good idea for efficiency's sake to
 * keep down the number of bins returned by the {@link MatchEngine#getBins}
 * method of the component match engines.
 *
 * @author   Mark Taylor (Starlink)
 */
public class CombinedMatchEngine implements MatchEngine {

    private final MatchEngine[] engines;
    private final int[] tupleSizes;
    private final int[] tupleStarts;
    private final int nPart;

    // Some work arrays for holding subtuples - benchmarking shows that
    // there actually is a bottleneck if you create new empty arrays
    // every time you need one.
    private final Object[][] work0;
    private final Object[][] work1;
    private final Object[][] work2;

    /**
     * Constructs a new MatchEngine based on a sequence of others.
     * <tt>engines</tt> and <tt>tupleSizes</tt> are arrays with the
     * same number of elements; each tuple submitted to this engine 
     * should be <i>x</i> element long, where <i>x</i> is the sum of
     * the elements of <tt>tupleSizes</tt>; <tt>engine[0]</tt> should
     * accept tuples formed of the first <tt>tupleSizes[0]</tt> elements
     * of the tuples submitted to this object and so on.
     *
     * @param   engines  match engine sequence to be combined
     * @param   tupleSizes  sizes of tuples to be accepted by the corresponding
     *          element of <tt>engines</tt>
     * @throws  IllegalArgumentException  
     *          if <tt>engines.length!=tupleSizes.length</tt>
     */
    public CombinedMatchEngine( MatchEngine[] engines, int[] tupleSizes ) {
        this.engines = engines;
        this.tupleSizes = tupleSizes;
        nPart = engines.length;
        if ( engines.length != tupleSizes.length ) {
            throw new IllegalArgumentException(
                "Mismatched lengths of engines and tuple sizes" );
        }
        tupleStarts = new int[ nPart ];
        int ts = 0;
        work0 = new Object[ nPart ][];
        work1 = new Object[ nPart ][];
        work2 = new Object[ nPart ][];
        for ( int i = 0; i < nPart; i++ ) {
            tupleStarts[ i ] = ts;
            ts += tupleSizes[ i ];
            work0[ i ] = new Object[ tupleSizes[ i ] ];
            work1[ i ] = new Object[ tupleSizes[ i ] ];
            work2[ i ] = new Object[ tupleSizes[ i ] ];
        }
    }

    public boolean matches( Object[] tuple1, Object[] tuple2 ) {
        for ( int i = 0; i < nPart; i++ ) {
            Object[] subTuple1 = work1[ i ];
            Object[] subTuple2 = work2[ i ];
            System.arraycopy( tuple1, tupleStarts[ i ], 
                              subTuple1, 0, tupleSizes[ i ] );
            System.arraycopy( tuple2, tupleStarts[ i ],
                              subTuple2, 0, tupleSizes[ i ] );
            if ( ! engines[ i ].matches( subTuple1, subTuple2 ) ) {
                return false;
            }
        }
        return true;
    }

    public Object[] getBins( Object[] tuple ) {

        /* Work out the bin set for each region of the tuple handled by a
         * different match engine. */
        Object[][] binBag = new Object[ nPart ][];
        for ( int i = 0; i < nPart; i++ ) {
            Object[] subTuple = work0[ i ];
            System.arraycopy( tuple, tupleStarts[ i ], 
                              subTuple, 0, tupleSizes[ i ] );
            binBag[ i ] = engines[ i ].getBins( subTuple );
        }

        /* "Multiply" these bin sets together to provide a number of possible
         * bins in nPart-dimensional space.  If you see what I mean.
         * Each bin object in the returned array is an nPart-element List 
         * containing one entry for each part.  The definition of the
         * List equals() and hashCode() methods make these suitable for
         * use as matching bins. */
        int nBin = 1;
        for ( int i = 0; i < nPart; i++ ) {
            nBin *= binBag[ i ].length;
        }

        Object[] bins = new Object[ nBin ];
        int[] offset = new int[ nPart ];
        for ( int ibin = 0; ibin < nBin; ibin++ ) {
            List bin = new ArrayList( nPart );
            for ( int i = 0; i < nPart; i++ ) {
                bin.add( binBag[ i ][ offset[ i ] ] );
            }
            bins[ ibin ] = bin;

            /* Bump the n-dimensional offset to the next cell. */
            for ( int j = 0; j < nPart; j++ ) {
                if ( ++offset[ j ] < binBag[ j ].length ) {
                    break;
                }
                else {
                    offset[ j ] = 0;
                }
            }
        }

        /* Sanity check. */
        for ( int i = 0; i < nPart; i++ ) {
            assert offset[ i ] == 0;
        }
        
        /* Return the array of bins. */
        return bins;
    }
}
