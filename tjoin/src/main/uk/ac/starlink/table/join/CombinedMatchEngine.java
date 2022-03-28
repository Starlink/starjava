package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * A matching engine which provides matching facilities by combining the
 * characteristics of a number of other matching engines.
 * Because of the way it calculates bins (effectively multiplying one
 * bin array by another), it is a good idea for efficiency's sake to
 * keep down the number of bins returned by the {@link MatchEngine#getBins}
 * method of the component match engines.
 *
 * <p>The match score is formed by taking the scaled match scores of the
 * constituent engines and adding them in quadrature
 * (if no scaling is available, unscaled values are used).
 * Versions of this class before 2017 did not do that, it just added
 * unscaled match scores together, which doesn't make much sense.
 *
 * @author   Mark Taylor (Starlink)
 */
public class CombinedMatchEngine implements MatchEngine {

    private final boolean inSphere;
    private final MatchEngine[] engines;
    private final int[] tupleSizes;
    private final int[] tupleStarts;
    private final int nPart;
    private String name;

    // ThreadLocal work arrays for holding subtuples - benchmarking shows that
    // there actually is a bottleneck if you create new empty arrays
    // every time you need one.
    private final ThreadLocal<CWork> workLocal_;

    private static final ValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Scaled distance between points "
                            + "in combined space" );

    /**
     * Constructs a new MatchEngine based on a sequence of others.
     * The tuples accepted by this engine are composed of the tuples
     * of its constituent engines (as specified by <tt>engines</tt>)
     * concatenated in sequence.
     *
     * @param   engines  match engine sequence to be combined
     */
    public CombinedMatchEngine( MatchEngine[] engines ) {
        this.inSphere = false;
        this.engines = engines;
        nPart = engines.length;
        tupleSizes = new int[ nPart ];
        tupleStarts = new int[ nPart ];
        int ts = 0;
        for ( int i = 0; i < nPart; i++ ) {
            tupleStarts[ i ] = ts;
            tupleSizes[ i ] = engines[ i ].getTupleInfos().length;
            ts += tupleSizes[ i ];
        }

        /* Set up workspace. */
        workLocal_ = ThreadLocal
                    .withInitial( () -> new CWork( nPart, tupleSizes ) );

        /* Set the name. */
        StringBuffer buf = new StringBuffer( "(" );
        for ( int i = 0; i < nPart; i++ ) {
            if ( i > 0 ) {
                buf.append( ", " );
            }
            buf.append( engines[ i ].toString() );
        }
        buf.append( ")" );
        name = buf.toString();
    }

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        double sum2 = 0.0;
        CWork cwork = workLocal_.get();
        for ( int i = 0; i < nPart; i++ ) {
            Object[] subTuple1 = cwork.work1_[ i ];
            Object[] subTuple2 = cwork.work2_[ i ];
            System.arraycopy( tuple1, tupleStarts[ i ], 
                              subTuple1, 0, tupleSizes[ i ] );
            System.arraycopy( tuple2, tupleStarts[ i ],
                              subTuple2, 0, tupleSizes[ i ] );
            MatchEngine engine = engines[ i ];
            double score = engine.matchScore( subTuple1, subTuple2 );
            if ( score < 0 ) {
                return -1.;
            }
            double scale = engine.getScoreScale();
            double d1 = scale > 0 ? ( score / scale ) : score;    
            sum2 += d1 * d1;
        }
        double sum1 = Math.sqrt( sum2 );
        if ( inSphere ) {
            return sum1 <= 1 ? sum1 : -1.0;
        }
        else {
            return sum1;
        }
    }

    /**
     * Returns the square root of the number of constituent matchers
     * if they all have definite score scaling values.
     * Otherwise, returns NaN.
     */
    public double getScoreScale() {
        for ( int i = 0; i < nPart; i++ ) {
            if ( ! ( engines[ i ].getScoreScale() > 0 ) ) {
                return Double.NaN;
            }
        }
        return Math.sqrt( nPart );
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public Object[] getBins( Object[] tuple ) {
        CWork cwork = workLocal_.get();

        /* Work out the bin set for each region of the tuple handled by a
         * different match engine. */
        Object[][] binBag = new Object[ nPart ][];
        for ( int i = 0; i < nPart; i++ ) {
            Object[] subTuple = cwork.work0_[ i ];
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
            List<Object> bin = new ArrayList<Object>( nPart );
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

    public ValueInfo[] getTupleInfos() {
        int nargs = tupleStarts[ nPart - 1 ] + tupleSizes[ nPart - 1 ];
        ValueInfo[] infos = new ValueInfo[ nargs ];
        for ( int i = 0; i < nPart; i++ ) {
            System.arraycopy( engines[ i ].getTupleInfos(), 0, 
                              infos, tupleStarts[ i ], tupleSizes[ i ] );
        }
        return infos;
    }

    public DescribedValue[] getMatchParameters() {
        List<DescribedValue> params = new ArrayList<DescribedValue>();
        for ( int i = 0; i < nPart; i++ ) {
            params.addAll( Arrays.asList( engines[ i ].getMatchParameters() ) );
        }
        return params.toArray( new DescribedValue[ 0 ] );
    }

    public DescribedValue[] getTuningParameters() {
        List<DescribedValue> params = new ArrayList<DescribedValue>();
        for ( int i = 0; i < nPart; i++ ) {
            params.addAll( Arrays.asList( engines[ i ]
                                         .getTuningParameters() ) );
        }
        return params.toArray( new DescribedValue[ 0 ] );
    }

    public boolean canBoundMatch() {
        for ( int i = 0; i < nPart; i++ ) {
            if ( engines[ i ].canBoundMatch() ) {
                return true;
            }
        }
        return false;
    }

    public NdRange getMatchBounds( NdRange[] inRanges, int index ) {
        int nr = inRanges.length;
        Comparable<?>[] outMins = inRanges[ index ].getMins().clone();
        Comparable<?>[] outMaxs = inRanges[ index ].getMaxs().clone();
        for ( int ip = 0; ip < nPart; ip++ ) {
            MatchEngine engine = engines[ ip ];
            if ( engine.canBoundMatch() ) {
                int size = tupleSizes[ ip ];
                int start = tupleStarts[ ip ];
                NdRange[] subInRanges = new NdRange[ nr ];
                for ( int ir = 0; ir < nr; ir++ ) {
                    Comparable<?>[] subInMins = new Comparable<?>[ size ];
                    Comparable<?>[] subInMaxs = new Comparable<?>[ size ];
                    System.arraycopy( inRanges[ ir ].getMins(), start,
                                      subInMins, 0, size );
                    System.arraycopy( inRanges[ ir ].getMaxs(), start,
                                      subInMaxs, 0, size );
                    subInRanges[ ir ] = new NdRange( subInMins, subInMaxs );
                }
                NdRange subOutRange =
                     engine.getMatchBounds( subInRanges, index );
                System.arraycopy( subOutRange.getMins(), 0,
                                  outMins, start, size );
                System.arraycopy( subOutRange.getMaxs(), 0,
                                  outMaxs, start, size );
            }
        }
        return new NdRange( outMins, outMaxs );
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    /**
     * Object for holding work arrays used during calculations.
     */
    private static class CWork {
        final Object[][] work0_;
        final Object[][] work1_;
        final Object[][] work2_;
        CWork( int nPart, int[] tupleSizes ) {
            work0_ = new Object[ nPart ][];
            work1_ = new Object[ nPart ][];
            work2_ = new Object[ nPart ][];
            for ( int i = 0; i < nPart; i++ ) {
                work0_[ i ] = new Object[ tupleSizes[ i ] ];
                work1_[ i ] = new Object[ tupleSizes[ i ] ];
                work2_[ i ] = new Object[ tupleSizes[ i ] ];
            }
        }
    }
}
