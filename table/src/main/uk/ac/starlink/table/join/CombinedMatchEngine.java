package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * @author   Mark Taylor (Starlink)
 */
public class CombinedMatchEngine implements MatchEngine {

    private final MatchEngine[] engines;
    private final int[] tupleSizes;
    private final int[] tupleStarts;
    private final int nPart;
    private String name;

    // Some work arrays for holding subtuples - benchmarking shows that
    // there actually is a bottleneck if you create new empty arrays
    // every time you need one.
    private final Object[][] work0;
    private final Object[][] work1;
    private final Object[][] work2;

    /**
     * Constructs a new MatchEngine based on a sequence of others.
     * The tuples accepted by this engine are composed of the tuples
     * of its constituent engines (as specified by <tt>engines</tt>)
     * concatenated in sequence.
     *
     * @param   engines  match engine sequence to be combined
     */
    public CombinedMatchEngine( MatchEngine[] engines ) {
        this.engines = engines;
        nPart = engines.length;
        tupleSizes = new int[ nPart ];
        for ( int i = 0; i < nPart; i++ ) {
            tupleSizes[ i ] = engines[ i ].getTupleInfos().length;
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
        double totalScore = 0.0;
        for ( int i = 0; i < nPart; i++ ) {
            Object[] subTuple1 = work1[ i ];
            Object[] subTuple2 = work2[ i ];
            System.arraycopy( tuple1, tupleStarts[ i ], 
                              subTuple1, 0, tupleSizes[ i ] );
            System.arraycopy( tuple2, tupleStarts[ i ],
                              subTuple2, 0, tupleSizes[ i ] );
            double score = engines[ i ].matchScore( subTuple1, subTuple2 );
            if ( score < 0 ) {
                return -1.;
            }
            totalScore += score;
        }
        return totalScore;
    }

    /**
     * The match score is got by adding together the scores from all
     * the constituent matchers.  Since the metrics they employ will
     * all be different, the resulting value is probabl meaningless.
     * Therefore we decline to tag it with a meaning.
     *
     * @return  null
     */
    public ValueInfo getMatchScoreInfo() {
        return null;
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
        List params = new ArrayList();
        for ( int i = 0; i < nPart; i++ ) {
            params.addAll( Arrays.asList( engines[ i ].getMatchParameters() ) );
        }
        return (DescribedValue[]) params.toArray( new DescribedValue[ 0 ] );
    }

    public DescribedValue[] getTuningParameters() {
        List params = new ArrayList();
        for ( int i = 0; i < nPart; i++ ) {
            params.addAll( Arrays.asList( engines[ i ]
                                         .getTuningParameters() ) );
        }
        return (DescribedValue[]) params.toArray( new DescribedValue[ 0 ] );
    }

    public boolean canBoundMatch() {
        for ( int i = 0; i < nPart; i++ ) {
            if ( engines[ i ].canBoundMatch() ) {
                return true;
            }
        }
        return false;
    }

    public Comparable[][] getMatchBounds( Comparable[] min, Comparable[] max ) {
        min = (Comparable[]) min.clone();
        max = (Comparable[]) max.clone();
        for ( int i = 0; i < nPart; i++ ) {
            if ( engines[ i ].canBoundMatch() ) {
                int size = tupleSizes[ i ];
                int start = tupleStarts[ i ];
                Comparable[] subMin = new Comparable[ size ];
                Comparable[] subMax = new Comparable[ size ];
                System.arraycopy( min, start, subMin, 0, size );
                System.arraycopy( max, start, subMax, 0, size );
                Comparable[][] subBounds = engines[ i ]
                                          .getMatchBounds( subMin, subMax );
                subMin = subBounds[ 0 ];
                subMax = subBounds[ 1 ];
                System.arraycopy( subMin, 0, min, start, size );
                System.arraycopy( subMax, 0, max, start, size );
            }
        }
        return new Comparable[][] { min, max };
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
