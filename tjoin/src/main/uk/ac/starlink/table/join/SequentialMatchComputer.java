package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import uk.ac.starlink.table.StarTable;

/**
 * Sequential MatchComputer implementation.
 *
 * @author   Mark Taylor
 * @since    25 Aug 2021
 */
class SequentialMatchComputer implements MatchComputer {

    public String getDescription() {
        return "Sequential";
    }

    public BinnedRows binRowIndices( Supplier<MatchKit> kitFact,
                                     Supplier<Predicate<Object[]>> rowSelector,
                                     StarTable tableR,
                                     ProgressIndicator indicator,
                                     String stageTxt )
            throws IOException, InterruptedException {
        final LongBinner binner =
            Binners.createLongBinner( tableR.getRowCount() );
        long nrow = 0;
        long nref = 0;
        long nexclude = 0;
        MatchKit matchKit = kitFact.get();
        Predicate<Object[]> inclusion = rowSelector.get();
        try ( ProgressRowSequence rseq =
                  new ProgressRowSequence( tableR, indicator, stageTxt ) ) {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                Object[] row = rseq.getRow();
                if ( inclusion.test( row ) ) {
                    Object[] keys = matchKit.getBins( row );
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
            assert nrow == tableR.getRowCount();
        }
        final long nref0 = nref;
        final long nexclude0 = nexclude;
        return new BinnedRows() {
            public LongBinner getLongBinner() {
                return binner;
            }
            public long getNref() {
                return nref0;
            }
            public long getNexclude() {
                return nexclude0;
            }
        };
    }

    public long binRowRefs( Supplier<MatchKit> kitFact,
                            Supplier<Predicate<Object[]>> rowSelector,
                            StarTable table, int tIndex,
                            ObjectBinner<Object,RowRef> binner, boolean newBins,
                            ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException {
        long nrow = 0;
        long ninclude = 0;
        MatchKit matchKit = kitFact.get();
        Predicate<Object[]> inclusion = rowSelector.get();
        try ( ProgressRowSequence rseq =
                  new ProgressRowSequence( table, indicator, stageTxt ) ) {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                Object[] row = rseq.getRow();
                if ( inclusion.test( row ) ) {
                    ninclude++;
                    Object[] keys = matchKit.getBins( row );
                    if ( keys.length > 0 ) {
                        RowRef rref = new RowRef( tIndex, lrow );
                        for ( Object key : keys ) {
                            if ( newBins || binner.containsKey( key ) ) {
                                binner.addItem( key, rref );
                            }
                        }
                    }
                }
                nrow++;
            }
        }
        assert nrow == table.getRowCount() || table.getRowCount() < 0;
        return ninclude;
    }

    public LinkSet scanBinsForPairs( Supplier<MatchKit> kitFact,
                                     Supplier<Predicate<Object[]>> rowSelector,
                                     StarTable tableR, int indexR,
                                     StarTable tableS, int indexS,
                                     boolean bestOnly, LongBinner binnerR,
                                     Supplier<LinkSet> linksetCreator,
                                     ProgressIndicator indicator,
                                     String stageTxt )
            throws IOException, InterruptedException {
        LinkSet linkSet = linksetCreator.get();
        MatchKit matchKit = kitFact.get();
        Predicate<Object[]> inclusion = rowSelector.get();
        try ( ProgressRowSequence sseq =
                  new ProgressRowSequence( tableS, indicator, stageTxt ) ) {
            List<RowLink2> linkList = new ArrayList<>();
            Set<Long> rrowSet = new HashSet<>();
            for ( long isrow = 0; sseq.nextProgress(); isrow++ ) {
                Object[] srowData = sseq.getRow();
                if ( inclusion.test( srowData ) ) {

                    /* Identify rows from table R which may match table S. */
                    Object[] keys = matchKit.getBins( srowData );
                    int nkey = keys.length;
                    rrowSet.clear();
                    for ( int ikey = 0; ikey < nkey; ikey++ ) {
                        long[] rrows = binnerR.getLongs( keys[ ikey ] );
                        if ( rrows != null ) {
                            for ( int ir = 0; ir < rrows.length; ir++ ) {
                                rrowSet.add( Long.valueOf( rrows[ ir ] ) );
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
                    linkList.clear();
                    double bestScore = Double.MAX_VALUE;
                    for ( ir = 0; ir < rrows.length; ir++ ) {
                        long irrow = rrows[ ir ];
                        Object[] rrowData = tableR.getRow( irrow );
                        double score =
                            matchKit.matchScore( srowData, rrowData );
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
        return linkSet;
    }

    public Coverage readCoverage( Supplier<Coverage> covFact, StarTable table,
                                  ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException {
        Coverage cov = covFact.get();
        try ( ProgressRowSequence rseq =
                  new ProgressRowSequence( table, indicator, stageTxt ) ) {
            while ( rseq.nextProgress() ) {
                cov.extend( rseq.getRow() );
            }
        }
        return cov;
    }

    public long countRows( StarTable table,
                           Supplier<Predicate<Object[]>> rowSelector,
                           ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException {
        Predicate<Object[]> inclusion = rowSelector.get();
        long nInclude = 0;
        try ( ProgressRowSequence rseq =
                  new ProgressRowSequence( table, indicator, stageTxt ) ) {
            while ( rseq.nextProgress() ) {
                if ( inclusion.test( rseq.getRow() ) ) {
                    nInclude++;
                }
            }
        }
        return nInclude;
    }
}
