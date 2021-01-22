package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.util.SplitCollector;
import uk.ac.starlink.util.Splittable;

/**
 * Partial SplitCollector implementation for accumulating range data.
 * The {@link #accumulate accumulate} method should be implemented
 * to submit values to an N-element array of ranges.
 *
 * <p>On completion, the {@link #mergeRanges} method may be used
 * to update a supplied Range array with the collected result.
 *
 * @author   Mark Taylor
 * @since    22 Jan 2021
 */
public abstract class RangeCollector<S extends Splittable<S>>
        implements SplitCollector<S,Range[]> {

    private final int ndim_;

    /**
     * Constructor.
     *
     * @param  ndim  number of range objects (data dimensions)
     */
    protected RangeCollector( int ndim ) {
        ndim_ = ndim;
    }

    public Range[] createAccumulator() {
        Range[] ranges = new Range[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            ranges[ i ] = new Range();
        }
        return ranges;
    }

    public Range[] combine( Range[] ranges1, Range[] ranges2 ) {
        mergeRanges( ranges1, ranges2 );
        return ranges1;
    }

    /**
     * Merges the content of the second range into the first one.
     *
     * @param  ranges0  first input range, modified on exit
     * @param  ranges1  second input range, unmodified on exit
     */
    public void mergeRanges( Range[] ranges0, Range[] ranges1 ) {
        for ( int i = 0; i < ndim_; i++ ) {
            ranges0[ i ].extend( ranges1[ i ] );
        }
    }
}
