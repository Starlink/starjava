package uk.ac.starlink.ttools.plot2.layer;

import java.util.Iterator;

/**
 * Represents a bounded list of bins.
 * Each bin is given a fixed integer label, from zero to a specified maximum.
 * In practice, not all the bins may be used, and implementations may
 * take advantage of this.
 *
 * <p>Zero or more data values may be submitted to each bin,
 * and a floating point result value may later be obtained from each bin,
 * which forms a digest of the values submitted to that bin.
 * The nature of this digest is determined by a Combiner object.
 *
 * <p>Instances of this class are in general not thread-safe.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2015
 */
public interface BinList {

    /**
     * Returns the maximum number of bins.  All bins have an index in the
     * range 0..size-1.
     *
     * @return  bin count
     */
    long getSize();

    /**
     * Returns the combination method used for bins.
     *
     * @return  combiner
     */
    Combiner getCombiner();

    /**
     * Adds a given numeric value to the bin at the given index.
     * In general, NaN values should not be submitted.
     *
     * @param   index  bin index
     * @param   datum   finite value to submit to the bin
     */
    void submitToBin( long index, double datum );

    /**
     * Returns an object containing the result values accumulated into
     * the bins so far.
     *
     * <p>It is up to implementations to decide how to implement this method.
     * In some cases the return value may be an adapter that extracts results
     * as required from the data structure used for value accumulation,
     * but in others it may return a new data structure which copies
     * the accumulated values to a more compact form up front.
     * Therefore this may or may not be an expensive method, and the return
     * value may or may not be affected by subsequent 
     * {@link #submitToBin} calls.
     *
     * @return  accumulated bin values
     */
    Result getResult();

    /**
     * Accessor for the results of accumulating values in a bit list.
     */
    interface Result {

        /**
         * Returns the value that has been accumulated into the given bin index.
         * The value is the result of using this object's Combiner to combine
         * the submitted values.
         * In general, if no values have been submitted to the bin in question,
         * a NaN should be returned.
         *
         * @param  index  bin index
         * @return   bin value
         */
        double getBinValue( long index );

        /**
         * Returns the number of bins actually populated.
         * This is the number of values that will be returned by
         * {@link #indexIterator}.
         *
         * @return  number of non-empty bins
         */
        long getBinCount();

        /**
         * Returns an iterator over the indices of the
         * populated bins int this result.
         *
         * @return  iterator over distinct indices of all the non-empty bins;
         *          none of the values will be larger than the BinList's size
         */
        Iterator<Long> indexIterator();

        /**
         * Returns a Result instance with behaviour the same as this one,
         * but with implementation optimised for long-term storage.
         * If you're only going to iterate over this object's values once,
         * don't call this method, since it will probably iterate over
         * the values anyway if it does anything at all.
         * But if you're going to keep the result around for potentially
         * multiple uses, retaining the output of this method, rather than
         * the original object, may be more efficient on memory.
         *
         * @return  compacted result with the same content as this;
         *          may return this object
         */
        Result compact();
    }
}
