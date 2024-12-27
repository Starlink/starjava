package uk.ac.starlink.util;

/**
 * Defines an object that can collect values from a Splittable into
 * an accumulator.
 * The order of the split items is not considered significant,
 * so splittables may be presented to accumulator instances in any order.
 *
 * @param   <A>  accumulator type
 * @param   <S>  splittable content type
 *
 * @author   Mark Taylor
 * @since    12 Sep 2019
 */
public interface SplitCollector<S extends Splittable<S>,A> {

    /**
     * Returns a new accumulator into which results can be collected.
     * Accumulator instances may only be used from one thread at
     * any one time.
     *
     * @return  new accumulator
     */
    A createAccumulator();

    /**
     * Consumes the content of a splittable, collecting results
     * into the supplied accumulator.
     * This method may not be called concurrently on the same splittable.
     *
     * @param  splittable  splittable object
     * @param  accumulator  accumulator
     */
    void accumulate( S splittable, A accumulator );

    /**
     * Combines the content of two accumulators.
     * The returned value may or may not be the same object as
     * one of the input values.
     * The input values should not be used following this call.
     * The sequence of the input values is not significant.
     *
     * @param  acc1  one input accumulator
     * @param  acc2  other input accumulator
     * @return   accumulator containing the combined result of the inputs
     */
    A combine( A acc1, A acc2 );
}
