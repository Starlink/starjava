package uk.ac.starlink.table;

import java.io.IOException;
import java.util.function.Supplier;
import uk.ac.starlink.util.SplitPolicy;
import uk.ac.starlink.util.SplitProcessor;

/**
 * Manages potentially parallel processing of StarTable row data.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2020
 */
public class RowRunner {

    private final SplitProcessor<RowSplittable> processor_;

    /** Default split policy. */
    public static final SplitPolicy DFLT_POLICY = SplitPolicy.DFLT_POLICY;

    /** Determines whether the default parallel instance uses pooling. */
    private static final boolean STD_IS_POOL = false;

    /** General purpose instance; will be parallel for suitable environments. */
    public static final RowRunner DEFAULT =
        new RowRunner( SplitProcessor
                      .createStandardProcessor( DFLT_POLICY, STD_IS_POOL ) );

    /** Sequential-only instance; no parallel processing. */
    public static final RowRunner SEQUENTIAL =
            new RowRunner( SplitProcessor.createSequentialProcessor() ) {
        @Override
        public RowSplittable createRowSplittable( StarTable table )
                throws IOException {
            return new SequentialRowSplittable( table );
        }
    };

    /** Testing instance; force parallel processing even for small tables. */
    public static final RowRunner PARTEST = createParallelTestRunner( 10 );

    /**
     * Constructor.
     *
     * @param  processor   processor that manages the executions
     */
    public RowRunner( SplitProcessor<?> processor ) {
        @SuppressWarnings("unchecked")
        SplitProcessor<RowSplittable> rowProcessor =
            (SplitProcessor<RowSplittable>) processor;
        processor_ = rowProcessor;
    }

    /**
     * Performs a collection operation on table rows.
     *
     * @param   collector   collector defining operation
     * @param   table      table supplying data
     * @return   result of collection
     */
    public <A> A collect( RowCollector<A> collector, StarTable table )
            throws IOException {
        RowSplittable splittable = createRowSplittable( table );
        return invokeWithSmuggledIOException(
            () -> processor_.collect( collector, splittable )
        );
    }

    /**
     * Acquires a splittable row sequence from a table.
     * The default implementation just calls {@link StarTable#getRowSplittable},
     * but this may be overridden if required.
     *
     * @param  table  input table
     * @return   potentially splittable row sequence
     */
    public RowSplittable createRowSplittable( StarTable table )
            throws IOException {
        return table.getRowSplittable();
    }

    /**
     * Returns the processor on which this runner is based.
     *
     * @return  processor
     */
    public SplitProcessor<RowSplittable> getSplitProcessor() {
        return processor_;
    }

    /**
     * Returns the value of a supplier with custom exception handling.
     * If the invocation throws an exception, it will be rethrown by
     * this method.  However, if the exception is a RuntimeException whose
     * cause chain includes an IOException, the exception thrown
     * by this method will be an IOException.
     *
     * <p>These gymnastics are done in order to work around the behaviour
     * of {@link java.util.concurrent.ForkJoinTask},
     * most of whose methods throw no checked exceptions,
     * but bury caught execution throwables in RuntimeExceptions.
     * 
     * @param  supplier  value supplier
     * @return  result of invoking supplier
     */
    private static <T> T invokeWithSmuggledIOException( Supplier<T> supplier )
            throws IOException {
        try {
            return supplier.get();
        }
        catch ( RuntimeException err ) {
            IOException ioErr = getNestedIOException( err );
            if ( ioErr == null ) {
                throw err;
            }
            else {
                throw new IOException( ioErr.getMessage(), err );
            }
        }
    }

    /**
     * Returns the first IOException in the cause chain of a throwable.
     *
     * @param  err  exception
     * @return   nested IOException, or null if there isn't one
     */
    private static IOException getNestedIOException( Throwable err ) {
        for ( Throwable e = err; e != null; e = e.getCause() ) {
            if ( e instanceof IOException ) {
                return (IOException) e;
            }
        }
        return null;
    }

    /**
     * Creates a runner that forces parallel execution for all row counts
     * above a given value.  Generally useful for testing only.
     *
     * @param  minTaskSize  parallel execution threshold
     * @return  new runner
     */
    private static RowRunner createParallelTestRunner( int minTaskSize ) {
        short maxTasksPerCore = -1;
        SplitPolicy policy =
            new SplitPolicy( null, minTaskSize, maxTasksPerCore );
        SplitProcessor<RowSplittable> processor =
            STD_IS_POOL ? SplitProcessor.createPoolParallelProcessor( policy )
                        : SplitProcessor.createBasicParallelProcessor( policy );
        return new RowRunner( processor );
    }
}
