package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * RowMatcher concrete subclass that uses sequential processing.
 *
 * @author   Mark Taylor
 * @since    13 Jan 2004
 */
public class SequentialRowMatcher extends RowMatcher {

    private final MatchEngine engine_;
    private final StarTable[] tables_;

    /**
     * Constructs a new matcher with match characteristics defined by
     * a given matching engine.
     *
     * @param  engine  matching engine
     * @param  tables  the array of tables on which matches are to be done
     */
    public SequentialRowMatcher( MatchEngine engine, StarTable[] tables ) {
        super( engine, tables );
        engine_ = engine;
        tables_ = tables;
    }

    LongBinner binRowIndices( int indexR, Predicate<Object[]> rowSelector )
            throws IOException, InterruptedException {
        ProgressIndicator indicator = getIndicator();
        ProgressRowSequence rseq =
            new ProgressRowSequence( tables_[ indexR ], indicator,
                                     "Binning rows for table "
                                     + ( indexR + 1 ) );
        LongBinner binner =
            Binners.createLongBinner( tables_[ indexR ].getRowCount() );
        long nrow = 0;
        long nref = 0;
        long nexclude = 0;
        try {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                Object[] row = rseq.getRow();
                if ( rowSelector.test( row ) ) {
                    Object[] keys = engine_.getBins( row );
                    int nkey = keys.length;
                    for ( int ikey = 0; ikey < nkey; ikey++ ) {
                        binner.addItem( keys[ ikey ], lrow );
                    }
                    nref += nkey;
                }
                else {
                    nexclude++;
                }
                nrow++;
            }
            assert nrow == tables_[ indexR ].getRowCount();
        }
        finally {
            rseq.close();
        }
        if ( nexclude > 0 ) {
            indicator.logMessage( nexclude + "/" + nrow + " rows excluded "
                                + "(out of match region)" );
        }
        long nbin = binner.getBinCount();
        indicator.logMessage( nref + " row refs for " + nrow + " rows in "
                            + nbin + " bins" );
        indicator.logMessage( "(average bin occupancy " +
                              ( (float) nref / (float) nbin ) + ")" );
        return binner;
    }

    LinkSet scanBinsForPairs( int indexR, int indexS,
                              Predicate<Object[]> rowSelector,
                              boolean bestOnly, LongBinner binnerR )
            throws IOException, InterruptedException {
        LinkSet linkSet = createLinkSet();
        ProgressRowSequence sseq =
            new ProgressRowSequence( tables_[ indexS ], getIndicator(),
                                     "Scanning rows for table "
                                   + ( indexS + 1 ) );
        try {
            for ( long isrow = 0; sseq.nextProgress(); isrow++ ) {
                Object[] srowData = sseq.getRow();
                if ( rowSelector.test( srowData ) ) {

                    /* Identify rows from table R which may match table S. */
                    Object[] keys = engine_.getBins( srowData );
                    int nkey = keys.length;
                    Set<Long> rrowSet = new HashSet<Long>();
                    for ( int ikey = 0; ikey < nkey; ikey++ ) {
                        long[] rrows = binnerR.getLongs( keys[ ikey ] );
                        if ( rrows != null ) {
                            for ( int ir = 0; ir < rrows.length; ir++ ) {
                                rrowSet.add( new Long( rrows[ ir ] ) );
                            }
                        }
                    }
                    long[] rrows = new long[ rrowSet.size() ];
                    int ir = 0;
                    for ( Long rr : rrowSet ) {
                        rrows[ ir++ ] = rr.longValue();
                    }
                    Arrays.sort( rrows );

                    /* Score and accumulate matched links. */
                    List<RowLink2> linkList = new ArrayList<RowLink2>( 1 );
                    double bestScore = Double.MAX_VALUE;
                    for ( ir = 0; ir < rrows.length; ir++ ) {
                        long irrow = rrows[ ir ];
                        Object[] rrowData = tables_[ indexR ].getRow( irrow );
                        double score = engine_.matchScore( srowData, rrowData );
                        if ( score >= 0 &&
                             ( ! bestOnly || score < bestScore ) ) {
                            RowRef rref = new RowRef( indexR, irrow );
                            RowRef sref = new RowRef( indexS, isrow );
                            RowLink2 pairLink = new RowLink2( rref, sref );
                            pairLink.setScore( score );
                            if ( bestOnly ) {
                                bestScore = score;
                                linkList.clear();
                            }
                            linkList.add( pairLink );
                            assert ( ! bestOnly ) || ( linkList.size() == 1 );
                        }
                    }

                    /* Add matched links to output set. */
                    for ( RowLink2 pairLink : linkList ) {
                        assert ! linkSet.containsLink( pairLink );
                        linkSet.addLink( pairLink );
                    }
                }
            }
        }
        finally {
            sseq.close();
        }
        return linkSet;
    }

    NdRange rangeColumns( int tIndex, boolean[] colFlags )
            throws IOException, InterruptedException {
        int ncol = colFlags.length;
        NdRange range = new NdRange( ncol );

        /* Go through each row finding the minimum and maximum value 
         * for each column (coordinate). */
        Comparable<?>[] mins = new Comparable<?>[ ncol ];
        Comparable<?>[] maxs = new Comparable<?>[ ncol ];
        ProgressRowSequence rseq =
            new ProgressRowSequence( tables_[ tIndex ], getIndicator(),
                                     "Assessing range of coordinates " +
                                     "from table " + ( tIndex + 1 ) );
        try {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( colFlags[ icol ] ) {
                        Object cell = row[ icol ];
                        if ( cell instanceof Comparable &&
                             ! Tables.isBlank( cell ) ) {
                            Comparable<?> val = (Comparable<?>) cell;
                            mins[ icol ] =
                                NdRange.min( mins[ icol ], val, false );
                            maxs[ icol ] =
                                NdRange.max( maxs[ icol ], val, false );
                        }
                    }
                }
            }
        }

        /* It's possible, though not particularly likely, that a 
         * compare invocation can result in a ClassCastException 
         * (e.g. comparing an Integer to a Double).  Such ClassCastExceptions
         * should get caught higher up, but we need to make sure the
         * row sequence is closed or the logging will get in a twist. */
        finally {
            rseq.close();
        }
        return new NdRange( mins, maxs );
    }
}
