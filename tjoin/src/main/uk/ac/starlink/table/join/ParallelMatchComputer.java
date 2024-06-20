package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import uk.ac.starlink.table.ProgressRowSplittable;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowCollector;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Multithreaded MatchComputer implementation.
 *
 * @author   Mark Taylor
 * @since    25 Aug 2021
 */
class ParallelMatchComputer implements MatchComputer {

    private final RowRunner runner_;

    /**
     * Constructor.
     *
     * @param  runner  controls multithreaded processing
     */
    public ParallelMatchComputer( RowRunner runner ) {
        runner_ = runner;
    }

    public String getDescription() {
        return "Split, " + runner_.getSplitProcessor();
    }

    public BinnedRows binRowIndices( Supplier<MatchKit> kitFact,
                                     Supplier<Predicate<Object[]>> rowSelector,
                                     StarTable tableR,
                                     ProgressIndicator indicator,
                                     String stageTxt )
            throws IOException, InterruptedException {
        long nrowR = tableR.getRowCount();
        boolean isIntSizeR = nrowR >= 0 && nrowR < Integer.MAX_VALUE;
        BinCollector collector =
            new BinCollector( kitFact, isIntSizeR, rowSelector );
        return progressCollect( collector, tableR, indicator, stageTxt );
    }

    public long binRowRefs( Supplier<MatchKit> kitFact,
                            Supplier<Predicate<Object[]>> rowSelector,
                            StarTable table, int tIndex,
                            ObjectBinner<Object,RowRef> binner, boolean newBins,
                            ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException {
        Predicate<Object> canAddKey = newBins
                                    ? key -> true
                                    : key -> binner.containsKey( key );
        Supplier<ObjectBinner<Object,RowRef>> binnerFactory =
            Binners::createObjectBinner;
        RefCollector collector =
            new RefCollector( kitFact, rowSelector, tIndex, canAddKey,
                              binnerFactory );
        SplitBinnedRefs binned =
            progressCollect( collector, table, indicator, stageTxt );

        /* Note that this step is not parallelised.  Since the input binner
         * is modified, the collection result must be merged into it.
         * This could mean that the parallel implementation runs slower
         * than the sequential one. */
        binner.addContent( binned.binner_ );
        return binned.ninclude_;
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
        PairCollector collector =
            new PairCollector( kitFact, indexR, indexS, rowSelector,
                               bestOnly, tableR, binnerR, linksetCreator );
        return progressCollect( collector, tableS, indicator, stageTxt );
    }

    public Coverage readCoverage( Supplier<Coverage> covFact, StarTable table,
                                  ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException {
        RowCollector<Coverage> collector = new CoverageCollector( covFact );
        return progressCollect( collector, table, indicator, stageTxt );
    }

    public long countRows( StarTable table,
                           Supplier<Predicate<Object[]>> rowSelector,
                           ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException {
        RowCollector<long[]> collector = new CountCollector( rowSelector );
        return progressCollect( collector, table, indicator, stageTxt )[ 0 ];
    }

    /**
     * Invokes a supplied RowCollector with progress logging to this
     * matcher's ProgressIndicator.
     * The computational effect is equivalent to
     * <code>runner_.collect(collector,table)</code>.
     * 
     * @param  collector   row collector
     * @param  table    table on which to operate
     * @param  indicator   progress indicator
     * @param  msg    progress stage message
     * @return  result of collection 
     */
    public <A> A progressCollect( RowCollector<A> collector, StarTable table,
                                  ProgressIndicator indicator, String msg )
            throws IOException {
        long nrow = table.getRowCount();
        final double rowFactor = nrow > 0 ? 1.0 / nrow : 0.0;
        ProgressRowSplittable.Target progTarget =
                new ProgressRowSplittable.Target() {
            public void updateCount( long count ) throws IOException {
                try {
                    indicator.setLevel( count * rowFactor );
                }
                catch ( InterruptedException e ) {
                    throw new IOException( "Progress interrupted" );
                }
            }
            public void done( long count ) {
                indicator.endStage();
            }
        };
        RowRunner progRunner = new RowRunner( runner_.getSplitProcessor() ) {
            @Override
            public RowSplittable createRowSplittable( StarTable table )
                    throws IOException {
                RowSplittable baseSplit = runner_.createRowSplittable( table );
                indicator.startStage( msg );
                return new ProgressRowSplittable( baseSplit, progTarget );
            }
        };
        return progRunner.collect( collector, table );
    }

    /**
     * BinnedRows implementation for this computer.
     * It also features a combine method for use in collection.
     */
    private static class SplitBinnedRows implements BinnedRows {
        LongBinner binner_;
        long nrow_;
        long nref_;
        long nexclude_;
        SplitBinnedRows( LongBinner binner ) {
            binner_ = binner;
        }
        public LongBinner getLongBinner() {
            return binner_;
        }
        public long getNref() {
            return nref_;
        }
        public long getNexclude() {
            return nexclude_;
        }

        /**
         * Combines the contents of another binning result with this one.
         *
         * @param  other  other binning result
         * @return   combined binning result
         */
        SplitBinnedRows combine( SplitBinnedRows other ) {
            this.binner_ = this.binner_.combine( other.binner_ );
            this.nrow_ += other.nrow_;
            this.nref_ += other.nref_;
            this.nexclude_ += other.nexclude_;
            return this;
        }
    }

    /**
     * Aggregates an ObjectBinner and a count of included rows.
     */
    private static class SplitBinnedRefs {
        ObjectBinner<Object,RowRef> binner_;
        long ninclude_;
        SplitBinnedRefs( ObjectBinner<Object,RowRef> binner ) {
            binner_ = binner;
        }

        /**
         * Adds the content of another SplitBinnedRefs to this one.
         *
         * @param  other  other object
         */
        void addBinnedRefs( SplitBinnedRefs other ) {
            ninclude_ += other.ninclude_;
            binner_.addContent( other.binner_ );
        }
    }

    /**
     * RowCollector implementation for binning row indices by match key.
     */
    private static class BinCollector extends RowCollector<SplitBinnedRows> {

        private final Supplier<MatchKit> kitFact_;
        private final boolean isIntSize_;
        private final Supplier<Predicate<Object[]>> rowSelector_;

        /**
         * Constructor.
         *
         * @param  kitFact  defines matching criteria
         * @param  isIntSize  true if maximum row index known to be &lt;=2**31
         * @param  rowSelector  rows that fail this test are ignored
         */
        BinCollector( Supplier<MatchKit> kitFact, boolean isIntSize,
                      Supplier<Predicate<Object[]>> rowSelector ) {
            kitFact_ = kitFact;
            isIntSize_ = isIntSize;
            rowSelector_ = rowSelector;
        }
        public SplitBinnedRows createAccumulator() {
            return new SplitBinnedRows( Binners.createLongBinner( isIntSize_ ));
        }
        public SplitBinnedRows combine( SplitBinnedRows binned1,
                                        SplitBinnedRows binned2 ) {
            return binned1.combine( binned2 );
        }
        public void accumulateRows( RowSplittable rseq, SplitBinnedRows binned )
                throws IOException {
            LongBinner binner = binned.binner_;
            LongSupplier rowIndex = rseq.rowIndex();
            assert rowIndex != null;
            MatchKit matchKit = kitFact_.get();
            Predicate<Object[]> inclusion = rowSelector_.get();
            while( rseq.next() ) {
                Object[] row = rseq.getRow();
                if ( inclusion.test( row ) ) {
                    Object[] keys = matchKit.getBins( row );
                    int nkey = keys.length;
                    if ( nkey > 0 ) {
                        long lrow = rowIndex.getAsLong();
                        for ( int ikey = 0; ikey < nkey; ikey++ ) {
                            binner.addItem( keys[ ikey ], lrow );
                        }
                        binned.nref_ += nkey;
                    }
                }
                else {
                    binned.nexclude_++;
                }
                binned.nrow_++;
            }
        }
    }

    /**
     * RowCollector implementation for binning row refs by match key.
     */
    private static class RefCollector extends RowCollector<SplitBinnedRefs> {

        private final Supplier<MatchKit> kitFact_;
        private final Supplier<Predicate<Object[]>> rowSelector_; 
        private final int tIndex_;
        private final Predicate<Object> canAddKey_;
        private final Supplier<ObjectBinner<Object,RowRef>> binnerFactory_;

        /**
         * Constructor.
         *
         * @param   kitFact  defines matching criteria
         * @param   rowSelector   filter for rows to be included;
         *                        row values that fail this test are ignored
         * @param   tIndex  index of table for use in row references
         * @param   canAddKey  test on key values to determine
         *                     whether a new key may be entered in the binner;
         *                     if not, the only changes permitted are new
         *                     elements added for an existing key
         * @param   binnerFactory  factory for new ObjectBinner instances
         *                         to use in accumulators
         */
        RefCollector( Supplier<MatchKit> kitFact,
                      Supplier<Predicate<Object[]>> rowSelector,
                      int tIndex, Predicate<Object> canAddKey,
                      Supplier<ObjectBinner<Object,RowRef>> binnerFactory ) {
            kitFact_ = kitFact;
            rowSelector_ = rowSelector;
            tIndex_ = tIndex;
            canAddKey_ = canAddKey;
            binnerFactory_ = binnerFactory;
        }
        public SplitBinnedRefs createAccumulator() {
            return new SplitBinnedRefs( binnerFactory_.get() );
        }
        public SplitBinnedRefs combine( SplitBinnedRefs binned1,
                                        SplitBinnedRefs binned2 ) {
            if ( binned1.binner_.getBinCount() >
                 binned2.binner_.getBinCount() ) {
                binned1.addBinnedRefs( binned2 );
                return binned1;
            }
            else {
                binned2.addBinnedRefs( binned1 );
                return binned2;
            }
        }
        public void accumulateRows( RowSplittable rseq,
                                    SplitBinnedRefs binned )
                throws IOException {
            MatchKit matchKit = kitFact_.get();
            Predicate<Object[]> inclusion = rowSelector_.get();
            ObjectBinner<Object,RowRef> binner = binned.binner_;
            int nin = 0;
            LongSupplier rowIndex = rseq.rowIndex();
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                if ( inclusion.test( row ) ) {
                    nin++;
                    Object[] keys = matchKit.getBins( row );
                    if ( keys.length > 0 ) {
                        long lrow = rowIndex.getAsLong();
                        RowRef rref = new RowRef( tIndex_, lrow );
                        for ( Object key : keys ) {
                            if ( canAddKey_.test( key ) ) {
                                binner.addItem( key, rref );
                            }
                        }
                    }
                }
            }
            binned.ninclude_ += nin;
        }
    }

    /**
     * RowCollector implementation for scanning a table for pair matches.
     */
    private static class PairCollector extends RowCollector<LinkSet> {

        private final Supplier<MatchKit> kitFact_;
        private final int indexR_;
        private final int indexS_;
        private final Supplier<Predicate<Object[]>> rowSelector_;
        private final boolean bestOnly_;
        private final StarTable tableR_;
        private final LongBinner binnerR_;
        private final Supplier<LinkSet> linksetCreator_;

        /**
         * Constructor.
         *
         * @param  kitFact  defines matching criteria
         * @param  indexR  index of table R for use in row references
         * @param  indexS  index of table S for use in row references
         * @param  rowSelector  rows that fail this test are ignored
         * @param  bestOnly  true iff only the best pair match is required
         * @param  tableR  table R which will be accessed randomly
         * @param  binnerR   map from bin value to list of row indices in R
         *                   to which that bin relates
         * @param  linksetCreator  LinkSet factory
         */
        PairCollector( Supplier<MatchKit> kitFact, int indexR, int indexS,
                       Supplier<Predicate<Object[]>> rowSelector,
                       boolean bestOnly, StarTable tableR, LongBinner binnerR,
                       Supplier<LinkSet> linksetCreator ) {
            kitFact_ = kitFact;
            indexR_ = indexR;
            indexS_ = indexS;
            rowSelector_ = rowSelector;
            bestOnly_ = bestOnly;
            tableR_ = tableR;
            binnerR_ = binnerR;
            linksetCreator_ = linksetCreator;
        }
        public LinkSet createAccumulator() {
            return linksetCreator_.get();
        }
        public LinkSet combine( LinkSet links1, LinkSet links2 ) {
            final LinkSet result;
            final LinkSet addendum;
            if ( links1.size() > links2.size() ) {
                result = links1;
                addendum = links2;
            }
            else {
                result = links2;
                addendum = links1;
            }
            for ( RowLink link : addendum ) {
                result.addLink( link );
            }
            return result;
        }
        public void accumulateRows( RowSplittable rseqS, LinkSet linkSet )
                throws IOException {
            MatchKit matchKit = kitFact_.get();
            Predicate<Object[]> inclusion = rowSelector_.get();
            LongSupplier rowIndexS = rseqS.rowIndex();
            assert rowIndexS != null;
            try ( RowAccess accessR = tableR_.getRowAccess() ) {
                List<RowLink2> linkList = new ArrayList<>();
                Set<Long> rrowSet = new HashSet<>();
                while ( rseqS.next() ) {
                    Object[] rowS = rseqS.getRow();
                    if ( inclusion.test( rowS ) ) {

                        /* Identify rows from table R which may match table S.*/
                        Object[] keys = matchKit.getBins( rowS );
                        rrowSet.clear();
                        for ( Object key : keys ) {
                            long[] rrows = binnerR_.getLongs( key );
                            if ( rrows != null ) {
                                for ( long lrow : rrows ) {
                                    rrowSet.add( Long.valueOf( lrow ) );
                                }
                            }
                        }
                        int nr = rrowSet.size();
                        if ( nr > 0 ) {
                            // there are probably faster ways of doing
                            // this accumulate/sort
                            long[] rrows = new long[ nr ];
                            int ir = 0;
                            for ( Long rr : rrowSet ) {
                                rrows[ ir++ ] = rr.longValue();
                            }
                            Arrays.sort( rrows );

                            /* Score and accumulate matched links. */
                            long irS = rowIndexS.getAsLong();
                            linkList.clear();
                            double bestScore = Double.MAX_VALUE;
                            for ( long irR : rrows ) {
                                accessR.setRowIndex( irR );
                                Object[] rowR = accessR.getRow();
                                double score =
                                    matchKit.matchScore( rowS, rowR );
                                if ( score >= 0 &&
                                     ( ! bestOnly_ || score < bestScore ) ) {
                                    RowRef refR = new RowRef( indexR_, irR );
                                    RowRef refS = new RowRef( indexS_, irS );
                                    RowLink2 pairLink =
                                        new RowLink2( refR, refS );
                                    pairLink.setScore( score );
                                    if ( bestOnly_ ) {
                                        bestScore = score;
                                        linkList.clear();
                                    }
                                    linkList.add( pairLink );
                                    assert !bestOnly_ || linkList.size() == 1;
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
            }
        }
    }

    /**
     * RowCollector implementation for accumulating coordinate coverage.
     */
    private static class CoverageCollector extends RowCollector<Coverage> {
        private final Supplier<Coverage> covFact_;

        /**
         * Constructor.
         *
         * @param   covFact  factory for suitable Coverage objects
         */
        CoverageCollector( Supplier<Coverage> covFact ) {
            covFact_ = covFact;
        }
        public Coverage createAccumulator() {
            return covFact_.get();
        }
        public Coverage combine( Coverage cov1, Coverage cov2 ) {
            cov1.union( cov2 );
            return cov1;
        }
        public void accumulateRows( RowSplittable rseq, Coverage cov )
                throws IOException {
            while ( rseq.next() ) {
                cov.extend( rseq.getRow() );
            }
        }
    }

    /**
     * RowCollector implementation for counting filtered rows.
     * The accumulator is a 1-element long[] array whose sole element
     * is the count.
     */
    private static class CountCollector extends RowCollector<long[]> {
        private final Supplier<Predicate<Object[]>> rowSelector_;

        /**
         * Constructor.
         *
         * @param  rowSelector  filter for rows to be counted
         */
        CountCollector( Supplier<Predicate<Object[]>> rowSelector ) {
            rowSelector_ = rowSelector;
        }
        public long[] createAccumulator() {
            return new long[] { 0L };
        }
        public long[] combine( long[] acc1, long[] acc2 ) {
            return new long[] { acc1[ 0 ] + acc2[ 0 ] };
        }
        public void accumulateRows( RowSplittable rseq, long[] acc )
                throws IOException {
            Predicate<Object[]> inclusion = rowSelector_.get();
            long count = acc[ 0 ];
            while ( rseq.next() ) {
                if ( inclusion.test( rseq.getRow() ) ) {
                    count++;
                }
            }
            acc[ 0 ] = count;
        }
    }
}
