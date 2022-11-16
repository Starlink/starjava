package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.ValueInfo;

/**
 * Defines an aggregation operation.
 * An instance of this class can take multiple values associated with a
 * given metadata item, and collapse them down to a single value.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2022
 */
public interface Aggregator {

    /**
     * Returns the name of this aggregator.
     *
     * @return   user-readable short name
     */
    String getName();

    /**
     * Returns a short textual description for this aggregator.
     *
     * @return   plain text description
     */
    String getDescription();

    /**
     * Creates an object that can manage aggregation for data
     * described by given metadata.
     * If the metadata describes data that is not suitable for use
     * by this object, null is returned.
     * 
     * @param  info  input data description
     * @return   new aggregation, or null
     */
    Aggregation createAggregation( ValueInfo info );

    /**
     * Object that can manage aggregation for a given type of input data.
     */
    interface Aggregation {

        /**
         * Returns metadata describing the result of the aggregation.
         *
         * @return  output metadata
         */
        ValueInfo getResultInfo();

        /**
         * Returns an object which can perform aggregation on a number
         * of typed values.
         *
         * @return  accumulator, not threadsafe
         */
        Accumulator createAccumulator();
    }

    /**
     * Can accumulate multiple data items of a consistent type and
     * yield an aggregate value corresponding to the set.
     */
    interface Accumulator {

        /**
         * Submits a value for accumulation.
         *
         * @param  datum  value to accumulate
         */
        void submit( Object datum );

        /**
         * Returns the aggregated value for the values accumulated so far.
         *
         * @return  accumulated result
         */
        Object getResult();

        /**
         * Combines the content of another compatible accumulator with
         * the content of this one.
         * The effect is as if all the data that have been accumulated
         * into <code>other</code> were accumulated additionally into this one.
         *
         * @param  other  other compatible accumulator
         */
        void add( Accumulator other );
    }
}
