package uk.ac.starlink.ttools.plot2.data;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.plot2.CoordSequence;
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.SplitRunner;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.util.SplitCollector;

/**
 * SplitRunner subclass for use with TupleSequences.
 *
 * @author  Mark Taylor
 * @since   17 Sep 2019
 */
public class TupleRunner extends SplitRunner<TupleSequence> {

    private final SplitRunner<TupleSequence> runner_;
    private final SplitRunner<CoordSequence> coordRunner_;

    /** Standard instance for parallel processing. */
    public static final TupleRunner DEFAULT =
        new TupleRunner( createDefaultRunner() );

    /** Always runs sequentially, no parallel processing. */
    public static final TupleRunner SEQUENTIAL =
        new TupleRunner( createSequentialRunner() );

    /** Tries different implementations and logs timings to stdout. */
    public static final TupleRunner BENCH =
        new TupleRunner( createBenchRunner() );

    /**
     * Constructor.
     *
     * @param  runner  provides basic behaviour
     */
    public TupleRunner( SplitRunner<?> runner ) {
        @SuppressWarnings("unchecked")
        SplitRunner<TupleSequence> tr = (SplitRunner<TupleSequence>) runner;
        @SuppressWarnings("unchecked")
        SplitRunner<CoordSequence> cr = (SplitRunner<CoordSequence>) runner;
        runner_ = tr;
        coordRunner_ = cr;
    }

    public boolean willAttemptSplit( TupleSequence tseq ) {
        return runner_.willAttemptSplit( tseq );
    }

    public <A> A collect( SplitCollector<TupleSequence,A> collector,
                          Supplier<TupleSequence> tupleSupplier ) {
        return runner_.collect( collector, tupleSupplier );
    }

    public <A> A collectPool( SplitCollector<TupleSequence,A> collector, 
                              Supplier<TupleSequence> tupleSupplier ) {
        return runner_.collectPool( collector, tupleSupplier );
    }

    /**
     * Paints tuple-based data onto a Paper instance, possibly in parallel.
     * The supplied <code>tuplePainter</code> argument corresponds to
     * method {@link uk.ac.starlink.util.SplitCollector#accumulate}.
     *
     * @param  tuplePainter  defines how a TupleSequence is painted onto paper
     * @param  paper     paper onto which painting will be done
     * @param  dataSpec   data spec
     * @param  dataStore  data storage
     */
    public void paintData( final BiConsumer<TupleSequence,Paper> tuplePainter,
                           final Paper paper, 
                           final DataSpec dataSpec,
                           final DataStore dataStore ) {
        TupleSequenceFactory tseqFact =
            new TupleSequenceFactory( dataSpec, dataStore );

        /* If concurrency is possible, use a collector and merge the
         * results to the supplied paper object. */
        if ( paper.canMerge() && tseqFact.willAttemptSplit() ) {
            SplitCollector<TupleSequence,Paper> collector =
                    new SplitCollector<TupleSequence,Paper>() {
                public Paper createAccumulator() {
                    return paper.createSheet();
                }
                public void accumulate( TupleSequence tseq, Paper p0 ) {
                    tuplePainter.accept( tseq, p0 );
                }
                public Paper combine( Paper p1, Paper p2 ) {
                    p1.mergeSheet( p2 );
                    return p1;
                }
            };

            /* Collect using an accumulator pool - in at least some cases
             * pooling does improve performance here. */
            paper.mergeSheet( collectPool( collector, tseqFact ) );
        }

        /* If no concurrency, accumulate directly to the supplied paper
         * instance, which will save a merge. */
        else {
            tuplePainter.accept( tseqFact.get(), paper );
        }
    }

    /**
     * Gathers range information from tuple-based data, possibly in parallel.
     * The supplied <code>rangeFiller</code> argument corresponds to
     * method {@link uk.ac.starlink.util.SplitCollector#accumulate}.
     *
     * @param  rangeFiller  defines how a TupleSequence is used to populate
     *                      a ranger
     * @param  ranger    ranger to which the results will be written
     * @param  dataSpec   data spec
     * @param  dataStore  data storage
     */
    public void rangeData( final BiConsumer<TupleSequence,Ranger> rangeFiller,
                           final Ranger ranger,
                           final DataSpec dataSpec,
                           final DataStore dataStore ) {
        TupleSequenceFactory tseqFact =
            new TupleSequenceFactory( dataSpec, dataStore );
        if ( tseqFact.willAttemptSplit() ) {
            SplitCollector<TupleSequence,Ranger> collector =
                    new SplitCollector<TupleSequence,Ranger>() {
                public Ranger createAccumulator() {
                    return ranger.createCompatibleRanger();
                }
                public void accumulate( TupleSequence tseq, Ranger ranger ) {
                    rangeFiller.accept( tseq, ranger );
                }
                public Ranger combine( Ranger r1, Ranger r2 ) {
                    r1.add( r2 );
                    return r1;
                }
            };
            ranger.add( collect( collector, tseqFact ) );
        }
        else {
            rangeFiller.accept( tseqFact.get(), ranger );
        }
    }

    /**
     * Returns a SplitRunner use with CoordSequences associted with this
     * object.
     *
     * @return  coord runner
     */
    public SplitRunner<CoordSequence> coordRunner() {
        return coordRunner_;
    }

    /**
     * Used to generate TupleSequences.  This object wraps a base supplier
     * and provides an additional method <code>willAttemptSplit</code>,
     * which it determines by looking at the first tuple sequence it sees.
     * It reuses this later, so that (as long as at least one tseq is used)
     * the tuple sequence is not acquired and then wasted.
     * I don't really know if this is necessary - TupleSequences should not
     * be expensive to create - but it seems tidy and might help sometimes.
     */
    private class TupleSequenceFactory implements Supplier<TupleSequence> {

        private final Supplier<TupleSequence> baseFact_;
        private final AtomicReference<TupleSequence> tseqRef_;
        private final boolean willAttemptSplit_;

        /**
         * Convenience constructor using a DataSpec and DataStore.
         *
         * @param  dataSpec  dataspec
         * @param  dataStore  datastore
         */
        TupleSequenceFactory( DataSpec dataSpec, DataStore dataStore ) {
            this( () -> dataStore.getTupleSequence( dataSpec ) );
        }

        /**
         * Constructor.
         *
         * @param  baseFact  base supplier of TupleSequences
         */
        TupleSequenceFactory( Supplier<TupleSequence> baseFact ) {
            baseFact_ = baseFact;
            TupleSequence tseq = baseFact.get();
            willAttemptSplit_ = TupleRunner.this.willAttemptSplit( tseq );
            tseqRef_ = new AtomicReference<TupleSequence>( tseq );
        }

        public TupleSequence get() {
            TupleSequence tseq = tseqRef_.getAndSet( null );
            return tseq == null ? baseFact_.get() : tseq;
        }

        /**
         * Indicates whether this runner will attempt to split the
         * TupleSequences that this factory produces.
         *
         * @return   true iff supplied TupleSequences are expected to be
         *           splittable in practice
         */
        public boolean willAttemptSplit() {
            return willAttemptSplit_;
        }
    }
}
