package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * A matching engine which provides matching facilities by combining the
 * characteristics of a number of other matching engines.
 * Because of the way it calculates bins (effectively multiplying one
 * bin array by another), it is a good idea for efficiency's sake to
 * keep down the number of bins returned by the {@link MatchKit#getBins}
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

    private final boolean inSphere_;
    private final MatchEngine[] engines_;
    private final int[] tupleSizes_;
    private final int[] tupleStarts_;
    private final int nPart_;
    private String name_;

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
        inSphere_ = false;
        engines_ = engines;
        nPart_ = engines.length;
        tupleSizes_ = new int[ nPart_ ];
        tupleStarts_ = new int[ nPart_ ];
        int ts = 0;
        for ( int i = 0; i < nPart_; i++ ) {
            tupleStarts_[ i ] = ts;
            tupleSizes_[ i ] = engines_[ i ].getTupleInfos().length;
            ts += tupleSizes_[ i ];
        }

        /* Set the name. */
        StringBuffer buf = new StringBuffer( "(" );
        for ( int i = 0; i < nPart_; i++ ) {
            if ( i > 0 ) {
                buf.append( ", " );
            }
            buf.append( engines_[ i ].toString() );
        }
        buf.append( ")" );
        name_ = buf.toString();
    }

    public Supplier<MatchKit> createMatchKitFactory() {
        final boolean inSphere = inSphere_;
        final int[] tupleStarts = tupleStarts_.clone();
        final int[] tupleSizes = tupleSizes_.clone();
        final List<Supplier<MatchKit>> kitFacts = new ArrayList<>( nPart_ );
        final double[] scales = new double[ nPart_ ];
        for ( int i = 0; i < nPart_; i++ ) {
            kitFacts.add( engines_[ i ].createMatchKitFactory() );
            scales[ i ] = engines_[ i ].getScoreScale();
        }
        return () -> {
            MatchKit[] subKits = new MatchKit[ nPart_ ];
            for ( int i = 0; i < nPart_; i++ ) {
                subKits[ i ] = kitFacts.get( i ).get();
            }
            return new CombinedMatchKit( subKits, scales, inSphere,
                                         tupleStarts, tupleSizes );
        };
    }

    public Supplier<Coverage> createCoverageFactory() {
        final boolean inSphere = inSphere_;
        final int[] tupleStarts = tupleStarts_.clone();
        final int[] tupleSizes = tupleSizes_.clone();
        final List<Supplier<Coverage>> subFacts = new ArrayList<>( nPart_ );
        for ( int i = 0; i < nPart_; i++ ) {
            subFacts.add( engines_[ i ].createCoverageFactory() );
        }
        return () -> {
            Coverage[] subCovs = new Coverage[ nPart_ ];
            for ( int i = 0; i < nPart_; i++ ) {
                subCovs[ i ] = subFacts.get( i ).get();
            }
            return new CombinedCoverage( subCovs, tupleStarts, tupleSizes );
        };
    }

    /**
     * Returns the square root of the number of constituent matchers
     * if they all have definite score scaling values.
     * Otherwise, returns NaN.
     */
    public double getScoreScale() {
        for ( int i = 0; i < nPart_; i++ ) {
            if ( ! ( engines_[ i ].getScoreScale() > 0 ) ) {
                return Double.NaN;
            }
        }
        return Math.sqrt( nPart_ );
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public ValueInfo[] getTupleInfos() {
        int nargs = tupleStarts_[ nPart_ - 1 ] + tupleSizes_[ nPart_ - 1 ];
        ValueInfo[] infos = new ValueInfo[ nargs ];
        for ( int i = 0; i < nPart_; i++ ) {
            System.arraycopy( engines_[ i ].getTupleInfos(), 0, 
                              infos, tupleStarts_[ i ], tupleSizes_[ i ] );
        }
        return infos;
    }

    public DescribedValue[] getMatchParameters() {
        List<DescribedValue> params = new ArrayList<DescribedValue>();
        for ( int i = 0; i < nPart_; i++ ) {
            params.addAll( Arrays.asList( engines_[ i ].getMatchParameters() ));
        }
        return params.toArray( new DescribedValue[ 0 ] );
    }

    public DescribedValue[] getTuningParameters() {
        List<DescribedValue> params = new ArrayList<DescribedValue>();
        for ( int i = 0; i < nPart_; i++ ) {
            params.addAll( Arrays.asList( engines_[ i ]
                                         .getTuningParameters() ) );
        }
        return params.toArray( new DescribedValue[ 0 ] );
    }

    public void setName( String name ) {
        name_ = name;
    }

    public String toString() {
        return name_;
    }

    /**
     * MatchKit implementation for use with this class.
     */
    private static class CombinedMatchKit implements MatchKit {

        final MatchKit[] subKits_;
        final double[] scales_;
        final boolean inSphere_;
        final int[] tupleStarts_;
        final int[] tupleSizes_;
        final int nPart_;
        final Object[][] work0_;
        final Object[][] work1_;
        final Object[][] work2_;

        /**
         * Constructor.
         *
         * @param  subKits  nPart-element array of match kits for
         *                  component MatchEngines
         * @param  scales  nPart-element arra of score scales
         * @param  inSphere   require matches to be within unit sphere
         * @param  tupleStarts  nPart-element array of sub-engine start index
         *                      in combined tuple
         * @param  tupleSizes   nPart-element array of sub-engine element
         *                      counts in combined tuple
         */
        CombinedMatchKit( MatchKit[] subKits, double[] scales, boolean inSphere,
                          int[] tupleStarts, int[] tupleSizes ) {
            subKits_ = subKits;
            scales_ = scales;
            inSphere_ = inSphere;
            tupleStarts_ = tupleStarts;
            tupleSizes_ = tupleSizes;
            nPart_ = subKits.length;
            work0_ = new Object[ nPart_ ][];
            work1_ = new Object[ nPart_ ][];
            work2_ = new Object[ nPart_ ][];
            for ( int i = 0; i < nPart_; i++ ) {
                int ts = tupleSizes[ i ];
                work0_[ i ] = new Object[ ts ];
                work1_[ i ] = new Object[ ts ];
                work2_[ i ] = new Object[ ts ];
            }
        }

        public double matchScore( Object[] tuple1, Object[] tuple2 ) {
            double sum2 = 0.0;
            for ( int i = 0; i < nPart_; i++ ) {
                Object[] subTuple1 = work1_[ i ];
                Object[] subTuple2 = work2_[ i ];
                int tStart = tupleStarts_[ i ];
                int tSize = tupleSizes_[ i ];
                System.arraycopy( tuple1, tStart, subTuple1, 0, tSize );
                System.arraycopy( tuple2, tStart, subTuple2, 0, tSize );
                double score = subKits_[ i ].matchScore( subTuple1, subTuple2 );
                if ( score < 0 ) {
                    return -1.;
                }
                double scale = scales_[ i ];
                double d1 = scale > 0 ? ( score / scale ) : score;    
                sum2 += d1 * d1;
            }
            double sum1 = Math.sqrt( sum2 );
            if ( inSphere_ ) {
                return sum1 <= 1 ? sum1 : -1.0;
            }
            else {
                return sum1;
            }
        }

        public Object[] getBins( Object[] tuple ) {

            /* Work out the bin set for each region of the tuple handled by a
             * different match engine. */
            Object[][] binBag = new Object[ nPart_ ][];
            for ( int i = 0; i < nPart_; i++ ) {
                Object[] subTuple = work0_[ i ];
                System.arraycopy( tuple, tupleStarts_[ i ], 
                                  subTuple, 0, tupleSizes_[ i ] );
                binBag[ i ] = subKits_[ i ].getBins( subTuple );
            }

            /* "Multiply" these bin sets together to provide a number of
             * possible bins in nPart-dimensional space. If you see what I mean.
             * Each bin object in the returned array is an nPart-element List 
             * containing one entry for each part.  The definition of the
             * List equals() and hashCode() methods make these suitable for
             * use as matching bins. */
            int nBin = 1;
            for ( int i = 0; i < nPart_; i++ ) {
                nBin *= binBag[ i ].length;
            }

            Object[] bins = new Object[ nBin ];
            int[] offset = new int[ nPart_ ];
            for ( int ibin = 0; ibin < nBin; ibin++ ) {
                List<Object> bin = new ArrayList<>( nPart_ );
                for ( int i = 0; i < nPart_; i++ ) {
                    bin.add( binBag[ i ][ offset[ i ] ] );
                }
                bins[ ibin ] = bin;

                /* Bump the n-dimensional offset to the next cell. */
                for ( int j = 0; j < nPart_; j++ ) {
                    if ( ++offset[ j ] < binBag[ j ].length ) {
                        break;
                    }
                    else {
                        offset[ j ] = 0;
                    }
                }
            }

            /* Sanity check. */
            for ( int i = 0; i < nPart_; i++ ) {
                assert offset[ i ] == 0;
            }
        
            /* Return the array of bins. */
            return bins;
        }
    }

    /**
     * Coverage implementation for use with this class.
     */
    private static class CombinedCoverage implements Coverage {

        private final Coverage[] subCoverages_;
        private final int[] tupleStarts_;
        private final int[] tupleSizes_;
        private final int nPart_;
        private final Object[][] work_;

        /**
         * Constructor.
         *
         * @param  subCoverages  nPart-element array of coverages for
         *                       component MatchEngines
         * @param  tupleStarts  nPart-element array of sub-engine start index
         *                      in combined tuple
         * @param  tupleSizes   nPart-element array of sub-engine element
         *                      counts in combined tuple
         */
        public CombinedCoverage( Coverage[] subCoverages, 
                                 int[] tupleStarts, int[] tupleSizes ) {
            subCoverages_ = subCoverages;
            tupleStarts_ = tupleStarts;
            tupleSizes_ = tupleSizes;
            nPart_ = subCoverages.length;
            work_ = createWorkspace( tupleSizes );
        }

        public boolean isEmpty() {
            for ( Coverage cov : subCoverages_ ) {
                if ( cov.isEmpty() ) {
                    return true;
                }
            }
            return false;
        }

        public void extend( Object[] tuple ) {
            for ( int i = 0; i < nPart_; i++ ) {
                Object[] subTuple = work_[ i ];
                int tStart = tupleStarts_[ i ];
                int tSize = tupleSizes_[ i ];
                System.arraycopy( tuple, tStart, subTuple, 0, tSize );
                subCoverages_[ i ].extend( subTuple );
            }
        }

        public Supplier<Predicate<Object[]>> createTestFactory() {
            final int[] tupleStarts = tupleStarts_.clone();
            final int[] tupleSizes = tupleSizes_.clone();
            final List<Supplier<Predicate<Object[]>>> subTestFacts =
                new ArrayList<>();
            for ( int i = 0; i < nPart_; i++ ) {
                subTestFacts.add( subCoverages_[ i ].createTestFactory() );
            }
            return () -> {
                final Object[][] work = createWorkspace( tupleSizes );
                final List<Predicate<Object[]>> subTests = new ArrayList<>();
                for ( int i = 0; i < nPart_; i++ ) {
                    subTests.add( subTestFacts.get( i ).get() );
                }
                return tuple -> {
                    for ( int i = 0; i < nPart_; i++ ) {
                        Object[] subTuple = work[ i ];
                        int tStart = tupleStarts[ i ];
                        int tSize = tupleSizes[ i ];
                        System.arraycopy( tuple, tStart, subTuple, 0, tSize );
                        if ( ! subTests.get( i ).test( subTuple ) ) {
                            return false;
                        }
                    }
                    return true;
                };
            };
        }

        public void intersection( Coverage other ) {
            Coverage[] otherSubcovs = ((CombinedCoverage) other).subCoverages_;
            for ( int i = 0; i < nPart_; i++ ) {
                subCoverages_[ i ].intersection( otherSubcovs[ i ] );
            }
        }

        public void union( Coverage other ) {
            Coverage[] otherSubcovs = ((CombinedCoverage) other).subCoverages_;
            for ( int i = 0; i < nPart_; i++ ) {
                subCoverages_[ i ].union( otherSubcovs[ i ] );
            }
        }

        public String coverageText() {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "(" );
            for ( int i = 0; i < nPart_; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( subCoverages_[ i ].coverageText() );
            }
            sbuf.append( ")" );
            return sbuf.toString();
        }

        /**
         * Creates a new workspace array with a sub-array for each sub-tuple.
         *
         * @param  tupleSizes  array of tuple sizes
         * @return  array of arrays, each the right size for a sub-tuple
         */
        private static Object[][] createWorkspace( int[] tupleSizes ) {
            int nPart = tupleSizes.length;
            Object[][] work = new Object[ nPart ][];
            for ( int i = 0; i < nPart; i++ ) {
                work[ i ] = new Object[ tupleSizes[ i ] ];
            }
            return work;
        }
    }
}
